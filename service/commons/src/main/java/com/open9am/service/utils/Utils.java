/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.service.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class Utils {

    private static final AtomicLong id = new AtomicLong(0);

    public static Long getId() {
        return id.incrementAndGet();
    }

    public static UUID getUuid() {
        return UUID.randomUUID();
    }

    private Utils() {
    }
}
