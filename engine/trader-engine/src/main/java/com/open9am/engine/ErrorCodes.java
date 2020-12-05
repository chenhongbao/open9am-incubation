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
    CANCEL_REQS_NULL(0x00000007, "Cance; request(s) null."),
    DATASOURCE_NULL(0x00000008, "Data source null."),
    CONTRACT_NULL(0x00000009, "Contract collection null."),
    ALGORITHM_NULL(0x0000000A, "Algorithm null."),
    ACCOUNT_NULL(0x0000000B, "Account null."),
    USER_CODE_ERROR(0x0000000C, "User code throwed exception."),
    ORDER_REQS_NULL(0x0000000D, "Order request(s) null."),
    ORDER_RSPS_NULL(0x0000000E, "Order response(s) null."),
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
    UNEXPECTED_ERROR(0x00000024, "Unexpected error."),
    OBJECT_COPY_FAILED(0x00000025, "Object copy failed."),
    DEST_ID_NOT_FOUND(0x00000026, "Destinated ID(s) not found."),
    COUNTDOWN_NOT_FOUND(0x00000027, "Count down not found."),
    PREPROC_RSPS_FAILED(0x00000028, "Preprocess response failed."),
    PROPERTIES_NULL(0x00000029, "Properties null."),
    PROPERTY_NOT_FOUND(0x0000002A, "Property not found."),
    PROPERTY_WRONG_DATE_TYPE(0x0000002B, "Wrong date type in properties."),
    PROPERTY_WRONG_PRICE_TYPE(0x0000002B, "Wrong price type in properties."),
    PROPERTY_WRONG_INSTRUMENT_TYPE(0x0000002C, "Wrong instrument type in properties."),
    PROPERTY_WRONG_MARGIN_TYPE(0x0000002D, "Wrong margin type in propertoes."),
    CONTRACT_STATUS_NULL(0x0000002E, "Contract status null."),
    INVALID_CONTRACT_STATUS(0x0000002F, "Invalid contract status."),
    RATIO_TYPE_NULL(0x00000030, "Ratio type null."),
    RATIO_NULL(0x00000031, "Ratio null."),
    MULTIPLE_NULL(0x00000032, "Volumn multiple null."),
    INSTRUMENT_ID_NULL(0x00000033, "Instrument ID null."),
    INVALID_INSTRUMENT_ID(0x00000034, "Invalid instrument ID."),
    CANCEL_RSPS_NULL(0x00000035, "Cancel response(s) null."),
    INCONSISTENT_CONTRACT_ORDER_INFO(0x00000036, "inconsistent information between contracts and order."),
    ORDER_ID_NULL(0x00000037, "Order ID null."),
    WITHDRAW_NULL(0x00000038, "Withdraw(s) null."),
    DEPOSIT_NULL(0x00000039, "Deposit(s) null."),
    WITHDRAW_AMOUNT_NULL(0x0000003A, "Withdraw amount null."),
    DEPOSIT_AMOUNT_NULL(0x0000003B, "Deposit amount null."),
    POSITION_NULL(0x0000003C, "Position(s) null."),
    POSITION_FIELD_NULL(0x0000003D, "Position field(s) null."),
    COMMISSION_AMOUNT_NULL(0x0000003E, "Commission amount null."),
    INVALID_FEE_STATUS(0x0000003F, "Invalid fee status."),
    TRADER_ENGINE_HANDLER_NULL(0x00000040, "Trader engine handler null.");

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
