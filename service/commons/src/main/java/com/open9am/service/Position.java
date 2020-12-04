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
 * <p>
 * <b>Pre-like fields</b> are yesterday's settled data and remains unchanged for
 * today. <b>Today-like fields</b> are owning today's position that is open
 * today but not yet closed. <b>Open-like fields</b> are all today's opened
 * position including both closed and not yet closed.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Position {

    private Double amount;
    private Double closeProfit;
    private Double commission;
    private Long frozenCloseVolumn;
    private Double frozenCommission;
    private Double frozenMargin;
    private Long frozenOpenVolumn;
    private String instrumentId;
    private Double margin;
    private Double openAmount;
    private Double openMargin;
    private Long openVolumn;
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
    }

    public Double getCloseProfit() {
        return closeProfit;
    }

    public void setCloseProfit(Double closeProfit) {
        this.closeProfit = closeProfit;
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
    }

    public Long getFrozenCloseVolumn() {
        return frozenCloseVolumn;
    }

    public void setFrozenCloseVolumn(Long frozenCloseVolumn) {
        this.frozenCloseVolumn = frozenCloseVolumn;

    }

    public Double getFrozenCommission() {
        return frozenCommission;
    }

    public void setFrozenCommission(Double frozenCommission) {
        this.frozenCommission = frozenCommission;
    }

    public Double getFrozenMargin() {
        return frozenMargin;
    }

    public void setFrozenMargin(Double frozenMargin) {
        this.frozenMargin = frozenMargin;
    }

    public Long getFrozenOpenVolumn() {
        return frozenOpenVolumn;
    }

    public void setFrozenOpenVolumn(Long frozenOpenVolumn) {
        this.frozenOpenVolumn = frozenOpenVolumn;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getOpenAmount() {
        return openAmount;
    }

    public void setOpenAmount(Double openAmount) {
        this.openAmount = openAmount;
    }

    public Double getOpenMargin() {
        return openMargin;
    }

    public void setOpenMargin(Double openMargin) {
        this.openMargin = openMargin;
    }

    public Long getOpenVolumn() {
        return openVolumn;
    }

    public void setOpenVolumn(Long openVolumn) {
        this.openVolumn = openVolumn;
    }

    public Double getPositionProfit() {
        return positionProfit;
    }

    public void setPositionProfit(Double positionProfit) {
        this.positionProfit = positionProfit;
    }

    public Double getPreAmount() {
        return preAmount;
    }

    public void setPreAmount(Double preAmount) {
        this.preAmount = preAmount;
    }

    public Double getPreMargin() {
        return preMargin;
    }

    public void setPreMargin(Double preMargin) {
        this.preMargin = preMargin;
    }

    public Long getPreVolumn() {
        return preVolumn;
    }

    public void setPreVolumn(Long preVolumn) {
        this.preVolumn = preVolumn;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getTodayAmount() {
        return todayAmount;
    }

    public void setTodayAmount(Double todayAmount) {
        this.todayAmount = todayAmount;
    }

    public Double getTodayMargin() {
        return todayMargin;
    }

    public void setTodayMargin(Double todayMargin) {
        this.todayMargin = todayMargin;
    }

    public Long getTodayVolumn() {
        return todayVolumn;
    }

    public void setTodayVolumn(Long todayVolumn) {
        this.todayVolumn = todayVolumn;
    }

    public LocalDate getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(LocalDate tradingDay) {
        this.tradingDay = tradingDay;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public Long getVolumn() {
        return volumn;
    }

    public void setVolumn(Long volumn) {
        this.volumn = volumn;
    }

}
