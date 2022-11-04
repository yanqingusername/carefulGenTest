package com.org.framework.util;


/**
 * User: rizenguo
 * Date: 2014/10/29
 * Time: 14:40
 * 这里放置各种配置数据
 */
public class Configure {
	//这个就是自己要保管好的私有Key了（切记只能放在自己的后台代码里，不能放在任何可能被看到源代码的客户端程序中）
	// 每次自己Post数据给API的时候都要用这个key来对所有字段进行签名，生成的签名会放在Sign这个字段，API收到Post数据的时候也会用同样的签名算法对Post过来的数据进行签名和验证
	// 收到API的返回的时候也要用这个key来对返回的数据算下签名，跟API的Sign数据进行比较，如果值不一致，有可能数据被第三方给篡改

	//云片发送短信key
	private static String apikey = "94d2a3412b632da4e568aebdcb3df497";
	private static String ApiKey = "8bdd9d4650261052070b440355479b0b";
	//云片单发短信接口
	private static String urlForMsg = "https://sms.yunpian.com/v2/sms/single_send.json";
		
	//微信分配的公众号ID（开通公众号之后可以获取到）
	private static String appID = "wx8f54bf05b01f77f4";//Care服务号
	private static String AppSecrte="06bf3b5ce791a3c42046cc941549d945";//Care服务号
	//	//保存对应的域名
	private static String appIp = "http://store.coyotebio-lab.com/carefulGenTest/";
//	// 绑定成功，模板消息-》 绑定成功通知 -》OPENTM407402933
	private static String bindSuccessModelId = "kxSaH15dyclDwWqARDGC3J8tntq78CsX8SlXZxTBw8s";
	
	//测试
	//private static String appID = "wxf62881bfaa2822f4";
	//private static String AppSecrte="e3a89283fc9c5af1e4cfad296d01835e";
	
	//private static String appIp = "http://syrdev.coyotebio-lab.com/carefulGenTest/";
	//private static String bindSuccessModelId = "undlckOfUD71T_-OMQuIBhqnP_dETqbyrvjNKZ4cGlY";
	/*private static String appID = "wxc68ef077311cc36f";//卡尤迪精准健康管家
    private static String AppSecrte="f4b3eab1f460693cc23f06410ea65147";//卡尤迪精准健康管家
*/	//快递鸟电子面单相关参数
//	private static String KDNEBusinessID = "1462056"; // 正式EBusinessID
//	private static String KDNAppKey = "941ec0ba-092e-4051-bd22-374f336a280c"; // 正式AppKey 
//	private static String KDNReqURL = "http://api.kdniao.com/api/EOrderService"; // 正式请求url
	private static String KDNEBusinessID="1707904";
	private static String KDNAppKey="bde2a579-62db-4e3d-a996-a0e3efe35d24";
	private static String KDNReqURL = "http://api.kdniao.com/api/EOrderService";
	
	// 测试
//	private static String KDNEBusinessID = "test1462056";
//	private static String KDNAppKey = "954b344c-c8cd-494d-ad63-4968cb07f1e9";
//	private static String KDNReqURL = "http://sandboxapi.kdniao.com:8080/kdniaosandbox/gateway/exterfaceInvoke.json";
//	private static String KDNEBusinessID = "test1707904";
//	private static String KDNAppKey = "e173711a-79c0-4e9f-8313-390cec3213c7";
//	private static String KDNReqURL = "http://sandboxapi.kdniao.com:8080/kdniaosandbox/gateway/exterfaceInvoke.json";
	//---------------阿里云短信模块-----------------------
	//阿里云发短信接口
	private static String UrlForSMS = "http://cloud.coyotebio-lab.com/smsApi/sendsms.hn";

	//阿里云短信模板代号
	//验证码
	//您的验证码是${code}。如非本人操作，请忽略本短信。
	private static String CodeForVerification = "SMS_213678955";

	//尊敬的客户，感谢您购买我们的产品，微信搜索并关注公众号："Careful Gen"，点击"检测服务->Care商城->个人中心"即可查看物流和报告，祝您生活愉快！
	private static String CodeForOrderSuccessfullyPlaced  = "SMS_215065993";

	public static String getUrlForSMS() {
		return UrlForSMS;
	}

	public static void setUrlForSMS(String urlForSMS) {
		UrlForSMS = urlForSMS;
	}

	public static String getCodeForVerification() {
		return CodeForVerification;
	}

	public static void setCodeForVerification(String codeForVerification) {
		CodeForVerification = codeForVerification;
	}

	public static String getCodeForOrderSuccessfullyPlaced() {
		return CodeForOrderSuccessfullyPlaced;
	}

	public static void setCodeForOrderSuccessfullyPlaced(
			String codeForOrderSuccessfullyPlaced) {
		CodeForOrderSuccessfullyPlaced = codeForOrderSuccessfullyPlaced;
	}

	public static String getCodeForCodeSuccessfullyBinding() {
		return CodeForCodeSuccessfullyBinding;
	}

	public static void setCodeForCodeSuccessfullyBinding(
			String codeForCodeSuccessfullyBinding) {
		CodeForCodeSuccessfullyBinding = codeForCodeSuccessfullyBinding;
	}

	//尊敬的用户，您好！您已经成功绑定样本编码，请您按接下来的步骤继续操作，祝您生活愉快！
	private static String CodeForCodeSuccessfullyBinding  = "SMS_214525721";
	
	//尊敬的用户，您好！您已经成功绑定样本编号，信息如下： 受检人姓名：${name} 样本编号：${sample_id} 检测项目：${test_name} 温馨提示：请您尽快安排采样并回寄样本，如您有任何疑问，敬请您致电客服电话 ${service_tel}，我们真诚地期待为您提供服务。
	//private static String CodeForCodeSuccessfullyBinding2  = "SMS_219617137";
		
	//尊敬的用户，您好！您已经成功绑定样本编号，信息如下： 受检人姓名：${name} 样本编号：${sample_id} 温馨提示：请您尽快安排采样并回寄样本，如您有任何疑问，敬请您致电客服电话 ${service_tel}，我们真诚地期待为您提供服务。
	private static String CodeForCodeSuccessfullyBinding2  = "SMS_221726837";
		
	public static String getAppid(){
		return appID;
	}
	
	public static String getAppSecrte() {
		return AppSecrte;
	}

	public static void setAppSecrte(String appSecrte) {
		AppSecrte = appSecrte;
	}

	
	public static String getApikey() {
		return apikey;
	}
	public static String getApiKeys() {
		return ApiKey;
	}
	
	public static String getUrlForMsg() {
		return urlForMsg;
	}
	
	public static String getAppIp() {
		return appIp;
	}


	public static String getBindSuccessId() {
		return bindSuccessModelId;
	}
	
	public static String getKDNEBusinessID() {
		return KDNEBusinessID;
	}
	
	public static String getKDNAppKey() {
		return KDNAppKey;
	}
	
	public static String getKDNReqURL() {
		return KDNReqURL;
	}

	public static String getCodeForCodeSuccessfullyBinding2() {
		return CodeForCodeSuccessfullyBinding2;
	}

	public static void setCodeForCodeSuccessfullyBinding2(String codeForCodeSuccessfullyBinding2) {
		CodeForCodeSuccessfullyBinding2 = codeForCodeSuccessfullyBinding2;
	}
	
	
}
