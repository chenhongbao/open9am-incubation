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

import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Loggers {

    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>(64);

    public static Logger get() {
        var st = Thread.currentThread().getStackTrace();
        if (st.length < 2) {
            throw new IllegalThreadStateException("Can't find current stack.");
        }
        var n = st[1].getClassName();
        var r = loggers.get(n);
        if (r == null) {
            r = LogManager.getLogger(n);
            loggers.put(n, r);
        }
        return r;
    }

    private Loggers() {
    }
}
