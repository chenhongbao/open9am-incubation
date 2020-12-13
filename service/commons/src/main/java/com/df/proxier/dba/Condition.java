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
class Condition implements ICondition {

    Condition(Field field, Long value) {

    }

    Condition(Field field, Integer value) {

    }

    Condition(Field field, String value) {

    }

    @Override
    public String getSqlFragement() {
        // TODO getSqlFragement
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
