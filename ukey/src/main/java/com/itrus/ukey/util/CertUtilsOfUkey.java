package com.itrus.ukey.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Created by jackie on 14-9-3.
 */
public class CertUtilsOfUkey {
    /**
     * 转换为统一格式序列号
     * 大体分为3类情况：
     * 1. 转换成16进制标识的可视字符，字符数为奇数。
     * 2. 转换成16进制标识的可视字符，字符数为偶数，有效比特位首位为1时（十六进制可视化表示时，首位字符为“8”，“9”，“A”，“B”，“C”，“D”，“E”，“F”）。
     * 3. 转换成16进制标识的可视字符，字符数为偶数，有效比特位首位为1时（十六进制可视化表示时，首位字符为情况2描述以外的字符）。
     *
     * 统一编码规则：
     * 情况1：首位补一个“0”        例：“0632”
     * 情况2：首位仅补一个“00”   例：“008FCA”
     * 情况3：保持                    例：“7F1B”
     *
     * 字母采用大写方式
     * @param serialNumber
     * @return String 16进制字符串
     */
    public static String getValidSerialNumber(String serialNumber) {
        if(StringUtils.isNotBlank(serialNumber)){
            serialNumber = serialNumber.trim().toUpperCase(Locale.ENGLISH);//去除前后空格，并将英文字母大写
            if (serialNumber.length() % 2 == 1)// java有可能把第一个0去掉，不可能为单数
                serialNumber = "0" + serialNumber;
            String firstWord = serialNumber.substring(0, 1);
            int firstNumber = 0;
            try {
                firstNumber = Integer.parseInt(firstWord, 16);
            } catch (Exception e) {
                return serialNumber;
            }
            if (firstNumber >= 8) {
                return "00" + serialNumber;
            } else {
                return serialNumber;
            }
        }else{
            return serialNumber;
        }
    }
}
