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
import com.open9am.service.CancelRequest;
import com.open9am.service.CancelResponse;
import com.open9am.service.Commission;
import com.open9am.service.Contract;
import com.open9am.service.ContractStatus;
import com.open9am.service.Deposit;
import com.open9am.service.FeeStatus;
import com.open9am.service.Instrument;
import com.open9am.service.Margin;
import com.open9am.service.OrderRequest;
import com.open9am.service.OrderResponse;
import com.open9am.service.Tick;
import com.open9am.service.Withdraw;
import java.time.LocalDate;
import java.util.Collection;

/**
 * Data connection.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface IDataConnection {

    Collection<Margin> getMarginsByStatus(FeeStatus status) throws DataSourceException;

    Collection<Margin> getMarginsByOrderId(long orderId) throws DataSourceException;

    Collection<Margin> getMargins() throws DataSourceException;

    Margin getMarginById(Long marginId) throws DataSourceException;

    void addMargin(Margin margin) throws DataSourceException;

    void removeMargin(long marginId) throws DataSourceException;

    void updateMargin(Margin margin) throws DataSourceException;

    Collection<Commission> getCommissionsByStatus(FeeStatus status) throws DataSourceException;

    Collection<Commission> getCommissionsByOrderId(long orderId) throws DataSourceException;

    Collection<Commission> getCommissions() throws DataSourceException;

    Commission getCommissionById(Long commissionId) throws DataSourceException;

    void addCommission(Commission commission) throws DataSourceException;

    void removeCommission(long commissionId) throws DataSourceException;

    void updateCommission(Commission commission) throws DataSourceException;

    Collection<Contract> getContractsByStatus(ContractStatus status) throws DataSourceException;

    Contract getContractById(Long contractId) throws DataSourceException;

    Collection<Contract> getContractsByResponseId(long responseId) throws DataSourceException;

    Collection<Contract> getContractsByInstrumentId(String instrumentId) throws DataSourceException;

    Collection<Contract> getContracts() throws DataSourceException;

    void addContract(Contract contract) throws DataSourceException;

    void removeContract(long contractId) throws DataSourceException;

    void updateContract(Contract contract) throws DataSourceException;

    Account getAccount() throws DataSourceException;

    void updateAccount(Account account) throws DataSourceException;

    Collection<Deposit> getDeposits() throws DataSourceException;

    void addDeposit(Deposit deposit) throws DataSourceException;

    Collection<Withdraw> getWithdraws() throws DataSourceException;

    void addWithdraw(Withdraw withdraw) throws DataSourceException;

    Collection<Instrument> getInstrumentsByExchangeId(String exchangeId) throws DataSourceException;

    Instrument getInstrumentById(String instrumentId) throws DataSourceException;

    void addInstrument(Instrument instrument) throws DataSourceException;

    void removeInstrument(String instrumentId) throws DataSourceException;

    void updateInstrument(Instrument instrument) throws DataSourceException;

    Collection<OrderRequest> getOrderRequests() throws DataSourceException;

    OrderRequest getOrderRequestById(long orderId) throws DataSourceException;

    void addOrderRequest(OrderRequest request) throws DataSourceException;

    Collection<CancelRequest> getCancelRequests() throws DataSourceException;

    CancelRequest getCancelRequestByOrderId(long orderId) throws DataSourceException;

    void addCancelRequest(CancelRequest request) throws DataSourceException;

    Collection<OrderResponse> getOrderResponses() throws DataSourceException;

    Collection<OrderResponse> getOrderResponsesByOrderId(long orderId) throws DataSourceException;

    void addOrderResponse(OrderResponse response) throws DataSourceException;

    Collection<CancelResponse> getCancelResponses() throws DataSourceException;

    Collection<CancelResponse> getCancelResponseByOrderId(long orderId) throws DataSourceException;

    void addCancelResponse(CancelResponse response) throws DataSourceException;

    Tick getTickByInstrumentId(String instrumentId) throws DataSourceException;

    void addTick(Tick tick) throws DataSourceException;

    void removeTick(String instrumentId) throws DataSourceException;

    void updateTick(Tick tick) throws DataSourceException;

    LocalDate getTradingDay() throws DataSourceException;

    void setTradingDay(LocalDate day);

    void transaction() throws DataSourceException;

    void commit() throws DataSourceException;

    void rollback() throws DataSourceException;
}
