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
import com.open9am.service.ITraderService;
import com.open9am.service.Instrument;
import com.open9am.service.OrderRequest;
import com.open9am.service.TraderException;
import java.util.Collection;
import java.util.Properties;

/**
 * Trader engine interface.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderEngine {

    void registerTrader(int traderId, ITraderService trader) throws TraderException;

    void enableTrader(int traderId, boolean enabled) throws TraderException;

    void unregisterTrader(int traderId) throws TraderException;

    Collection<TraderServiceRuntime> getTraderServiceRuntimes() throws TraderException;

    TraderServiceRuntime getTraderServiceInfo(int traderId) throws TraderException;

    void setStartProperties(int traderId, Properties properties) throws TraderException;

    void addHandler(ITraderEngineHandler handler) throws TraderException;

    void removeHanlder(ITraderEngineHandler handler) throws TraderException;

    void start(Properties properties) throws TraderException;

    void stop() throws TraderException;

    void setInitProperties(int traderId, Properties properties) throws TraderException;

    void initialize(Properties properties) throws TraderException;

    void setSettleProperties(int traderId, Properties properties) throws TraderException;

    void settle(Properties properties) throws TraderException;

    void request(OrderRequest request, Instrument instrument, Properties properties, int requestId) throws TraderException;

    void request(CancelRequest request, int requestId) throws TraderException;

    EngineStatus getStatus();

    void setDataSource(IDataSource dataSource) throws TraderException;

    IDataSource getDataSource();

    void setAlgorithm(ITraderEngineAlgorithm algo) throws TraderException;

    ITraderEngineAlgorithm getAlgorithm();

    Collection<ITraderEngineHandler> handlers();

    Instrument getRelatedInstrument(String instrumentId) throws TraderException;
}
