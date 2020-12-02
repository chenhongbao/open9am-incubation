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
 *
 * @author chenh
 */
public class TraderRuntimeException extends RuntimeException implements CodeMessage {

    private static final long serialVersionUID = 2955886225335647L;

    private final int code;

    public TraderRuntimeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public TraderRuntimeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}
