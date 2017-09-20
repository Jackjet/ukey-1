package com.itrus.ukey.test.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bouncycastle.util.encoders.Base64;

/**
 * 将base64文件解析为图片
 * 
 */
public class ReadFileBase64 {

	public static void main(String[] args) throws IOException {
		//testBase64();
		test2();
	}

	public static void testBase64() throws IOException {
		InputStream in = ReadFileBase64.class.getResourceAsStream("/com/itrus/ukey/test/util/csr.pem");
		String fileBase64 = stream2String(in, "UTF-8");
		byte[] b = Base64.decode(fileBase64);
		File saveFile = new File("F:\\a.jpg");
		OutputStream out = new FileOutputStream(saveFile);
		out.write(b);
		out.flush();
		out.close();
	}

	/**
	 * 文件转换为字符串
	 * 
	 * @param in
	 *            字节流
	 * @param charset
	 *            文件的字符集
	 * @return 文件内容
	 */
	public static String stream2String(InputStream in, String charset) {
		StringBuffer sb = new StringBuffer();
		try {
			Reader r = new InputStreamReader(in, charset);
			int length = 0;
			for (char[] c = new char[1024]; (length = r.read(c)) != -1;) {
				sb.append(c, 0, length);
			}
			r.close();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	public static void test2(){
		InputStream in = ReadFileBase64.class.getResourceAsStream("/com/itrus/ukey/test/util/file1.txt");
		String fileBase64 = stream2String(in, "UTF-8");
		Pattern p = Pattern.compile("\r|\n");
        Matcher m = p.matcher(fileBase64);
        fileBase64 = m.replaceAll(",");
        //再用户noteapd++替换
		System.out.println(fileBase64);
	}
}
