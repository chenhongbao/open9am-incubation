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
package com.open9am.engine;

import com.open9am.service.Account;
import com.open9am.service.CancelReason;
import com.open9am.service.CancelRequest;
import com.open9am.service.CancelResponse;
import com.open9am.service.Commission;
import com.open9am.service.Contract;
import com.open9am.service.ContractStatus;
import com.open9am.service.FeeStatus;
import com.open9am.service.ITraderService;
import com.open9am.service.Instrument;
import com.open9am.service.Margin;
import com.open9am.service.OrderRequest;
import com.open9am.service.OrderResponse;
import com.open9am.service.OrderStatus;
import com.open9am.service.OrderType;
import com.open9am.service.TraderException;
import com.open9am.service.TraderRuntimeException;
import com.open9am.service.utils.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class TraderEngine implements ITraderEngine {

    private ITraderEngineAlgorithm algo;
    private IDataSource ds;
    private final Properties globalStartProps;
    private final ConcurrentHashMap<ITraderEngineHandler, Object> handlers;
    private final ConcurrentHashMap<String, Instrument> instruments;
    private final ConcurrentHashMap<Long, Integer> orderTraders;
    private EngineStatus status;
    private final ConcurrentHashMap<Integer, ExtendedTraderServiceRuntime> traders;

    public TraderEngine() {
        handlers = new ConcurrentHashMap<>(32);
        traders = new ConcurrentHashMap<>(32);
        orderTraders = new ConcurrentHashMap<>(1024);
        instruments = new ConcurrentHashMap<>(512);
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
            conn.updateAccount(getSettledAccount(properties));
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
    public void request(OrderRequest request,
                        Instrument instrument,
                        Properties properties,
                        int requestId) throws TraderException {
        check0();
        check2(request, instrument);
        /*
         * Remmeber the instrument it once operated.
         */
        instruments.put(instrument.getInstrumentId(), instrument);
        var type = request.getType();
        if (type == OrderType.BUY_OPEN || type == OrderType.SELL_OPEN) {
            decideTrader(request);
            checkAssetsOpen(request, instrument, properties);
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
    public void request(CancelRequest request, int requestId) throws TraderException {
        if (request == null) {
            throw new TraderException(ExceptionCodes.CANCEL_REQS_NULL.code(),
                                      ExceptionCodes.CANCEL_REQS_NULL.message());
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

    private boolean canClose(Contract c, OrderRequest request) throws TraderException {
        if (c.getStatus() != ContractStatus.OPEN) {
            return false;
        }
        var t = request.getType();
        if (null == t) {
            throw new TraderException(ExceptionCodes.INVALID_ORDER_TYPE.code(),
                                      ExceptionCodes.INVALID_ORDER_TYPE.message());
        }
        else {
            switch (t) {
                case BUY_CLOSE:
                case BUY_CLOSE_TODAY:
                    return c.getOpenType() == OrderType.SELL_OPEN;
                case SELL_CLOSE:
                case SELL_CLOSE_TODAY:
                    return c.getOpenType() == OrderType.BUY_OPEN;
                default:
                    throw new TraderException(ExceptionCodes.INVALID_ORDER_TYPE.code(),
                                              ExceptionCodes.INVALID_ORDER_TYPE.message());
            }
        }
    }

    private void cancelOrderRequest(OrderRequest request) throws TraderException {
        var orderId = request.getOrderId();
        var traderId = findTraderIdByOrderId(orderId);
        var rt = findTraderServiceRuntimeByTraderId(traderId);
        var h = rt.getHandler();
        if (h == null) {
            throw new TraderException(ExceptionCodes.TRADER_SVC_HANDLER_NULL.code(),
                                      ExceptionCodes.TRADER_SVC_HANDLER_NULL.message());
        }
        var r = new CancelResponse();
        r.setInstrumentId(request.getInstrumentId());
        r.setOrderId(orderId);
        r.setTraderId(traderId);
        r.setTradingDay(rt.getTrader().getServiceInfo().getTradingDay());
        r.setUuid(Utils.getUuid().toString());
        r.setReason(CancelReason.MARKET_CLOSE);
        r.setStatusCode(0);

        try {
            h.onCancelResponse(r);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ExceptionCodes.CANCEL_ORDER_FAILED.code(),
                                                       ExceptionCodes.CANCEL_ORDER_FAILED.message(),
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

    private void check2(OrderRequest request, Instrument instrument) throws TraderException {
        if (request == null) {
            throw new TraderException(ExceptionCodes.ORDER_REQS_NULL.code(),
                                      ExceptionCodes.ORDER_REQS_NULL.message());
        }
        if (instrument == null) {
            throw new TraderException(ExceptionCodes.INSTRUMENT_NULL.code(),
                                      ExceptionCodes.INSTRUMENT_NULL.message());
        }
    }

    private Collection<Contract> checkAssetsClose(OrderRequest request, Instrument instrument) throws TraderException {
        checkVolumn(request.getVolumn());
        var cs = getAvailableContracts(request);
        if (cs.size() < request.getVolumn()) {
            throw new TraderException(ExceptionCodes.INSUFFICIENT_POSITION.code(),
                                      ExceptionCodes.INSUFFICIENT_POSITION.message());
        }
        var r = new HashSet<Contract>(32);
        var c = algo.getCommission(request.getPrice(),
                               instrument,
                               request.getType());
        for (int i = 0; i < request.getVolumn(); ++i) {
            var ctr = cs.get(i);
            r.add(ctr);
            setFrozenClose(c, ctr, request.getType());
        }
        return r;
    }

    private void checkAssetsOpen(OrderRequest request, Instrument instrument, Properties properties)
            throws TraderEngineAlgorithmException, DataSourceException, TraderException {
        checkVolumn(request.getVolumn());
        var a = algo.getAmount(request.getPrice(), instrument);
        var m = algo.getMargin(request.getPrice(), instrument);
        var c = algo.getCommission(request.getPrice(),
                               instrument,
                               request.getType());
        var total = request.getVolumn() * (m + c);
        var available = getAvailableMoney(properties);
        if (available < total) {
            throw new TraderException(ExceptionCodes.INSUFFICIENT_MONEY.code(),
                                      ExceptionCodes.INSUFFICIENT_MONEY.message());
        }
        for (int i = 0; i < request.getVolumn(); ++i) {
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

    private void decideTrader(OrderRequest request) throws TraderException {
        var rt = getProperTrader(request);
        if (!Objects.equals(rt.getTraderId(), request.getTraderId())) {
            request.setTraderId(rt.getTraderId());
        }
    }

    private Integer findTraderIdByOrderId(long orderId) throws TraderException {
        var traderId = orderTraders.get(orderId);
        if (traderId == null) {
            throw new TraderException(ExceptionCodes.ORDER_ID_NOT_FOUND.code(),
                                      ExceptionCodes.ORDER_ID_NOT_FOUND.message() + "(Order ID:" + orderId + ")");
        }
        return traderId;
    }

    private ExtendedTraderServiceRuntime findTraderRandomly(OrderRequest request) throws TraderException {
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

    private void forward(OrderRequest request,
                         ExtendedTraderServiceRuntime tr,
                         int requestId) throws TraderException {
        var destId = tr.getIdTranslator().getDestinatedId(request.getOrderId(), request.getVolumn());
        request.setOrderId(destId);
        tr.getTrader().insert(request, requestId);
    }

    private void forward(CancelRequest request,
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
            tr.getTrader().cancel(c, requestId);
        }
    }

    private <T> void forwardRequest(T request, Integer traderId, int requestId) throws TraderException {
        var tr = findTraderServiceRuntimeByTraderId(traderId);
        check1(traderId, tr);
        if (request instanceof OrderRequest) {
            forward((OrderRequest) request, tr, requestId);
        }
        else if (request instanceof CancelRequest) {
            forward((CancelRequest) request, tr, requestId);
        }
        else {
            throw new TraderException(ExceptionCodes.INVALID_REQUEST_INSTANCE.code(),
                                      ExceptionCodes.INVALID_REQUEST_INSTANCE.message());
        }
    }

    private List<Contract> getAvailableContracts(OrderRequest request) throws TraderException {
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

    private double getAvailableMoney(Properties properties) throws DataSourceException, TraderEngineAlgorithmException {
        var a = getSettledAccount(properties);
        return (a.getBalance() - a.getMargin() - a.getFrozenMargin() - a.getFrozenCommission());
    }

    private Collection<Contract> getContractsByOrderResponses(Collection<OrderResponse> rsps) throws TraderException {
        var cs = new HashSet<Contract>(128);
        var conn = ds.getConnection();
        for (var r : rsps) {
            var s = conn.getContractsByResponseId(r.getResponseId());
            if (s == null) {
                throw new TraderException(ExceptionCodes.CONTRACT_NULL.code(),
                                          ExceptionCodes.CONTRACT_NULL.message()
                                          + "(Response ID:" + r.getResponseId() + ")");
            }
            cs.addAll(s);
        }
        return cs;
    }

    private TraderServiceRuntime getProperTrader(OrderRequest request) throws TraderException {
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

    private Account getSettledAccount(Properties properties)
            throws DataSourceException, TraderEngineAlgorithmException {
        final var conn = ds.getConnection();
        return algo.getAccount(conn.getAccount(),
                               conn.getDeposits(),
                               conn.getWithdraws(),
                               algo.getPositions(conn.getContracts(),
                                                 conn.getCommissions(),
                                                 conn.getMargins(),
                                                 properties));
    }

    private Collection<OrderRequest> group(Collection<Contract> cs, OrderRequest request) throws DataSourceException {
        final var today = new HashMap<Integer, OrderRequest>(64);
        final var yd = new HashMap<Integer, OrderRequest>(64);
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
                                   if (co.getType() == OrderType.BUY_CLOSE_TODAY) {
                                       co.setType(OrderType.BUY_CLOSE);
                                   }
                                   else if (co.getType() == OrderType.SELL_CLOSE_TODAY) {
                                       co.setType(OrderType.SELL_CLOSE);
                                   }
                                   co.setVolumn(0L);
                                   co.setTraderId(k);
                                   return co;

                               });
                o.setVolumn(o.getVolumn() + 1);
            }
            else {
                var o = today.computeIfAbsent(c.getTraderId(), k -> {
                                      var co = Utils.copy(request);
                                      if (co == null) {
                                          throw new TraderRuntimeException(
                                                  ExceptionCodes.OBJECT_COPY_FAILED.code(),
                                                  ExceptionCodes.OBJECT_COPY_FAILED.message());
                                      }
                                      if (co.getType() == OrderType.BUY_CLOSE) {
                                          co.setType(OrderType.BUY_CLOSE_TODAY);
                                      }
                                      else if (co.getType() == OrderType.SELL_CLOSE) {
                                          co.setType(OrderType.SELL_CLOSE_TODAY);
                                      }
                                      co.setVolumn(0L);
                                      co.setTraderId(k);
                                      return co;

                                  });
                o.setVolumn(o.getVolumn() + 1);
            }
        }

        var r = new HashSet<OrderRequest>(today.values());
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
                                Contract contract,
                                OrderType type) throws TraderException {
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
            cms.setCommissionId(Utils.getId());
            cms.setContractId(contract.getContractId());
            cms.setStatus(FeeStatus.FORZEN);
            cms.setTradingDay(tradingDay);
            cms.setType(type);
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
                               OrderRequest request) throws DataSourceException {
        IDataConnection conn = null;
        try {
            conn = ds.getConnection();
            final var tradingDay = conn.getTradingDay();
            conn.transaction();
            /*
             * Add preparing contract.
             */
            var ctr = new Contract();
            ctr.setContractId(Utils.getId());
            ctr.setTraderId(request.getTraderId());
            ctr.setInstrumentId(request.getInstrumentId());
            ctr.setOpenAmount(amount);
            ctr.setOpenTradingDay(tradingDay);
            ctr.setOpenType(request.getType());
            ctr.setStatus(ContractStatus.OPENING);
            conn.addContract(ctr);
            /*
             * Add frozen margin.
             */
            var cmn = new Commission();
            cmn.setCommission(commission);
            cmn.setCommissionId(Utils.getId());
            cmn.setContractId(ctr.getContractId());
            cmn.setOrderId(request.getOrderId());
            cmn.setStatus(FeeStatus.FORZEN);
            cmn.setTradingDay(tradingDay);
            cmn.setType(request.getType());
            conn.addCommission(cmn);
            /*
             * Add frozen commission.
             */
            var mn = new Margin();
            mn.setContractId(ctr.getContractId());
            mn.setMargin(margin);
            mn.setMarginId(Utils.getId());
            mn.setOrderId(request.getOrderId());
            mn.setStatus(FeeStatus.FORZEN);
            mn.setTradingDay(tradingDay);
            mn.setType(request.getType());
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
        var rs = conn.getOrderRequests();
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
            var rsps = conn.getOrderResponsesByOrderId(orderId);
            if (rsps == null) {
                throw new TraderException(ExceptionCodes.ORDER_RSPS_NULL.code(),
                                          ExceptionCodes.ORDER_RSPS_NULL.message());
            }
            var ctrs = getContractsByOrderResponses(rsps);
            if (ctrs == null) {
                throw new TraderException(ExceptionCodes.CONTRACT_NULL.code(),
                                          ExceptionCodes.CONTRACT_NULL.message());

            }
            var cals = conn.getCancelResponseByOrderId(orderId);
            if (cals == null) {
                throw new TraderException(ExceptionCodes.CANCEL_RSPS_NULL.code(),
                                          ExceptionCodes.CANCEL_RSPS_NULL.message());

            }
            var o = algo.getOrder(r, ctrs, rsps, cals);
            var s = o.getStatus();
            if (s == OrderStatus.ACCEPTED
                || s == OrderStatus.PART_TRADED_INQUE
                || s == OrderStatus.PART_TRADED_NOQUE) {
                cancelOrderRequest(r);
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
