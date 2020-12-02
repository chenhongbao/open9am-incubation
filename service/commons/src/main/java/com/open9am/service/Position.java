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
 * aLong with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.open9am.service;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Position.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Position {

    private Double amount;
    private Double closeProfit;
    private Double frozenCloseVolumn;
    private Double frozenMargin;
    private Double frozenOpenVolumn;
    private String instrumentId;
    private Double margin;
    private Double positionProfit;
    private Double preAmount;
    private Double preMargin;
    private Long preVolumn;
    private ZonedDateTime timestamp;
    private Double todayAmount;
    private Double todayMargin;
    private Long todayVolumn;
    private LocalDate tradingDay;
    private OrderType type;
    private Long volumn;

    public Position() {
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
        updateTimestamp();
    }

    public Double getCloseProfit() {
        return closeProfit;
    }

    public void setCloseProfit(Double closeProfit) {
        this.closeProfit = closeProfit;
        updateTimestamp();
    }

    public Double getFrozenCloseVolumn() {
        return frozenCloseVolumn;
    }

    public void setFrozenCloseVolumn(Double frozenCloseVolumn) {
        this.frozenCloseVolumn = frozenCloseVolumn;
        updateTimestamp();
    }

    public Double getFrozenMargin() {
        return frozenMargin;
    }

    public void setFrozenMargin(Double frozenMargin) {
        this.frozenMargin = frozenMargin;
        updateTimestamp();
    }

    public Double getFrozenOpenVolumn() {
        return frozenOpenVolumn;
    }

    public void setFrozenOpenVolumn(Double frozenOpenVolumn) {
        this.frozenOpenVolumn = frozenOpenVolumn;
        updateTimestamp();
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
        updateTimestamp();
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
        updateTimestamp();
    }

    public Double getPositionProfit() {
        return positionProfit;
    }

    public void setPositionProfit(Double positionProfit) {
        this.positionProfit = positionProfit;
        updateTimestamp();
    }

    public Double getPreAmount() {
        return preAmount;
    }

    public void setPreAmount(Double preAmount) {
        this.preAmount = preAmount;
        updateTimestamp();
    }

    public Double getPreMargin() {
        return preMargin;
    }

    public void setPreMargin(Double preMargin) {
        this.preMargin = preMargin;
        updateTimestamp();
    }

    public Long getPreVolumn() {
        return preVolumn;
    }

    public void setPreVolumn(Long preVolumn) {
        this.preVolumn = preVolumn;
        updateTimestamp();
    }

    public Double getTodayAmount() {
        return todayAmount;
    }

    public void setTodayAmount(Double todayAmount) {
        this.todayAmount = todayAmount;
        updateTimestamp();
    }

    public Double getTodayMargin() {
        return todayMargin;
    }

    public void setTodayMargin(Double todayMargin) {
        this.todayMargin = todayMargin;
        updateTimestamp();
    }

    public Long getTodayVolumn() {
        return todayVolumn;
    }

    public void setTodayVolumn(Long todayVolumn) {
        this.todayVolumn = todayVolumn;
        updateTimestamp();
    }

    public LocalDate getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(LocalDate tradingDay) {
        this.tradingDay = tradingDay;
        updateTimestamp();
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
        updateTimestamp();
    }

    public ZonedDateTime getUpdateTimestamp() {
        return timestamp;
    }

    public Long getVolumn() {
        return volumn;
    }

    public void setVolumn(Long volumn) {
        this.volumn = volumn;
        updateTimestamp();
    }

    private void updateTimestamp() {
        timestamp = ZonedDateTime.now();
    }

}
