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
 * Order status.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Order {

    private Double amount;
    private ZonedDateTime cancelTimestamp;
    private ZonedDateTime insertTimestamp;
    private String instrumentId;
    private Boolean isCanceled;
    private Long orderId;
    private Double price;
    private OrderStatus status;
    private String statusMessage;
    private Long tradedVolumn;
    private Integer traderId;
    private LocalDate tradingDay;
    private OrderType type;
    private ZonedDateTime updateTimestamp;
    private Long volumn;

    public Order() {
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
        updateTimestamp();
    }

    public ZonedDateTime getCancelTimestamp() {
        return cancelTimestamp;
    }

    public void setCancelTimestamp(ZonedDateTime cancelTimestamp) {
        this.cancelTimestamp = cancelTimestamp;
        updateTimestamp();
    }

    public ZonedDateTime getInsertTimestamp() {
        return insertTimestamp;
    }

    public void setInsertTimestamp(ZonedDateTime insertTimestamp) {
        this.insertTimestamp = insertTimestamp;
        updateTimestamp();
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
        updateTimestamp();
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
        updateTimestamp();
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
        updateTimestamp();
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        updateTimestamp();
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
        updateTimestamp();
    }

    public Long getTradedVolumn() {
        return tradedVolumn;
    }

    public void setTradedVolumn(Long tradedVolumn) {
        this.tradedVolumn = tradedVolumn;
        updateTimestamp();
    }

    public Integer getTraderId() {
        return traderId;
    }

    public void setTraderId(Integer traderId) {
        this.traderId = traderId;
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
        return updateTimestamp;
    }

    public Long getVolumn() {
        return volumn;
    }

    public void setVolumn(Long volumn) {
        this.volumn = volumn;
        updateTimestamp();
    }

    public Boolean isIsCanceled() {
        return isCanceled;
    }

    public void setIsCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;
        updateTimestamp();
    }

    private void updateTimestamp() {
        updateTimestamp = ZonedDateTime.now();
    }

}
