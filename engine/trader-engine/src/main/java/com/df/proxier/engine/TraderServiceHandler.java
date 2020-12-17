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

import com.df.proxier.ActionType;
import com.df.proxier.Commission;
import com.df.proxier.Contract;
import com.df.proxier.ContractStatus;
import com.df.proxier.FeeStatus;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Offset;
import com.df.proxier.Request;
import com.df.proxier.Response;
import com.df.proxier.Trade;
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
    public void onException(Request request,
                            TraderRuntimeException exception,
                            int requestId) {
        if (request.getAction() == ActionType.DELETE) {
            callOnDeleteException(request,
                                  exception,
                                  requestId);
            /*
             * Delete action fails, so order is unchanged.
             */
        }
        else {
            try {
                /*
                 * Call cancel handler to cancel a bad request.
                 */
                var delete = initResponse(request);
                delete.setAction(ActionType.DELETE);
                delete.setStatusCode(exception.getCode());
                delete.setStatusMessage(exception.getMessage());
                onResponse(delete);
                /*
                 * Call user handler.
                 */
                callOnRequestException(request,
                                       exception,
                                       requestId);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        }
    }

    @Override
    public void onResponse(Response response) {
        try {
            info.getEngine().getDataSource().getConnection().addResponse(response);
        }
        catch (DataSourceException ex) {
            callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                       "Fail saving response to data source.",
                                                       ex));
        }
        if (response.getAction() == ActionType.DELETE) {
            deleteOrder(response);
        }
        callOnResponse(response);
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

    @Override
    public void onTrade(Trade trade) {
        IDataConnection conn = null;
        try {
            preprocess(trade);
            /*
             * Get data source and start transaction.
             */
            conn = getDataSource().getConnection();
            conn.transaction();
            /*
             * Add trade. Please note that volumn in trade could be zero,
             * notifying a status change of the inserted order request.
             */
            conn.addTrade(trade);
            var offset = trade.getOffset();
            if (offset == null) {
                throw new TraderRuntimeException(ExceptionCodes.OFFSET_NULL.code(),
                                                 ExceptionCodes.OFFSET_NULL.message());
            }
            if (Offset.OPEN == offset) {
                /*
                 * Deal opening order.
                 */
                var bs = getFrozenBundles(trade.getOrderId(), conn);
                int count = 0;
                var it = bs.iterator();
                while (count < trade.getQuantity() && it.hasNext()) {
                    var b = it.next();
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.OPENING) {
                        continue;
                    }
                    dealOpen(b.getCommission(),
                             b.getMargin(),
                             b.getContract(),
                             trade,
                             conn
                    );
                    ++count;
                }
                if (count < trade.getQuantity()) {
                    throw new TraderRuntimeException(ExceptionCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                     ExceptionCodes.INCONSISTENT_FROZEN_INFO.message());
                }
            }
            else {
                /*
                 * Deal closing order.
                 */
                var bs = getFrozenBundles(trade.getOrderId(), conn);
                int count = 0;
                var it = bs.iterator();
                while (count < trade.getQuantity() && it.hasNext()) {
                    var b = it.next();
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.CLOSING) {
                        continue;
                    }
                    dealClose(b.getCommission(),
                              b.getMargin(),
                              b.getContract(),
                              trade,
                              conn);
                    ++count;
                }
                if (count < trade.getQuantity()) {
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
        callOnTrade(trade);
    }

    private void callOnDeleteException(Request request,
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

    private void callOnRequestException(Request request,
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

    private void callOnResponse(Response response) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onResponse(response);
            }
            catch (Throwable th) {
                callOnException(new TraderRuntimeException(ExceptionCodes.USER_CODE_ERROR.code(),
                                                           ExceptionCodes.USER_CODE_ERROR.message(),
                                                           th));
            }
        });
    }

    private void callOnTrade(Trade trade) {
        var handlers = info.getEngine().handlers();
        if (handlers.isEmpty()) {
            return;
        }
        handlers.parallelStream().forEach(h -> {
            try {
                h.onTrade(trade);
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
                           Trade response,
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
                          Trade trade,
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
        var price = trade.getPrice();
        var instrument = getInstrument(trade.getInstrumentId());
        var amount = info.getEngine().getAlgorithm().getAmount(price, instrument);
        contract.setOpenAmount(amount);
        contract.setStatus(ContractStatus.OPEN);
        contract.setTradeId(trade.getTradeId());
        contract.setOpenTimestamp(trade.getTimestamp());
        contract.setOpenTradingDay(trade.getTradingDay());
        conn.updateCommission(commission);
    }

    private void deleteOrder(Response response) {
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
            conn.addResponse(response);
            var o = conn.getRequestByOrderId(response.getOrderId());
            if (o == null) {
                throw new TraderRuntimeException(ExceptionCodes.ORDER_ID_NOT_FOUND.code(),
                                                 ExceptionCodes.ORDER_ID_NOT_FOUND.message());
            }
            var offset = o.getOffset();
            if (offset == null) {
                throw new TraderRuntimeException(ExceptionCodes.OFFSET_NULL.code(),
                                                 ExceptionCodes.OFFSET_NULL.message());
            }
            if (offset == Offset.OPEN) {
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

    private Response initResponse(Request request) {
        var r = new Response();
        r.setInstrumentId(request.getInstrumentId());
        r.setOrderId(request.getOrderId());
        r.setTraderId(request.getTraderId());
        r.setTradingDay(info.getTrader().getServiceInfo().getTradingDay());
        r.setUuid(Utils.nextUuid().toString());
        return r;
    }

    private void preprocess(Trade trade) throws TraderException {
        try {
            /*
             * Order is deleted, so count down to zero.
             */
            super.countDown(trade.getOrderId(), trade.getQuantity());
            trade.setOrderId(getSrcId(trade.getOrderId()));
            trade.setTraderId(info.getTraderId());
        }
        catch (Throwable th) {
            throw new TraderException(ExceptionCodes.PREPROC_RSPS_FAILED.code(),
                                      ExceptionCodes.PREPROC_RSPS_FAILED.message(),
                                      th);
        }
    }

    private void preprocess(Response response) throws TraderException {
        try {
            var rest = super.getDownCountByDestId(response.getOrderId());
            if (rest == null) {
                throw new NullPointerException("Count down not found(" + response.getOrderId() + ").");
            }
            /*
             * Order is deleted, so count down to zero.
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
            throw new TraderRuntimeException(ExceptionCodes.INVALID_DELETING_CONTRACT_STATUS.code(),
                                             ExceptionCodes.INVALID_DELETING_CONTRACT_STATUS.message()
                                             + "(Contract ID:" + c.getContractId() + ")");
        }
    }

    private void requireStatus(Margin m, FeeStatus s) {
        if (!Objects.equals(m.getStatus(), s)) {
            throw new TraderRuntimeException(ExceptionCodes.INVALID_DELETING_MARGIN_STATUS.code(),
                                             ExceptionCodes.INVALID_DELETING_MARGIN_STATUS.message()
                                             + "(Contract ID:" + m.getMarginId() + ")");
        }
    }

    private void requireStatus(Commission c, FeeStatus s) {
        if (!Objects.equals(c.getStatus(), s)) {
            throw new TraderRuntimeException(ExceptionCodes.INVALID_DELETING_COMMISSION_STATUS.code(),
                                             ExceptionCodes.INVALID_DELETING_COMMISSION_STATUS.message()
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
