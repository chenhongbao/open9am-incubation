/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.platform;

import com.open9am.engine.ITraderEngine;
import java.util.Properties;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderEngineListener {

    void OnLoad(ITraderEngine engine);

    void BeforeStart(Properties properties);

    void AfterStart();

    void BeforeStop();

    void AfterStop();

    void BeforeInit(Properties properties);

    void AfterInit();

    void BeforeSettle(Properties properties);

    void AfterSettle();
}
