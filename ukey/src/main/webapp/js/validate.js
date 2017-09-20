/**
 * 统一社会信用代码判断
 * @param code
 * @return
 */
function checkCreditCode(code) {
	var reg = /^([123456789ABCDEFGY]{1})([1239]{1})([0-9]{6})([0-9ABCDEFGHJKLMNPQRTUWXY]{10})$/;
	var str = '0123456789ABCDEFGHJKLMNPQRTUWXY';
	var ws =[1,3,9,27,19,26,16,17,20,29,25,13,8,24,10,30,28];
	var errorMsg;
	if(!code){
		errorMsg = "社会信用代码不能为空";
	}else if(code.length != 18) {
		errorMsg = "社会信用代码长度错误";
	}else if(!reg.test(code)){
		errorMsg = "社会信用代码校验错误";
	}else {
		var i,sum = 0;
		for (i = 0; i < 17; i++) {
			sum += str.indexOf(code.charAt(i)) * ws[i];
		}
		var c18 = 31 - (sum % 31);
		if (c18 == 31) {
			c18 = 0;
		}
		if (str.indexOf(code.charAt(17)) != c18) {
			errorMsg = "社会信用代码校验错误";
		}
	}
	return errorMsg;
}

