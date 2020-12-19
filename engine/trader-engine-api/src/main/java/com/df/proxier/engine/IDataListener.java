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
import com.df.proxier.Instrument;
import com.df.proxier.Margin;
import com.df.proxier.Request;
import com.df.proxier.Response;
import com.df.proxier.Tick;
import com.df.proxier.Trade;
import com.df.proxier.Withdraw;
import java.time.LocalDate;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface IDataListener {

    void onChange(Margin margin, DataChange change, IDataConnection source);

    void onChange(Commission commission, DataChange change, IDataConnection source);

    void onChange(Contract contract, DataChange change, IDataConnection source);

    void onChange(Account account, DataChange change, IDataConnection source);

    void onChange(Deposit deposit, DataChange change, IDataConnection source);

    void onChange(Withdraw withdraw, DataChange change, IDataConnection source);

    void onChange(Instrument instrument, DataChange change, IDataConnection source);

    void onChange(Request request, DataChange change, IDataConnection source);

    void onChange(Response response, DataChange change, IDataConnection source);

    void onChange(Trade trade, DataChange change, IDataConnection source);

    void onChange(Tick tick, DataChange change, IDataConnection source);

    void onTradingDay(LocalDate day, DataChange change, IDataConnection source);
}
