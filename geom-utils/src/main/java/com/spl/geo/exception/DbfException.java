package com.spl.geo.exception;

/**
 * @author surpassliang
 * @version 1.0
 * @date 2022/12/17
 */
public class DbfException extends RuntimeException {
    public DbfException() {
    }

    public DbfException(String message) {
        super(message);
    }

    public DbfException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbfException(Throwable cause) {
        super(cause);
    }

    public DbfException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
