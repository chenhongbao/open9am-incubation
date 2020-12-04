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
    private Integer statusCode;
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
    }

    public ZonedDateTime getCancelTimestamp() {
        return cancelTimestamp;
    }

    public void setCancelTimestamp(ZonedDateTime cancelTimestamp) {
        this.cancelTimestamp = cancelTimestamp;
    }

    public ZonedDateTime getInsertTimestamp() {
        return insertTimestamp;
    }

    public void setInsertTimestamp(ZonedDateTime insertTimestamp) {
        this.insertTimestamp = insertTimestamp;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public void setIsCanceled(Boolean isCanceled) {
        this.isCanceled = isCanceled;

    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Long getTradedVolumn() {
        return tradedVolumn;
    }

    public void setTradedVolumn(Long tradedVolumn) {
        this.tradedVolumn = tradedVolumn;
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
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public ZonedDateTime getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(ZonedDateTime updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public Long getVolumn() {
        return volumn;
    }

    public void setVolumn(Long volumn) {
        this.volumn = volumn;
    }

    public Boolean isIsCanceled() {
        return isCanceled;
    }

}
