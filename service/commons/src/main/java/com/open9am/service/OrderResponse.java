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
 * Reponse for a filled trade corresponding to the {@link OrderRequest} denoted
 * by the order ID whose volumn should not exceed the total volumn of the order.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class OrderResponse {

    private String instrumentId;
    private Long orderId;
    private Double price;
    private Long responseId;
    private ZonedDateTime timestamp;
    private Integer traderId;
    private LocalDate tradingDay;
    private OrderType type;
    private String uuid;
    private Long volumn;

    public OrderResponse() {
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

    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
        updateTimestamp();
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public Integer getTraderId() {
        return traderId;
    }

    public void setTraderId(Integer traderId) {
        this.traderId = traderId;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
        updateTimestamp();
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
