package com.itrus.ukey.exception;

/**
 * Created by jackie on 2014/11/12.
 * 加解密异常
 */
public class EncDecException extends Exception {
    private static final long serialVersionUID = 1L;
    public EncDecException() {
        super();
    }
    public EncDecException(String msg) {
        super(msg);
    }
    public EncDecException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
    public EncDecException(Throwable throwable) {
        super(throwable);
    }
}
