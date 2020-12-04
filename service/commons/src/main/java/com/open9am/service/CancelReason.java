/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.service;

/**
 * Order cancel reasons.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public enum CancelReason {
    USER(0x70),
    MARKET_CLOSE(0x71),
    INVALID_REQUEST(0x72);

    private final int code;

    private CancelReason(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
