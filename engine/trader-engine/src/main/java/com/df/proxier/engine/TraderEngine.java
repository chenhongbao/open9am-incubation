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
import com.df.proxier.ActionType;
import com.df.proxier.Commission;
import com.df.proxier.Contract;
import com.df.proxier.ContractStatus;
import com.df.proxier.Direction;
import com.df.proxier.FeeStatus;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Offset;
import com.df.proxier.OrderStatus;
import com.df.proxier.Request;
import com.df.proxier.Response;
import com.df.proxier.Tick;
import com.df.proxier.Trade;
import com.df.proxier.service.ITraderService;
import com.df.proxier.service.TraderException;
import com.df.proxier.service.TraderRuntimeException;
import com.df.proxier.utils.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;

public class TraderEngine implements ITraderEngine {

    private ITraderEngineAlgorithm algo;
    private IDataSource ds;
    private final Properties globalStartProps;
    private final HashMap<ITraderEngineHandler, Object> handlers;
    private final HashMap<String, Instrument> instruments;
    private final HashMap<Long, Integer> orderTraders;
    private EngineStatus status;
    private final HashMap<Integer, ExtendedTraderServiceRuntime> traders;

    public TraderEngine() {
        handlers = new HashMap<>(32);
        traders = new HashMap<>(32);
        orderTraders = new HashMap<>(1024);
        instruments = new HashMap<>(512);
        globalStartProps = new Properties();
    }

    @Override
    public void addHandler(ITraderEngineHandler handler) throws TraderException {
        if (handler == null) {
            throw new TraderException(ExceptionCodes.TRADER_ENGINE_HANDLER_NULL.code(),
                                      ExceptionCodes.TRADER_ENGINE_HANDLER_NULL.message());
        }
        handlers.put(handler, new Object());
    }

    @Override
    public void enableTrader(int traderId, boolean enabled) throws TraderException {
        var i = getTraderServiceInfo(traderId);
        i.setEnabled(enabled);
    }

    @Override
    public ITraderEngineAlgorithm getAlgorithm() {
        return algo;
    }

    @Override
    public void setAlgorithm(ITraderEngineAlgorithm algo) throws TraderException {
        if (algo == null) {
            throw new TraderException(ExceptionCodes.ALGORITHM_NULL.code(),
                                      ExceptionCodes.ALGORITHM_NULL.message());
        }
        this.algo = algo;
    }

    @Override
    public IDataSource getDataSource() {
        return ds;
    }

    @Override
    public void setDataSource(IDataSource dataSource) throws TraderException {
        if (dataSource == null) {
            throw new TraderException(ExceptionCodes.DATASOURCE_NULL.code(),
                                      ExceptionCodes.DATASOURCE_NULL.message());
        }
        ds = dataSource;
    }

    @Override
    public Instrument getRelatedInstrument(String instrumentId) throws TraderException {
        return instruments.get(instrumentId);
    }

    @Override
    public EngineStatus getStatus() {
        return status;
    }

    @Override
    public TraderServiceRuntime getTraderServiceInfo(int traderId) throws TraderException {
        return findTraderServiceRuntimeByTraderId(traderId);
    }

    @Override
    public Collection<TraderServiceRuntime> getTraderServiceRuntimes() throws TraderException {
        return new HashSet<>(traders.values());
    }

    @Override
    public void settle(Properties properties) throws TraderException {
        changeStatus(EngineStatus.SETTLING);
        try {
            check0();
            settle(ds, algo);
            var conn = ds.getConnection();
            conn.updateAccount(getSettledAccount());
            changeStatus(EngineStatus.WORKING);
        }
        catch (TraderException e) {
            changeStatus(EngineStatus.SETTLE_FAILED);
            throw e;
        }
        catch (Throwable th) {
            changeStatus(EngineStatus.SETTLE_FAILED);
            throw new TraderException(ExceptionCodes.UNEXPECTED_ERROR.code(),
                                      ExceptionCodes.UNEXPECTED_ERROR.message(),
                                      th);
        }
    }

    @Override
    public Collection<ITraderEngineHandler> handlers() {
        return handlers.keySet();
    }

    @Override
    public void initialize(Properties properties) throws TraderException {
        changeStatus(EngineStatus.INITIALIZING);
        if (ds == null) {
            changeStatus(EngineStatus.INIT_FAILED);
            throw new TraderException(ExceptionCodes.DATASOURCE_NULL.code(),
                                      ExceptionCodes.DATASOURCE_NULL.message());
        }
        try {
            var conn = ds.getConnection();
            initAccount(conn.getAccount());
            initContracts(conn.getContractsByStatus(ContractStatus.CLOSED), conn);
            changeStatus(EngineStatus.WORKING);
        }
        catch (TraderException e) {
            changeStatus(EngineStatus.INIT_FAILED);
            throw e;
        }
        catch (Throwable th) {
            changeStatus(EngineStatus.INIT_FAILED);
            throw new TraderException(ExceptionCodes.UNEXPECTED_ERROR.code(),
                                      ExceptionCodes.UNEXPECTED_ERROR.message(),
                                      th);
        }
    }

    @Override
    public void registerTrader(int traderId, ITraderService trader) throws TraderException {
        if (trader == null) {
            throw new TraderException(ExceptionCodes.TRADER_SERVICE_NULL.code(),
                                      ExceptionCodes.TRADER_SERVICE_NULL.message());
        }
        if (traders.containsKey(traderId)) {
            throw new TraderException(ExceptionCodes.TRADER_ID_DUPLICATED.code(),
                                      ExceptionCodes.TRADER_ID_DUPLICATED.message());
        }
        addTrader(traderId, trader);
    }

    @Override
    public void removeHanlder(ITraderEngineHandler handler) throws TraderException {
        if (handler == null) {
            throw new TraderException(ExceptionCodes.TRADER_ENGINE_HANDLER_NULL.code(),
                                      ExceptionCodes.TRADER_ENGINE_HANDLER_NULL.message());
        }
        handlers.remove(handler);
    }

    @Override
    public void request(Request request,
                        Instrument instrument,
                        Properties properties,
                        int requestId) throws TraderException {
        check0();
        check2(request, instrument);
        /*
         * Remmeber the instrument it once operated.
         */
        instruments.put(instrument.getInstrumentId(), instrument);
        if (request.getOffset() == Offset.OPEN) {
            decideTrader(request);
            checkAssetsOpen(request, instrument);
            forwardRequest(request, request.getTraderId(), requestId);
        }
        else {
            var cs = checkAssetsClose(request, instrument);
            for (var r : group(cs, request)) {
                forwardRequest(r, r.getTraderId(), requestId);
            }
        }
    }

    @Override
    public void request(Request request, int requestId) throws TraderException {
        if (request == null) {
            throw new TraderException(ExceptionCodes.DELETE_REQS_NULL.code(),
                                      ExceptionCodes.DELETE_REQS_NULL.message());
        }
        forwardRequest(request, request.getTraderId(), requestId);
    }

    @Override
    public void setInitProperties(int traderId, Properties properties) throws TraderException {
        getTraderServiceInfo(traderId).setInitProperties(properties);
    }

    @Override
    public void setSettleProperties(int traderId, Properties properties) throws TraderException {
        getTraderServiceInfo(traderId).setSettleProperties(properties);
    }

    @Override
    public void setStartProperties(int traderId, Properties properties) throws TraderException {
        getTraderServiceInfo(traderId).setStartProperties(properties);
    }

    @Override
    public void start(Properties properties) throws TraderException {
        changeStatus(EngineStatus.STARTING);
        try {
            globalStartProps.clear();
            if (properties != null) {
                globalStartProps.putAll(properties);
            }
            for (var p : traders.entrySet()) {
                startEach(p.getKey(), p.getValue());
            }
            changeStatus(EngineStatus.WORKING);
        }
        catch (TraderException e) {
            changeStatus(EngineStatus.START_FAILED);
            throw e;
        }
        catch (Throwable th) {
            changeStatus(EngineStatus.START_FAILED);
            throw new TraderException(ExceptionCodes.UNEXPECTED_ERROR.code(),
                                      ExceptionCodes.UNEXPECTED_ERROR.message(),
                                      th);
        }
    }

    @Override
    public void stop() throws TraderException {
        changeStatus(EngineStatus.STOPPING);
        try {
            for (var p : traders.entrySet()) {
                stopEach(p.getKey(), p.getValue());
            }
            changeStatus(EngineStatus.STOPPED);
        }
        catch (TraderException e) {
            changeStatus(EngineStatus.STOP_FAILED);
            throw e;
        }
        catch (Throwable th) {
            changeStatus(EngineStatus.STOP_FAILED);
            throw new TraderException(ExceptionCodes.UNEXPECTED_ERROR.code(),
                                      ExceptionCodes.UNEXPECTED_ERROR.message(),
                                      th);
        }
    }

    @Override
    public void unregisterTrader(int traderId) throws TraderException {
        /*
         * Verify trader with specified ID exists, or throw exception.
         */
        getTraderServiceInfo(traderId);
        traders.remove(traderId);
    }

    private void addTrader(int traderId, ITraderService trader) {
        var i = new ExtendedTraderServiceRuntime();
        i.setEnabled(false);
        i.setEngine(this);
        i.setTrader(trader);
        i.setTraderId(traderId);
        traders.put(traderId, i);
    }

    private void callOnErasedAccount(Account a) {
        if (handlers.isEmpty()) {
            return;
        }
        handlers.keySet().parallelStream().forEach(h -> {
            try {
                h.onErasingAccount(a);
            }
            catch (Throwable th) {
                try {
                    h.onException(new TraderRuntimeException(
                            ExceptionCodes.USER_CODE_ERROR.code(),
                            ExceptionCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        });
    }

    private void callOnErasedContracts(Collection<Contract> cs) {
        if (handlers.isEmpty()) {
            return;
        }
        handlers.keySet().parallelStream().forEach(h -> {
            try {
                h.onErasingContracts(cs);
            }
            catch (Throwable th) {
                try {
                    h.onException(new TraderRuntimeException(
                            ExceptionCodes.USER_CODE_ERROR.code(),
                            ExceptionCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        });
    }

    /*
     * If something is wrong, tell user to handle it. If the handling is wrong,
     * tell user the handling is wrong.
     */
    private void callOnException(TraderRuntimeException e) {
        if (handlers.isEmpty()) {
            return;
        }
        handlers.keySet().parallelStream().forEach(h -> {
            try {
                h.onException(e);
            }
            catch (Throwable th) {
                try {
                    h.onException(new TraderRuntimeException(
                            ExceptionCodes.USER_CODE_ERROR.code(),
                            ExceptionCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        });
    }

    private void callOnStatusChange() {
        if (handlers.isEmpty()) {
            return;
        }
        handlers.keySet().parallelStream().forEach(h -> {
            try {
                h.onStatusChange(status);
            }
            catch (Throwable th) {
                try {
                    callOnException(new TraderRuntimeException(
                            ExceptionCodes.USER_CODE_ERROR.code(),
                            ExceptionCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        });
    }

    private boolean canClose(Contract c, Request request) throws TraderException {
        if (c.getStatus() != ContractStatus.OPEN) {
            return false;
        }
        var offset = request.getOffset();
        var direction = request.getDirection();
        if (null == offset) {
            throw new TraderException(ExceptionCodes.OFFSET_NULL.code(),
                                      ExceptionCodes.OFFSET_NULL.message());
        }
        if (null == direction) {
            throw new TraderException(ExceptionCodes.DIRECTION_NULL.code(),
                                      ExceptionCodes.DIRECTION_NULL.message());
        }
        if (direction == Direction.BUY) {
            return c.getDirection() == Direction.SELL;
        }
        else {
            return c.getDirection() == Direction.BUY;
        }
    }

    private void deleteOrderRequest(Request request) throws TraderException {
        var orderId = request.getOrderId();
        var traderId = findTraderIdByOrderId(orderId);
        var rt = findTraderServiceRuntimeByTraderId(traderId);
        var h = rt.getHandler();
        if (h == null) {
            throw new TraderException(ExceptionCodes.TRADER_SVC_HANDLER_NULL.code(),
                                      ExceptionCodes.TRADER_SVC_HANDLER_NULL.message());
        }
        var r = new Response();
        r.setInstrumentId(request.getInstrumentId());
        r.setOrderId(orderId);
        r.setTraderId(traderId);
        r.setAction(ActionType.DELETE);
        r.setOffset(request.getOffset());
        r.setDirection(request.getDirection());
        r.setTradingDay(rt.getTrader().getServiceInfo().getTradingDay());
        r.setUuid(Utils.nextUuid().toString());
        r.setStatusCode(0);

        try {
            h.onResponse(r);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ExceptionCodes.DELETE_ORDER_FAILED.code(),
                                                       ExceptionCodes.DELETE_ORDER_FAILED.message(),
                                                       th));
        }
    }

    private void changeStatus(EngineStatus status) {
        this.status = status;
        callOnStatusChange();
    }

    private void check0() throws TraderException {
        if (ds == null) {
            throw new TraderException(ExceptionCodes.DATASOURCE_NULL.code(),
                                      ExceptionCodes.DATASOURCE_NULL.message());
        }
        if (algo == null) {
            throw new TraderException(ExceptionCodes.ALGORITHM_NULL.code(),
                                      ExceptionCodes.ALGORITHM_NULL.message());
        }
    }

    private void check1(Integer key, TraderServiceRuntime rt) throws TraderException {
        if (rt == null) {
            throw new TraderException(
                    ExceptionCodes.TRADER_ID_NOT_FOUND.code(),
                    ExceptionCodes.TRADER_ID_NOT_FOUND.message() + "(Trader ID:" + key.toString() + ")");
        }
        if (rt.getTrader() == null) {
            throw new TraderException(
                    ExceptionCodes.TRADER_SERVICE_NULL.code(),
                    ExceptionCodes.TRADER_SERVICE_NULL.message() + "(Trader ID:" + key.toString() + ")");
        }
    }

    private void check2(Request request, Instrument instrument) throws TraderException {
        if (request == null) {
            throw new TraderException(ExceptionCodes.ORDER_REQS_NULL.code(),
                                      ExceptionCodes.ORDER_REQS_NULL.message());
        }
        if (instrument == null) {
            throw new TraderException(ExceptionCodes.INSTRUMENT_NULL.code(),
                                      ExceptionCodes.INSTRUMENT_NULL.message());
        }
    }

    private Collection<Contract> checkAssetsClose(Request request, Instrument instrument) throws TraderException {
        checkVolumn(request.getQuantity());
        var cs = getAvailableContracts(request);
        if (cs.size() < request.getQuantity()) {
            throw new TraderException(ExceptionCodes.INSUFFICIENT_POSITION.code(),
                                      ExceptionCodes.INSUFFICIENT_POSITION.message());
        }
        var r = new HashSet<Contract>(32);
        var c = algo.getCommission(request.getPrice(),
                               instrument,
                               request.getDirection(),
                               request.getOffset());
        for (int i = 0; i < request.getQuantity(); ++i) {
            var ctr = cs.get(i);
            r.add(ctr);
            setFrozenClose(c, ctr);
        }
        return r;
    }

    private void checkAssetsOpen(Request request, Instrument instrument)
            throws TraderEngineAlgorithmException, DataSourceException, TraderException {
        checkVolumn(request.getQuantity());
        var a = algo.getAmount(request.getPrice(), instrument);
        var m = algo.getMargin(request.getPrice(), instrument);
        var c = algo.getCommission(request.getPrice(),
                               instrument,
                               request.getDirection(),
                               request.getOffset());
        var total = request.getQuantity() * (m + c);
        var available = getAvailableMoney();
        if (available < total) {
            throw new TraderException(ExceptionCodes.INSUFFICIENT_MONEY.code(),
                                      ExceptionCodes.INSUFFICIENT_MONEY.message());
        }
        for (int i = 0; i < request.getQuantity(); ++i) {
            setFrozenOpen(a, m, c, request);
        }
    }

    private void checkVolumn(Long v) throws TraderException {
        if (v == null) {
            throw new TraderException(ExceptionCodes.VOLUMN_NULL.code(),
                                      ExceptionCodes.VOLUMN_NULL.message());
        }
        if (v <= 0) {
            throw new TraderException(ExceptionCodes.NONPOSITIVE_VOLUMN.code(),
                                      ExceptionCodes.NONPOSITIVE_VOLUMN.message());
        }
    }

    private void clearInternals() {
        orderTraders.clear();
        instruments.clear();
        traders.values().forEach(s -> {
            s.getIdTranslator().clear();
        });
    }

    private void decideTrader(Request request) throws TraderException {
        var rt = getProperTrader(request);
        if (!Objects.equals(rt.getTraderId(), request.getTraderId())) {
            request.setTraderId(rt.getTraderId());
        }
    }

    private Map<String, Instrument> findRelatedInstruments(Collection<String> instrumentIds,
                                                           IDataConnection conn) throws TraderException {
        final var r = new HashMap<String, Instrument>(512);
        for (var i : instrumentIds) {
            var instrument = conn.getInstrumentById(i);
            if (instrument == null) {
                throw new TraderException(ExceptionCodes.INSTRUMENT_NULL.code(),
                                          ExceptionCodes.INSTRUMENT_NULL.message() + "(" + i + ")");
            }
            r.put(i, instrument);
        }
        return r;
    }

    private Map<String, Tick> findRelatedTicks(Collection<String> instrumentIds,
                                               IDataConnection conn) throws TraderException {
        final var r = new HashMap<String, Tick>(512);
        for (var i : instrumentIds) {
            var instrument = conn.getTickByInstrumentId(i);
            if (instrument == null) {
                throw new TraderException(ExceptionCodes.TICK_NULL.code(),
                                          ExceptionCodes.TICK_NULL.message() + "(" + i + ")");
            }
            r.put(i, instrument);
        }
        return r;
    }

    private Integer findTraderIdByOrderId(long orderId) throws TraderException {
        var traderId = orderTraders.get(orderId);
        if (traderId == null) {
            throw new TraderException(ExceptionCodes.ORDER_ID_NOT_FOUND.code(),
                                      ExceptionCodes.ORDER_ID_NOT_FOUND.message() + "(Order ID:" + orderId + ")");
        }
        return traderId;
    }

    private ExtendedTraderServiceRuntime findTraderRandomly(Request request) throws TraderException {
        var a = new ArrayList<>(traders.keySet());
        traders.forEach((k, v) -> {
            if (v.isEnabled()) {
                a.add(k);
            }
        });
        if (a.isEmpty()) {
            throw new TraderException(ExceptionCodes.NO_TRADER.code(),
                                      ExceptionCodes.NO_TRADER.message());
        }
        var traderId = a.get(new Random().nextInt(a.size()));
        orderTraders.put(request.getOrderId(), traderId);
        return traders.get(traderId);
    }

    private ExtendedTraderServiceRuntime findTraderServiceRuntimeByTraderId(int traderId) throws TraderException {
        var rt = traders.get(traderId);
        check1(traderId, rt);
        return rt;
    }

    private void newRequest(Request request,
                            ExtendedTraderServiceRuntime tr,
                            int requestId) throws TraderException {
        var destId = tr.getIdTranslator().getDestinatedId(request.getOrderId(), request.getQuantity());
        request.setOrderId(destId);
        tr.getTrader().insert(request, requestId);
    }

    private void deleteRequest(Request request,
                               ExtendedTraderServiceRuntime tr,
                               int requestId) throws TraderException {
        var ids = tr.getIdTranslator().getDestinatedIds(request.getOrderId());
        if (ids == null) {
            throw new TraderException(ExceptionCodes.DEST_ID_NOT_FOUND.code(),
                                      ExceptionCodes.DEST_ID_NOT_FOUND.message()
                                      + "(Source order ID:" + request.getOrderId() + ")");
        }
        for (var i : ids) {
            /*
             * If the order is fulfilled, don't cancel it any more.
             */
            var cd = tr.getIdTranslator().getDownCountByDestId(i);
            if (cd == null) {
                throw new TraderException(
                        ExceptionCodes.COUNTDOWN_NOT_FOUND.code(),
                        ExceptionCodes.COUNTDOWN_NOT_FOUND.message() + "(Destinated ID: " + i + ")");
            }
            if (cd <= 0) {
                continue;
            }
            var c = Utils.copy(request);
            c.setOrderId(i);
            tr.getTrader().insert(c, requestId);
        }
    }

    private void forwardRequest(Request request, Integer traderId, int requestId) throws TraderException {
        var tr = findTraderServiceRuntimeByTraderId(traderId);
        check1(traderId, tr);
        if (null == request.getAction()) {
            throw new TraderException(ExceptionCodes.ACTION_NULL.code(),
                                      ExceptionCodes.ACTION_NULL.message());
        }
        if (request.getAction() == ActionType.NEW) {
            newRequest(request, tr, requestId);
        }
        else {
            deleteRequest(request, tr, requestId);
        }
    }

    private List<Contract> getAvailableContracts(Request request) throws TraderException {
        var conn = ds.getConnection();
        var cs = conn.getContractsByInstrumentId(request.getInstrumentId());
        if (cs == null) {
            throw new TraderException(ExceptionCodes.CONTRACT_NULL.code(),
                                      ExceptionCodes.CONTRACT_NULL.message());
        }
        var sorted = new LinkedList<Contract>(cs);
        sorted.sort((Contract o1, Contract o2)
                -> o1.getOpenTimestamp().compareTo(o2.getOpenTimestamp()));
        // Scan from earlier to later.
        var it = sorted.iterator();
        while (it.hasNext()) {
            var c = it.next();
            if (!canClose(c, request)) {
                it.remove();
            }
        }
        return sorted;
    }

    private double getAvailableMoney() throws TraderException {
        var a = getSettledAccount();
        return (a.getBalance() - a.getMargin() - a.getFrozenMargin() - a.getFrozenCommission());
    }

    private Collection<Contract> getContractsByOrderResponses(Collection<Trade> rsps) throws TraderException {
        var cs = new HashSet<Contract>(128);
        var conn = ds.getConnection();
        for (var r : rsps) {
            var s = conn.getContractsByTradeId(r.getTradeId());
            if (s == null) {
                throw new TraderException(ExceptionCodes.NO_CONTRACT.code(),
                                          ExceptionCodes.NO_CONTRACT.message()
                                          + "(Trade ID:" + r.getTradeId() + ")");
            }
            cs.addAll(s);
        }
        return cs;
    }

    private TraderServiceRuntime getProperTrader(Request request) throws TraderException {
        var traderId = request.getTraderId();
        if (traderId == null) {
            return findTraderRandomly(request);
        }
        else {
            var rt = findTraderServiceRuntimeByTraderId(traderId);
            check1(traderId, rt);
            if (!rt.isEnabled()) {
                throw new TraderException(ExceptionCodes.TRADER_NOT_ENABLED.code(),
                                          ExceptionCodes.TRADER_NOT_ENABLED.message() + "(Trader ID:" + traderId + ")");
            }
            orderTraders.put(request.getOrderId(), traderId);
            return rt;
        }
    }

    private Collection<String> getRalatedInstrumentIds() {
        return null;
    }

    private Account getSettledAccount() throws TraderException {
        final var conn = ds.getConnection();
        final var tradingDay = conn.getTradingDay();
        final var ids = getRalatedInstrumentIds();
        return algo.getAccount(conn.getAccount(),
                               conn.getDeposits(),
                               conn.getWithdraws(),
                               algo.getPositions(conn.getContracts(),
                                                 conn.getCommissions(),
                                                 conn.getMargins(),
                                                 findRelatedTicks(ids, conn),
                                                 findRelatedInstruments(ids, conn),
                                                 tradingDay));
    }

    private Collection<Request> group(Collection<Contract> cs, Request request) throws DataSourceException {
        final var today = new HashMap<Integer, Request>(64);
        final var yd = new HashMap<Integer, Request>(64);
        final var conn = ds.getConnection();
        var tradingDay = conn.getTradingDay();
        for (var c : cs) {
            if (c.getOpenTradingDay().isBefore(tradingDay)) {
                var o = yd.computeIfAbsent(c.getTraderId(), k -> {
                                   var co = Utils.copy(request);
                                   if (co == null) {
                                       throw new TraderRuntimeException(
                                               ExceptionCodes.OBJECT_COPY_FAILED.code(),
                                               ExceptionCodes.OBJECT_COPY_FAILED.message());
                                   }
                                   co.setOffset(Offset.CLOSE);
                                   co.setQuantity(0L);
                                   co.setTraderId(k);
                                   return co;

                               });
                o.setQuantity(o.getQuantity() + 1);
            }
            else {
                var o = today.computeIfAbsent(c.getTraderId(), k -> {
                                      var co = Utils.copy(request);
                                      if (co == null) {
                                          throw new TraderRuntimeException(
                                                  ExceptionCodes.OBJECT_COPY_FAILED.code(),
                                                  ExceptionCodes.OBJECT_COPY_FAILED.message());
                                      }
                                      co.setOffset(Offset.CLOSE_TODAY);
                                      co.setQuantity(0L);
                                      co.setTraderId(k);
                                      return co;

                                  });
                o.setQuantity(o.getQuantity() + 1);
            }
        }

        var r = new HashSet<Request>(today.values());
        r.addAll(yd.values());
        return r;
    }

    private void initAccount(Account a) throws TraderException {
        if (a == null) {
            throw new TraderException(ExceptionCodes.ACCOUNT_NULL.code(),
                                      ExceptionCodes.ACCOUNT_NULL.message());
        }
        callOnErasedAccount(a);
        final var conn = ds.getConnection();
        final var tradingDay = conn.getTradingDay();

        a.setPreBalance(a.getBalance());
        a.setPreDeposit(a.getDeposit());
        a.setPreMargin(a.getMargin());
        a.setPreWithdraw(a.getWithdraw());
        a.setBalance(0.0D);
        a.setDeposit(0.0D);
        a.setMargin(0.0D);
        a.setWithdraw(0.0D);
        a.setTradingDay(tradingDay);

        conn.updateAccount(a);
    }

    private void initContracts(Collection<Contract> cs, IDataConnection conn) throws TraderException {
        if (cs == null) {
            throw new TraderException(ExceptionCodes.CONTRACT_NULL.code(),
                                      ExceptionCodes.CONTRACT_NULL.message());
        }
        callOnErasedContracts(cs);
        for (var c : cs) {
            conn.removeContract(c.getContractId());
        }
    }

    private void setFrozenClose(double commission,
                                Contract contract) throws TraderException {
        IDataConnection conn = null;
        try {
            conn = ds.getConnection();
            final var tradingDay = conn.getTradingDay();
            conn.transaction();
            /*
             * Update contracts status to make it frozen.
             */
            contract.setStatus(ContractStatus.CLOSING);
            conn.updateContract(contract);
            /*
             * Add new commission for the current order, and make it frozen
             * before order is filled.
             */
            var cms = new Commission();
            cms.setCommission(commission);
            cms.setCommissionId(Utils.nextId());
            cms.setContractId(contract.getContractId());
            cms.setStatus(FeeStatus.FORZEN);
            cms.setTradingDay(tradingDay);
            conn.addCommission(cms);
            /*
             * Commit change.
             */
            conn.commit();
        }
        catch (DataSourceException e) {
            /*
             * Rollback data source.
             */
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
    }

    private void setFrozenOpen(double amount,
                               double margin,
                               double commission,
                               Request request) throws DataSourceException {
        IDataConnection conn = null;
        try {
            conn = ds.getConnection();
            final var tradingDay = conn.getTradingDay();
            conn.transaction();
            /*
             * Add preparing contract.
             */
            var ctr = new Contract();
            ctr.setContractId(Utils.nextId());
            ctr.setTraderId(request.getTraderId());
            ctr.setInstrumentId(request.getInstrumentId());
            ctr.setOpenAmount(amount);
            ctr.setOpenTradingDay(tradingDay);
            ctr.setDirection(request.getDirection());
            ctr.setStatus(ContractStatus.OPENING);
            conn.addContract(ctr);
            /*
             * Add frozen margin.
             */
            var cmn = new Commission();
            cmn.setCommission(commission);
            cmn.setCommissionId(Utils.nextId());
            cmn.setContractId(ctr.getContractId());
            cmn.setOrderId(request.getOrderId());
            cmn.setStatus(FeeStatus.FORZEN);
            cmn.setTradingDay(tradingDay);
            conn.addCommission(cmn);
            /*
             * Add frozen commission.
             */
            var mn = new Margin();
            mn.setContractId(ctr.getContractId());
            mn.setMargin(margin);
            mn.setMarginId(Utils.nextId());
            mn.setOrderId(request.getOrderId());
            mn.setStatus(FeeStatus.FORZEN);
            mn.setTradingDay(tradingDay);
            conn.addMargin(mn);
            /*
             * Commit change.
             */
            conn.commit();
        }
        catch (DataSourceException e) {
            /*
             * Rollback on exception.
             */
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        }
    }

    private void settle(IDataSource ds, ITraderEngineAlgorithm algo) throws TraderException {
        final var conn = ds.getConnection();
        var rs = conn.getRequests();
        if (rs == null) {
            throw new TraderException(ExceptionCodes.ORDER_REQS_NULL.code(),
                                      ExceptionCodes.ORDER_REQS_NULL.message());
        }
        for (var r : rs) {
            var orderId = r.getOrderId();
            if (orderId == null) {
                throw new TraderException(ExceptionCodes.ORDER_ID_NULL.code(),
                                          ExceptionCodes.ORDER_ID_NULL.message());
            }
            var trades = conn.getTradesByOrderId(orderId);
            if (trades == null) {
                throw new TraderException(ExceptionCodes.NO_TRADE.code(),
                                          ExceptionCodes.NO_TRADE.message());
            }
            var ctrs = getContractsByOrderResponses(trades);
            if (ctrs == null) {
                throw new TraderException(ExceptionCodes.NO_CONTRACT.code(),
                                          ExceptionCodes.NO_CONTRACT.message());

            }
            var cals = conn.getResponseByOrderId(orderId);
            if (cals == null) {
                throw new TraderException(ExceptionCodes.NO_RESPONSE.code(),
                                          ExceptionCodes.NO_RESPONSE.message());

            }
            var o = algo.getOrder(r, ctrs, trades, cals);
            var s = o.getStatus();
            if (s == OrderStatus.ACCEPTED
                || s == OrderStatus.QUEUED
                || s == OrderStatus.UNQUEUED) {
                deleteOrderRequest(r);
            }
        }
        // Clear everyday to avoid mem leak.
        clearInternals();
    }

    private void startEach(Integer key, ExtendedTraderServiceRuntime info) throws TraderException {
        check1(key, info);
        if (!info.isEnabled()) {
            return;
        }
        var properties = new Properties();
        if (info.getStartProperties() != null) {
            properties.putAll(info.getStartProperties());
        }
        properties.putAll(globalStartProps);
        /*
         * Trader services share the same class of handler, not same instance of
         * handler. To shared information among these handlers, use STATIC.
         */
        if (info.getHandler() == null) {
            var h = new TraderServiceHandler(info);
            info.setHandler(h);
            info.setIdTranslator(h);
        }
        try {
            info.getTrader().start(properties, info.getHandler());
        }
        catch (TraderException ex) {
            throw new TraderException(
                    ex.getCode(),
                    ex.getMessage() + "(Trader ID:" + key.toString() + ")",
                    ex);

        }
    }

    private void stopEach(Integer key, TraderServiceRuntime info) throws TraderException {
        check1(key, info);
        try {
            info.getTrader().stop();
        }
        catch (TraderException ex) {
            throw new TraderException(
                    ex.getCode(),
                    ex.getMessage() + "(Trader ID:" + key.toString() + ")",
                    ex);
        }
    }

}
