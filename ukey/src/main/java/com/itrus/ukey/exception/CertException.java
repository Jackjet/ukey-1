package com.itrus.ukey.exception;

public class CertException extends Exception{
	private static final long serialVersionUID = 1L;
	public CertException() {
        super();
    } 
    public CertException(String msg) {
        super(msg);
    }   
    public CertException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
    public CertException(Throwable throwable) {
        super(throwable);
    }
}
