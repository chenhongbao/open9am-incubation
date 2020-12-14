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

import com.df.proxier.CancelRequest;
import com.df.proxier.CancelResponse;
import com.df.proxier.Commission;
import com.df.proxier.Contract;
import com.df.proxier.ContractStatus;
import com.df.proxier.FeeStatus;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.OrderRequest;
import com.df.proxier.OrderResponse;
import com.df.proxier.OrderType;
import com.df.proxier.service.ITraderServiceHandler;
import com.df.proxier.service.TraderException;
import com.df.proxier.service.TraderRuntimeException;
import com.df.proxier.utils.Utils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

/**
 * Implementation of service handler to process responses.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class TraderServiceHandler extends IdTranslator implements ITraderServiceHandler {

    private final TraderServiceRuntime info;

    public TraderServiceHandler(TraderServiceRuntime info) {
        this.info = info;
    }

    @Override
    public void onCancelResponse(CancelResponse response) {
        IDataConnection conn = null;
        try {
            preprocess(response);
            /*
             * Get data source and start transaction.
             */
            conn = getDataSource().getConnection();
            conn.transaction();
            /*
             * Add cancel response.
             */
            conn.addCancelResponse(response);
            var o = conn.getOrderRequestById(response.getOrderId());
            if (o == null) {
                throw new TraderRuntimeException(ExceptionCodes.ORDER_ID_NOT_FOUND.code(),
                                                 ExceptionCodes.ORDER_ID_NOT_FOUND.message());
            }
            var type = o.getType();
            if (type == OrderType.BUY_OPEN || type == OrderType.SELL_OPEN) {
                /*
                 * Cancel opening order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), conn);
                for (var b : bs) {
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.OPENING) {
                        continue;
                    }
                    cancelOpen(b.getCommission(),
                               b.getMargin(),
                               b.getContract(),
                               conn);
                }
            }
            else {
                /*
                 * Cancel closing order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), conn);
                for (var b : bs) {
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.CLOSING) {
                        continue;
                    }
                    cancelClose(b.getCommission(),
                                b.getContract(),
                                conn);
                }
            }
            conn.commit();
        }
        catch (TraderException e) {
            if (conn != null) {
                rollback(conn);
            }
            callOnException(new TraderRuntimeException(e.getCode(),
                                                       e.getMessage(),
                                                       e));
        }
        catch (TraderRuntimeException e) {
            if (conn != null) {
                rollback(conn);
            }
            callOnException(e);
        }
        callOnCanceResponse(response);
    }

    @Override

    public void onException(TraderRuntimeException exception) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onException(exception);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    @Override
    public void onException(OrderRequest request,
                            TraderRuntimeException exception,
                            int requestId) {
        try {
            /*
             * Call cancel handler to cancel a bad request.
             */
            var cancel = initCancelResponse(request);
            cancel.setStatusCode(exception.getCode());
            cancel.setStatusMessage(exception.getMessage());
            onCancelResponse(cancel);
            /*
             * Call user handler.
             */
            callOnOrderException(request,
                                 exception,
                                 requestId);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                       ExceptionCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    @Override

    public void onException(CancelRequest request,
                            TraderRuntimeException exception,
                            int requestId) {
        callOnCancelException(request,
                              exception,
                              requestId);
        /*
         * Cancel failed, the order status is not changed.
         */
    }

    @Override
    public void onOrderReponse(OrderResponse response) {
        IDataConnection conn = null;
        try {
            preprocess(response);
            /*
             * Get data source and start transaction.
             */
            conn = getDataSource().getConnection();
            conn.transaction();
            /*
             * Add order response. Please note that volumn in response could be
             * zero, notifying a status change of the inserted order request.
             */
            conn.addOrderResponse(response);
            var type = response.getType();
            if (type == OrderType.BUY_OPEN || type == OrderType.SELL_OPEN) {
                /*
                 * Deal opening order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), conn);
                int count = 0;
                var it = bs.iterator();
                while (count < response.getVolumn() && it.hasNext()) {
                    var b = it.next();
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.OPENING) {
                        continue;
                    }
                    dealOpen(b.getCommission(),
                             b.getMargin(),
                             b.getContract(),
                             response,
                             conn
                    );
                    ++count;
                }
                if (count < response.getVolumn()) {
                    throw new TraderRuntimeException(ExceptionCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                     ExceptionCodes.INCONSISTENT_FROZEN_INFO.message());
                }
            }
            else {
                /*
                 * Deal closing order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), conn);
                int count = 0;
                var it = bs.iterator();
                while (count < response.getVolumn() && it.hasNext()) {
                    var b = it.next();
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.CLOSING) {
                        continue;
                    }
                    dealClose(b.getCommission(),
                              b.getMargin(),
                              b.getContract(),
                              response,
                              conn);
                    ++count;
                }
                if (count < response.getVolumn()) {
                    throw new TraderRuntimeException(ExceptionCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                     ExceptionCodes.INCONSISTENT_FROZEN_INFO.message());
                }
            }
            conn.commit();
        }
        catch (TraderException e) {
            if (conn != null) {
                rollback(conn);
            }
            callOnException(new TraderRuntimeException(e.getCode(),
                                                       e.getMessage(),
                                                       e));
        }
        catch (TraderRuntimeException e) {
            if (conn != null) {
                rollback(conn);
            }
            callOnException(e);
        }
        callOnOrderResponse(response);
    }

    @Override
    public void onStatusChange(int status) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onTraderServiceStatusChange(status);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    private void callOnCanceResponse(CancelResponse response) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onCancelResponse(response);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    private void callOnCancelException(CancelRequest request,
                                       TraderRuntimeException exception,
                                       int requestId) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onException(request,
                              exception,
                              requestId);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    private void callOnException(TraderRuntimeException e) {
        try {
            onException(e);
        }
        catch (Throwable ignored) {
        }
    }

    private void callOnOrderException(OrderRequest request,
                                      TraderRuntimeException exception,
                                      int requestId) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onException(request,
                              exception,
                              requestId);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    private void callOnOrderResponse(OrderResponse response) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onOrderReponse(response);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    private void cancelClose(Commission commission,
                             Contract contract,
                             IDataConnection conn) throws DataSourceException {
        requireStatus(contract, ContractStatus.CLOSING);
        contract.setStatus(ContractStatus.OPEN);
        conn.updateContract(contract);
        conn.removeCommission(commission.getCommissionId());
    }

    private void cancelOpen(Commission commission,
                            Margin margin,
                            Contract contract,
                            IDataConnection conn) throws DataSourceException {
        requireStatus(commission, FeeStatus.FORZEN);
        requireStatus(margin, FeeStatus.FORZEN);
        requireStatus(contract, ContractStatus.OPENING);
        conn.removeContract(contract.getContractId());
        conn.removeCommission(commission.getCommissionId());
        conn.removeMargin(margin.getMarginId());

    }

    private void checkCommissionsNull(Collection<Commission> cs) {
        if (cs == null) {
            throw new TraderRuntimeException(ExceptionCodes.COMMISSION_NULL.code(),
                                             ExceptionCodes.COMMISSION_NULL.message());
        }
    }

    private void checkContractIdNull(Long cid) {
        if (cid == null) {
            throw new TraderRuntimeException(ExceptionCodes.CONTRACT_ID_NULL.code(),
                                             ExceptionCodes.CONTRACT_ID_NULL.message());
        }
    }

    private void checkContractNull(Contract cc) {
        if (cc == null) {
            throw new TraderRuntimeException(ExceptionCodes.CONTRACT_NULL.code(),
                                             ExceptionCodes.CONTRACT_NULL.message());
        }
    }

    private void checkFrozenInfo(Collection<FrozenBundle> bs) {
        bs.forEach(v -> {
            if (v.getCommission() == null || v.getContract() == null || v.getMargin() == null) {
                throw new TraderRuntimeException(ExceptionCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                 ExceptionCodes.INCONSISTENT_FROZEN_INFO.message());
            }
        });
    }

    private void checkMarginNull(Margin n) {
        if (n == null) {
            throw new TraderRuntimeException(ExceptionCodes.MARGIN_NULL.code(),
                                             ExceptionCodes.MARGIN_NULL.message());
        }
    }

    private void checkMarginsNull(Collection<Margin> cs) {
        if (cs == null) {
            throw new TraderRuntimeException(ExceptionCodes.MARGIN_NULL.code(),
                                             ExceptionCodes.MARGIN_NULL.message());
        }
    }

    private void dealClose(Commission commission,
                           Margin margin,
                           Contract contract,
                           OrderResponse response,
                           IDataConnection conn) throws TraderException {
        requireStatus(commission, FeeStatus.FORZEN);
        requireStatus(margin, FeeStatus.DEALED);
        requireStatus(contract, ContractStatus.CLOSING);
        /*
         * Update commission.
         */
        commission.setStatus(FeeStatus.DEALED);
        conn.updateCommission(commission);
        /*
         * Update margin.
         */
        margin.setStatus(FeeStatus.REMOVED);
        conn.updateMargin(margin);
        /*
         * Update contract.
         */
        var price = response.getPrice();
        var instrument = getInstrument(response.getInstrumentId());
        var amount = info.getEngine().getAlgorithm().getAmount(price, instrument);
        contract.setCloseAmount(amount);
        contract.setStatus(ContractStatus.CLOSED);
        conn.updateContract(contract);
    }

    private void dealOpen(Commission commission,
                          Margin margin,
                          Contract contract,
                          OrderResponse response,
                          IDataConnection conn) throws TraderException {
        requireStatus(commission, FeeStatus.FORZEN);
        requireStatus(margin, FeeStatus.FORZEN);
        requireStatus(contract, ContractStatus.OPENING);
        /*
         * Update commission.
         */
        commission.setStatus(FeeStatus.DEALED);
        conn.updateCommission(commission);
        /*
         * Update margin.
         */
        margin.setStatus(FeeStatus.DEALED);
        conn.updateMargin(margin);
        /*
         * Update contract.
         */
        var price = response.getPrice();
        var instrument = getInstrument(response.getInstrumentId());
        var amount = info.getEngine().getAlgorithm().getAmount(price, instrument);
        contract.setOpenAmount(amount);
        contract.setStatus(ContractStatus.OPEN);
        contract.setResponseId(response.getResponseId());
        contract.setOpenTimestamp(response.getTimestamp());
        contract.setOpenTradingDay(response.getTradingDay());
        conn.updateCommission(commission);
    }

    private IDataSource getDataSource() throws TraderException {
        var ds = info.getEngine().getDataSource();
        if (ds == null) {
            throw new TraderException(ExceptionCodes.DATASOURCE_NULL.code(),
                                      ExceptionCodes.DATASOURCE_NULL.message());
        }
        return ds;
    }

    private Collection<FrozenBundle> getFrozenBundles(Long orderId, IDataConnection conn) throws DataSourceException {
        final var map = new HashMap<Long, FrozenBundle>(128);
        var ms = conn.getMarginsByOrderId(orderId);
        checkMarginsNull(ms);
        var cs = conn.getCommissionsByOrderId(orderId);
        checkCommissionsNull(cs);
        for (var c : cs) {
            var cid = c.getContractId();
            checkContractIdNull(cid);
            var cc = conn.getContractById(cid);
            checkContractNull(cc);
            var m = getMarginByContractId(cid, ms);
            checkMarginNull(m);
            map.put(cid, new FrozenBundle(c, m, cc));
        }
        checkFrozenInfo(map.values());
        return map.values();
    }

    private Instrument getInstrument(String instrumentId) throws TraderException {
        var instrument = info.getEngine().getRelatedInstrument(instrumentId);
        if (instrument == null) {
            throw new TraderException(
                    ExceptionCodes.INSTRUMENT_NULL.code(),
                    ExceptionCodes.INSTRUMENT_NULL.message() + "(Instrument ID:"
                    + instrumentId + ")");
        }
        return instrument;
    }

    private Margin getMarginByContractId(Long contractId, Collection<Margin> ms) {
        for (var m : ms) {
            if (Objects.equals(m.getContractId(), contractId)) {
                return m;
            }
        }
        return null;
    }

    private Long getSrcId(Long destId) {
        if (destId == null) {
            throw new NullPointerException("Destinated ID null.");
        }
        var srcId = getSourceId(destId);
        if (srcId == null) {
            throw new NullPointerException("Source ID not found(Destinated ID:" + destId + ").");
        }
        return srcId;
    }

    private CancelResponse initCancelResponse(OrderRequest request) {
        var r = new CancelResponse();
        r.setInstrumentId(request.getInstrumentId());
        r.setOrderId(request.getOrderId());
        r.setTraderId(request.getTraderId());
        r.setTradingDay(info.getTrader().getServiceInfo().getTradingDay());
        r.setUuid(Utils.nextUuid().toString());
        return r;
    }

    private void preprocess(OrderResponse response) throws TraderException {
        try {
            /*
             * Order is canceled, so count down to zero.
             */
            super.countDown(response.getOrderId(), response.getVolumn());
            response.setOrderId(getSrcId(response.getOrderId()));
            response.setTraderId(info.getTraderId());
        }
        catch (Throwable th) {
            throw new TraderException(ExceptionCodes.PREPROC_RSPS_FAILED.code(),
                                      ExceptionCodes.PREPROC_RSPS_FAILED.message(),
                                      th);
        }
    }

    private void preprocess(CancelResponse response) throws TraderException {
        try {
            var rest = super.getDownCountByDestId(response.getOrderId());
            if (rest == null) {
                throw new NullPointerException("Count down not found(" + response.getOrderId() + ").");
            }
            /*
             * Order is canceled, so count down to zero.
             */
            super.countDown(response.getOrderId(), rest);
            response.setOrderId(getSrcId(response.getOrderId()));
            response.setTraderId(info.getTraderId());
        }
        catch (Throwable th) {
            throw new TraderException(ExceptionCodes.PREPROC_RSPS_FAILED.code(),
                                      ExceptionCodes.PREPROC_RSPS_FAILED.message(),
                                      th);
        }
    }

    private void requireStatus(Contract c, ContractStatus s) {
        if (!Objects.equals(c.getStatus(), s)) {
            throw new TraderRuntimeException(ExceptionCodes.INVALID_CANCELING_CONTRACT_STATUS.code(),
                                             ExceptionCodes.INVALID_CANCELING_CONTRACT_STATUS.message()
                                             + "(Contract ID:" + c.getContractId() + ")");
        }
    }

    private void requireStatus(Margin m, FeeStatus s) {
        if (!Objects.equals(m.getStatus(), s)) {
            throw new TraderRuntimeException(ExceptionCodes.INVALID_CANCELING_MARGIN_STATUS.code(),
                                             ExceptionCodes.INVALID_CANCELING_MARGIN_STATUS.message()
                                             + "(Contract ID:" + m.getMarginId() + ")");
        }
    }

    private void requireStatus(Commission c, FeeStatus s) {
        if (!Objects.equals(c.getStatus(), s)) {
            throw new TraderRuntimeException(ExceptionCodes.INVALID_CANCELING_MARGIN_STATUS.code(),
                                             ExceptionCodes.INVALID_CANCELING_MARGIN_STATUS.message()
                                             + "(Contract ID:" + c.getCommissionId() + ")");
        }
    }

    private void rollback(IDataConnection conn) {
        try {
            conn.rollback();
        }
        catch (DataSourceException ex) {
            callOnException(new TraderRuntimeException(
                    ExceptionCodes.DS_FAILURE_UNFIXABLE.code(),
                    ExceptionCodes.DS_FAILURE_UNFIXABLE.message(),
                    ex));
        }
    }

    private class FrozenBundle {

        private Commission commission;
        private final Contract contract;
        private final Margin margin;

        FrozenBundle(Commission commission, Margin margin, Contract contract) {
            this.commission = commission;
            this.margin = margin;
            this.contract = contract;
        }

        Commission getCommission() {
            return commission;
        }

        void setCommission(Commission commission) {
            this.commission = commission;
        }

        Contract getContract() {
            return contract;
        }

        Margin getMargin() {
            return margin;
        }

    }
}
