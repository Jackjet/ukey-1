package com.itrus.ukey.spring.token;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
/**
 * 用于验证码验证的token
 * @author jackie
 *
 */
public class CaptchaUsernamePasswordAuthenticationToken extends
		UsernamePasswordAuthenticationToken {
	private String captcha;
	private static final long serialVersionUID = 1L;
	public CaptchaUsernamePasswordAuthenticationToken(Object principal,
			Object credentials, String captcha) {
		super(principal, credentials);
		this.captcha = captcha;
	}
	public String getCaptcha() {
		return captcha;
	}
	public void setCaptcha(String captcha) {
		this.captcha = captcha;
	}
	
}
