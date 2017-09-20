package com.itrus.ukey.exception;

/**
 * Created by jackie on 2014/11/26.
 */
public class ESignServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    public ESignServiceException(){
        super();
    }
    public ESignServiceException(String message){
        super(message);
    }
    public ESignServiceException(String msg, Throwable t) {
        super(msg, t);
    }
}
