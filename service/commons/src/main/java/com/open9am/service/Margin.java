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
 * Margin.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Margin {

    private Long contractId;
    private Double margin;
    private Long marginId;
    private Long orderId;
    private FeeStatus status;
    private ZonedDateTime timestamp;
    private LocalDate tradingDay;
    private OrderType type;

    public Margin() {
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
        updateTimestamp();
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
        updateTimestamp();
    }

    public Long getMarginId() {
        return marginId;
    }

    public void setMarginId(Long marginId) {
        this.marginId = marginId;
        updateTimestamp();
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public FeeStatus getStatus() {
        return status;
    }

    public void setStatus(FeeStatus status) {
        this.status = status;
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

    public ZonedDateTime getUpdateTime() {
        return timestamp;
    }

    private void updateTimestamp() {
        timestamp = ZonedDateTime.now();
    }

}
