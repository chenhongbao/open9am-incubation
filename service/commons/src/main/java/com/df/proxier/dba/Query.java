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
package com.df.proxier.dba;

import java.sql.Connection;
import java.util.Collection;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
class Query implements IQuery {

    private final Connection conn;
    private final MetaTable meta;

    Query(Connection connection, MetaTable table) {

        conn = connection;
        meta = table;
    }

    @Override
    public <T> int insert(Class<T> clazz, T object) {
        // TODO insert
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> int remove(Class<T> clazz, ICondition condition) {
        // TODO remove
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> Collection<T> select(Class<T> clazz, ICondition condition) {
        // TODO select
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public <T> int update(Class<T> clazz, T object, ICondition condition) {
        // TODO update
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
