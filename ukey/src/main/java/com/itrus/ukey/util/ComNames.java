package com.itrus.ukey.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用名称类
 * 
 * @author thinker
 *
 */
public final class ComNames {
	public static final String ITRUS_UKEY = "iTrusUKEY";
	public static final String ITRUS_AUX = "iTrusAUX";
	// 自动补全功能查找条目
	public static final int AUTOCOMPLETE_SHOW_NUM = 20;
	// 匹配版本号模式，例 12.123.32.234
	public static final String VERSION_REGEX = "^\\d+\\.\\d+\\.\\d+\\.\\d+$";
	// session存放错误信息的attributed名称
	public static final String AUTHENTICATION_EXCEPTION_MES = "SPRING_SECURITY_LAST_EXCEPTION_MES";
	// 延迟时间，默认为5分钟
	public static final long DELAY_TIME = 5 * 60 * 1000;
	public static final String LOGIN_CAPTCHA = "CAPTCHA_SESSION_KEY";
	// 授权码 有效状态 已完成与PC端的确认，可进行授权操作
	public static final String CODE_STATUS_ENROLL = "ENROLL";
	// 授权码 确认状态 待与PC端进行确认，不可进行授权
	public static final String CODE_STATUS_VERIFYING = "VERIFYING";
	// 已使用
	public static final String CODE_STATUS_COMSUMED = "COMSUMED";

	// 是否开启地税同步
	public static final String OPEN_TAX_SYNC = "openTaxSync";
	//是否开启回写数据给地税
	public static final String OPEN_WRITE_BACK_TAX = "openWriteBackTax";

	// 手机验证授权码状态 有效状态
	public static final int M_PHONE_CODE_ENROLL = 0;
	// 手机验证授权码状态 已使用状态
	public static final int M_PHONE_CDDE_USED = 1;
	// 客户端用户唯一标示
	public static final String CLIENT_UID = "clientUid";
	// 返回客户端邮箱
	public static final String USER_MAIL = "userMail";

	// key序列号
	public static final String KEY_SN = "keySn";
	// 允许的ip
	public static final String ALLOW_IP = "allowIP";

	/** 设置的公用假证书certBase64 **/
	public static final String PUBLICCERTBASE64 = "MIIEITCCA4qgAwIBAgIVALIJIBWD6P3TxJ8iuMmVnFVyRwEAMA0GCSqGSIb3DQEBBQUAMIHIMQswCQYDVQQGEwJDTjEdMBsGA1UECgwUaVRydXNjaGluYSBDby4sIEx0ZC4xHDAaBgNVBAsME0NoaW5hIFRydXN0IE5ldHdvcmsxQDA+BgNVBAsMN1Rlcm1zIG9mIHVzZSBhdCBodHRwczovL3d3dy5pdHJ1cy5jb20uY24vY3RucnBhIChjKTIwMDcxOjA4BgNVBAMMMWlUcnVzY2hpbmEgQ04gRW50ZXJwcmlzZSBJbmRpdmlkdWFsIFN1YnNjcmliZXIgQ0EwHhcNMTUwNzA4MDAwMDAwWhcNMTcwOTE1MjM1OTU5WjCCAUcxIzAhBgNVBAMeGluBbOJeAlMXTtFsNE6nZwmWUI0jTvtRbFP4MR0wGwYJKoZIhvcNAQkBFg5jYUBuYmNhLmNvbS5jbjENMAsGA1UEBh4EAEMATjENMAsGA1UEBx4EW4Fs4jENMAsGA1UECB4EbVlsXzEbMBkGA1UEAR4SADcAMQAzADMAMQA0ADIAMQAwMR8wHQYDVQQUHhYAMQAzADgANQA3ADQANgA1ADAANwA4MRswGQYDVQQJHhJn9Ghlen9ccZBTWTQANwA4U/cxGzAZBgNVBAseEgAwADAAMAAyAC0AMAAwADAAMTElMCMGA1UECx4cAE4AUwBSAFUASQBEADoAaAA1ADUANAA4ADYANzE1MDMGA1UECx4sAEQARQBQAFUASQBEADoAMwAzADAAMgAwADYANwAxADMAMwAxADQAMgAxADAwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAJyWANIJwS6HbhQOV8VrTWBZWCujdiAISErU90s2wvWFoEE8QB6y7b01EhkJmGlx+0sBzGf9OGBOzGxlH3WLINQoBwqWMO2tsNw0+MkBLSoYXTG1N2hSc9HK7kbTLmHoiA3UoD8tNACUjxKQi2/oglZ1AlR18MB2+mVPEa6ChABLAgMBAAGjgYQwgYEwCQYDVR0TBAIwADALBgNVHQ8EBAMCBLAwZwYDVR0fBGAwXjBcoFqgWIZWaHR0cDovL2ljYS1wdWJsaWMuaXRydXMuY29tLmNuL2NnaS1iaW4vaXRydXNjcmwucGw/Q0E9N0QyMzA3MjM3ODU2NjJCRjlEMkU3QTU1NDJFNzBCQ0IwDQYJKoZIhvcNAQEFBQADgYEAfJ7BsLTrHU74J/14y3Uk+t1P8vzSVAtmR8IGHdLKYetaIGMpxSD9Ri5DWk5Ry759HPzfe/3CQnhtHrYvAixH9ZxRcQ8dgpchnlq6QSEHOxUfvUGFPqKya0KjuPKgTJT1NS+wNwjxwrncxO0FbwDD5Lkhrrsl8EulEu+N+VvEsy8=";

	// 授权码在session中名字
	public static final String SESSION_CODE_NAME = "authCode";
	// 签名原文在session中名字
	public static final String SESSION_ORIGINAL_NAME = "originalText";
	// ra连接方式
	public static final String RA_PROTOCOL_API = "ica";
	public static final String RA_PROTOCOL_WS = "webservice";
	public static final String RA_PROTOCOL_ICA = "ica";
	public static final String RA_PROTOCOL_TCA = "webservice";

	public static final String AA_PASS_PORT = "password";
	// 操作系统标记
	public static final String OS_WINDOWS = "windows";
	public static final String OS_ANDROID = "android";
	public static final String OS_IOS = "ios";

	public static final int OS_WIN_TYPE = 1;
	public static final int OS_ANDROID_TYPE = 2;
	public static final int OS_IOS_TYPE = 3;
	public static List<Integer> OS_TYPE_LIST = new ArrayList<Integer>();
	static {
		OS_TYPE_LIST.add(OS_WIN_TYPE);
		OS_TYPE_LIST.add(OS_ANDROID_TYPE);
		OS_TYPE_LIST.add(OS_IOS_TYPE);
	}
}
