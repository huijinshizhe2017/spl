package com.spl.geo.exception;

/**
 * @author surpassliang
 * @version 1.0
 * @date 2022/12/17
 */
public class CoordinatorException extends RuntimeException {
    public CoordinatorException() {
    }

    public CoordinatorException(String message) {
        super(message);
    }

    public CoordinatorException(String message, Throwable cause) {
        super(message, cause);
    }

    public CoordinatorException(Throwable cause) {
        super(cause);
    }

    public CoordinatorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
