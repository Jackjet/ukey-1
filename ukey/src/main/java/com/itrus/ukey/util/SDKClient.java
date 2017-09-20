package com.itrus.ukey.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cn.emay.sdk.client.api.Client;
/**
 * 获取亿美短信网关client
 * @author jackie
 *
 */
@Component
public class SDKClient {
	private static Client client=null;
	
	private static String softwareSerialNo;
	private static String key;
	private SDKClient(){}
	public synchronized static Client getClient(String softwareSerialNo,String key) throws Exception{
		if(client==null){
			client=new Client(softwareSerialNo,key);
		}
		return client;
	}
	
	public synchronized static Client getClient() throws Exception{
		if(client==null){
			client=new Client(softwareSerialNo, key);
		}
		return client;
	}
	/**
	 * 根据错误编号，返回错误信息
	 * @param errorNum
	 * @return
	 */
	public static String getSendErrorMsg(int errorNum){
		String errorMsg = null;
		switch (errorNum) {
		case 0:
			errorMsg = "短信发送成功";
			break;
		case 17:
			errorMsg = "发送信息失败";
			break;
		case 101:
			errorMsg = "客户端网络故障";
			break;
		case 305:
			errorMsg = "服务器端返回错误，错误的返回值（返回值不是数字字符串）";
			break;
		case 307:
			errorMsg = "目标电话号码不符合规则，电话号码必须是以0、1开头";
			break;
		case 997:
			errorMsg = "平台返回找不到超时的短信，该信息是否成功无法确定";
			break;
		case 998:
			errorMsg = "由于客户端网络问题导致信息发送超时，该信息是否成功下发无法确定";
			break;

		default:
			errorMsg = "未知编号";
			break;
		}
		return errorMsg;
	}
	@Autowired(required=true)
	@Value("#{confInfo.softwareSerialNo}")
	public void setSoftwareSerialNo(String softwareSerialNo) {
		SDKClient.softwareSerialNo = softwareSerialNo;
	}
	@Autowired(required=true)
	@Value("#{confInfo.key}")
	public void setKey(String key) {
		SDKClient.key = key;
	}
	
}
