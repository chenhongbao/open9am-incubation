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
import com.df.proxier.Commission;
import com.df.proxier.Contract;
import com.df.proxier.ContractStatus;
import com.df.proxier.Deposit;
import com.df.proxier.FeeStatus;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Request;
import com.df.proxier.Response;
import com.df.proxier.Tick;
import com.df.proxier.Trade;
import com.df.proxier.Withdraw;
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

    Collection<Contract> getContractsByTradeId(long tradeId) throws DataSourceException;

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

    Collection<Request> getRequests() throws DataSourceException;

    Request getRequestByOrderId(long orderId) throws DataSourceException;

    void addRequest(Request request) throws DataSourceException;

    Collection<Trade> getTrades() throws DataSourceException;

    Collection<Trade> getTradesByOrderId(long orderId) throws DataSourceException;

    Trade getTradeById(Long tradeId) throws DataSourceException;

    void addTrade(Trade trade) throws DataSourceException;

    Collection<Response> getResponses() throws DataSourceException;

    Collection<Response> getResponseByOrderId(long orderId) throws DataSourceException;

    Response getResponseById(long responseId) throws DataSourceException;

    void addResponse(Response response) throws DataSourceException;

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
