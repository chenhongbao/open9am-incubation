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
 * @since 1.0
 */
public interface IPlatformHandler extends INamedService {

    void onException(PlatformRuntimeException exception);

    void onException(Properties properties, PlatformRuntimeException exception);

    void onStatusChange(PlatformStatus status);
}
