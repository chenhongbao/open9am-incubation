/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.open9am.platform;

/**
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class PlatformException extends Exception {

    private static final long serialVersionUID = 3444655959436299L;

    private final int code;

    public PlatformException(int code, String message) {
        super(message);
        this.code = code;
    }

    public PlatformException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
