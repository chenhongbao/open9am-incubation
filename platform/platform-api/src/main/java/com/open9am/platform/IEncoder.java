/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.platform;

import com.open9am.service.INamedService;
import java.util.Properties;

/**
 *
 * @author Hongbao Chen
 * @param <T>
 *
 * @since 1.0
 */
public interface IEncoder<T> extends INamedService {

    void encode(T object, Properties properties, IEncoderOutput out);
}
