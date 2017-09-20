package com.itrus.ukey.exception;

/**
 * Created by jackie on 2014/11/11.
 */
public class TerminalServiceException extends Exception {
    private static final long serialVersionUID = 1L;
    public TerminalServiceException(){
        super();
    }
    public TerminalServiceException(String message){
        super(message);
    }
    public TerminalServiceException(String msg, Throwable t) {
        super(msg, t);
    }
}
