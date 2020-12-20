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
import com.df.proxier.Deposit;
import com.df.proxier.Direction;
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Offset;
import com.df.proxier.Order;
import com.df.proxier.Position;
import com.df.proxier.Request;
import com.df.proxier.Response;
import com.df.proxier.Tick;
import com.df.proxier.Trade;
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

    Account getAccount(Account pre, Collection<Deposit> deposits, Collection<Withdraw> withdraws, Collection<Position> positions) throws AlgorithmException;

    Collection<Position> getPositions(Collection<Contract> contracts, Collection<Commission> commissions, Collection<Margin> margins, Map<String, Tick> ticks, Map<String, Instrument> instruments, LocalDate tradingDay) throws AlgorithmException;

    Order getOrder(Request request, Collection<Contract> contracts, Collection<Trade> trades, Collection<Response> responses) throws AlgorithmException;

    double getAmount(double price, Instrument instrument) throws AlgorithmException;

    double getMargin(double price, Instrument instrument) throws AlgorithmException;

    double getCommission(double price, Instrument instrument, Direction direction, Offset offset) throws AlgorithmException;
}
