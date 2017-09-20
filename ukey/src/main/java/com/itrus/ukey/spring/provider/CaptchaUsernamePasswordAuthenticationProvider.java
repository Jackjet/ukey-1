package com.itrus.ukey.spring.provider;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.itrus.ukey.spring.token.CaptchaUsernamePasswordAuthenticationToken;
import com.itrus.ukey.util.ComNames;
/**
 * 验证码验证provide
 * @author jackie
 *
 */
public class CaptchaUsernamePasswordAuthenticationProvider extends
		DaoAuthenticationProvider {
	@Override
	public boolean supports(Class<?> authentication) {
		return (CaptchaUsernamePasswordAuthenticationToken.class
				.isAssignableFrom(authentication));
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails,
			UsernamePasswordAuthenticationToken authentication)
			throws AuthenticationException {
		CaptchaUsernamePasswordAuthenticationToken captchaToken = (CaptchaUsernamePasswordAuthenticationToken) authentication;
		if(StringUtils.isBlank(captchaToken.getCaptcha())){
			throw new BadCredentialsException(messages.getMessage("CaptchaUsernamePasswordAuthenticationProvider.missingCaptcha", "Missing request captcha")); 
		}	
		
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
		String sessionCaptcha = (String) session.getAttribute(ComNames.LOGIN_CAPTCHA);
		// calculate expected signature   
		if(!captchaToken.getCaptcha().equals(sessionCaptcha)) {  
			throw new BadCredentialsException(messages.getMessage("CaptchaUsernamePasswordAuthenticationProvider.badCaptcha", "Invalid request captcha"));  
		} 
		super.additionalAuthenticationChecks(userDetails, authentication);
	}
}
