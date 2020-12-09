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
import com.open9am.service.CancelResponse;
import com.open9am.service.Commission;
import com.open9am.service.Contract;
import com.open9am.service.Deposit;
import com.open9am.service.INamedService;
import com.open9am.service.Instrument;
import com.open9am.service.Margin;
import com.open9am.service.Order;
import com.open9am.service.OrderRequest;
import com.open9am.service.OrderResponse;
import com.open9am.service.OrderType;
import com.open9am.service.Position;
import com.open9am.service.Withdraw;
import java.util.Collection;
import java.util.Properties;

/**
 * Algorithms.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderEngineAlgorithm extends INamedService {

    Account getAccount(Account pre,
                       Collection<Deposit> deposits,
                       Collection<Withdraw> withdraws,
                       Collection<Position> positions) throws TraderEngineAlgorithmException;

    Collection<Position> getPositions(Collection<Contract> contracts,
                                      Collection<Commission> commissions,
                                      Collection<Margin> margins,
                                      Properties properties) throws TraderEngineAlgorithmException;

    Order getOrder(OrderRequest request,
                   Collection<Contract> contracts,
                   Collection<OrderResponse> trades,
                   Collection<CancelResponse> cancels) throws TraderEngineAlgorithmException;

    double getAmount(double price, Instrument instrument) throws TraderEngineAlgorithmException;

    double getMargin(double price, Instrument instrument) throws TraderEngineAlgorithmException;

    double getCommission(double price, Instrument instrument, OrderType type) throws TraderEngineAlgorithmException;
}
