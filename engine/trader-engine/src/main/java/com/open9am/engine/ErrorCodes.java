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

/**
 *
 * @author chenh
 */
public enum ErrorCodes {
    TRADER_ID_NOT_FOUND(0x00000001, "No such trader ID in map."),
    TRADER_SELECT_TYPE_NULL(0x00000002, "Trader selection type null."),
    TRADER_ID_DUPLICATED(0x00000003, "Duplicated trader ID."),
    TRADER_SERVICE_NULL(0x00000004, "Trader service null."),
    TRADER_START_FAILED(0x00000005, "Trader service failed starting."),
    TRADER_STOP_FAILED(0x00000006, "Trader service failed stopping."),
    REQUEST_NULL(0x00000007, "Request null."),
    DATASOURCE_NULL(0x00000008, "Data source null."),
    CONTRACT_NULL(0x00000009, "Contract collection null."),
    ALGORITHM_NULL(0x0000000A, "Algorithm null."),
    ACCOUNT_NULL(0x0000000B, "Account null."),
    USER_CODE_ERROR(0x0000000C, "User code throwed exception."),
    ORDER_REQS_NULL(0x0000000D, "Order request collection null."),
    ORDER_RSPS_NULL(0x0000000E, "Order response collection null."),
    ORDER_ID_NOT_FOUND(0x0000000F, "No such order ID in map."),
    TRADER_SVC_HANDLER_NULL(0x00000010, "Trader service handler null."),
    CANCEL_ORDER_FAILED(0x00000011, "Failed canceling order."),
    TRADER_NOT_ENABLED(0x00000012, "Specified trader not enabled."),
    NO_TRADER(0x00000013, "No trader."),
    NO_TRADER_AVAILABLE(0x00000014, "No trader available."),
    INSTRUMENT_NULL(0x00000015, "Instrument null."),
    INSUFFICIENT_MONEY(0x00000016, "Insufficient money."),
    INSUFFICIENT_POSITION(0x00000017, "Insufficient position."),
    NONPOSITIVE_VOLUMN(0x00000018, "Negative volumn."),
    VOLUMN_NULL(0x00000019, "Null volumn."),
    INVALID_ORDER_TYPE(0x0000001A, "Invalid order type."),
    DS_FAILURE_UNFIXABLE(0x0000001B, "Data source operation failed and unfixable."),
    INCONSISTENT_COMMISSION_CONTRACT_STATUSES(0x0000001C, "Inconsistent commission and contract statuses."),
    INCONSISTENT_MARGIN_CONTRACT_STATUSES(0x0000001D, "Inconsistent margin and contract statuses."),
    INVALID_CANCELING_CONTRACT_STATUS(0x0000001E, "Invalid canceling contract status."),
    CONTRACT_ID_NULL(0x0000001F, "Contract ID null."),
    COMMISSION_NULL(0x00000020, "Commission null."),
    INCONSISTENT_FROZEN_INFO(0x00000021, "Incompleted info."),
    INVALID_CANCELING_MARGIN_STATUS(0x00000022, "Invalid canceling status."),
    MARGIN_NULL(0x00000023, "Margin null."),
    UNEXPECTED_ERROR(0x00000024, "Unexpected error.");

    private final int code;
    private final String message;

    private ErrorCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
