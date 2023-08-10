package com.spl.geo.exception;

/**
 * @author surpassliang
 * @version 1.0
 * @date 2022/12/17
 */
public class GeometryException extends RuntimeException {
    public GeometryException() {
    }

    public GeometryException(String message) {
        super(message);
    }

    public GeometryException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeometryException(Throwable cause) {
        super(cause);
    }

    public GeometryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
