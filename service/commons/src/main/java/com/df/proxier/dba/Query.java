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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
class Query implements IQuery {

    private final Connection conn;
    private final Map<String, MetaTable<?>> meta;

    Query(Connection connection) {
        conn = connection;
        meta = new HashMap<>(64);
    }

    @Override
    public <T> int insert(Class<T> clazz, T object) throws SQLException {
        return execute(getInsertSql(findMeta(clazz), object));
    }

    @Override
    public <T> int remove(Class<T> clazz, ICondition condition) throws SQLException {
        return execute(getRemoveSql(findMeta(clazz), condition));
    }

    @Override
    public <T> Collection<T> select(Class<T> clazz,
                                    ICondition condition,
                                    IDefaultFactory<T> factory) throws SQLException,
                                                                       ReflectiveOperationException {
        var m = findMeta(clazz);
        return executeSelect(m, getSelectSql(m, condition), factory);
    }

    @Override
    public <T> int update(Class<T> clazz, T object, ICondition condition) throws SQLException {
        return execute(getUpdateSql(findMeta(clazz), object, condition));
    }

    private <T> Collection<T> convert(MetaTable<T> meta,
                                      ResultSet rs,
                                      IDefaultFactory<T> factory) throws ReflectiveOperationException {
        Collection<T> c = new LinkedList<>();
        @SuppressWarnings("unchecked")
        T r = factory.contruct();
        // TODO parse result set to collection.
        return c;
    }

    private int execute(String sql) throws SQLException {
        try (Statement stat = conn.createStatement()) {
            stat.execute(sql);
            return stat.getUpdateCount();
        }
    }

    private <T> Collection<T> executeSelect(MetaTable<T> meta,
                                            String sql,
                                            IDefaultFactory<T> factory) throws SQLException,
                                                                               ReflectiveOperationException {
        ResultSet rs;
        try (Statement stat = conn.createStatement()) {
            rs = stat.executeQuery(sql);
            return convert(meta, rs, factory);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> MetaTable<T> findMeta(Class<T> clazz) {
        return (MetaTable<T>) meta.computeIfAbsent(clazz.getCanonicalName(), k -> new MetaTable<T>(clazz));
    }

    private <T> String getInsertSql(MetaTable<T> meta, Object object) {
        return ""; // TODO getInsertSql
    }

    private <T> String getRemoveSql(MetaTable<T> meta, ICondition condition) {
        return ""; // TODO getRemoveSql
    }

    private <T> String getSelectSql(MetaTable<T> meta, ICondition condition) {
        return ""; // TODO getSelectSql
    }

    private <T> String getUpdateSql(MetaTable<T> meta, Object object, ICondition condition) {
        return ""; // TODO getUpdateSql
    }

}
