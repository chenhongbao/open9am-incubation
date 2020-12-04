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
 * Contract.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Contract {

    private Double closeAmount;
    private Long contractId;
    private String instrumentId;
    private Double openAmount;
    private ZonedDateTime openTimestamp;
    private LocalDate openTradingDay;
    private OrderType openType;
    private Long responseId;
    private ContractStatus status;
    private ZonedDateTime timestamp;
    private Integer traderId;

    public Contract() {
    }

    public Double getCloseAmount() {
        return closeAmount;
    }

    public void setCloseAmount(Double closeAmount) {
        this.closeAmount = closeAmount;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Double getOpenAmount() {
        return openAmount;
    }

    public void setOpenAmount(Double openAmount) {
        this.openAmount = openAmount;
    }

    public ZonedDateTime getOpenTimestamp() {
        return openTimestamp;
    }

    public void setOpenTimestamp(ZonedDateTime openTimestamp) {
        this.openTimestamp = openTimestamp;
    }

    public LocalDate getOpenTradingDay() {
        return openTradingDay;
    }

    public void setOpenTradingDay(LocalDate tradingDay) {
        this.openTradingDay = tradingDay;
    }

    public OrderType getOpenType() {
        return openType;
    }

    public void setOpenType(OrderType openType) {
        this.openType = openType;
    }

    public Long getResponseId() {
        return responseId;
    }

    public void setResponseId(Long responseId) {
        this.responseId = responseId;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getTraderId() {
        return traderId;
    }

    public void setTraderId(Integer traderId) {
        this.traderId = traderId;
    }
}
