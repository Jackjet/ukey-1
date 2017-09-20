package com.itrus.ukey.exception;
/**
 * 移动终端处理异常信息
 * @author jackie
 *
 */
public class MobileHandlerServiceException extends Exception {
	private static final long serialVersionUID = 1L;
	public MobileHandlerServiceException(){
		super();
	}
	public MobileHandlerServiceException(String message){
		super(message);
	}
	public MobileHandlerServiceException(String msg, Throwable t) {
        super(msg, t);
    }
}
