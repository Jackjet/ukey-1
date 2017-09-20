
//  密钥用法选项
var KEY_USAGE_UNDEFINED		= 0x00
var KEY_USAGE_ALL			= 0x01
var KEY_USAGE_CRL_SIGN		= 0x02
var KEY_USAGE_CERT_SIGN		= 0x04
var KEY_USAGE_KEY_AGREEMENT	= 0x08
var KEY_USAGE_DATA_ENCIPHERMENT	= 0x10
var KEY_USAGE_KEY_ENCIPHERMENT	= 0x20
var KEY_USAGE_NON_REPUDIATION	= 0x40
var KEY_USAGE_DIGITAL_SIGNATURE	= 0x80

//  签名验签选项
var INPUT_BASE64		= 0x01; //SignMessage和VerifySignature输入参数
var INPUT_HEX			= 0x02; //SignMessage和VerifySignature输入参数
var OUTPUT_BASE64		= 0x04; //以BASE64编码pkcs7签名值
var OUTPUT_HEX			= 0x08; //以16进制字符串编码pkcs7签名值
var	INNER_CONTENT		= 0x10; //SignMessage...signData参数：是否包含原文
var	PLAINTEXT_UTF8		= 0x20; //原文UTF-8编码(SignMessage & VerifySignature函数)，缺省使用GB2312编码原文
var	MIN_CERTSTORE		= 0x40; //最小化证书链(仅包含签名证书)
var	HTML_SHOW			= 0x80; //接受HTML数据并渲染
var	MSG_BASE64			= 0x04; //VerifySignature输入参数
var MSG_HEX				= 0x08; //VerifySignature输入参数
var MSG_IMAGE			= 0x100; //VerifySignature输入参数

//  证书导入导出选项
var MARK_CERT_TO_UNEXPORTABLE = 0x02;//标记证书为不可导出
var DELETE_CERT = 0x04;//删除该证书

//  全局变量
var iTrusPTA;
var ienroll;

function encrypt(msg){
	alert(msg);
	var CertFilter = iTrusPTA.Filter;
	CertFilter.Clear();
	CertFilter.Issuer = ftIssuer.value;
	CertFilter.SerialNumber = ftSerial.value;
	CertFilter.Subject = ftSubject.value;
	var certs = iTrusPTA.MyCertificates;
	return certs.EncryptMessage(msg,OUTPUT_BASE64);
}

function encryptFile(srcPath,desPath){
	var CertFilter = iTrusPTA.Filter;
	CertFilter.Clear();
	CertFilter.Issuer = ftIssuer.value;
	CertFilter.SerialNumber = ftSerial.value;
	CertFilter.Subject = ftSubject.value;
	var certs = iTrusPTA.MyCertificates;
	return certs.EncryptFileEx(srcPath,desPath,OUTPUT_BASE64);
}

function decrypt(msg){
	try{
		alert(iTrusPTA.DecryptMessage(msg,INPUT_BASE64));
		}catch(e){
		alert( "解密失败\n错误代码：0x" + (e.number>0?e.number: 0x100000000+e.number).toString(16));
	}
	
}

function decryptFile(srcPath,desPath){
	iTrusPTA.DecryptFileEx(srcPath,desPath,INPUT_BASE64);
}

/**获取证书信息
 *@param cert证书对象
 *@param field要获取的证书属性字段
 *@return 相应属性的值，如果输入了不支持属性，返回null
 */
function getCertField(cert,field){
	var value =null;
	if(field=="subject"){
		value=cert.Subject;
	}else if(field=="issuer"){
		value=cert.Issuer;
	}else if(field=="serialNumber"){
		value=cert.SerialNumber;
	}else if(field=="validFrom"){
		value=eval(cert.ValidFrom)+"";
	}else if(field=="validTo"){
		value=eval(cert.ValidTo)+"";
	}else if(field=="keyContainer"){
		value=cert.KeyContainer;
	}else if(field=="commonName"){
		value=cert.CommonName;
	}else if(field=="keyUsage"){
		var KeyUsage=cert.KeyUsage;
		usage="";
		if (KeyUsage == 0) usage = " UNDEFINED";
		if (KeyUsage & KEY_USAGE_CRL_SIGN )			usage += " CRL_SIGN";
		if (KeyUsage & KEY_USAGE_CERT_SIGN)			usage += " CERT_SIGN";
		if (KeyUsage & KEY_USAGE_KEY_AGREEMENT)		usage += " KEY_AGREEMENT";
		if (KeyUsage & KEY_USAGE_DATA_ENCIPHERMENT) usage += " DATA_ENCIPHERMENT";
		if (KeyUsage & KEY_USAGE_KEY_ENCIPHERMENT)	usage += " KEY_ENCIPHERMENT";
		if (KeyUsage & KEY_USAGE_NON_REPUDIATION)	usage += " NON_REPUDIATION";
		if (KeyUsage & KEY_USAGE_DIGITAL_SIGNATURE)	usage += " DIGITAL_SIGNATURE";
		value=usage.substr(1);
	}else if(field=="csp"){
		value=cert.CSP;
	}
	return value;
}

/**签名
 * @param plainType 明文的类型，可以为 base64,hex,image,file,
 * @param signedType 输出的签名值的类型，可以为base64,hex
 * @param detached 当为false时，签名数据将会含有原文，当为true时，签名数据没有原文
 * @param plainData 代签名数据，当plainType为image时，该项为图片的url;
 * @param cert 签名使用的私钥对应的证书
 * @return 签名后的数据，签名失败返回null
 */
function SignMessage(plainType,signedType,detached,plainData,cert){
	var signopt=0;
	var signedData = null;
	//设置原文的类型标识
	if(plainType=="base64"){
		signopt=INPUT_BASE64;
	}else if(plainType=="hex"){
		signopt=INPUT_HEX;
	}
//	else if(plainType=="image"){
//		signopt=MSG_IMAGE;
//	}
	//设置签名值类型的标识
	if(signedType == "hex"){
		signopt |= OUTPUT_HEX;
	}else if (signedType=="base64"){		
		signopt |= OUTPUT_BASE64;
	}
	//设置签名值里是否含有原文
	if(!detached){
		signopt|=INNER_CONTENT;
	}
	signopt|=MIN_CERTSTORE;//在签名中，最小化证书列，可以减少签名值长度
//	signopt|=HTML_SHOW;		//弹出的确认框，采用IE引擎渲染样式
//	signopt|=PLAINTEXT_UTF8;//如果签名值是UTF-8编码，需要加上此行，默认UTF-8;
//	signopt|=512;
	try{
		if(plainType=="image") {
			signedData =cert.SignImage(plainData,signopt);
		}else if(plainType=="file"){
			//TODO:
		}else if (plainType=="logon"){
			signedData = cert.SignLogonData(plainData,signopt);
		}else{
			signedData = cert.SignMessage(plainData,signopt);
		}
	}catch(e){
		signedData = null;
		alert(e.message+"["+e.number+"]");
	}
	return signedData;
}

/**验证签名
 * @param msgType 明文的类型，可以为 base64,hex,image,file,
 * @param signedType 输出的签名值的类型，可以为base64,hex
 * @param detached 当为false时，签名数据里含有原文，当为true时，签名数据里没有原文
 * @param plainData 代签名数据，当plainType为image时，该项为图片的url;
 * @param signedData 签名后的数据
 * @return 验签结果，验签成功返回对应证书，验签失败，返回null;
 */
function VerifySignature(msgType,signedType,detached,plainData,signedData){
	var signopt=0;
	//设置原文的类型标识
	if(msgType=="base64"){
		signopt=MSG_BASE64;
	}else if(msgType=="hex"){
		signopt=MSG_HEX;
	}else if(msgType=="image"){
		signopt=MSG_IMAGE;
	}
	//设置签名值类型的标识
	if(signedType == "hex"){
		signopt |= INPUT_HEX;
	}else if (signedType=="base64"){		
		signopt |= INPUT_BASE64;
	}
	//设置签名值里是否含有原文
	if(!detached){
		signopt|=INNER_CONTENT;
	}
	var Signer = null;
	try{
		Signer=iTrusPTA.VerifySignature(plainData,signedData,signopt);
	}catch(e){
		alert( "签名验证失败。\n错误代码：0x" + (e.number>0?e.number: 0x100000000+e.number).toString(16));
	}
	return Signer;
}

/**删除证书
 * @param cert 要删除的证书
 * @return void;
 */
function removeCert(cert){
	try{
		cert.Remove();
		return true;
	}catch(e){
		//alert("error code:["+iTrusPTA.ErrorCode+"]");
		return false;
	}
}

/**导入证书
 * @param certPath 待导入的证书路径
 * @param password 待导入证书的密码
 * @param option 导入证书的选项 MARK_CERT_TO_UNEXPORTABLE；
 * @return void
 */
function importCert(certPath,password,option){	
	try{
		iTrusPTA.ImportPKCS12(certPath,password,option);
		return true;
	}catch(e){
		//alert("error code:["+iTrusPTA.ErrorCode+"]");
		return false;
	}
}

/**导出证书
 * @param certPath 导出的证书路径
 * @param password 导出证书的密码
 * @param option 导出证书的选项 MARK_CERT_TO_UNEXPORTABLE | DELETE_CERT;
 * @return void
 */
function exportCert(cert, option, password){
	try{
		var filePath = iTrusPTA.GetExportPath(cert.CommonName);
		var iRet = cert.ExportPKCS12(password,option,filePath);
		return true;
	}catch(e){
		//alert("code:[" + toHex(e.number) + "],message[" + e.message+"]");
		return false;
	}
}

//申请证书
function enroll(name){
	ienroll.Reset();
	ienroll.DeleteRequestCert = true;
	ienroll.ProviderType = 1;
	ienroll.ProviderName = "Microsoft Base Cryptographic Provider v1.0";			
	ienroll.HashAlgorithm = "MD5";
	ienroll.KeySpec = 1;		
	ienroll.GenKeyFlags = 0x04000000 | 0; //1024bits
	var p10ret = ienroll.createPKCS10("CN="+name, "1.3.6.1.5.5.7.3.2");
	return p10ret;
}

//让用户选择导出的路径,并返回选择的路径
function exportPath(){
	var path = ienroll.GetExportPath("");
	return path;
}

//让用户选择导入的路径，并返回选择的路径
function importPath(){
	var path = ienroll.GetImportPath("");
	return path;
}

//产生用户登录时随机数
function getLogonData(){
	var logonData="LOGONDATA:"+Date()+"|"+Math.random().toString().substr(2);
	return logonData
}


/**
 * filterCerts 根据所设置条件过滤证书
 * @param arrayIssuerDN(optional) Array() or string，缺省为""，证书的颁发者字符串和字符串数组，支持多个CA时使用字符串数组
 * @param arraySerialNumber(optional)缺省为""，证书序列号（微软格式）
 * @param dateFlag(optional) 缺省为0，0表示所有证书，1表示处于有效期内的证书，2表示待更新证书，3表示未生效或已过期证书
 * @param keyUsage(option) 缺省为1;具体可以为密钥用法的各个值及相应组合
 * @return Array(), PTALib.Certificate
 */
function filterCerts(arrayIssuerDN, subject, arraySerialNumber, dateFlag, keyUsage, weak) {
	var m_certs = new Array();
	var i = 0;var m=0;
	if (typeof(arrayIssuerDN) == "undefined") {
		arrayIssuerDN = new Array("");
	} else if (typeof(arrayIssuerDN) == "string") {
		arrayIssuerDN = new Array(arrayIssuerDN);
	}
	if (typeof(arraySerialNumber) == "undefined"){
		arraySerialNumber = new Array("");
	} else if (typeof(arraySerialNumber) == "string") {
		arraySerialNumber = new Array(arraySerialNumber);
	}
	
	if(typeof(subject) == "undefined")
		subject = "";
	for (i = 0; i < arrayIssuerDN.length; i++) {
		for(m = 0; m < arraySerialNumber.length; m++){
			var CertFilter = iTrusPTA.Filter;
			CertFilter.Clear();
//			alert(arrayIssuerDN[i].length);
			CertFilter.Issuer = arrayIssuerDN[i];
//			alert(arraySerialNumber[m].length);
			CertFilter.SerialNumber = arraySerialNumber[m];
//			alert(subject.length);
			CertFilter.Subject = subject;
//			alert("issuer:"+arrayIssuerDN[i]+":"+CertFilter.Issuer);
//			alert("serial:"+arraySerialNumber[m]+":"+CertFilter.SerialNumber);
			var t_Certs = iTrusPTA.MyCertificates; // 临时变量
			var now = new Date();
			var t_count = parseInt(t_Certs.Count);
			if ( t_count> 0) { // 找到了证书
				for (var j = 1; j <= t_count; j++) {
					if(!containUsage(t_Certs.Item(j),keyUsage,weak))
						continue;
					var validFrom = eval(t_Certs.Item(j).ValidFrom);
					var validTo = eval(t_Certs.Item(j).ValidTo);
					switch (dateFlag) {
						case 0 :// 所有证书
							m_certs.push(t_Certs.Item(j));
							break;
						case 1 :// 处于有效期内的证书
							if (validFrom < now && now < validTo)
								m_certs.push(t_Certs.Item(j));
							break;
						case 2 :// 待更新证书
							if (JSDateAdd(validTo, -30) < now && now < validTo)
								m_certs.push(t_Certs.Item(j));
							break;
						case 3 :// 未生效或已过期证书
							if (now < validFrom || validTo < now)
								m_certs.push(t_Certs.Item(j));
							break;
						default :// 缺省当作所有证书处理
							m_certs.push(t_Certs.Item(j));
							break;
					}
				}
			}
		}
	}
	return m_certs;
}

/**判断该证书是否包含给出的密钥用法
 * @param cert  要判断的证书；
 * @param usage 要判断的密钥用法；
 * @param weak  判断的模式，当weak为true时，只要证书的
 *			密钥用法包含所给出用法的任意一个就返回true。否则需要包含所有的给出的用法,才返回true
 *
*/
function containUsage(cert,usage,weak){
	var keyUsage=cert.KeyUsage;
	var flag = true;
	if(weak){
		if((keyUsage&usage)==0){
			flag=false;
		}
	}else{
		if (usage & KEY_USAGE_CRL_SIGN)
			flag= flag &&(keyUsage & KEY_USAGE_CRL_SIGN);
		if (usage & KEY_USAGE_CERT_SIGN)
			flag= flag &&(keyUsage & KEY_USAGE_CERT_SIGN)
		if (usage & KEY_USAGE_KEY_AGREEMENT)
			flag= flag &&(keyUsage & KEY_USAGE_KEY_AGREEMENT);
		if (usage & KEY_USAGE_DATA_ENCIPHERMENT)
			flag= flag &&(keyUsage & KEY_USAGE_DATA_ENCIPHERMENT);
		if (usage & KEY_USAGE_KEY_ENCIPHERMENT)
			flag= flag &&(keyUsage & KEY_USAGE_KEY_ENCIPHERMENT);
		if (usage & KEY_USAGE_NON_REPUDIATION)
			flag= flag &&(keyUsage & KEY_USAGE_NON_REPUDIATION);
		if (usage & KEY_USAGE_NON_REPUDIATION)
			flag= flag &&(keyUsage & KEY_USAGE_NON_REPUDIATION);
	}
	return flag;
}

/**
 * JSDateAdd Javascript 计算给定日期+天数
 * @param theDate:给定日期，Date类型
 * @param days:整型
 * @return 计算结果，Date类型
 */
function JSDateAdd(theDate, days) {
	var dateValue = theDate.valueOf();
	dateValue += days * 1000 * 60 * 60 * 24;
	var newDate = new Date(dateValue);
	return newDate;
}

/**
 * JSDateDiffByDays Javascript 计算两个日期之间的间隔天数
 * @param date1:给定日期1，Date类型
 * @param date2:给定日期2，Date类型
 * @return 天数，整型
 */
function JSDateDiffByDays(date1, date2) {
	var mill = date1.valueOf() - date2.valueOf();
	var millStr = new String(mill / 1000 / 60 / 60 / 24)
	return parseInt(millStr);
}

//从证书数组里获取，主题中的name为value的证书
function chooseCertFromArray(certs,name,value){
	var cert = null;
	var i,count = certs.length;
	for(i=0;i<count;i++){
		cert = certs[i];
		var names = new Names(getCertField(certs[i],"subject"));
		if(value==names.getItem(name))
			return certs[i];
	}
	return null;
}

//根据颁发者和序列号过滤证书，如果存在，返回第一张证书，否则返回null
function selectSingleCert(issuer,serial){
	var filter=iTrusPTA.Filter;
	filter.Clear();
	if(issuer.length>0)
		filter.Issuer=issuer;
	if(serial.length>0)
		filter.SerialNumber=serial;
	if(iTrusPTA.MyCertificates.Count==0){
		alert("未找到指定的数字证书");
		return null;
	}

	return iTrusPTA.MyCertificates.Item(1);
}

//将数据转化为十六进制格式
function toHex(number)
{
	number = number >>> 0;
	return number.toString(16);
}
//Base64Encode
function Base64Encode(str) {
	var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
	var encoded = [];
	var c = 0;
	while (c < str.length) {
		var b0 = str.charCodeAt(c++);
		var b1 = str.charCodeAt(c++);
		var b2 = str.charCodeAt(c++);
		var buf = (b0 << 16) + ((b1 || 0) << 8) + (b2 || 0);
		var i0 = (buf & (63 << 18)) >> 18;
		var i1 = (buf & (63 << 12)) >> 12;
		var i2 = isNaN(b1) ? 64 : (buf & (63 << 6)) >> 6;
		var i3 = isNaN(b2) ? 64 : (buf & 63);
		encoded[encoded.length] = chars.charAt(i0);
		encoded[encoded.length] = chars.charAt(i1);
		encoded[encoded.length] = chars.charAt(i2);
		encoded[encoded.length] = chars.charAt(i3);
	}
	return encoded.join('');
}
//Base64Decode
function Base64Decode(str) {
	var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
	var invalid = {
		strlen: (str.length % 4 != 0),
		chars:  new RegExp('[^' + chars + ']').test(str),
		equals: (/=/.test(str) && (/=[^=]/.test(str) || /={3}/.test(str)))
	};
	if (invalid.strlen || invalid.chars || invalid.equals)
		alert('Invalid base64 data');
	var decoded = [];
	var c = 0;
	while (c < str.length) {
		var i0 = chars.indexOf(str.charAt(c++));
		var i1 = chars.indexOf(str.charAt(c++));
		var i2 = chars.indexOf(str.charAt(c++));
		var i3 = chars.indexOf(str.charAt(c++));
		var buf = (i0 << 18) + (i1 << 12) + ((i2 & 63) << 6) + (i3 & 63);
		var b0 = (buf & (255 << 16)) >> 16;
		var b1 = (i2 == 64) ? -1 : (buf & (255 << 8)) >> 8;
		var b2 = (i3 == 64) ? -1 : (buf & 255);
		decoded[decoded.length] = String.fromCharCode(b0);
		if (b1 >= 0) decoded[decoded.length] = String.fromCharCode(b1);
		if (b2 >= 0) decoded[decoded.length] = String.fromCharCode(b2);
	}
	return decoded.join('');
}

/**
 * class Names
 * 
 * @method getItem(name) return names' first value
 * @method getItems(name) return names' value sting array object
 */
function Names(distinguishName) {
	this.names = init(distinguishName);

	this.getItem = function(name) {
		var values = this.names.get(name);
		if (null == values) {
			return null;
		} else {
			return values[0];
		}
	}

	this.getItems = function(name) {
		return this.names.get(name);
	}

	function init(dn) {
		var _names = new Hashtable();
		var partition = ", ";

		var Items = dn.split(partition);
		var itemString = "";
		for (var i = Items.length - 1; i >= 0; i--) {
			if (itemString != "") {
				itemString = Items[i] + itemString;
			} else {
				itemString = Items[i];
			}

			var pos = itemString.indexOf("=");
			if (-1 == pos) {
				itemString = partition + itemString;
				continue;
			} else {
				var name = itemString.substring(0, pos);
				var value = itemString.substring(pos + 1, itemString.length);
				// wipe off the limitrophe quotation marks
				if (value.indexOf("\"") == 0
						&& (value.length - 1) == value.lastIndexOf("\"")) {
					value = value.substring(1, value.length);
					value = value.substring(0, value.length - 1);
				}

				if (_names.containsKey(name)) {
					var array = _names.get(name);

					array.push(value);
					_names.remove(name);
					_names.put(name, array);
				} else {
					var array = new Array();
					array.push(value);
					_names.put(name, array);
				}
				itemString = "";
			}
		}
		return _names;
	}
}


/*******************************************************************************
 * Object: Hashtable Description: Implementation of hashtable Author: Uzi
 * Refaeli
 ******************************************************************************/

// ======================================= Properties
// ========================================
Hashtable.prototype.hash = null;
Hashtable.prototype.keys = null;
Hashtable.prototype.location = null;

/**
 * Hashtable - Constructor Create a new Hashtable object.
 */
function Hashtable() {
	this.hash = new Array();
	this.keys = new Array();

	this.location = 0;
}

Hashtable.prototype.containsKey = function(key) {
	if (this.hash[key] == null)
		return false;
	else
		return true;
}

/**
 * put Add new key param: key - String, key name param: value - Object, the
 * object to insert
 */
Hashtable.prototype.put = function(key, value) {
	if (value == null)
		return;

	if (this.hash[key] == null)
		this.keys[this.keys.length] = key;

	this.hash[key] = value;
}

/**
 * get Return an element param: key - String, key name Return: object - The
 * requested object
 */
Hashtable.prototype.get = function(key) {
	return this.hash[key];
}

/**
 * remove Remove an element param: key - String, key name
 */
Hashtable.prototype.remove = function(key) {
	for (var i = 0; i < this.keys.length; i++) {
		// did we found our key?
		if (key == this.keys[i]) {
			// remove it from the hash
			this.hash[this.keys[i]] = null;
			// and throw away the key...
			this.keys.splice(i, 1);
			return;
		}
	}
}

/**
 * size Return: Number of elements in the hashtable
 */
Hashtable.prototype.size = function() {
	return this.keys.length;
}

/**
 * populateItems Deprecated
 */
Hashtable.prototype.populateItems = function() {
}

/**
 * next Return: true if theres more items
 */
Hashtable.prototype.next = function() {
	if (++this.location < this.keys.length)
		return true;
	else
		return false;
}

/**
 * moveFirst Move to the first item.
 */
Hashtable.prototype.moveFirst = function() {
	try {
		this.location = -1;
	} catch (e) {/* //do nothing here :-) */
	}
}

/**
 * moveLast Move to the last item.
 */
Hashtable.prototype.moveLast = function() {
	try {
		this.location = this.keys.length - 1;
	} catch (e) {/* //do nothing here :-) */
	}
}

/**
 * getKey Return: The value of item in the hash
 */
Hashtable.prototype.getKey = function() {
	try {
		return this.keys[this.location];
	} catch (e) {
		return null;
	}
}

/**
 * getValue Return: The value of item in the hash
 */
Hashtable.prototype.getValue = function() {
	try {
		return this.hash[this.keys[this.location]];
	} catch (e) {
		return null;
	}
}

/**
 * getKey Return: The first key contains the given value, or null if not found
 */
Hashtable.prototype.getKeyOfValue = function(value) {
	for (var i = 0; i < this.keys.length; i++)
		if (this.hash[this.keys[i]] == value)
			return this.keys[i]
	return null;
}

/**
 * toString Returns a string representation of this Hashtable object in the form
 * of a set of entries, enclosed in braces and separated by the ASCII characters ", "
 * (comma and space). Each entry is rendered as the key, an equals sign =, and
 * the associated element, where the toString method is used to convert the key
 * and element to strings. Return: a string representation of this hashtable.
 */
Hashtable.prototype.toString = function() {

	try {
		var s = new Array(this.keys.length);
		s[s.length] = "{";

		for (var i = 0; i < this.keys.length; i++) {
			s[s.length] = this.keys[i];
			s[s.length] = "=";
			var v = this.hash[this.keys[i]];
			if (v)
				s[s.length] = v.toString();
			else
				s[s.length] = "null";

			if (i != this.keys.length - 1)
				s[s.length] = ", ";
		}
	} catch (e) {
		// do nothing here :-)
	} finally {
		s[s.length] = "}";
	}

	return s.join("");
}

/**
 * add Concatanates hashtable to another hashtable.
 */
Hashtable.prototype.add = function(ht) {
	try {
		ht.moveFirst();
		while (ht.next()) {
			var key = ht.getKey();
			// put the new value in both cases (exists or not).
			this.hash[key] = ht.getValue();
			// but if it is a new key also increase the key set
			if (this.get(key) != null) {
				this.keys[this.keys.length] = key;
			}
		}
	} catch (e) {
		// do nothing here :-)
	} finally {
		return this;
	}
};