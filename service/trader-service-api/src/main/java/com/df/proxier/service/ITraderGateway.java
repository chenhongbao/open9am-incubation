package com.df.proxier.service;

import com.df.proxier.exceptions.GatewayException;
import com.df.proxier.Request;
import java.util.Properties;

/**
 * Underlying service that provides access to trading facilities.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderGateway {

    void start(Properties properties, ITraderGatewayHandler handler) throws GatewayException;

    void stop() throws GatewayException;

    void insert(Request request, long requestId) throws GatewayException;

    int getStatus();

    Properties getProperties();

    TraderGatewayInfo getServiceInfo();
}
