package com.itrus.ukey.exception;
/**
 * 返回null异常
 * @author jackie
 *
 */
public class ServiceNullException extends Exception{
	private static final long serialVersionUID = 1L;
	public ServiceNullException(){
		super();
	}
	public ServiceNullException(String message){
		super(message);
	}
	public ServiceNullException(String msg, Throwable t) {
        super(msg, t);
    }
}
