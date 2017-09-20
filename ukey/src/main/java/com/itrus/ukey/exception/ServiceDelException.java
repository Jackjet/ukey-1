package com.itrus.ukey.exception;

/**
 * Created by jackie on 2015/3/25.
 */
public class ServiceDelException extends Exception {
    private static final long serialVersionUID = 1L;
    public ServiceDelException(){
        super();
    }
    public ServiceDelException(String message){
        super(message);
    }
    public ServiceDelException(String msg, Throwable t) {
        super(msg, t);
    }
}
