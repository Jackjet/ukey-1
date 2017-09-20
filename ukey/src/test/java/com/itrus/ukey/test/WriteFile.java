package com.itrus.ukey.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFile {
	public static void main(String[] args) {
		String sql = "insert into activity_msg_ny(id,project,key_sn,os_type,active_time,online_num) select id,project,key_sn,os_type,date_format(on_line_time,\"%Y%m\"),1 from activity_msg where id > IFNULL((select max(id) from activity_msg_ny),0) LIMIT 0,100000 on duplicate key update id=values(id),online_num=online_num+1;\n\n";
		try {
			File aFile = new File("D:\\excuteSQL.txt");// 指定文件名
			FileOutputStream out = new FileOutputStream(aFile);
			// 建立输出流
			for (int i = 0; i < 150; i++) {

				byte[] b;
				
				b = ("# SQL" + i + "\n").getBytes();
				out.write(b); // 写入文本内容
				b = sql.getBytes();// 进行String到byte[]的转化
				out.write(b); // 写入文本内容
			}
		} catch (IOException e) {
			System.out.println(e.toString());
		}
	}
}
