/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.platform;

import com.open9am.service.INamedService;
import java.nio.ByteBuffer;
import java.util.Properties;

/**
 *
 * @author Hongbao Chen
 *
 * @since 1.0
 */
public interface IDecoder extends INamedService {

    void decode(String text, Properties properties, IDecoderOutput out, boolean isLast);

    void decode(ByteBuffer bytes, Properties properties, IDecoderOutput out, boolean isLast);
}
