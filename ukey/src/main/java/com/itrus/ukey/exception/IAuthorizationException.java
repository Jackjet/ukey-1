package com.itrus.ukey.exception;

/**
 * 权限不足异常
 * Created by thinker on 2015/3/16.
 */
public class IAuthorizationException extends Exception{
    private static final long serialVersionUID = 1L;
    public IAuthorizationException(){
        super();
    }
    public IAuthorizationException(String message){
        super(message);
    }
    public IAuthorizationException(String msg, Throwable t) {
        super(msg, t);
    }
}
