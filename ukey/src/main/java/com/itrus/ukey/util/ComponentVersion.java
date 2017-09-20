package com.itrus.ukey.util;

public class ComponentVersion {
	static final int EXTENDED_LEN = 10;
	static final String EXTENDED_MAX_FILLING = "0000000000";
	static final String VERSION_SPLIT_STRING = "\\.|,";
	
	String m_strVersion;
	String m_strExtendedVersion;

	public ComponentVersion(){
		
	}
	public ComponentVersion(String strVersion){
		m_strExtendedVersion = Stand2Extended(strVersion);	
		m_strVersion = Extended2Stand(m_strExtendedVersion);
	}
	
	 public static ComponentVersion getInstance(String strExtendedVersion){
		ComponentVersion componentVersion = new ComponentVersion();
		componentVersion.SetExtendedVersion(strExtendedVersion);
		return componentVersion;
	}
	 
	public String GetVersion(){
		return m_strVersion;
	}
	
	public boolean SetVersion(String strVersion){
		String strExtendedVersion = Stand2Extended(strVersion);
		if(strExtendedVersion==null)
			return false;
		
		m_strVersion = Extended2Stand(strExtendedVersion);
		m_strExtendedVersion = strExtendedVersion;
		return true;
	}
	
	public String GetExtendedVersion(){
		return m_strExtendedVersion;
	}
	
	public boolean SetExtendedVersion(String strExtendedVersion){
		String strVersion = Extended2Stand(strExtendedVersion);
		if(strVersion==null)
			return false;
		
		m_strVersion = strVersion;
		m_strExtendedVersion = strExtendedVersion;
		return true;
	}
	
	public static String Extended2Stand(String strExtendedVersion){
		String strVersion = new String();
		String[] strSecs = strExtendedVersion.split(VERSION_SPLIT_STRING);
		for(int i=0;i<strSecs.length;i++){
			String strSec = strSecs[i];
			strSec.trim();
			int iPos = 0;
			int iLen = strSec.length();
			if(iLen!=EXTENDED_LEN)
				return null;
			for(int j=0;j<iLen;j++){
				if(strSec.charAt(j)!='0'){
					break;
				}
				iPos++;
			}
			strVersion += strSec.substring(iPos==iLen?iLen-1:iPos, iLen);
			strVersion += ".";
		}
		if(strVersion.length()>0)
			strVersion = strVersion.substring(0, strVersion.length()-1);
		
		return strVersion;
	}
	
	public static String Stand2Extended(String strVersion){
		String strExtendedVersion = new String();
		String[] strSecs = strVersion.split(VERSION_SPLIT_STRING);
		for(int i=0;i<strSecs.length;i++){
			String strSec = strSecs[i];
			strSec.trim();
			int iLen = strSec.length();
			if(iLen>EXTENDED_LEN)
				return null;
			strExtendedVersion += EXTENDED_MAX_FILLING.substring(0, EXTENDED_LEN - iLen);
			strExtendedVersion += strSec;
			strExtendedVersion += ".";
		}
		if(strExtendedVersion.length()>0)
			strExtendedVersion = strExtendedVersion.substring(0, strExtendedVersion.length()-1);
		
		return strExtendedVersion;
	}

	public static void main(String[] args){
		ComponentVersion cv = new ComponentVersion("2.0.0.01");
		System.out.println("---------------------------");
		System.out.println(cv.GetVersion());
		System.out.println(cv.GetExtendedVersion());
		cv.SetVersion("2.1.0011.22");
		System.out.println("---------------------------");
		System.out.println(cv.GetVersion());
		System.out.println(cv.GetExtendedVersion());
		cv.SetVersion("100");
		System.out.println("---------------------------");
		System.out.println(cv.GetVersion());
		System.out.println(cv.GetExtendedVersion());
		cv.SetExtendedVersion("0000000001.0000009872.0001234567");
		System.out.println("---------------------------");
		System.out.println(cv.GetVersion());
		System.out.println(cv.GetExtendedVersion());
		cv = ComponentVersion.getInstance("0000000001.0000000000.0001234567.1212123456");
		System.out.println("---------------------------");
		System.out.println(cv.GetVersion());
		System.out.println(cv.GetExtendedVersion());
	}

}
