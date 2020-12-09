package com.open9am.service;

import java.util.Properties;

/**
 * Underlying service that provides access to trading facilities.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderService extends INamedService {

    void start(Properties properties, ITraderServiceHandler handler) throws TraderException;

    void stop() throws TraderException;

    void insert(OrderRequest request, long requestId) throws TraderException;

    void cancel(CancelRequest request, long requestId) throws TraderException;

    int getStatus();

    Properties getProperties();

    TraderServiceInfo getServiceInfo();
}
