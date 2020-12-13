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
import com.df.proxier.CancelResponse;
import com.df.proxier.Commission;
import com.df.proxier.Contract;
import com.df.proxier.Deposit;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Order;
import com.df.proxier.OrderRequest;
import com.df.proxier.OrderResponse;
import com.df.proxier.OrderType;
import com.df.proxier.Position;
import com.df.proxier.Tick;
import com.df.proxier.Withdraw;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

/**
 * Algorithms.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderEngineAlgorithm {

    Account getAccount(Account pre, Collection<Deposit> deposits, Collection<Withdraw> withdraws, Collection<Position> positions) throws TraderEngineAlgorithmException;

    Collection<Position> getPositions(Collection<Contract> contracts, Collection<Commission> commissions, Collection<Margin> margins, Map<String, Tick> ticks, Map<String, Instrument> instruments, LocalDate tradingDay) throws TraderEngineAlgorithmException;

    Order getOrder(OrderRequest request, Collection<Contract> contracts, Collection<OrderResponse> trades, Collection<CancelResponse> cancels) throws TraderEngineAlgorithmException;

    double getAmount(double price, Instrument instrument) throws TraderEngineAlgorithmException;

    double getMargin(double price, Instrument instrument) throws TraderEngineAlgorithmException;

    double getCommission(double price, Instrument instrument, OrderType type) throws TraderEngineAlgorithmException;
}
