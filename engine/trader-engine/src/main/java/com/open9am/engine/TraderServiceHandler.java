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

import com.open9am.service.CancelRequest;
import com.open9am.service.CancelResponse;
import com.open9am.service.Commission;
import com.open9am.service.Contract;
import com.open9am.service.ContractStatus;
import com.open9am.service.FeeStatus;
import com.open9am.service.ITraderServiceHandler;
import com.open9am.service.Instrument;
import com.open9am.service.Margin;
import com.open9am.service.OrderRequest;
import com.open9am.service.OrderResponse;
import com.open9am.service.OrderType;
import com.open9am.service.TraderException;
import com.open9am.service.TraderRuntimeException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

/**
 * Implementation of service handler to process responses.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class TraderServiceHandler implements ITraderServiceHandler {

    private final TraderServiceRuntime info;

    public TraderServiceHandler(TraderServiceRuntime info) {
        this.info = info;
    }

    @Override
    public void OnCancelResponse(CancelResponse response) {
        IDataSource ds = null;
        try {
            ds = getDataSource();
            ds.transaction();
            /*
             * Add cancel response.
             */
            ds.addCancelResponse(response);
            var o = ds.getOrderRequestById(response.getOrderId());
            if (o == null) {
                throw new TraderRuntimeException(ErrorCodes.ORDER_ID_NOT_FOUND.code(),
                                                 ErrorCodes.ORDER_ID_NOT_FOUND.message());
            }
            var type = o.getType();
            if (type == OrderType.BUY_OPEN || type == OrderType.SELL_OPEN) {
                /*
                 * Cancel opening order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), ds);
                for (var b : bs) {
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.OPENING) {
                        continue;
                    }
                    cancelOpen(b.getCommission(),
                               b.getMargin(),
                               b.getContract(),
                               ds);
                }
            }
            else {
                /*
                 * Cancel closing order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), ds);
                for (var b : bs) {
                    var s = b.getContract().getStatus();
                    if (s != ContractStatus.CLOSING) {
                        continue;
                    }
                    cancelClose(b.getCommission(),
                                b.getContract(),
                                ds);
                }
            }
            ds.commit();
        }
        catch (TraderException e) {
            if (ds != null) {
                rollback(ds);
            }
            callOnException(new TraderRuntimeException(e.getCode(),
                                                       e.getMessage(),
                                                       e));
        }
        catch (TraderRuntimeException e) {
            if (ds != null) {
                rollback(ds);
            }
            callOnException(e);
        }
        try {
            info.getEngine().getHandler().OnCancelResponse(response);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    @Override
    public void OnException(TraderRuntimeException exception) {
        try {
            info.getEngine().getHandler().OnException(exception);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    @Override
    public void OnException(OrderRequest request,
                            TraderRuntimeException exception,
                            int requestId) {
        try {
            info.getEngine().getHandler().OnException(request,
                                                      exception,
                                                      requestId);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    @Override
    public void OnException(CancelRequest request, TraderRuntimeException exception, int requestId) {
        try {
            info.getEngine().getHandler().OnException(request,
                                                      exception,
                                                      requestId);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    @Override
    public void OnOrderReponse(OrderResponse response) {
        IDataSource ds = null;
        try {
            ds = getDataSource();
            ds.transaction();
            /*
             * Add order response.
             */
            ds.addOrderResponse(response);
            var type = response.getType();
            if (type == OrderType.BUY_OPEN || type == OrderType.SELL_OPEN) {
                /*
                 * Deal opening order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), ds);
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
                             ds
                    );
                    ++count;
                }
                if (count < response.getVolumn()) {
                    throw new TraderRuntimeException(ErrorCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                     ErrorCodes.INCONSISTENT_FROZEN_INFO.message());
                }
            }
            else {
                /*
                 * Deal closing order.
                 */
                var bs = getFrozenBundles(response.getOrderId(), ds);
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
                              ds);
                    ++count;
                }
                if (count < response.getVolumn()) {
                    throw new TraderRuntimeException(ErrorCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                     ErrorCodes.INCONSISTENT_FROZEN_INFO.message());
                }
            }
            ds.commit();
        }
        catch (TraderException e) {
            if (ds != null) {
                rollback(ds);
            }
            callOnException(new TraderRuntimeException(e.getCode(),
                                                       e.getMessage(),
                                                       e));
        }
        catch (TraderRuntimeException e) {
            if (ds != null) {
                rollback(ds);
            }
            callOnException(e);
        }
        try {
            info.getEngine().getHandler().OnOrderReponse(response);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    @Override
    public void OnStatusChange(int status) {
        Loggers.get().debug("Trader service status: {}.", status);
        try {
            info.getEngine().getHandler().OnTraderServiceStatusChange(status);
        }
        catch (Throwable th) {
            callOnException(new TraderRuntimeException(ErrorCodes.USER_CODE_ERROR.code(),
                                                       ErrorCodes.USER_CODE_ERROR.message(),
                                                       th));
        }
    }

    private void callOnException(TraderRuntimeException e) {
        try {
            OnException(e);
        }
        catch (Throwable th) {
            Loggers.get().error("Calling OnExcepion() throws exception.", th);
        }
    }

    private void cancelClose(Commission commission,
                             Contract contract,
                             IDataSource ds) throws DataSourceException {
        requireStatus(contract, ContractStatus.CLOSING);
        contract.setStatus(ContractStatus.OPEN);
        ds.updateContract(contract);
        ds.removeCommission(commission.getCommissionId());
    }

    private void cancelOpen(Commission commission,
                            Margin margin,
                            Contract contract,
                            IDataSource ds) throws DataSourceException {
        requireStatus(commission, FeeStatus.FORZEN);
        requireStatus(margin, FeeStatus.FORZEN);
        requireStatus(contract, ContractStatus.OPENING);
        ds.removeContract(contract.getContractId());
        ds.removeCommission(commission.getCommissionId());
        ds.removeMargin(margin.getMarginId());

    }

    private void checkCommissionsNull(Collection<Commission> cs) {
        if (cs == null) {
            throw new TraderRuntimeException(ErrorCodes.COMMISSION_NULL.code(),
                                             ErrorCodes.COMMISSION_NULL.message());
        }
    }

    private void checkContractIdNull(Long cid) {
        if (cid == null) {
            throw new TraderRuntimeException(ErrorCodes.CONTRACT_ID_NULL.code(),
                                             ErrorCodes.CONTRACT_ID_NULL.message());
        }
    }

    private void checkContractNull(Contract cc) {
        if (cc == null) {
            throw new TraderRuntimeException(ErrorCodes.CONTRACT_NULL.code(),
                                             ErrorCodes.CONTRACT_NULL.message());
        }
    }

    private void checkFrozenInfo(Collection<FrozenBundle> bs) {
        bs.forEach(v -> {
            if (v.getCommission() == null || v.getContract() == null || v.getMargin() == null) {
                throw new TraderRuntimeException(ErrorCodes.INCONSISTENT_FROZEN_INFO.code(),
                                                 ErrorCodes.INCONSISTENT_FROZEN_INFO.message());
            }
        });
    }

    private void checkMarginNull(Margin n) {
        if (n == null) {
            throw new TraderRuntimeException(ErrorCodes.MARGIN_NULL.code(),
                                             ErrorCodes.MARGIN_NULL.message());
        }
    }

    private void checkMarginsNull(Collection<Margin> cs) {
        if (cs == null) {
            throw new TraderRuntimeException(ErrorCodes.MARGIN_NULL.code(),
                                             ErrorCodes.MARGIN_NULL.message());
        }
    }

    private void dealClose(Commission commission,
                           Margin margin,
                           Contract contract,
                           OrderResponse response,
                           IDataSource ds) throws TraderException {
        requireStatus(commission, FeeStatus.FORZEN);
        requireStatus(margin, FeeStatus.DEALED);
        requireStatus(contract, ContractStatus.CLOSING);
        /*
         * Update commission.
         */
        commission.setStatus(FeeStatus.DEALED);
        ds.updateCommission(commission);
        /*
         * Remove margin.
         */
        ds.removeMargin(margin.getMarginId());
        /*
         * Update contract.
         */
        var price = response.getPrice();
        var instrument = getInstrument(response.getInstrumentId());
        var amount = info.getEngine().getAlgorithm().getAmount(price, instrument);
        contract.setCloseAmount(amount);
        contract.setStatus(ContractStatus.CLOSED);
        ds.updateContract(contract);
    }

    private void dealOpen(Commission commission,
                          Margin margin,
                          Contract contract,
                          OrderResponse response,
                          IDataSource ds) throws TraderException {
        requireStatus(commission, FeeStatus.FORZEN);
        requireStatus(margin, FeeStatus.FORZEN);
        requireStatus(contract, ContractStatus.OPENING);
        /*
         * Update commission.
         */
        commission.setStatus(FeeStatus.DEALED);
        ds.updateCommission(commission);
        /*
         * Update margin.
         */
        margin.setStatus(FeeStatus.DEALED);
        ds.updateMargin(margin);
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
        ds.updateCommission(commission);
    }

    private IDataSource getDataSource() throws TraderException {
        var ds = info.getEngine().getDataSource();
        if (ds == null) {
            throw new TraderException(ErrorCodes.DATASOURCE_NULL.code(),
                                      ErrorCodes.DATASOURCE_NULL.message());
        }
        return ds;
    }

    private Collection<FrozenBundle> getFrozenBundles(Long orderId, IDataSource ds) throws DataSourceException {
        final var map = new HashMap<Long, FrozenBundle>(128);
        var ms = ds.getMarginsByOrderId(orderId);
        checkMarginsNull(ms);
        var cs = ds.getCommissionsByOrderId(orderId);
        checkCommissionsNull(cs);
        for (var c : cs) {
            var cid = c.getContractId();
            checkContractIdNull(cid);
            var cc = ds.getContractById(cid);
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
                    ErrorCodes.INSTRUMENT_NULL.code(),
                    ErrorCodes.INSTRUMENT_NULL.message() + "(Instrument ID:"
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

    private void requireStatus(Contract c, ContractStatus s) {
        if (!Objects.equals(c.getStatus(), s)) {
            throw new TraderRuntimeException(ErrorCodes.INVALID_CANCELING_CONTRACT_STATUS.code(),
                                             ErrorCodes.INVALID_CANCELING_CONTRACT_STATUS.message()
                                             + "(Contract ID:" + c.getContractId() + ")");
        }
    }

    private void requireStatus(Margin m, FeeStatus s) {
        if (!Objects.equals(m.getStatus(), s)) {
            throw new TraderRuntimeException(ErrorCodes.INVALID_CANCELING_MARGIN_STATUS.code(),
                                             ErrorCodes.INVALID_CANCELING_MARGIN_STATUS.message()
                                             + "(Contract ID:" + m.getMarginId() + ")");
        }
    }

    private void requireStatus(Commission c, FeeStatus s) {
        if (!Objects.equals(c.getStatus(), s)) {
            throw new TraderRuntimeException(ErrorCodes.INVALID_CANCELING_MARGIN_STATUS.code(),
                                             ErrorCodes.INVALID_CANCELING_MARGIN_STATUS.message()
                                             + "(Contract ID:" + c.getCommissionId() + ")");
        }
    }

    private void rollback(IDataSource ds) {
        try {
            ds.rollback();
        }
        catch (DataSourceException ex) {
            callOnException(new TraderRuntimeException(
                    ErrorCodes.DS_FAILURE_UNFIXABLE.code(),
                    ErrorCodes.DS_FAILURE_UNFIXABLE.message(),
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
