package com.df.proxier.service;

import com.df.proxier.Request;
import java.util.Properties;

/**
 * Underlying service that provides access to trading facilities.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderService {

    void start(Properties properties, ITraderServiceHandler handler) throws TraderException;

    void stop() throws TraderException;

    void insert(Request request, long requestId) throws TraderException;

    int getStatus();

    Properties getProperties();

    TraderServiceInfo getServiceInfo();
}
