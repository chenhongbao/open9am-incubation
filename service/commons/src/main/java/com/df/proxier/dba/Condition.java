/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.df.proxier.dba;

import java.lang.reflect.Field;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
class Condition<T> implements ICondition<T> {

    private final MetaField meta;
    private final String sqlv;
    private final ConditionType t;
    private T v0;
    private T v1;

    Condition(Field field, T value, ConditionType type) {
        meta = DbaUtils.inspectField(field);
        v0 = value;
        t = type;
        sqlv = stringValue(v0);
    }

    Condition(Field field, T c0, T c1, ConditionType type) {
        if (type != ConditionType.AND && type != ConditionType.OR) {
            throw new IllegalArgumentException("Wrong condition type.");
        }
        this.meta = DbaUtils.inspectField(field);
        v0 = c0;
        v1 = c1;
        t = type;
        this.sqlv = stringValue(v0, v1);
    }

    private String stringValue(T v) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String stringValue(T v0, T v1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Field getField() {
        return meta.getField();
    }

    @Override
    public ConditionType getType() {
        return t;
    }

    @Override
    public T getValue0() {
        return v0;
    }

    @Override
    public T getValue1() {
        return v1;
    }

    boolean checkBelonging(Class<?> clazz) {
        return meta.getField().getDeclaringClass() == clazz;
    }

}
