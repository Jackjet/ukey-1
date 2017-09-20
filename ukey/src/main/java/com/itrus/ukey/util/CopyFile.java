package com.itrus.ukey.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 复制文件
 *
 */
public class CopyFile {

	/**
	 * 将一个目录中的文件，拷贝到另一个目录中
	 * 
	 * @param oldUrl
	 * @param newUrl
	 * @throws IOException
	 */
	public static void copyFile(String oldUrl, String newUrl)
			throws IOException {
		// 判断原目录是否存在
		File oldFile = new File(oldUrl);
		File[] oldfiles = null;
		if (oldFile.exists()) {
			// 创建目标目录
			new File(newUrl).mkdirs();// 目录不存在就创建
			oldfiles = oldFile.listFiles();
			// 复制文件
			for (File tempfile : oldfiles) {
				if (tempfile.isFile()) {// 判断是否为文件
					copyFile(tempfile, new File(newUrl + File.separator
							+ tempfile.getName()));
				}
			}

		}
	}

	/**
	 * 将sourceFile,写入到targetFile中
	 * 
	 * @param sourceFile
	 * @param targetFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File targetFile)
			throws IOException {
		FileInputStream in = new FileInputStream(sourceFile);// 新建文件输入流并对他进行缓冲
		BufferedInputStream inBuff = new BufferedInputStream(in);

		// 新建文件输出流并对它进行缓冲
		FileOutputStream output = new FileOutputStream(targetFile);
		BufferedOutputStream outBuff = new BufferedOutputStream(output);

		byte[] b = new byte[1024 * 5];
		int len;
		while ((len = inBuff.read(b)) != -1) {
			outBuff.write(b, 0, len);
		}
		// 刷新此缓冲的输出流
		outBuff.flush();

		// 关闭流
		inBuff.close();
		outBuff.close();
		output.close();
		in.close();

	}
}
