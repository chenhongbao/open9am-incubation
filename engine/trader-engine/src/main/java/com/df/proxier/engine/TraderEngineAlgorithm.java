/*
 * Copyright (C) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.df.proxier.engine;

import com.df.proxier.Account;
import com.df.proxier.CancelResponse;
import com.df.proxier.Commission;
import com.df.proxier.Contract;
import com.df.proxier.ContractStatus;
import com.df.proxier.Deposit;
import com.df.proxier.FeeStatus;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Order;
import com.df.proxier.OrderRequest;
import com.df.proxier.OrderResponse;
import com.df.proxier.OrderStatus;
import com.df.proxier.OrderType;
import com.df.proxier.Position;
import com.df.proxier.RatioType;
import com.df.proxier.Tick;
import com.df.proxier.Withdraw;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Algorithm implemetation.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class TraderEngineAlgorithm implements ITraderEngineAlgorithm {

    public TraderEngineAlgorithm() {
    }

    @Override
    public Account getAccount(Account pre,
                              Collection<Deposit> deposits,
                              Collection<Withdraw> withdraws,
                              Collection<Position> positions) throws TraderEngineAlgorithmException {
        double closeProfit = 0D;
        double positionProfit = 0D;
        double frozenMargin = 0D;
        double frozenCommission = 0D;
        double margin = 0D;
        double commission = 0D;
        if (positions == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.POSITION_NULL.code(),
                                                     ExceptionCodes.POSITION_NULL.message());
        }
        for (var p : positions) {
            check4(p.getCloseProfit(),
                   p.getPositionProfit(),
                   p.getFrozenMargin(),
                   p.getFrozenCommission(),
                   p.getMargin(),
                   p.getCommission());
            closeProfit += p.getCloseProfit();
            positionProfit += p.getPositionProfit();
            frozenMargin += p.getFrozenMargin();
            frozenCommission += p.getFrozenCommission();
            margin += p.getMargin();
            commission += p.getCommission();
        }
        double deposit = getProperDeposit(deposits);
        double withdraw = getProperWithdraw(withdraws);
        var r = initAccount(pre);
        r.setCloseProfit(closeProfit);
        r.setCommission(commission);
        r.setDeposit(deposit);
        r.setFrozenCommission(frozenCommission);
        r.setFrozenMargin(frozenMargin);
        r.setMargin(margin);
        r.setPositionProfit(positionProfit);
        r.setWithdraw(withdraw);
        var balance = r.getPreBalance() + r.getDeposit() - r.getWithdraw()
                  + r.getCloseProfit() + r.getPositionProfit() - r.getCommission();
        r.setBalance(balance);
        return r;
    }

    @Override
    public double getAmount(double price, Instrument instrument) throws TraderEngineAlgorithmException {
        check0(instrument);
        var multiple = instrument.getMultiple();
        if (multiple == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.MULTIPLE_NULL.code(),
                                                     ExceptionCodes.MULTIPLE_NULL.message()
                                                     + "(" + instrument.getInstrumentId() + ")");
        }
        return price * multiple;
    }

    @Override
    public double getCommission(double price,
                                Instrument instrument,
                                OrderType type) throws TraderEngineAlgorithmException {
        check0(instrument);
        var ctype = instrument.getCommissionType();
        check1(ctype);
        var ratio = getProperCommissionRatio(instrument, type);
        if (ratio == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.RATIO_NULL.code(),
                                                     ExceptionCodes.RATIO_NULL.message()
                                                     + "(" + instrument.getInstrumentId() + ")");
        }
        if (ctype == RatioType.BY_MONEY) {
            return getAmount(price, instrument) * ratio;
        }
        else {
            return ratio;
        }
    }

    @Override
    public double getMargin(double price,
                            Instrument instrument) throws TraderEngineAlgorithmException {
        check0(instrument);
        var type = instrument.getMarginType();
        check1(type);
        var ratio = instrument.getMarginRatio();
        if (ratio == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.RATIO_NULL.code(),
                                                     ExceptionCodes.RATIO_NULL.message()
                                                     + "(" + instrument.getInstrumentId() + ")");
        }
        if (type == RatioType.BY_MONEY) {
            return getAmount(price, instrument) * ratio;
        }
        else {
            return ratio;
        }
    }

    @Override
    public Order getOrder(OrderRequest request,
                          Collection<Contract> contracts,
                          Collection<OrderResponse> trades,
                          Collection<CancelResponse> cancels) throws TraderEngineAlgorithmException {
        var r = new Order();
        /*
         * Don't change the order of calls.
         */
        setRequestOrder(r, request);
        setContractOrder(r, contracts);
        setResponseOrder(r, trades);
        setCancelOrder(r, cancels);
        setOrderStatus(r);
        return r;
    }

    @Override
    public Collection<Position> getPositions(Collection<Contract> contracts,
                                             Collection<Commission> commissions,
                                             Collection<Margin> margins,
                                             Map<String, Tick> ticks,
                                             Map<String, Instrument> instruments,
                                             LocalDate tradingDay) throws TraderEngineAlgorithmException {
        if (contracts == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.CONTRACT_NULL.code(),
                                                     ExceptionCodes.CONTRACT_NULL.message());
        }
        final var ls = new HashMap<String, Position>(64);
        final var ss = new HashMap<String, Position>(64);
        /*
         * Store margins/commissions in map for constant access time.
         */
        final var map = new HashMap<Long, Margin>(64);
        final var cmap = new HashMap<Long, Set<Commission>>(64);
        margins.forEach(m -> {
            map.put(m.getContractId(), m);
        });
        commissions.forEach(c -> {
            var s = cmap.computeIfAbsent(c.getContractId(), k -> new HashSet<>(2));
            s.add(c);
        });

        for (var c : contracts) {
            Position p;
            var type = c.getOpenType();
            if (null == type) {
                throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_ORDER_TYPE.code(),
                                                         ExceptionCodes.INVALID_ORDER_TYPE.message());
            }
            switch (type) {
                case BUY_OPEN:
                    p = ls.computeIfAbsent(c.getInstrumentId(), k -> {
                                       return initPosition(c, tradingDay);
                                   });
                    break;
                case SELL_OPEN:
                    p = ss.computeIfAbsent(c.getInstrumentId(), k -> {
                                       return initPosition(c, tradingDay);
                                   });
                    break;
                default:
                    throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_ORDER_TYPE.code(),
                                                             ExceptionCodes.INVALID_ORDER_TYPE.message());
            }
            var iid = c.getInstrumentId();
            check2(iid);
            var cid = c.getContractId();
            check3(cid);
            var pk = iid + ".Price";
            var ik = iid + ".Instrument";
            var margin = findMargin(cid, map);
            var commission = findCommission(cid, cmap);
            var price = findPriceProperty(pk, ticks);
            var instrument = findInstrumentProperty(ik, instruments);
            if (c.getOpenTradingDay().isBefore(tradingDay)) {
                addPrePosition(p,
                               c,
                               commission,
                               margin,
                               price,
                               instrument);
            }
            else {
                addTodayPosition(p,
                                 c,
                                 commission,
                                 margin,
                                 price,
                                 instrument);
            }
        }
        var r = new HashSet<Position>(ls.values());
        r.addAll(ss.values());
        return r;
    }

    private void addClosedContract(Position p,
                                   Contract c,
                                   Collection<Commission> commissions) throws TraderEngineAlgorithmException {
        var closeProfit = getProperProfit(c.getOpenAmount(),
                                      c.getCloseAmount(),
                                      c.getOpenType());
        if (p.getCloseProfit() == null) {
            p.setCloseProfit(closeProfit);
        }
        else {
            p.setCloseProfit(p.getCloseProfit() + closeProfit);
        }
        var commission = getProperCommission(c.getContractId(),
                                         commissions,
                                         FeeStatus.DEALED);
        if (p.getCommission() == null) {
            p.setCommission(commission);
        }
        else {
            p.setCommission(p.getCommission() + commission);
        }
    }

    private void addClosingContract(Position p,
                                    Contract c,
                                    Collection<Commission> commissions,
                                    Margin margin,
                                    Double price,
                                    Instrument instrument) throws TraderEngineAlgorithmException {
        if (p.getAmount() == null) {
            p.setAmount(c.getOpenAmount());
        }
        else {
            p.setAmount(p.getAmount() + c.getOpenAmount());
        }
        var commission = getProperCommission(c.getContractId(),
                                         commissions,
                                         FeeStatus.DEALED);
        var frozenCommission = getProperCommission(c.getContractId(),
                                               commissions,
                                               FeeStatus.FORZEN);
        if (p.getCommission() == null) {
            p.setCommission(commission);
        }
        else {
            p.setCommission(p.getCommission() + commission);
        }
        if (p.getFrozenCommission() == null) {
            p.setFrozenCommission(frozenCommission);
        }
        else {
            p.setFrozenCommission(p.getFrozenCommission() + frozenCommission);
        }
        var frozenCloseVolumn = getProperVolumn(c.getStatus(), ContractStatus.CLOSING);
        var volumn = frozenCloseVolumn;
        if (p.getVolumn() == null) {
            p.setVolumn(volumn);
        }
        else {
            p.setVolumn(p.getVolumn() + volumn);
        }
        if (p.getFrozenCloseVolumn() == null) {
            p.setFrozenCloseVolumn(frozenCloseVolumn);
        }
        else {
            p.setFrozenCloseVolumn(p.getFrozenCloseVolumn() + frozenCloseVolumn);
        }
        var m = getProperMargin(c.getContractId(),
                            margin,
                            FeeStatus.DEALED);
        if (p.getMargin() == null) {
            p.setMargin(m);
        }
        else {
            p.setMargin(p.getMargin() + m);
        }
        var pprofit = getProperPositionProfit(c,
                                          price,
                                          instrument);
        if (p.getPositionProfit() == null) {
            p.setPositionProfit(pprofit);
        }
        else {
            p.setPositionProfit(p.getPositionProfit() + pprofit);
        }

    }

    private void addOpenContract(Position p,
                                 Contract c,
                                 Collection<Commission> commissions,
                                 Margin margin,
                                 Double price,
                                 Instrument instrument) throws TraderEngineAlgorithmException {
        if (p.getAmount() == null) {
            p.setAmount(c.getOpenAmount());
        }
        else {
            p.setAmount(p.getAmount() + c.getOpenAmount());
        }
        var commission = getProperCommission(c.getContractId(),
                                         commissions,
                                         FeeStatus.DEALED);
        if (p.getCommission() == null) {
            p.setCommission(commission);
        }
        else {
            p.setCommission(p.getCommission() + commission);
        }
        var volumn = getProperVolumn(c.getStatus(), ContractStatus.OPEN);
        if (p.getVolumn() == null) {
            p.setVolumn(volumn);
        }
        else {
            p.setVolumn(p.getVolumn() + volumn);
        }
        var m = getProperMargin(c.getContractId(),
                            margin,
                            FeeStatus.DEALED);
        if (p.getMargin() == null) {
            p.setMargin(m);
        }
        else {
            p.setMargin(p.getMargin() + m);
        }
        var pprofit = getProperPositionProfit(c,
                                          price,
                                          instrument);
        if (p.getPositionProfit() == null) {
            p.setPositionProfit(pprofit);
        }
        else {
            p.setPositionProfit(p.getPositionProfit() + pprofit);
        }
    }

    private void addOpeningContract(Position p,
                                    Contract c,
                                    Collection<Commission> commissions,
                                    Margin margin) throws TraderEngineAlgorithmException {
        var frozenCommission = getProperCommission(c.getContractId(),
                                               commissions,
                                               FeeStatus.FORZEN);
        if (p.getFrozenCommission() == null) {
            p.setFrozenCommission(frozenCommission);
        }
        else {
            p.setFrozenCommission(p.getFrozenCommission() + frozenCommission);
        }
        var frozenOpenVolumn = getProperVolumn(c.getStatus(), ContractStatus.OPENING);
        if (p.getFrozenOpenVolumn() == null) {
            p.setFrozenOpenVolumn(frozenOpenVolumn);
        }
        else {
            p.setFrozenOpenVolumn(p.getFrozenOpenVolumn() + frozenOpenVolumn);
        }
        var frozenMargin = getProperMargin(c.getContractId(),
                                       margin,
                                       FeeStatus.FORZEN);
        if (p.getFrozenMargin() == null) {
            p.setFrozenMargin(frozenMargin);
        }
        else {
            p.setFrozenMargin(p.getFrozenMargin() + frozenMargin);
        }
    }

    private void addPreContract(Position p, Contract c, Margin margin) {
        if (p.getPreAmount() == null) {
            p.setPreAmount(c.getOpenAmount());
        }
        else {
            p.setPreAmount(p.getPreAmount() + c.getOpenAmount());
        }
        if (p.getPreVolumn() == null) {
            p.setPreVolumn(1L);
        }
        else {
            p.setPreVolumn(p.getPreVolumn() + 1L);
        }
        if (p.getPreMargin() == null) {
            p.setPreMargin(margin.getMargin());
        }
        else {
            p.setPreMargin(p.getPreMargin() + margin.getMargin());
        }
    }

    private void addPrePosition(Position p,
                                Contract c,
                                Collection<Commission> commissions,
                                Margin margin,
                                Double price,
                                Instrument instrument) throws TraderEngineAlgorithmException {
        var status = c.getStatus();
        if (status == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.CONTRACT_STATUS_NULL.code(),
                                                     ExceptionCodes.CONTRACT_STATUS_NULL.message()
                                                     + "(Contract ID:" + c.getContractId() + ")");
        }
        switch (status) {
            case CLOSED:
                addClosedContract(p,
                                  c,
                                  commissions);
                break;
            case CLOSING:
                addClosingContract(p,
                                   c,
                                   commissions,
                                   margin,
                                   price,
                                   instrument);
                break;
            case OPEN:
                addOpenContract(p,
                                c,
                                commissions,
                                margin,
                                price,
                                instrument);
                break;
            default:
                throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_CONTRACT_STATUS.code(),
                                                         ExceptionCodes.INVALID_CONTRACT_STATUS.message()
                                                         + "(Contract ID:" + c.getContractId() + ")");
        }
        addPreContract(p,
                       c,
                       margin);
    }

    private void addTodayContract(Position p,
                                  Contract c,
                                  Margin margin) throws TraderEngineAlgorithmException {
        if (p.getTodayAmount() == null) {
            p.setTodayAmount(c.getOpenAmount());
        }
        else {
            p.setTodayAmount(p.getTodayAmount() + c.getOpenAmount());
        }
        var volumn = getProperVolumn(c.getStatus(), ContractStatus.OPEN);
        if (p.getTodayVolumn() == null) {
            p.setTodayVolumn(volumn);
        }
        else {
            p.setTodayVolumn(p.getTodayVolumn() + volumn);
        }
        var m = getProperMargin(c.getContractId(),
                            margin,
                            FeeStatus.DEALED);
        if (p.getTodayMargin() == null) {
            p.setTodayMargin(m);
        }
        else {
            p.setTodayMargin(p.getTodayMargin() + m);
        }
    }

    private void addTodayOpenContract(Position p, Contract c, Margin margin) {
        if (p.getOpenAmount() == null) {
            p.setOpenAmount(c.getOpenAmount());
        }
        else {
            p.setOpenAmount(p.getOpenAmount() + c.getOpenAmount());
        }
        if (p.getOpenVolumn() == null) {
            p.setOpenVolumn(1L);
        }
        else {
            p.setOpenVolumn(p.getOpenVolumn() + 1L);
        }
        if (p.getOpenMargin() == null) {
            p.setOpenMargin(margin.getMargin());
        }
        else {
            p.setOpenMargin(p.getOpenMargin() + margin.getMargin());
        }
    }

    private void addTodayPosition(Position p,
                                  Contract c,
                                  Collection<Commission> commissions,
                                  Margin margin,
                                  Double price,
                                  Instrument instrument) throws TraderEngineAlgorithmException {
        var status = c.getStatus();
        if (status == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.CONTRACT_STATUS_NULL.code(),
                                                     ExceptionCodes.CONTRACT_STATUS_NULL.message()
                                                     + "(Contract ID:" + c.getContractId() + ")");
        }
        switch (status) {
            case CLOSED:
                addClosedContract(p,
                                  c,
                                  commissions);
                addTodayOpenContract(p,
                                     c,
                                     margin);
                break;
            case OPENING:
                addOpeningContract(p,
                                   c,
                                   commissions,
                                   margin);
                break;
            case CLOSING:
                addClosingContract(p,
                                   c,
                                   commissions,
                                   margin,
                                   price,
                                   instrument);
                addTodayContract(p,
                                 c,
                                 margin);
                addTodayOpenContract(p,
                                     c,
                                     margin);
                break;
            default: // OPEN
                addOpenContract(p,
                                c,
                                commissions,
                                margin,
                                price,
                                instrument);
                addTodayContract(p,
                                 c,
                                 margin);
                addTodayOpenContract(p,
                                     c,
                                     margin);
                break;
        }
    }

    private void check0(Instrument instrument) throws TraderEngineAlgorithmException {
        if (instrument == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INSTRUMENT_NULL.code(),
                                                     ExceptionCodes.INSTRUMENT_NULL.message());
        }
    }

    private void check1(RatioType type) throws TraderEngineAlgorithmException {
        if (type == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.RATIO_TYPE_NULL.code(),
                                                     ExceptionCodes.RATIO_TYPE_NULL.message());
        }
    }

    private void check2(String instrumentId) throws TraderEngineAlgorithmException {
        if (instrumentId == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INSTRUMENT_ID_NULL.code(),
                                                     ExceptionCodes.INSTRUMENT_ID_NULL.message());
        }
        if (instrumentId.isBlank()) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_INSTRUMENT_ID.code(),
                                                     ExceptionCodes.INVALID_INSTRUMENT_ID.message());
        }
    }

    private void check3(Long contractId) throws TraderEngineAlgorithmException {
        if (contractId == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.CONTRACT_ID_NULL.code(),
                                                     ExceptionCodes.CONTRACT_ID_NULL.message());
        }
    }

    private void check4(Object... values) throws TraderEngineAlgorithmException {
        for (var v : values) {
            if (v == null) {
                throw new TraderEngineAlgorithmException(ExceptionCodes.POSITION_FIELD_NULL.code(),
                                                         ExceptionCodes.POSITION_FIELD_NULL.message());
            }
        }
    }

    private Collection<Commission> findCommission(
            Long contractId,
            Map<Long, Set<Commission>> commissions) throws TraderEngineAlgorithmException {
        var v = commissions.get(contractId);
        if (v == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.COMMISSION_NULL.code(),
                                                     ExceptionCodes.COMMISSION_NULL.message()
                                                     + "(Contract ID:" + contractId + ")");
        }
        return v;
    }

    private Instrument findInstrumentProperty(String key,
                                              Map<String, Instrument> instruments) throws TraderEngineAlgorithmException {
        var v = instruments.get(key);
        if (v == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INSTRUMENT_NULL.code(),
                                                     ExceptionCodes.INSTRUMENT_NULL.message()
                                                     + "(" + key + ")");
        }
        return v;
    }

    private Margin findMargin(Long contractId, Map<Long, Margin> margins)
            throws TraderEngineAlgorithmException {
        var v = margins.get(contractId);
        if (v == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.MARGIN_NULL.code(),
                                                     ExceptionCodes.MARGIN_NULL.message()
                                                     + "(Contract ID:" + contractId + ")");
        }
        return v;
    }

    private double findPriceProperty(String key,
                                     Map<String, Tick> ticks) throws TraderEngineAlgorithmException {
        var v = ticks.get(key);
        if (v == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.TICK_NULL.code(),
                                                     ExceptionCodes.TICK_NULL.message()
                                                     + "(" + key + ")");
        }
        var p = v.getPrice();
        if (p == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.PRICE_NULL.code(),
                                                     ExceptionCodes.PRICE_NULL.message()
                                                     + "(" + key + ")");
        }
        return p;
    }

    private void setOrderStatus(Order order) throws TraderEngineAlgorithmException {
        if (order.getStatus() != null) {
            return;
        }
        var traded = order.getTradedVolumn();
        if (traded > order.getVolumn()) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INCONSISTENT_CONTRACT_ORDER_INFO.code(),
                                                     ExceptionCodes.INCONSISTENT_CONTRACT_ORDER_INFO.message()
                                                     + "(Order ID:" + order.getOrderId() + ")");
        }
        if (traded > 0) {
            if (Objects.equals(traded, order.getVolumn())) {
                order.setStatus(OrderStatus.ALL_TRADED);
            }
            else {
                order.setStatus(OrderStatus.PART_TRADED_INQUE);
            }
        }
        else {
            order.setStatus(OrderStatus.ACCEPTED);
        }
    }

    private double getProperCommission(Long contractId,
                                       Collection<Commission> commissions,
                                       FeeStatus status) throws TraderEngineAlgorithmException {
        double v = 0D;
        for (var c : commissions) {
            if (Objects.equals(c.getContractId(), contractId) && c.getStatus() == status) {
                var x = c.getCommission();
                if (x == null) {
                    throw new TraderEngineAlgorithmException(ExceptionCodes.COMMISSION_AMOUNT_NULL.code(),
                                                             ExceptionCodes.COMMISSION_AMOUNT_NULL.message());
                }
                v += x;
            }
        }
        return v;
    }

    private Double getProperCommissionRatio(Instrument instrument,
                                            OrderType type) throws TraderEngineAlgorithmException {
        check0(instrument);
        switch (type) {
            case BUY_OPEN:
            case SELL_OPEN:
                return instrument.getCommissionOpenRatio();
            case BUY_CLOSE:
            case SELL_CLOSE:
                return instrument.getCommissionCloseRatio();
            default:
                return instrument.getCommissionCloseTodayRatio();
        }
    }

    private double getProperDeposit(Collection<Deposit> deposits) throws TraderEngineAlgorithmException {
        if (deposits == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.DEPOSIT_NULL.code(),
                                                     ExceptionCodes.DEPOSIT_NULL.message());
        }
        double deposit = 0D;
        for (var d : deposits) {
            var a = d.getAmount();
            if (a == null) {
                throw new TraderEngineAlgorithmException(ExceptionCodes.DEPOSIT_AMOUNT_NULL.code(),
                                                         ExceptionCodes.DEPOSIT_AMOUNT_NULL.message());
            }
            deposit += d.getAmount();
        }
        return deposit;
    }

    private double getProperMargin(Long contractId,
                                   Margin margin,
                                   FeeStatus status) throws TraderEngineAlgorithmException {
        if (Objects.equals(contractId, margin.getContractId())
            && margin.getStatus() == status) {
            return margin.getMargin();
        }
        else {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_FEE_STATUS.code(),
                                                     ExceptionCodes.INVALID_FEE_STATUS.message());
        }
    }

    private double getProperPositionProfit(Contract c,
                                           Double price,
                                           Instrument instrument) throws TraderEngineAlgorithmException {
        var a = getAmount(price, instrument);
        return getProperProfit(c.getOpenAmount(), a, c.getOpenType());
    }

    private double getProperProfit(double pre,
                                   double current,
                                   OrderType type) throws TraderEngineAlgorithmException {
        if (null == type) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_ORDER_TYPE.code(),
                                                     ExceptionCodes.INVALID_ORDER_TYPE.message());
        }
        else {
            switch (type) {
                case BUY_OPEN:
                    return current - pre;
                case SELL_OPEN:
                    return pre - current;
                default:
                    throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_ORDER_TYPE.code(),
                                                             ExceptionCodes.INVALID_ORDER_TYPE.message());
            }
        }
    }

    private long getProperVolumn(ContractStatus status,
                                 ContractStatus wantedStatus) throws TraderEngineAlgorithmException {
        if (status == wantedStatus) {
            return 1L;
        }
        else {
            throw new TraderEngineAlgorithmException(ExceptionCodes.INVALID_CONTRACT_STATUS.code(),
                                                     ExceptionCodes.INVALID_CONTRACT_STATUS.message());
        }
    }

    private double getProperWithdraw(Collection<Withdraw> withdraws) throws TraderEngineAlgorithmException {
        if (withdraws == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.WITHDRAW_NULL.code(),
                                                     ExceptionCodes.DEPOSIT_NULL.message());
        }
        double withdraw = 0D;
        for (var w : withdraws) {
            var a = w.getAmount();
            if (a == null) {
                throw new TraderEngineAlgorithmException(ExceptionCodes.WITHDRAW_AMOUNT_NULL.code(),
                                                         ExceptionCodes.WITHDRAW_AMOUNT_NULL.message());
            }
            withdraw += w.getAmount();
        }
        return withdraw;
    }

    private Account initAccount(Account a) {
        var r = new Account();
        r.setBalance(0D);
        r.setCloseProfit(0D);
        r.setCommission(0D);
        r.setDeposit(0D);
        r.setFrozenCommission(0D);
        r.setFrozenMargin(0D);
        r.setMargin(0D);
        r.setPositionProfit(0D);
        r.setPreBalance(a.getBalance());
        r.setPreDeposit(a.getDeposit());
        r.setPreMargin(a.getMargin());
        r.setPreWithdraw(a.getWithdraw());
        r.setWithdraw(0D);
        return r;
    }

    private Position initPosition(Contract c, LocalDate tradingDay) {
        var p0 = new Position();
        p0.setAmount(0.0D);
        p0.setCloseProfit(0.0D);
        p0.setFrozenCloseVolumn(0L);
        p0.setFrozenMargin(0.0D);
        p0.setFrozenOpenVolumn(0L);
        p0.setInstrumentId(c.getInstrumentId());
        p0.setMargin(0.0D);
        p0.setPositionProfit(0.0D);
        p0.setPreAmount(0.0D);
        p0.setPreMargin(0.0D);
        p0.setPreVolumn(0L);
        p0.setTodayAmount(0.0D);
        p0.setTodayMargin(0.0D);
        p0.setTodayVolumn(0L);
        p0.setTradingDay(tradingDay);
        p0.setType(c.getOpenType());
        p0.setVolumn(0L);
        return p0;
    }

    private void setCancelOrder(Order order,
                                Collection<CancelResponse> cancels) throws TraderEngineAlgorithmException {
        if (cancels == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.CANCEL_RSPS_NULL.code(),
                                                     ExceptionCodes.CANCEL_RSPS_NULL.message());
        }
        if (cancels.isEmpty()) {
            order.setIsCanceled(Boolean.FALSE);
        }
        else {
            var l = new LinkedList<CancelResponse>(cancels);
            l.sort((CancelResponse o1, CancelResponse o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()));
            var last = l.getLast();
            order.setCancelTimestamp(last.getTimestamp());
            order.setIsCanceled(Boolean.TRUE);
            order.setStatus(OrderStatus.CANCELED);
            order.setStatusCode(last.getStatusCode());
            order.setStatusMessage(last.getStatusMessage());
        }
    }

    private void setContractOrder(Order order,
                                  Collection<Contract> contracts) throws TraderEngineAlgorithmException {
        double amount = 0D;
        long tradedVolumn = 0L;
        for (var c : contracts) {
            if (c.getOpenType() != order.getType()
                || c.getInstrumentId().equals(order.getInstrumentId())) {
                throw new TraderEngineAlgorithmException(ExceptionCodes.INCONSISTENT_CONTRACT_ORDER_INFO.code(),
                                                         ExceptionCodes.INCONSISTENT_CONTRACT_ORDER_INFO.message());
            }
            amount += c.getOpenAmount();
            ++tradedVolumn;
        }
        order.setAmount(amount);
        order.setTradedVolumn(tradedVolumn);
    }

    private void setRequestOrder(Order order, OrderRequest request) {
        order.setInstrumentId(request.getInstrumentId());
        order.setType(request.getType());
        order.setOrderId(request.getOrderId());
        order.setVolumn(request.getVolumn());
        order.setTraderId(request.getTraderId());
    }

    private void setResponseOrder(Order order,
                                  Collection<OrderResponse> trades) throws TraderEngineAlgorithmException {
        if (trades == null) {
            throw new TraderEngineAlgorithmException(ExceptionCodes.ORDER_RSPS_NULL.code(),
                                                     ExceptionCodes.ORDER_RSPS_NULL.message());
        }
        if (trades.isEmpty()) {
            return;
        }
        var responses = new LinkedList<OrderResponse>(trades);
        responses.sort((OrderResponse o1, OrderResponse o2) -> o1.getTimestamp().compareTo(o2.getTimestamp()));
        order.setTradingDay(responses.getFirst().getTradingDay());
        order.setInsertTimestamp(responses.getFirst().getTimestamp());
        order.setUpdateTimestamp(responses.getLast().getTimestamp());
    }
}