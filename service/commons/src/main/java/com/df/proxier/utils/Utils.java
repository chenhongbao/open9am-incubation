/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.df.proxier.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Utils {

    private static final AtomicLong ID = new AtomicLong(0);
    private static final long SYSTEM_SERIAL = Math.abs(getUuid().getLeastSignificantBits())
                                              + System.currentTimeMillis();

    @SuppressWarnings("unchecked")
    public static <T> T copy(T copied) {
        try (ByteArrayOutputStream bo = new ByteArrayOutputStream()) {
            new ObjectOutputStream(bo).writeObject(copied);
            return (T) new ObjectInputStream(
                    new ByteArrayInputStream(bo.toByteArray())).readObject();
        }
        catch (IOException | ClassNotFoundException ignored) {
            return null;
        }
    }

    /**
     * Get incremental ID.
     *
     * @return auto-incremental ID
     */
    public synchronized static Long getId() {
        return SYSTEM_SERIAL + ID.incrementAndGet();
    }

    public static UUID getUuid() {
        return UUID.randomUUID();
    }

    private Utils() {
    }
}
