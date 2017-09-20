package com.itrus.ukey.spring.filter;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.util.Assert;

import com.itrus.ukey.util.ComNames;
/**
 * 验证码过滤器
 * @author jackie
 *
 */
public class CaptchaProcessingFilter extends
		AbstractAuthenticationProcessingFilter {
	private String usernameParameter = "j_username";  
	private String passwordParameter = "j_password";  
	private String captchaParameter = "j_captcha";  
	protected CaptchaProcessingFilter() {
		super("/j_spring_security_check");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException,
			IOException, ServletException {
		String username = request.getParameter(usernameParameter);  
	    String password = request.getParameter(passwordParameter);  
	    String captcha = request.getParameter(captchaParameter);
	    if(StringUtils.isBlank(captcha)){
			throw new BadCredentialsException(messages.getMessage("CaptchaUsernamePasswordAuthenticationProvider.missingCaptcha", "Missing request captcha")); 
		}	
		
		HttpSession session = request.getSession(false);
		String sessionCaptcha = session ==null?null:(String) session.getAttribute(ComNames.LOGIN_CAPTCHA);
		// calculate expected signature   
		if(!captcha.equalsIgnoreCase(sessionCaptcha)) {  
			throw new BadCredentialsException(messages.getMessage("CaptchaUsernamePasswordAuthenticationProvider.badCaptcha", "Invalid request captcha"));  
		}
	    UsernamePasswordAuthenticationToken authRequest =   
	      new UsernamePasswordAuthenticationToken(username, password); 
	    setDetails(request, authRequest);
	    return this.getAuthenticationManager().authenticate(authRequest); 
	}
	
	/**
     * Provided so that subclasses may configure what is put into the authentication request's details
     * property.
     *
     * @param request that an authentication request is being created for
     * @param authRequest the authentication request object that should have its details set
     */
    protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

	public String getUsernameParameter() {
		return usernameParameter;
	}

	public void setUsernameParameter(String usernameParameter) {
		Assert.hasText(usernameParameter, "Username parameter must not be empty or null");
		this.usernameParameter = usernameParameter;
	}

	public String getPasswordParameter() {
		return passwordParameter;
	}

	public void setPasswordParameter(String passwordParameter) {
		Assert.hasText(passwordParameter, "Password parameter must not be empty or null");
		this.passwordParameter = passwordParameter;
	}

	public String getCaptchaParameter() {
		return captchaParameter;
	}

	public void setCaptchaParameter(String captchaParameter) {
		Assert.hasText(captchaParameter, "captcha parameter must not be empty or null");
		this.captchaParameter = captchaParameter;
	}
	
}
