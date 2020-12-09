/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.platform;

import com.open9am.service.INamedService;

/**
 * Platform.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface IPlatform extends INamedService {

    void setDecoder(IDecoder decoder) throws PlatformException;

    <T> void setEncoder(IEncoder<T> encoder, Class<T> clazz) throws PlatformException;

    void addEngineListener(ITraderEngineListener listener) throws PlatformException;

    void run() throws PlatformException;

    void join() throws PlatformException;

    void stop() throws PlatformException;
}
