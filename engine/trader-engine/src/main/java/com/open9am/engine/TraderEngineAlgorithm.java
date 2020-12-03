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
import com.open9am.service.Commission;
import com.open9am.service.Contract;
import com.open9am.service.Instrument;
import com.open9am.service.Margin;
import com.open9am.service.Order;
import com.open9am.service.OrderRequest;
import com.open9am.service.OrderResponse;
import com.open9am.service.Position;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class TraderEngineAlgorithm implements ITraderEngineAlgorithm {

    @Override
    public Account getAccount(Account pre, Collection<Position> positions) throws TraderEngineAlgorithmException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getAmount(double price, Instrument instrument) throws TraderEngineAlgorithmException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getCommission(double price, Instrument instrument) throws TraderEngineAlgorithmException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getMargin(double price, Instrument instrument) throws TraderEngineAlgorithmException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Order getOrder(OrderRequest request, Collection<OrderResponse> responses) throws TraderEngineAlgorithmException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Position> getPositions(Collection<Contract> contracts,
                                             Collection<Commission> commissions,
                                             Collection<Margin> margins,
                                             Properties properties) throws TraderEngineAlgorithmException {
        if (contracts == null) {
            throw new TraderEngineAlgorithmException(ErrorCodes.CONTRACT_NULL.code(),
                                                     ErrorCodes.CONTRACT_NULL.message());
        }
        final var ls = new HashMap<String, Position>(64);
        final var ss = new HashMap<String, Position>(64);
        final var tradingDay = getDate("TradingDay", properties);

        for (var c : contracts) {
            Position p;
            var type = c.getOpenType();
            if (null == type) {
                throw new TraderEngineAlgorithmException(ErrorCodes.INVALID_ORDER_TYPE.code(),
                                                         ErrorCodes.INVALID_ORDER_TYPE.message());
            }
            switch (type) {
                case BUY_OPEN:
                    p = ls.computeIfAbsent(c.getInstrumentId(), k -> {
                                       return initPosition(c, tradingDay);
                                   });
                    break;
                case SELL_OPEN:
                    p = ss.computeIfAbsent(c.getInstrumentId(), k -> {
                                       return initPosition(c, tradingDay);
                                   });
                    break;
                default:
                    throw new TraderEngineAlgorithmException(ErrorCodes.INVALID_ORDER_TYPE.code(),
                                                             ErrorCodes.INVALID_ORDER_TYPE.message());
            }
            if (c.getOpenTradingDay().isBefore(tradingDay)) {
                addPrePosition(p, c);
            }
            else {
                addTodayPosition(p, c);
            }
        }
        var r = new HashSet<Position>(ls.values());
        r.addAll(ss.values());
        return r;
    }

    private void addPrePosition(Position p, Contract c) {
        if (p.getAmount() == null) {
            p.setAmount(c.getOpenAmount());
        }
        else {
            p.setAmount(p.getAmount() + c.getOpenAmount());
        }
        // TODO add pre-position
    }

    private void addTodayPosition(Position p, Contract c) {
        // TODO add today position
    }

    private LocalDate getDate(String key, Properties properties) throws TraderEngineAlgorithmException {
        var v = getObject(key, properties);
        if (!(v instanceof LocalDate)) {
            throw new TraderEngineAlgorithmException(ErrorCodes.PROPERTY_WRONG_DATE_TYPE.code(),
                                                     ErrorCodes.PROPERTY_WRONG_DATE_TYPE.message() + "(" + key + ")");
        }
        return (LocalDate) v;
    }

    private Object getObject(String key, Properties properties) throws TraderEngineAlgorithmException {
        if (properties == null) {
            throw new TraderEngineAlgorithmException(ErrorCodes.PROPERTIES_NULL.code(),
                                                     ErrorCodes.PROPERTIES_NULL.message());
        }
        var v = properties.get(key);
        if (v == null) {
            throw new TraderEngineAlgorithmException(ErrorCodes.PROPERTY_NOT_FOUND.code(),
                                                     ErrorCodes.PROPERTY_NOT_FOUND.message() + "(" + key + ")");
        }
        return v;
    }

    private double getPrice(String key, Properties properties) throws TraderEngineAlgorithmException {
        var v = getObject(key, properties);
        if (!(v instanceof Double)) {
            throw new TraderEngineAlgorithmException(ErrorCodes.PROPERTY_WRONG_PRICE_TYPE.code(),
                                                     ErrorCodes.PROPERTY_WRONG_PRICE_TYPE.message() + "(" + key + ")");
        }
        return (Double) v;
    }

    private Position initPosition(Contract c, LocalDate tradingDay) {
        var p0 = new Position();
        p0.setAmount(0.0D);
        p0.setCloseProfit(0.0D);
        p0.setFrozenCloseVolumn(0L);
        p0.setFrozenMargin(0.0D);
        p0.setFrozenOpenVolumn(0L);
        p0.setInstrumentId(c.getInstrumentId());
        p0.setMargin(0.0D);
        p0.setPositionProfit(0.0D);
        p0.setPreAmount(0.0D);
        p0.setPreMargin(0.0D);
        p0.setPreVolumn(0L);
        p0.setTodayAmount(0.0D);
        p0.setTodayMargin(0.0D);
        p0.setTodayVolumn(0L);
        p0.setTradingDay(tradingDay);
        p0.setType(c.getOpenType());
        p0.setVolumn(0L);
        return p0;
    }
}
