package com.itrus.ukey.util;

/**
 * Created by jackie on 2015/1/12.
 * 解锁返回编码
 */
public class UnlockCode {
    //配置不正常
    public static final int ERROR_A_CONFIG = 101;
    //不存在设备信息
    public static final int ERROR_A_NO_DEVICE = 102;
    //设备未关联用户
    public static final int ERROR_A_NO_BIND = 103;
    //设备和证书与记录不一致 cert and device
    public static final int ERROR_A_CAD = 104;
    //未添加手机号或手机号未验证
    public static final int ERROR_A_E_PHONE = 105;
}
