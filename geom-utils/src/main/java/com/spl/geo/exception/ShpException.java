package com.spl.geo.exception;

/**
 * 扫描类异常
 *
 * @author surpassliang
 * @version 1.0
 * @date 2022/11/25
 */
public class ShpException extends RuntimeException {

    public ShpException(String message, Throwable cause){
        super(message,cause);
    }

    public ShpException(Throwable cause) {
        super(cause);
    }
    public ShpException(String message) {
        super(message);
    }


}
