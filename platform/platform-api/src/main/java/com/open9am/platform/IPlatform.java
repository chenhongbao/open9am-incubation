/*
 * Copyright (C) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.open9am.platform;

import java.util.Date;
import java.util.Properties;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Platform.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface IPlatform {

    void setDecoder(IDecoder decoder) throws PlatformException;

    <T> void add(IEncoder<T> encoder, Class<T> clazz) throws PlatformException;

    <T> void addInputHandler(IInputHandler<T> handler, Class<T> clazz) throws PlatformException;

    <T> void addOutputHandler(IOutputHandler<T> handler, Class<T> clazz) throws PlatformException;

    <T> IDisruptor<T> getDistruptor(Class<T> clazz);

    ITimer getTimer(TimerTask task, Date date, long period, TimeUnit unit);

    IDeamon getDaemon(Runnable runnable);

    void start(Properties properties) throws PlatformException;

    void join() throws PlatformException;

    void stop() throws PlatformException;
}
