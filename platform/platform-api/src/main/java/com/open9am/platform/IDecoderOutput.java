/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.platform;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface IDecoderOutput {

    <T> void write(T ouput, Class<T> clazz);
}
