package com.itrus.ukey.test.web;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class HomeControllerTest {

	@Test
	public void testQRCode() {
		String contents = "https://ukey.itrus.com.cn/itrusca/m";
		int width = 100;
		int height = 100;
		String imgPath = "D:/QRCode.png";
		Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
		// 指定纠错等级
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		// 指定编码格式
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		hints.put(EncodeHintType.MARGIN, 1);
		try {
			BitMatrix bitMatrix = new MultiFormatWriter().encode(contents,
					BarcodeFormat.QR_CODE, width, height, hints);

			MatrixToImageWriter.writeToFile(bitMatrix, "png", new File(imgPath));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
