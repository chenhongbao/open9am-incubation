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
package com.open9am.service;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Account.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Account {

    private Double balance;
    private Double closeProfit;
    private Double commission;
    private Double deposit;
    private Double frozenCommission;
    private Double frozenMargin;
    private Double margin;
    private Double positionProfit;
    private Double preBalance;
    private Double preDeposit;
    private Double preMargin;
    private Double preWithdraw;
    private LocalDate tradingDay;
    private ZonedDateTime updateTimestamp;
    private Double withdraw;

    public Account() {
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
        updateTimestamp();
    }

    public Double getCloseProfit() {
        return closeProfit;
    }

    public void setCloseProfit(Double closeProfit) {
        this.closeProfit = closeProfit;
        updateTimestamp();
    }

    public Double getCommission() {
        return commission;
    }

    public void setCommission(Double commission) {
        this.commission = commission;
        updateTimestamp();
    }

    public Double getDeposit() {
        return deposit;
    }

    public void setDeposit(Double deposit) {
        this.deposit = deposit;
        updateTimestamp();
    }

    public Double getFrozenCommission() {
        return frozenCommission;
    }

    public void setFrozenCommission(Double frozenCommission) {
        this.frozenCommission = frozenCommission;
        updateTimestamp();
    }

    public Double getFrozenMargin() {
        return frozenMargin;
    }

    public void setFrozenMargin(Double frozenMargin) {
        this.frozenMargin = frozenMargin;
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

    public Double getPreBalance() {
        return preBalance;
    }

    public void setPreBalance(Double preBalance) {
        this.preBalance = preBalance;
        updateTimestamp();
    }

    public Double getPreDeposit() {
        return preDeposit;
    }

    public void setPreDeposit(Double preDeposit) {
        this.preDeposit = preDeposit;
        updateTimestamp();
    }

    public Double getPreMargin() {
        return preMargin;
    }

    public void setPreMargin(Double preMargin) {
        this.preMargin = preMargin;
        updateTimestamp();
    }

    public Double getPreWithdraw() {
        return preWithdraw;
    }

    public void setPreWithdraw(Double preWithdraw) {
        this.preWithdraw = preWithdraw;
        updateTimestamp();
    }

    public LocalDate getTradingDay() {
        return tradingDay;
    }

    public void setTradingDay(LocalDate tradingDay) {
        this.tradingDay = tradingDay;
        updateTimestamp();
    }

    public ZonedDateTime getUpdateTime() {
        return updateTimestamp;
    }

    public Double getWithdraw() {
        return withdraw;
    }

    public void setWithdraw(Double withdraw) {
        this.withdraw = withdraw;
        updateTimestamp();
    }

    private void updateTimestamp() {
        updateTimestamp = ZonedDateTime.now();
    }

}
