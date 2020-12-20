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

import java.lang.reflect.Field;
import java.sql.Connection;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Queries {

    public static ICondition<ICondition<?>> and(ICondition<?> c0, ICondition<?> c1) throws DbaException {
        return new Condition<>(c0, c1, ConditionType.AND);
    }

    public static IQuery createQuery(Connection dbConnection) {
        return new Query(dbConnection);
    }

    public static ICondition<Long> equals(Field field, Long value) throws DbaException {
        return new Condition<>(field, value, ConditionType.EQUALS);
    }

    public static ICondition<Integer> equals(Field field, Integer value) throws DbaException {
        return new Condition<>(field, value, ConditionType.EQUALS);
    }

    public static ICondition<String> equals(Field field, String value) throws DbaException {
        return new Condition<>(field, value, ConditionType.EQUALS);
    }

    public static ICondition<String> like(Field field, String pattern) throws DbaException {
        return new Condition<>(field, pattern, ConditionType.LIKE);
    }

    public static ICondition<ICondition<?>> not(ICondition<?> condition) throws DbaException {
        return new Condition<>(condition, ConditionType.NOT);
    }

    public static ICondition<ICondition<?>> isNull(Field field) throws DbaException {
        return new Condition<>(field, ConditionType.IS_NULL);
    }

    public static ICondition<ICondition<?>> isNotNull(Field field) throws DbaException {
        return new Condition<>(field, ConditionType.IS_NOT_NULL);
    }

    public static ICondition<ICondition<?>> or(ICondition<?> c0, ICondition<?> c1) throws DbaException {
        return new Condition<>(c0, c1, ConditionType.OR);
    }

    public static ICondition<Long> lessThan(Field field, Long value) throws DbaException {
        return new Condition<>(field, value, ConditionType.LESS_THAN);
    }

    public static ICondition<Integer> lessThan(Field field, Integer value) throws DbaException {
        return new Condition<>(field, value, ConditionType.LESS_THAN);
    }

    public static ICondition<String> lessThan(Field field, String value) throws DbaException {
        return new Condition<>(field, value, ConditionType.LESS_THAN);
    }

    public static ICondition<Long> largerThan(Field field, Long value) throws DbaException {
        return new Condition<>(field, value, ConditionType.LARGER_THAN);
    }

    public static ICondition<Integer> largerThan(Field field, Integer value) throws DbaException {
        return new Condition<>(field, value, ConditionType.LARGER_THAN);
    }

    public static ICondition<String> largerThan(Field field, String value) throws DbaException {
        return new Condition<>(field, value, ConditionType.LARGER_THAN);
    }

    private Queries() {
    }
}
