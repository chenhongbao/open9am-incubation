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

/**
 * Order types.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public enum OrderType {

    BUY_OPEN(0x0),
    SELL_OPEN(0x01),
    BUY_CLOSE(0x02),
    BUY_CLOSE_TODAY(0x03),
    SELL_CLOSE(0x04),
    SELL_CLOSE_TODAY(0x05);

    private final int code;

    private OrderType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
