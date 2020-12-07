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
import com.open9am.service.Withdraw;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;

/**
 * Trader engine's data connection.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class TraderEngineDataConnection implements IDataConnection {

    private final Connection conn;

    private Boolean exAutoCommit;

    public TraderEngineDataConnection(Connection connection) throws DataSourceException {
        if (connection == null) {
            throw new DataSourceException(ExceptionCodes.DATA_CONNECTION_NULL.code(),
                                          ExceptionCodes.DATA_CONNECTION_NULL.message());
        }
        conn = connection;
    }

    @Override
    public void addCancelRequest(CancelRequest request) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addCancelResponse(CancelResponse response) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addCommission(Commission commission) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addContract(Contract contract) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addDeposit(Deposit deposit) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addInstrument(Instrument instrument) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addMargin(Margin margin) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addOrderRequest(OrderRequest request) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addOrderResponse(OrderResponse response) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addWithdraw(Withdraw withdraw) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void commit() throws DataSourceException {
        try {
            conn.commit();
        }
        catch (SQLException ex) {
            throw new DataSourceException(ExceptionCodes.TRANSACTION_COMMIT_FAILED.code(),
                                          ExceptionCodes.TRANSACTION_COMMIT_FAILED.message(),
                                          ex);
        }
        finally {
            restoreTransaction();
        }
    }

    @Override
    public Account getAccount() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CancelRequest getCancelRequest(long orderId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<CancelRequest> getCancelRequests() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<CancelResponse> getCancelResponseByOrderId(long orderId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<CancelResponse> getCancelResponses() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Commission getCommissionById(Long commissionId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Commission> getCommissions() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Commission> getCommissionsByOrderId(long orderId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Commission> getCommissionsByStatus(FeeStatus status) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Contract getContractById(Long contractId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Contract> getContracts() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Contract> getContractsByInstrumentId(String instrumentId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Contract> getContractsByResponseId(long responseId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Contract> getContractsByStatus(ContractStatus status) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Deposit> getDeposits() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Instrument getInstrument(String instrumentId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Instrument> getInstrumentsByExchangeId(String exchangeId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Margin getMarginById(Long marginId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Margin> getMargins() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Margin> getMarginsByOrderId(long orderId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Margin> getMarginsByStatus(FeeStatus status) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OrderRequest getOrderRequestById(long orderId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<OrderRequest> getOrderRequests() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<OrderResponse> getOrderResponses() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<OrderResponse> getOrderResponsesByOrderId(long orderId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LocalDate getTradingDay() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setTradingDay(LocalDate day) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Withdraw> getWithdraws() throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeCommission(long commissionId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeContract(long contractId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeInstrument(String instrumentId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeMargin(long marginId) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void rollback() throws DataSourceException {
        try {
            conn.rollback();
        }
        catch (SQLException ex) {
            throw new DataSourceException(ExceptionCodes.TRANSACTION_ROLLBACK_FAILED.code(),
                                          ExceptionCodes.TRANSACTION_ROLLBACK_FAILED.message(),
                                          ex);
        }
        finally {
            restoreTransaction();
        }
    }

    @Override
    public void transaction() throws DataSourceException {
        try {
            exAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
        }
        catch (SQLException ex) {
            restoreTransaction();
            throw new DataSourceException(ExceptionCodes.TRANSACTION_BEGIN_FAILED.code(),
                                          ExceptionCodes.TRANSACTION_BEGIN_FAILED.message(),
                                          ex);
        }
    }

    @Override
    public void updateAccount(Account account) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateCommission(Commission commission) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateContract(Contract contract) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateInstrument(Instrument instrument) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateMargin(Margin margin) throws DataSourceException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void restoreTransaction() throws DataSourceException {
        try {
            if (exAutoCommit != null) {
                conn.setAutoCommit(exAutoCommit);
            }
        }
        catch (SQLException ex) {
            throw new DataSourceException(ExceptionCodes.TRANSACTION_RESTORE_FAILED.code(),
                                          ExceptionCodes.TRANSACTION_RESTORE_FAILED.message(),
                                          ex);
        }
    }

}
