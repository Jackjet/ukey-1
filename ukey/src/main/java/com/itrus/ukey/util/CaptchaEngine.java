package com.itrus.ukey.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
/**
 * 验证码产生引擎
 * @author jackie
 *
 */
public class CaptchaEngine {
	private static final Random random = new Random();
	// 验证码图片的宽度。
	private int width = 80;
	// 验证码图片的高度。
	private int height = 30;
	// 设置默认生成4个验证码
	private int length = 4;
	// 设置备选验证码,默认'A-Z'和'0-9'
	private	String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	/**
	 * 产生验证码，并将验证码保存在session中
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public BufferedImage generatorCaptcha(HttpServletRequest request) throws Exception {
		BufferedImage buffImg = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = buffImg.createGraphics();
		// 设定图像背景色(因为是做背景，所以偏淡)
		g.setColor(getRandColor(200, 250));
		g.fillRect(0, 0, width, height);
		// 创建字体，字体的大小应该根据图片的高度来定。
		Font font = new Font("Times New Roman", Font.HANGING_BASELINE, 28);
		// 设置字体。
		g.setFont(font);

		// 画边框。
		g.setColor(Color.BLACK);
		g.drawRect(0, 0, width - 1, height - 1);
		// 随机产生155条干扰线，使图象中的认证码不易被其它程序探测到。
		// g.setColor(Color.GRAY);
		g.setColor(getRandColor(160, 200));
		for (int i = 0; i < 155; i++) {
			int x = random.nextInt(width);
			int y = random.nextInt(height);
			int xl = random.nextInt(12);
			int yl = random.nextInt(12);
			g.drawLine(x, y, x + xl, y + yl);
		}

		// randomCode用于保存随机产生的验证码，以便用户登录后进行验证。
		StringBuffer randomCode = new StringBuffer();
		int size = base.length();

		// 随机产生4位数字的验证码。
		for (int i = 0; i < length; i++) {
			// 得到随机产生的验证码数字。
			int start = random.nextInt(size);
			String strRand = base.substring(start, start + 1).toUpperCase();

			// 调用函数出来的颜色相同，可能是因为种子太接近，所以只能直接生成
			g.setColor(new Color(20 + random.nextInt(110), 20 + random
					.nextInt(110), 20 + random.nextInt(110)));

			g.drawString(strRand, 18 * i + 5, 24);

			// 将产生的四个随机数组合在一起。
			randomCode.append(strRand);
		}
		// 将四位数字的验证码保存到Session中。
		request.getSession().setAttribute(ComNames.LOGIN_CAPTCHA, randomCode.toString());

		// 图象生效
		g.dispose();

		return buffImg;
	}
	
	private Color getRandColor(int fc, int bc) {// 给定范围获得随机颜色
		Random random = new Random();
		if (fc > 255)
			fc = 255;
		if (bc > 255)
			bc = 255;
		int r = fc + random.nextInt(bc - fc);
		int g = fc + random.nextInt(bc - fc);
		int b = fc + random.nextInt(bc - fc);
		return new Color(r, g, b);
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

}
