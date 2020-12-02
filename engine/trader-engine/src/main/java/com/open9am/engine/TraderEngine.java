package com.open9am.engine;

import com.open9am.service.Account;
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
import com.open9am.service.OrderStatus;
import com.open9am.service.OrderType;
import com.open9am.service.TraderException;
import com.open9am.service.TraderRuntimeException;
import com.open9am.service.utils.Utils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class TraderEngine implements ITraderEngine {

    private final ITraderEngineAlgorithm algo;
    private IDataSource ds;
    private final Properties globalStartProps;
    private ITraderEngineHandler handler;
    private final ConcurrentHashMap<Long, Integer> orderTraders;
    private EngineStatus status;
    private final ConcurrentHashMap<Integer, TraderServiceRuntime> traders;

    public TraderEngine(ITraderEngineAlgorithm algorithm) {
        algo = algorithm;
        traders = new ConcurrentHashMap<>(32);
        orderTraders = new ConcurrentHashMap<>(1024);
        globalStartProps = new Properties();
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
    public IDataSource getDataSource() {
        return ds;
    }

    @Override
    public void setDataSource(IDataSource dataSource) throws TraderException {
        if (dataSource == null) {
            throw new TraderException(ErrorCodes.DATASOURCE_NULL.code(),
                                      ErrorCodes.DATASOURCE_NULL.message());
        }
        ds = dataSource;
    }

    @Override
    public ITraderEngineHandler getHandler() {
        return handler;
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
            ds.updateAccount(getSettledAccount(properties));
            changeStatus(EngineStatus.WORKING);
        }
        catch (TraderException e) {
            changeStatus(EngineStatus.SETTLE_FAILED);
            throw e;
        }
        catch (Throwable th) {
            changeStatus(EngineStatus.SETTLE_FAILED);
            throw new TraderException(ErrorCodes.UNEXPECTED_ERROR.code(),
                                      ErrorCodes.UNEXPECTED_ERROR.message(),
                                      th);
        }
    }

    @Override
    public void initialize(Properties properties) throws TraderException {
        changeStatus(EngineStatus.INITIALIZING);
        if (ds == null) {
            changeStatus(EngineStatus.INIT_FAILED);
            throw new TraderException(ErrorCodes.DATASOURCE_NULL.code(),
                                      ErrorCodes.DATASOURCE_NULL.message());
        }
        try {
            initAccount(ds.getAccount());
            initContracts(ds.getContractsByStatus(ContractStatus.CLOSED));
            changeStatus(EngineStatus.WORKING);
        }
        catch (TraderException e) {
            changeStatus(EngineStatus.INIT_FAILED);
            throw e;
        }
        catch (Throwable th) {
            changeStatus(EngineStatus.INIT_FAILED);
            throw new TraderException(ErrorCodes.UNEXPECTED_ERROR.code(),
                                      ErrorCodes.UNEXPECTED_ERROR.message(),
                                      th);
        }
    }

    @Override
    public void registerTrader(int traderId, ITraderService trader) throws TraderException {
        if (trader == null) {
            throw new TraderException(ErrorCodes.TRADER_SERVICE_NULL.code(),
                                      ErrorCodes.TRADER_SERVICE_NULL.message());
        }
        if (traders.containsKey(traderId)) {
            throw new TraderException(ErrorCodes.TRADER_ID_DUPLICATED.code(),
                                      ErrorCodes.TRADER_ID_DUPLICATED.message());
        }
        addTrader(traderId, trader);
    }

    @Override
    public void request(OrderRequest request, Instrument instrument, Properties properties, int requestId) throws TraderException {
        checkAssets(request, instrument, properties);
        forwardRequest(request, requestId);
    }

    @Override
    public void request(CancelRequest request, int requestId) throws TraderException {
        if (request == null) {
            throw new TraderException(ErrorCodes.REQUEST_NULL.code(),
                                      ErrorCodes.REQUEST_NULL.message());
        }
        forwardRequest(request, requestId);
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
    public void start(Properties properties, ITraderEngineHandler handler) throws TraderException {
        changeStatus(EngineStatus.STARTING);
        try {
            globalStartProps.clear();
            if (properties != null) {
                globalStartProps.putAll(properties);
            }
            this.handler = handler;
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
            throw new TraderException(ErrorCodes.UNEXPECTED_ERROR.code(),
                                      ErrorCodes.UNEXPECTED_ERROR.message(),
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
            throw new TraderException(ErrorCodes.UNEXPECTED_ERROR.code(),
                                      ErrorCodes.UNEXPECTED_ERROR.message(),
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
        var i = new TraderServiceRuntime();
        i.setEnabled(false);
        i.setEngine(this);
        i.setTrader(trader);
        i.setTraderId(traderId);
        traders.put(traderId, i);
    }

    private void callOnErasedAccount(Account a) {
        if (handler != null) {
            try {
                handler.OnErasingAccount(a);
            }
            catch (Throwable th) {
                try {
                    handler.OnException(new TraderRuntimeException(
                            ErrorCodes.USER_CODE_ERROR.code(),
                            ErrorCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        }
    }

    private void callOnErasedContracts(Collection<Contract> cs) {
        if (handler != null) {
            try {
                handler.OnErasingContracts(cs);
            }
            catch (Throwable th) {
                try {
                    handler.OnException(new TraderRuntimeException(
                            ErrorCodes.USER_CODE_ERROR.code(),
                            ErrorCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        }
    }

    /*
     * If something is wrong, tell user to handle it. If the handling is wrong,
     * tell user the handling is wrong.
     */
    private void callOnException(TraderRuntimeException e) {
        if (handler != null) {
            try {
                handler.OnException(e);
            }
            catch (Throwable th) {
                try {
                    handler.OnException(new TraderRuntimeException(
                            ErrorCodes.USER_CODE_ERROR.code(),
                            ErrorCodes.USER_CODE_ERROR.message(),
                            th));
                }
                catch (Throwable ignored) {
                }
            }
        }
    }

    private void callOnStatusChange() {
        try {
            handler.OnStatusChange(status);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    private boolean canClose(Contract c, OrderRequest request) throws TraderException {
        if (c.getStatus() != ContractStatus.OPEN) {
            return false;
        }
        var t = request.getType();
        if (null == t) {
            throw new TraderException(ErrorCodes.INVALID_ORDER_TYPE.code(),
                                      ErrorCodes.INVALID_ORDER_TYPE.message());
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
                    throw new TraderException(ErrorCodes.INVALID_ORDER_TYPE.code(),
                                              ErrorCodes.INVALID_ORDER_TYPE.message());
            }
        }
    }

    private void cancelOrderRequest(OrderRequest request) throws TraderException {
        var orderId = request.getOrderId();
        var traderId = findTraderIdByOrderId(orderId);
        var rt = findTraderServiceRuntimeByTraderId(traderId);
        var h = rt.getHandler();
        if (h == null) {
            throw new TraderException(ErrorCodes.TRADER_SVC_HANDLER_NULL.code(),
                                      ErrorCodes.TRADER_SVC_HANDLER_NULL.message());
        }
        var r = new CancelResponse();
        r.setInstrumentId(request.getInstrumentId());
        r.setOrderId(orderId);
        r.setTraderId(traderId);
        r.setTradingDay(ds.getTradingDay());
        r.setUuid(Utils.getUuid().toString());

        try {
            h.OnCancelResponse(r);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.CANCEL_ORDER_FAILED.code(),
                                                       ErrorCodes.CANCEL_ORDER_FAILED.message(),
                                                       th));
        }
    }

    private void changeStatus(EngineStatus status) {
        this.status = status;
        callOnStatusChange();
    }

    private void check0() throws TraderException {
        if (ds == null) {
            throw new TraderException(ErrorCodes.DATASOURCE_NULL.code(),
                                      ErrorCodes.DATASOURCE_NULL.message());
        }
        if (algo == null) {
            throw new TraderException(ErrorCodes.ALGORITHM_NULL.code(),
                                      ErrorCodes.ALGORITHM_NULL.message());
        }
    }

    private void check1(Integer key, TraderServiceRuntime rt) throws TraderException {
        if (rt == null) {
            throw new TraderException(
                    ErrorCodes.TRADER_ID_NOT_FOUND.code(),
                    ErrorCodes.TRADER_ID_NOT_FOUND.message() + "(Trader ID:" + key.toString() + ")");
        }
        if (rt.getTrader() == null) {
            throw new TraderException(
                    ErrorCodes.TRADER_SERVICE_NULL.code(),
                    ErrorCodes.TRADER_SERVICE_NULL.message() + "(Trader ID:" + key.toString() + ")");
        }
    }

    private void check2(OrderRequest request, Instrument instrument) throws TraderException {
        if (request == null) {
            throw new TraderException(ErrorCodes.ORDER_REQS_NULL.code(),
                                      ErrorCodes.ORDER_REQS_NULL.message());
        }
        if (instrument == null) {
            throw new TraderException(ErrorCodes.INSTRUMENT_NULL.code(),
                                      ErrorCodes.INSTRUMENT_NULL.message());
        }
    }

    private void checkAssets(OrderRequest request, Instrument instrument, Properties properties) throws TraderException {
        check0();
        check2(request, instrument);
        var type = request.getType();
        if (type == OrderType.BUY_OPEN || type == OrderType.SELL_OPEN) {
            checkAssetsOpen(request, instrument, properties);
        }
        else {
            checkAssetsClose(request, instrument);
        }
    }

    private void checkAssetsClose(OrderRequest request, Instrument instrument) throws TraderException {
        checkVolumn(request.getVolumn());
        var cs = getAvailableContracts(request);
        if (cs.size() < request.getVolumn()) {
            throw new TraderException(ErrorCodes.INSUFFICIENT_POSITION.code(),
                                      ErrorCodes.INSUFFICIENT_POSITION.message());
        }
        var c = algo.getCommission(request.getPrice(), instrument);
        for (int i = 0; i < request.getVolumn(); ++i) {
            setFrozenClose(c, cs.get(i), request.getType());
        }
    }

    private void checkAssetsOpen(OrderRequest request, Instrument instrument, Properties properties)
            throws TraderEngineAlgorithmException, DataSourceException, TraderException {
        checkVolumn(request.getVolumn());
        var a = algo.getAmount(request.getPrice(), instrument);
        var m = algo.getMargin(request.getPrice(), instrument);
        var c = algo.getCommission(request.getPrice(), instrument);
        var total = request.getVolumn() * (m + c);
        var available = getAvailableMoney(properties);
        if (available < total) {
            throw new TraderException(ErrorCodes.INSUFFICIENT_MONEY.code(),
                                      ErrorCodes.INSUFFICIENT_MONEY.message());
        }
        for (int i = 0; i < request.getVolumn(); ++i) {
            setFrozenOpen(a, m, c, request);
        }
    }

    private void checkVolumn(Long v) throws TraderException {
        if (v == null) {
            throw new TraderException(ErrorCodes.VOLUMN_NULL.code(),
                                      ErrorCodes.VOLUMN_NULL.message());
        }
        if (v <= 0) {
            throw new TraderException(ErrorCodes.NONPOSITIVE_VOLUMN.code(),
                                      ErrorCodes.NONPOSITIVE_VOLUMN.message());
        }
    }

    private Integer findTraderIdByOrderId(long orderId) throws TraderException {
        var traderId = orderTraders.get(orderId);
        if (traderId == null) {
            throw new TraderException(ErrorCodes.ORDER_ID_NOT_FOUND.code(),
                                      ErrorCodes.ORDER_ID_NOT_FOUND.message() + "(Order ID:" + orderId + ")");
        }
        return traderId;
    }

    private TraderServiceRuntime findTraderRandomly(OrderRequest request) throws TraderException {
        var a = new ArrayList<>(traders.keySet());
        traders.forEach((k, v) -> {
            if (v.isEnabled()) {
                a.add(k);
            }
        });
        if (a.isEmpty()) {
            throw new TraderException(ErrorCodes.NO_TRADER.code(),
                                      ErrorCodes.NO_TRADER.message());
        }
        var traderId = a.get(new Random().nextInt(a.size()));
        orderTraders.put(request.getOrderId(), traderId);
        return traders.get(traderId);
    }

    private TraderServiceRuntime findTraderServiceRuntimeByTraderId(int traderId) throws TraderException {
        var rt = traders.get(traderId);
        check1(traderId, rt);
        return rt;
    }

    private void forwardRequest(OrderRequest request, int requestId) throws TraderException {
        var trader = getProperTrader(request).getTrader();
        if (trader == null) {
            throw new TraderException(ErrorCodes.TRADER_SERVICE_NULL.code(),
                                      ErrorCodes.TRADER_SERVICE_NULL.message());
        }
        trader.insert(request, requestId);
    }

    private void forwardRequest(CancelRequest request, int requestId) throws TraderException {
        var traderId = request.getTraderId();
        var tr = findTraderServiceRuntimeByTraderId(traderId);
        check1(traderId, tr);
        tr.getTrader().cancel(request, requestId);
    }

    private List<Contract> getAvailableContracts(OrderRequest request) throws TraderException {
        var cs = ds.getContractsByInstrumentId(request.getInstrumentId());
        if (cs == null) {
            throw new TraderException(ErrorCodes.CONTRACT_NULL.code(),
                                      ErrorCodes.CONTRACT_NULL.message());
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

    private TraderServiceRuntime getProperTrader(OrderRequest request) throws TraderException {
        var traderId = request.getTraderId();
        if (traderId == null) {
            return findTraderRandomly(request);
        }
        else {
            var rt = findTraderServiceRuntimeByTraderId(traderId);
            check1(traderId, rt);
            if (!rt.isEnabled()) {
                throw new TraderException(ErrorCodes.TRADER_NOT_ENABLED.code(),
                                          ErrorCodes.TRADER_NOT_ENABLED.message() + "(Trader ID:" + traderId + ")");
            }
            orderTraders.put(request.getOrderId(), traderId);
            return rt;
        }
    }

    private Account getSettledAccount(Properties properties)
            throws DataSourceException, TraderEngineAlgorithmException {
        return algo.getAccount(ds.getAccount(),
                               algo.getPositions(ds.getContracts(),
                                                 properties));
    }

    private void initAccount(Account a) throws TraderException {
        if (a == null) {
            throw new TraderException(ErrorCodes.ACCOUNT_NULL.code(),
                                      ErrorCodes.ACCOUNT_NULL.message());
        }
        callOnErasedAccount(a);

        a.setPreBalance(a.getBalance());
        a.setPreDeposit(a.getDeposit());
        a.setPreMargin(a.getMargin());
        a.setPreWithdraw(a.getWithdraw());
        a.setBalance(0.0D);
        a.setDeposit(0.0D);
        a.setMargin(0.0D);
        a.setWithdraw(0.0D);
        a.setTradingDay(ds.getTradingDay());

        ds.updateAccount(a);
    }

    private void initContracts(Collection<Contract> cs) throws TraderException {
        if (cs == null) {
            throw new TraderException(ErrorCodes.CONTRACT_NULL.code(),
                                      ErrorCodes.CONTRACT_NULL.message());
        }
        callOnErasedContracts(cs);
        for (var c : cs) {
            ds.removeContract(c.getContractId());
        }
    }

    private void setFrozenClose(double cm, Contract c, OrderType type) throws DataSourceException, TraderException {
        try {
            ds.transaction();
            /*
             * Update contracts status to make it frozen.
             */
            c.setStatus(ContractStatus.CLOSING);
            ds.updateContract(c);
            /*
             * Add new commission for the current order, and make it frozen
             * before order is filled.
             */
            var cms = new Commission();
            cms.setCommission(cm);
            cms.setCommissionId(Utils.getId());
            cms.setContractId(c.getContractId());
            cms.setStatus(FeeStatus.FORZEN);
            cms.setTradingDay(ds.getTradingDay());
            cms.setType(type);
            ds.addCommission(cms);
            /*
             * Commit change.
             */
            ds.commit();
        }
        catch (DataSourceException e) {
            /*
             * Rollback data source.
             */
            ds.rollback();
            throw e;
        }
    }

    private void setFrozenOpen(double amount,
                               double margin,
                               double commission,
                               OrderRequest request) throws DataSourceException {
        try {
            ds.transaction();
            /*
             * Add preparing contract.
             */
            var ctr = new Contract();
            ctr.setContractId(Utils.getId());
            ctr.setInstrumentId(request.getInstrumentId());
            ctr.setOpenAmount(amount);
            ctr.setOpenTradingDay(ds.getTradingDay());
            ctr.setOpenType(request.getType());
            ctr.setStatus(ContractStatus.OPENING);
            ds.addContract(ctr);
            /*
             * Add frozen margin.
             */
            var cmn = new Commission();
            cmn.setCommission(commission);
            cmn.setCommissionId(Utils.getId());
            cmn.setContractId(ctr.getContractId());
            cmn.setOrderId(request.getOrderId());
            cmn.setStatus(FeeStatus.FORZEN);
            cmn.setTradingDay(ds.getTradingDay());
            cmn.setType(request.getType());
            ds.addCommission(cmn);
            /*
             * Add frozen commission.
             */
            var mn = new Margin();
            mn.setContractId(Utils.getId());
            mn.setMargin(margin);
            mn.setMarginId(Utils.getId());
            mn.setOrderId(request.getOrderId());
            mn.setStatus(FeeStatus.FORZEN);
            mn.setTradingDay(ds.getTradingDay());
            mn.setType(request.getType());
            ds.addMargin(mn);
            /*
             * Commit change.
             */
            ds.commit();
        }
        catch (DataSourceException e) {
            /*
             * Rollback on exception.
             */
            ds.rollback();
            throw e;
        }
    }

    private void settle(IDataSource ds, ITraderEngineAlgorithm algo) throws TraderException {
        var rs = ds.getOrderRequests();
        if (rs == null) {
            throw new TraderException(ErrorCodes.ORDER_REQS_NULL.code(),
                                      ErrorCodes.ORDER_REQS_NULL.message());
        }
        for (var r : rs) {
            var rsps = ds.getOrderResponseByOrderId(r.getOrderId());
            if (rsps == null) {
                throw new TraderException(ErrorCodes.ORDER_RSPS_NULL.code(),
                                          ErrorCodes.ORDER_RSPS_NULL.message());

            }
            var o = algo.getOrder(r, rsps);
            var s = o.getStatus();
            if (s == OrderStatus.ACCEPTED
                || s == OrderStatus.ACCEPTED_REMOTE
                || s == OrderStatus.PART_TRADED_INQUE
                || s == OrderStatus.PART_TRADED_NOQUE) {
                cancelOrderRequest(r);
            }
        }
        // Clear everyday to avoid mem leak.
        orderTraders.clear();
    }

    private void startEach(Integer key, TraderServiceRuntime info) throws TraderException {
        check1(key, info);
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
            info.setHandler(new TraderServiceHandler(info));
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
