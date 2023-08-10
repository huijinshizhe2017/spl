package com.spl.geo.exception;

/**
 * @author surpassliang
 * @version 1.0
 * @date 2022/12/17
 */
public class RasterException extends RuntimeException {
    public RasterException() {
    }

    public RasterException(String message) {
        super(message);
    }

    public RasterException(String message, Throwable cause) {
        super(message, cause);
    }

    public RasterException(Throwable cause) {
        super(cause);
    }

    public RasterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
