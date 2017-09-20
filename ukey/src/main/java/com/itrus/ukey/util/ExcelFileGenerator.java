package com.itrus.ukey.util;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;

/**
 * 系统数据导出Excel 生成器
 * 
 * @version 1.0
 */
public class ExcelFileGenerator {

	private final int SPLIT_COUNT = 1000; // Excel每个工作簿的行数

	private ArrayList fieldName = null; // excel标题数据集

	private ArrayList fieldData = null; // excel数据内容

	private HSSFWorkbook workBook = null; // POI报表的核心对象

	/**
	 * 构造器
	 * 
	 * @param fieldName
	 *            结果集的字段名
	 * @param data
	 */
	public ExcelFileGenerator(ArrayList fieldName, ArrayList fieldData) {

		this.fieldName = fieldName;
		this.fieldData = fieldData;
	}

	/**
	 * 创建HSSFWorkbook对象
	 * 
	 * @return HSSFWorkbook
	 */
	public HSSFWorkbook createWorkbook() {

		workBook = new HSSFWorkbook();// 创建workbook对象
		int rows = fieldData.size();
		int sheetNum = 1;

//		if (rows % SPLIT_COUNT == 0) {
//			sheetNum = rows / SPLIT_COUNT;
//		} else {
//			sheetNum = rows / SPLIT_COUNT + 1;
//		}

		for (int i = 1; i <= sheetNum; i++) {
			HSSFSheet sheet = workBook.createSheet("Page " + i);// 使用wookbook对象创建sheet对象
			HSSFRow headRow = sheet.createRow(0); // 使用HSSFSheet对象创建row，row的下标从0开始
			for (int j = 0; j < fieldName.size(); j++) {// 循环excel的标题
				HSSFCell cell = headRow.createCell(j);// 使用HSSFRow创建cell，cell的下标从0开始
				// 添加样式
				sheet.setColumnWidth(j, 6000);// 设置每一列的宽度
				// 创建样式
				HSSFCellStyle cellStyle = workBook.createCellStyle();
				// 设置字体
				HSSFFont font = workBook.createFont();// 创建字体对象
				// 将字体变为粗体
				font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
				// 将字体颜色变红色
				short color = HSSFColor.RED.index;
				font.setColor(color);
				cellStyle.setFont(font);// 设置之后的字体

				// 添加样式
				cell.setCellType(HSSFCell.CELL_TYPE_STRING);// 设置单元格的类型
				// poi3.6版本已经内部做了处理，不需设置编码，设置编码格式
				// cell.setEncoding(HSSFCell.ENCODING_UTF_16);

				if (fieldName.get(j) != null) {
					cell.setCellStyle(cellStyle);
					cell.setCellValue((String) fieldName.get(j));// 赋值
				} else {
					cell.setCellStyle(cellStyle);
					cell.setCellValue("-");
				}
			}

			for (int k = 0; k < rows ; k++) {// 分页显示数据
				
				HSSFRow row = sheet.createRow((k + 1));// 使用HSSFSheet对象创建row，row的下标从0开始
				// 将数据内容放入excel单元格
				ArrayList rowList = (ArrayList) fieldData.get((i - 1) + k);// 循环数据集
				for (int n = 0; n < rowList.size(); n++) {
					HSSFCell cell = row.createCell(n);// 使用HSSFRow创建cell，cell的下标从0开始
					if (rowList.get(n) != null) {
						cell.setCellValue((String) rowList.get(n).toString());
					} else {
						cell.setCellValue("");
					}
				}
			}
		}
		return workBook;
	}

	public void expordExcel(OutputStream os) throws Exception {
		workBook = createWorkbook();// 创建工作簿对象excel
		workBook.write(os);// 将workbook对象写到输出流
		os.close();
	}

	/**
	 * 设置下载文件中文件的名称
	 * 
	 * @param filename
	 * @param request
	 * @return
	 */
	public String encodeFilename(String filename, HttpServletRequest request) {
		/**
		 * 获取客户端浏览器和操作系统信息 在IE浏览器中得到的是：User-Agent=Mozilla/4.0 (compatible; MSIE
		 * 6.0; Windows NT 5.1; SV1; Maxthon; Alexa Toolbar)
		 * 在Firefox中得到的是：User-Agent=Mozilla/5.0 (Windows; U; Windows NT 5.1;
		 * zh-CN; rv:1.7.10) Gecko/20050717 Firefox/1.0.6
		 */
		String agent = request.getHeader("USER-AGENT");
		try {
			if ((agent != null) && (-1 != agent.indexOf("Trident"))) {
				String newFileName = URLEncoder.encode(filename, "UTF-8");
				newFileName = StringUtils.replace(newFileName, "+", "%20");
				if (newFileName.length() > 150) {
					newFileName = new String(filename.getBytes("GB2312"),
							"ISO8859-1");
					newFileName = StringUtils.replace(newFileName, " ", "%20");
				}
				return newFileName;
			}
			if ((agent != null) && (-1 != agent.indexOf("Mozilla")))
				return MimeUtility.encodeText(filename, "UTF-8", "B");

			return filename;
		} catch (Exception ex) {
			return filename;
		}
	}

}
