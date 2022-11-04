package com.org.framework.controller;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.util.GraphicsRenderingHints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import sun.security.x509.X400Address;

import com.google.zxing.Result;
import com.org.framework.service.UtilMethodService;
import com.org.framework.util.AccessTokenUtil;
import com.org.framework.util.Configure;
import com.org.framework.util.ResponseUtils;
import com.org.framework.util.Sign;
import com.org.framework.util.jsapi_ticketGetUtil;

@Controller
@RequestMapping("/api")
public class CommonApiController {

	private static final String DB = "sys";
	private static final String HealthDB = "health";

	@Autowired
	public UtilMethodService utilMethodService;
	private static Logger logger = Logger.getLogger(CommonApiController.class);

	/**
	 * 进入检测页面
	 * @param request
	 * @param response
	 */
	@RequestMapping("/enterPage.hn")
	public void enterPage(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入检测页面时触发的方法start");
			String code = request.getParameter("code") == null ? "" : request.getParameter("code").trim();
			logger.info("enterPage code:" + code);
			if ("".equals(code) || "undefined".equals(code)) {
				jsonback.put("success", false);
				jsonback.put("msg", "没对公众号授权！");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，进入检测页面时触发的方法end：" + jsonback.toString());
				return;
			}

			String openid = (String)request.getSession().getAttribute("user_id");
			logger.info("我是从session中获取的openid****"+openid);
			if ("".equals(openid) || "undefined".equals(openid) || openid==null) {
				JSONObject jsonObject = getAccessToken(code);
				logger.info("access token is" + jsonObject);
				if (!jsonObject.getBoolean("success")) {
					jsonObject.put("success", false);
					jsonObject.put("msg", "获取用户信息失败！");
					ResponseUtils.setToHttp(response, jsonback.toString());
					logger.info("care检测，进入检测页面时触发的方法end：" + jsonback.toString());
					return;
				}

				// 再session中存储openid
				openid = (String) jsonObject.get("openid");
				request.getSession().setAttribute("user_id", openid);
			}

			// 根据样本号获取检测记录
			String status = "0"; // 0 无绑定记录 1有绑定记录
			String sql = "SELECT COUNT(1) as num FROM care_userInfo WHERE sampling_wx_id=? and sample_status in (?,?,?,?)";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(openid);
			params.add("2"); // 2已绑定 （用户在公众号上进行了绑定）  
			params.add("3"); // 3已寄回（实验室接收到用户邮寄的样本）
			params.add("4"); // 4已接收（实验室接收到用户邮寄的样本）
			params.add("5"); // 5已报告
			int num = Integer.valueOf(utilMethodService.getObjectList(sql, params, DB).get(0).get("num").toString());
			if(num > 0){
				status = "1";
			}

			jsonback.put("success", true);
			jsonback.put("openid", openid);
			jsonback.put("status", status);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，进入检测页面时触发的方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，进入检测页面时触发的方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	
	/**
	 * 进入报告查询页面
	 * @param request
	 * @param response
	 */
	@RequestMapping("/report.hn")
	public void report(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入查询报告时触发的方法start");
			String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();
			logger.info("report.hn phone:" + phone);
			if (StringUtils.isBlank(phone)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全！");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，进入查询报告时触发的方法end：" + jsonback.toString());
				return;
			}


			// 根据样本号获取检测记录
			String status = "0"; // 0 无报告 1有报告
			String sql = "SELECT * FROM care_userInfo WHERE phone=? and sample_status = ?";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(phone);
			params.add("5"); // 5已报告
			List<HashMap<String, Object>> result = utilMethodService.getObjectList(sql, params, DB);

			if(result.size() > 0){
				status = "1";
			}

			jsonback.put("success", true);
			jsonback.put("status", status);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，进入查询报告时触发的方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，进入查询报告时触发的方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 手机验证码发送
	 * @return
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	@RequestMapping("/Verification.hn")
	public void Verification(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入手机验证码发送方法start");
			String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();
			logger.info("Verification phone:" + phone);
			if (StringUtils.isBlank(phone)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，手机验证码发送方法end：" + jsonback.toString());
				return;
			}

			//随机生成4位数字，存到session
			String str="0123456789";
			StringBuilder code=new StringBuilder(4);
			for(int i=0;i<4;i++){
				char ch=str.charAt(new Random().nextInt(str.length()));
				code.append(ch);
			}
			HttpSession session = request.getSession();
			session.setAttribute(phone, code.toString());
			logger.info("获取的验证码是："+code.toString());
			/*String text="【卡尤迪】您的验证码是"+code.toString()+"。如非本人操作，请忽略本短信";
			CloseableHttpClient clientForMsg = HttpClients.createDefault();
			HttpPost postForMsg = new HttpPost(Configure.getUrlForMsg());
			List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();
			listForMsg.add(new BasicNameValuePair("apikey", Configure.getApikey()));
			listForMsg.add(new BasicNameValuePair("text", text));
			listForMsg.add(new BasicNameValuePair("mobile", phone));

			UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg,"UTF-8");
			postForMsg.setEntity(uefEntityForMsg);
			HttpResponse responseForMsg = clientForMsg.execute(postForMsg);*/
			/*CloseableHttpClient clientForMsg = HttpClients.createDefault();
	        HttpPost postForMsg = new HttpPost(Configure.getUrlForSMS());
	        List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();


	        String contentCode = Configure.getCodeForVerification();
	        //短信参数
			JSONObject contentParam = new JSONObject();
			contentParam.put("code", code.toString());

			listForMsg.add(new BasicNameValuePair("phoneNumber", phone));
			listForMsg.add(new BasicNameValuePair("contentCode", contentCode));
			listForMsg.add(new BasicNameValuePair("contentParam", contentParam.toString()));
			listForMsg.add(new BasicNameValuePair("signName", "卡尤迪"));

			UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg,"UTF-8");
	        postForMsg.setEntity(uefEntityForMsg);
	        HttpResponse responseForMsg = clientForMsg.execute(postForMsg);

			if (responseForMsg.getStatusLine().getStatusCode() == 200) {
				HttpEntity httpEntityForMsg = responseForMsg.getEntity();
				String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
		    	System.out.println("阿里云发送短信返回信息："+string);
				jsonback.put("success", true);
				jsonback.put("msg", "发送成功");
			} else{
				jsonback.put("success", false);
				jsonback.put("msg", "发送失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，手机验证码发送方法end：" + jsonback.toString());
				return;
			}*/
			CloseableHttpClient clientForMsg = HttpClients.createDefault();
			List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();
			logger.info("开始发送云片短信");
			HttpPost postForMsg = new HttpPost(Configure.getUrlForMsg());
			String text = "【卡尤迪】您的验证码是" + str + "。如非本人操作，请忽略本短信。";

			listForMsg.add(new BasicNameValuePair("apikey", Configure.getApiKeys()));
			listForMsg.add(new BasicNameValuePair("text", text));
			listForMsg.add(new BasicNameValuePair("mobile", phone));

			UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg, "UTF-8");
			postForMsg.setEntity(uefEntityForMsg);
			HttpResponse responseForMsg = clientForMsg.execute(postForMsg);

			if (responseForMsg.getStatusLine().getStatusCode() == 200) {
				HttpEntity httpEntityForMsg = responseForMsg.getEntity();
				String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
				System.out.println("云片发送短信返回信息：" + string);
				jsonback.put("success", true);
				jsonback.put("msg", "发送成功");
			} else {
				jsonback.put("success", false);
				jsonback.put("msg", "发送失败");
				logger.info("手机发送审核不通过原因方法end：" + jsonback.toString());
				return;
			}
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，手机验证码发送方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，手机验证码发送方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	/**
	 * 手机验证
	 * @param opendID
	 * @return
	 */
	@RequestMapping("/VerificationPhone.hn")
	public void VerificationPhone(HttpServletRequest request,HttpServletResponse response) throws Exception {
        logger.info("进入手机号验证方法start");
		String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();
		String yanzheng = request.getParameter("verCode") == null ? "" : request.getParameter("verCode").trim();
		logger.info("VerificationPhone phone"+phone);
		logger.info("VerificationPhone yanzheng"+yanzheng);
		JSONObject jsonobject = new JSONObject();
		HttpSession session = request.getSession();
		String attribute = (String)session.getAttribute(phone);
		logger.info("VerificationPhone 从session中获取的验证码是"+attribute);
		logger.info("VerificationPhone 从页面接收的验证码是"+yanzheng);
		if(StringUtils.isBlank(attribute)){
			jsonobject.put("success", false);
		}else{
			if(attribute.equals(yanzheng)){
				jsonobject.put("success", true);
			}else{
				jsonobject.put("success", false);
				jsonobject.put("msg", "验证码错误");
			}
		}
		ResponseUtils.setToHttp(response, jsonobject.toString());

		logger.info("手机号验证方法end"+jsonobject.toString());
	}
	/**
	 * 调用扫一扫
	 * @param request
	 * @param response
	 */
	@RequestMapping("/JSSDKHELP.hn")
	public void JSSDKHelp(HttpServletRequest request,HttpServletResponse response) {
		logger.info("进入调用扫一扫方法");
		String url=request.getParameter("url");
		//获取jsapi_ticket
		Map<String, String> ret = new HashMap<String, String>();
		try {
			String jsapi_ticket = jsapi_ticketGetUtil.getJSApiTicket();
			logger.info("url:"+url);
			logger.info("jsapi_ticket:"+jsapi_ticket);
			ret = Sign.sign(jsapi_ticket, url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = JSONObject.fromObject(ret); 
		ResponseUtils.setToHttp(response, jsonObject.toString());
	}


	/**
	 * 获取微信openid方法
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/getUserId.hn")
	public void getUserId(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取微信openid方法start");
			String user_id = request.getParameter("user_id") == null ? "" : request.getParameter("user_id").trim();
			logger.info("getUserId user_id:" + user_id);

			String openid = (String)request.getSession().getAttribute("user_id");
			logger.info("我是从session中获取的openid****"+openid);
			if ("".equals(openid) || "undefined".equals(openid) || openid==null) {

				if (StringUtils.isNotBlank(user_id)) {
					logger.info("前端传递了openid，但是session过期，使用前端传递的"); // 一个页面停留时间过长
					openid = user_id;
					jsonback.put("success", true);
					jsonback.put("code", 200);
					jsonback.put("msg", openid);
				}else{ // 无openid可用，重新获取
					String redirectUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
							+Configure.getAppid()
							+"&redirect_uri=" 
							+Configure.getAppIp()
							+"test/dispatcher.html&response_type=code&scope=snsapi_base&state=STATE#wechat_redirect";
					jsonback.put("success", true);
					jsonback.put("code", 199);
					jsonback.put("msg", redirectUrl);
				}
			}else{ // 如果缓存里面有openid使用此openid
				jsonback.put("success", true);
				jsonback.put("code", 200);
				jsonback.put("msg", openid);
			}

			if(jsonback.has("code") && jsonback.getInt("code") == 200){
				// 防止session过期，重新存储
				request.getSession().setAttribute("user_id", openid);
			}

			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，获取微信openid方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取微信openid方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}



	/**
	 * 获取样本号信息
	 * @param request
	 * @param response
	 */
	@RequestMapping("/getSampleInfo.hn")
	public void getSampleInfo(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取样本号信息方法start");
			String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
			logger.info("getSampleInfo sample_id:" + sample_id);
			if (StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，获取样本号信息方法end：" + jsonback.toString());
				return;
			}

			String sql = " SELECT r.sample_id,r.batch_no,r.production_time,r.sample_type,r.effective_date,i.commodity_name FROM "
					+"(SELECT sample_id,test_items,batch_no,date_format(production_time,'%Y-%m-%d %H:%i:%s') as production_time,sample_type,effective_date " 
					+"FROM test_box_warehouse WHERE sample_id=? and sample_id in (SELECT sample_id FROM care_userInfo) ) r "
					+"LEFT JOIN wx_commodity_info i on r.test_items=i.commodity_id ";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(sample_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			if(list.size() == 0){
				jsonback.put("success", false);
				jsonback.put("msg", "样本信息不存在");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，获取样本号信息方法end：" + jsonback.toString());
				return;
			}


			jsonback.put("success", true);
			jsonback.put("msg", list.get(0));
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，获取样本号信息方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取样本号信息方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 样本绑定
	 * @param request
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("/sampleBind.hn")
	public void sampleBind(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入样本绑定方法start");
			String open_id = request.getParameter("open_id") == null ? "" : request.getParameter("open_id").trim();
			String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
			String name = request.getParameter("name") == null ? "" : request.getParameter("name").trim();
			String sex = request.getParameter("sex") == null ? "" : request.getParameter("sex").trim();
			String age = request.getParameter("age") == null ? "" : request.getParameter("age").trim();
			String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();
			String verCode = request.getParameter("verCode") == null ? "" : request.getParameter("verCode").trim();
			String if_treatment = request.getParameter("if_treatment") == null ? "" : request.getParameter("if_treatment").trim();
			String symptom = request.getParameter("symptomList") == null ? "" : request.getParameter("symptomList").trim();
			String infected = request.getParameter("infected") == null ? "" : request.getParameter("infected").trim();
			String drink_milk = request.getParameter("drink_milk") == null ? "" : request.getParameter("drink_milk").trim();
			String brush_teeth = request.getParameter("brush_teeth") == null ? "" : request.getParameter("brush_teeth").trim();
			String diet_preference = request.getParameter("diet_preference") == null ? "" : request.getParameter("diet_preference").trim();

			logger.info("sampleBind open_id:" + open_id);
			logger.info("sampleBind sample_id:" + sample_id);
			logger.info("sampleBind name:" + name);
			logger.info("sampleBind sex:" + sex);
			logger.info("sampleBind age:" + age);
			logger.info("sampleBind phone:" + phone);
			logger.info("sampleBind verCode:" + verCode);
			logger.info("sampleBind if_treatment:" + if_treatment);
			logger.info("sampleBind symptom:" + symptom);
			logger.info("sampleBind infected:" + infected);
			logger.info("sampleBind drink_milk:" + drink_milk);
			logger.info("sampleBind brush_teeth:" + brush_teeth);
			logger.info("sampleBind diet_preference:" + diet_preference);

			// code 100 无 101 验证码 102样本号
			if (StringUtils.isBlank(open_id) || StringUtils.isBlank(sample_id) || StringUtils.isBlank(name) || StringUtils.isBlank(sex) || 
					StringUtils.isBlank(age) || StringUtils.isBlank(phone) || StringUtils.isBlank(verCode) ) {
				jsonback.put("success", false);
				jsonback.put("code", 100);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}

			// 验证验证码
			HttpSession session = request.getSession();
			String attribute = (String)session.getAttribute(phone);
			logger.info("sampleBind 从session中获取的验证码是"+attribute);
			logger.info("sampleBind 从页面接收的验证码是"+verCode);
			if (attribute==null) {
				jsonback.put("success", false);
				jsonback.put("code", 101);
				jsonback.put("msg", "请获取验证码");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}
			if(!attribute.equals(verCode)){
				jsonback.put("success", false);
				jsonback.put("code", 101);
				jsonback.put("msg", "验证码错误");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}

			// 校验样本号是否可以使用
			String sqlForCheck = "SELECT sample_status, date_format(alive_time,'%Y-%m-%d %H:%i:%s') as alive_time FROM care_userInfo WHERE sample_id=? ";
			ArrayList<Object> paramsForCheck = new ArrayList<Object>();
			paramsForCheck.add(sample_id);
			List<HashMap<String, Object>> listForCheck = utilMethodService.getObjectList(sqlForCheck, paramsForCheck, DB);
			if(listForCheck.size() == 0){
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "未查询到样本编号");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}
			//cui 验证care_userInfo活动结束时间，如果为空，不进行处理
			String alive_time = listForCheck.get(0).get("alive_time").toString();
			if (alive_time!= "") {
				Boolean date_flag = dateFlag(alive_time);
				if (date_flag) {
					jsonback.put("success", false);
					jsonback.put("code", 102);
					jsonback.put("msg", "该样本已过期");
					ResponseUtils.setToHttp(response, jsonback.toString());
					logger.info("care检测，样本绑定方法end：" + jsonback.toString());
					return;
				}
			}
	



			// 样本状态 0已配货 1已发货 （已邮寄给客户） 2已绑定 （用户在公众号上进行了绑定）3已寄回（客户已将样本寄回）  4已接收（实验室接收到用户邮寄的样本） 5已报告
			String sample_status = listForCheck.get(0).get("sample_status").toString();
			if("0".equals(sample_status)){
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "样本编号暂时无法使用");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}

			if("2".equals(sample_status) || "3".equals(sample_status) || "4".equals(sample_status)|| "5".equals(sample_status)){
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "样本编号已使用");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}


			//cui 验证库存表是否过期 test_box_warehouse中的expiration_time

			// 校验样本号是否可以使用
			String sqlForwarehouse = "SELECT date_format(expiration_time,'%Y-%m-%d %H:%i:%s') as expiration_time FROM test_box_warehouse WHERE sample_id=? ";
			ArrayList<Object> paramsForwarehouse = new ArrayList<Object>();
			paramsForwarehouse.add(sample_id);
			List<HashMap<String, Object>> listForwarehouse = utilMethodService.getObjectList(sqlForwarehouse, paramsForwarehouse, DB);
			String expiration_time = listForwarehouse.get(0).get("expiration_time").toString();
			Boolean date_flag_warehouse = dateFlag(expiration_time);
			if (date_flag_warehouse) {
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "该样本已过期");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}




			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String sampling_time = dateFormat.format(Calendar.getInstance().getTime());

			String sqlForUpdate = "UPDATE care_userInfo SET name=?, gender=?, age=?, age_unit=?, phone=?, sampling_time=?, "
					+ "sampling_wx_id=?,sample_status=?,if_treatment=?,symptom = ?,infected=?,drink_milk=?,brush_teeth = ?,diet_preference = ? WHERE sample_id=?";
			ArrayList<Object> paramsForUpdate = new ArrayList<Object>();
			paramsForUpdate.add(name);
			paramsForUpdate.add(sex);
			paramsForUpdate.add(age);
			paramsForUpdate.add("岁");
			paramsForUpdate.add(phone);
			paramsForUpdate.add(sampling_time);
			paramsForUpdate.add(open_id);
			paramsForUpdate.add("2"); // 样本状态 0已配货 1已发货 （已邮寄给客户） 2已绑定 （用户在公众号上进行了绑定）3已寄回（客户已将样本寄回）  4已接收（实验室接收到用户邮寄的样本） 5已报告
			paramsForUpdate.add(if_treatment);
			paramsForUpdate.add(symptom);
			paramsForUpdate.add(infected);
			paramsForUpdate.add(drink_milk);
			paramsForUpdate.add(brush_teeth);
			paramsForUpdate.add(diet_preference);
			paramsForUpdate.add(sample_id);
			JSONObject result = JSONObject.fromObject(utilMethodService.updateObjectInfo(sqlForUpdate, DB, paramsForUpdate));
			if(!result.getBoolean("success")){
				jsonback.put("success", false);
				jsonback.put("code", 100);
				jsonback.put("msg", "绑定失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}

			jsonback.put("success", true);
			jsonback.put("msg", "绑定成功");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，样本绑定方法end：" + jsonback.toString());

			// 绑定成功，给用户发短信
			String service_tel = "4008693888";
			bindSuccessToSMS(phone,name,sample_id,service_tel);
			// 绑定成功，给用户发送消息模板
			buySuccessToWx(sample_id,name,phone,sampling_time,open_id);

		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，样本绑定方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 校验样本号
	 * @param request
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("/sampleTest.hn")
	public void sampleTest(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入样本绑定方法start");
			String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();




			logger.info("sampleBind sample_id:" + sample_id);




			// code 100 无 101 验证码 102样本号
			if ( StringUtils.isBlank(sample_id) ) {
				jsonback.put("success", false);
				jsonback.put("code", 100);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}

			

			// 校验样本号是否可以使用
			String sqlForCheck = "SELECT sample_status, date_format(alive_time,'%Y-%m-%d %H:%i:%s') as alive_time FROM care_userInfo WHERE sample_id=? ";
			ArrayList<Object> paramsForCheck = new ArrayList<Object>();
			paramsForCheck.add(sample_id);
			List<HashMap<String, Object>> listForCheck = utilMethodService.getObjectList(sqlForCheck, paramsForCheck, DB);
			if(listForCheck.size() == 0){
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "未查询到样本编号");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}
			//cui 验证care_userInfo活动结束时间，如果为空，不进行处理
			String alive_time = listForCheck.get(0).get("alive_time").toString();
			if (alive_time!= "") {
				Boolean date_flag = dateFlag(alive_time);
				if (date_flag) {
					jsonback.put("success", false);
					jsonback.put("code", 102);
					jsonback.put("msg", "该样本已过期");
					ResponseUtils.setToHttp(response, jsonback.toString());
					logger.info("care检测，样本绑定方法end：" + jsonback.toString());
					return;
				}
			}
	



			// 样本状态 0已配货 1已发货 （已邮寄给客户） 2已绑定 （用户在公众号上进行了绑定）3已寄回（客户已将样本寄回）  4已接收（实验室接收到用户邮寄的样本） 5已报告
			String sample_status = listForCheck.get(0).get("sample_status").toString();
			if("0".equals(sample_status)){
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "样本编号暂时无法使用");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}

			if("2".equals(sample_status) || "3".equals(sample_status) || "4".equals(sample_status)|| "5".equals(sample_status)){
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "样本编号已使用");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}


			//cui 验证库存表是否过期 test_box_warehouse中的expiration_time

			// 校验样本号是否可以使用
			String sqlForwarehouse = "SELECT date_format(expiration_time,'%Y-%m-%d %H:%i:%s') as expiration_time FROM test_box_warehouse WHERE sample_id=? ";
			ArrayList<Object> paramsForwarehouse = new ArrayList<Object>();
			paramsForwarehouse.add(sample_id);
			List<HashMap<String, Object>> listForwarehouse = utilMethodService.getObjectList(sqlForwarehouse, paramsForwarehouse, DB);
			String expiration_time = listForwarehouse.get(0).get("expiration_time").toString();
			Boolean date_flag_warehouse = dateFlag(expiration_time);
			if (date_flag_warehouse) {
				jsonback.put("success", false);
				jsonback.put("code", 102);
				jsonback.put("msg", "该样本已过期");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，样本绑定方法end：" + jsonback.toString());
				return;
			}


		
			jsonback.put("success", true);
			jsonback.put("msg", "该样本号可以使用");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，样本绑定方法end：" + jsonback.toString());



		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，样本绑定方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	/**
	 * 查看样本类型
	 * @param request
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("/getSamplingType.hn")
	public void getSamplingType(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入查看样本类型方法start");
			String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
			logger.info("sampleBind sample_id:" + sample_id);

			if (StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，查看样本类型方法end：" + jsonback.toString());
				return;
			}

			String sql = "SELECT sample_type FROM care_userInfo where sample_id = ? ";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(sample_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			if(list.size() == 0){
				jsonback.put("success", false);
				jsonback.put("msg", "未查询到样本信息");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，查看样本类型方法end：" + jsonback.toString());
				return;
			}

			String sample_type = list.get(0).get("sample_type").toString();
			jsonback.put("success", true);
			jsonback.put("msg", sample_type);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，查看样本类型方法end：" + jsonback.toString());
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，查看样本类型方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 获取省份
	 * @param request
	 * @param response
	 */
	@RequestMapping("/getProvince.hn")
	public void getProvince(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取省份方法start");
			String sql = "select id AS province_id,name AS province_name,parentid AS country_id"// 区域id，不想改
					+ " from area_info WHERE type=1";
			ArrayList<Object> params = new ArrayList<Object>();
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			jsonback.put("success", true);
			jsonback.put("msg", list);
			logger.info("care检测，获取省份方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取省份方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 获取城市 
	 * @param request
	 * @param response
	 */
	@RequestMapping("/getCity.hn")
	public void getCity(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取城市方法start");
			String province_id = request.getParameter("province_id") == null ? "": request.getParameter("province_id").trim();
			logger.info("getCity province_id:" + province_id);

			String sql = "select id AS city_id,name AS city_name,parentid AS province_id"
					+ " from area_info where parentid = ?";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(province_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			jsonback.put("success", true);
			jsonback.put("msg", list);
			logger.info("care检测，获取城市方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取城市方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 获取县区
	 * @param request
	 * @param response
	 */
	@RequestMapping("/getCounty.hn")
	public void getCounty(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取县区方法start");
			String city_id = request.getParameter("city_id") == null ? "": request.getParameter("city_id").trim();
			logger.info("getCounty city_id:" + city_id);

			String sql = "select id AS county_id,name AS county_name,parentid AS city_id"
					+ " from area_info where parentid = ?";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(city_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			jsonback.put("success", true);
			jsonback.put("msg", list);
			logger.info("care检测，获取县区方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取县区方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}



	/**
	 * 绑定成功后发送的短信消息
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public static void bindSuccessToSMS(String phone,String name,String sample_id,String service_tel) throws ClientProtocolException, IOException{
		logger.info("***********进入绑定成功后发送的短信消息方法*****************");
		logger.info("接收到的参数，phone=" + phone);
		JSONObject jsonback = new JSONObject();
	/*	String text = "【Care基因】尊敬的用户，您好！您已经成功绑定样本编码，请您按接下来的步骤继续操作，祝您生活愉快！";
		CloseableHttpClient clientForMsg = HttpClients.createDefault();
		HttpPost postForMsg = new HttpPost(Configure.getUrlForMsg());
		List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();
		listForMsg.add(new BasicNameValuePair("apikey", Configure.getApikey()));
		listForMsg.add(new BasicNameValuePair("text", text));
		listForMsg.add(new BasicNameValuePair("mobile",phone));
		UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg,"UTF-8");
		postForMsg.setEntity(uefEntityForMsg);
		HttpResponse responseForMsg = clientForMsg.execute(postForMsg);
		if (responseForMsg.getStatusLine().getStatusCode() == 200) {
			HttpEntity httpEntityForMsg = responseForMsg.getEntity();
			String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
			System.out.println("云片发送短信返回信息："+string);
		}*/
		/*CloseableHttpClient clientForMsg = HttpClients.createDefault();
        HttpPost postForMsg = new HttpPost(Configure.getUrlForSMS());
        List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();

        String contentCode = Configure.getCodeForCodeSuccessfullyBinding();
        //短信参数
		JSONObject contentParam = new JSONObject();

		listForMsg.add(new BasicNameValuePair("phoneNumber", phone));
		listForMsg.add(new BasicNameValuePair("contentCode", contentCode));
		listForMsg.add(new BasicNameValuePair("contentParam", contentParam.toString()));
		listForMsg.add(new BasicNameValuePair("signName", "卡尤迪"));

		UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg,"UTF-8");
        postForMsg.setEntity(uefEntityForMsg);
        HttpResponse responseForMsg = clientForMsg.execute(postForMsg);
        HttpEntity httpEntityForMsg = responseForMsg.getEntity();
    	String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
    	System.out.println("阿里云发送短信返回信息："+string);*/
		CloseableHttpClient clientForMsg = HttpClients.createDefault();
		List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();
		logger.info("开始发送云片短信");
		HttpPost postForMsg = new HttpPost(Configure.getUrlForMsg());
		String text = "尊敬的用户，您好！您已经成功绑定样本编号，信息如下： \n" +
				"受检人姓名："+name+" \n" +
				"样本编号："+sample_id+" \n" +
				"温馨提示：请您尽快安排采样并回寄样本，如您有任何疑问，敬请您致电客服电话 \n" +
				service_tel+"，我们真诚地期待为您提供服务。";

		listForMsg.add(new BasicNameValuePair("apikey", Configure.getApiKeys()));
		listForMsg.add(new BasicNameValuePair("text", text));
		listForMsg.add(new BasicNameValuePair("mobile", phone));

		UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg, "UTF-8");
		postForMsg.setEntity(uefEntityForMsg);
		HttpResponse responseForMsg = clientForMsg.execute(postForMsg);

		if (responseForMsg.getStatusLine().getStatusCode() == 200) {
			HttpEntity httpEntityForMsg = responseForMsg.getEntity();
			String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
			System.out.println("云片发送短信返回信息：" + string);
			jsonback.put("success", true);
			jsonback.put("msg", "发送成功");
		} else {
			jsonback.put("success", false);
			jsonback.put("msg", "发送失败");
			logger.info("手机发送审核不通过原因方法end：" + jsonback.toString());
			return;
		}

	}


	/**
	 * 绑定成功后发送的模板消息
	 * @param request
	 * @param response
	 */
	public static void buySuccessToWx(String sample_id, String name, String phone, String sampling_time, String open_id){
		logger.info("***********进入绑定成功后发送的模板消息方法*****************");
		logger.info("接收到的参数，sample_id=" + sample_id);
		logger.info("接收到的参数，name=" + name);
		logger.info("接收到的参数，phone=" + phone);
		logger.info("接收到的参数，sampling_time=" + sampling_time);
		logger.info("接收到的参数，open_id=" + open_id);

		String template_id = Configure.getBindSuccessId();
		String url = Configure.getAppIp() + "test/example_exam.html?open_id=" + open_id;

		String accessToken = AccessTokenUtil.getAccessToken(); //获取token
		String msgUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token="+accessToken; //api

		//post请求
		JSONObject big = new JSONObject();
		big.put("touser",open_id);
		big.put("template_id",template_id);
		big.put("url",url);

		JSONObject first = new JSONObject();
		JSONObject middle = new JSONObject();
		first.put("value", "恭喜您，样本编号："+sample_id+"绑定成功！");
		first.put("color", "#173177");
		middle.put("first", first);

		JSONObject keyword1 = new JSONObject();
		keyword1.put("value", name);
		keyword1.put("color", "#173177");
		middle.put("keyword1", keyword1);

		JSONObject keyword2 = new JSONObject();
		keyword2.put("value", phone);
		keyword2.put("color", "#173177");
		middle.put("keyword2", keyword2);

		JSONObject keyword3 = new JSONObject();
		keyword3.put("value", sampling_time);
		keyword3.put("color", "#173177");
		middle.put("keyword3", keyword3);

		JSONObject remark = new JSONObject();
		remark.put("value", "点击查看详情");
		remark.put("color", "#173177");
		middle.put("remark", remark);
		big.put("data", middle);

		String post = post(big, msgUrl);
		logger.info("post请求返回的结果："+post);
		logger.info("***********离开绑定成功后发送的模板消息方法*****************");
	}


	public static String post(JSONObject json,String URL) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(URL);
		post.setHeader("Content-Type", "application/json");
		post.addHeader("Authorization", "Basic YWRtaW46");
		String result = "";

		try {
			StringEntity s = new StringEntity(json.toString(), "utf-8");
			s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
			post.setEntity(s);
			// 发送请求
			HttpResponse httpResponse = client.execute(post);
			// 获取响应输入流
			InputStream inStream = httpResponse.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "utf-8"));
			StringBuilder strber = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null)
				strber.append(line + "\n");
			inStream.close();
			result = strber.toString();
			System.out.println(result);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				System.out.println("请求服务器成功，做相应处理");
			} else {
				System.out.println("请求服务端失败");
			}
		} catch (Exception e) {
			System.out.println("请求异常");
			throw new RuntimeException(e);
		}
		return result;
	}


	/**
	 * 获取快递回寄信息
	 * @param request
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("/getExpressReceiptInfo.hn")
	public void getExpressReceiptInfo(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取快递回寄信息方法start");
			String sqlForReceipt= "SELECT province,city,area,address,company,name,phone FROM coyote_receipt_info LIMIT 1";
			ArrayList<Object> paramsForReceipt = new ArrayList<Object>();
			List<HashMap<String, Object>> listForReceipt = utilMethodService.getObjectList(sqlForReceipt, paramsForReceipt, DB);
			if(listForReceipt.size() == 0){
				jsonback.put("success", false);
				jsonback.put("msg", "收件信息获取失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，获取快递回寄信息方法end：" + jsonback.toString());
				return;
			}

			JSONObject receiptInfo = JSONObject.fromObject(listForReceipt.get(0));
			jsonback.put("success", true);
			jsonback.put("msg", receiptInfo);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，获取快递回寄信息方法end：" + jsonback.toString());
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取快递回寄信息方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 根据样本号查询样本信息
	 * @param request
	 * @param response
	 * @throws Exception 
	 */
	@RequestMapping("/getSampleForExpress.hn")
	public void getSampleForExpress(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		String sample_id = request.getParameter("sample_id") == null ? "": request.getParameter("sample_id").trim();

		
		try {
			logger.info("care检测，进入获取样本全部信息方法start");
			String sqlForSample= "SELECT sample_status FROM care_userInfo where sample_id = ?";
			ArrayList<Object> paramsForSample = new ArrayList<Object>();
			paramsForSample.add(sample_id);
			
			List<HashMap<String, Object>> listForReceipt = utilMethodService.getObjectList(sqlForSample, paramsForSample, DB);
			if(listForReceipt.size() == 0){
				jsonback.put("success", false);
				jsonback.put("msg", "获取样本全部信息失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，获取样本全部信息方法end：" + jsonback.toString());
				return;
			}

			JSONObject receiptInfo = JSONObject.fromObject(listForReceipt.get(0));
			jsonback.put("success", true);
			jsonback.put("msg", receiptInfo);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，获取样本全部信息方法end：" + jsonback.toString());
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，获取样本全部信息方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 通过openId获取该用户绑定的所有样本
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/getSampleList.hn")
	public void getSampleList(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入通过openId获取该用户绑定的所有样本方法start");
			String open_id = request.getParameter("open_id") == null ? "" : request.getParameter("open_id").trim();
			logger.info("getSampleList open_id:" + open_id);

			if (StringUtils.isBlank(open_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，通过openId获取该用户绑定的所有样本方法end：" + jsonback.toString());
				return;
			}

			String sql = "SELECT sampling_wx_id,phone,sample_id,sample_status,reportUrl,send_number, "
					+ "date_format(sampling_time,'%Y-%m-%d %H:%i:%s') as sampling_time "
					+ "FROM care_userInfo WHERE sampling_wx_id=? and sample_status in (?,?,?,?) ORDER BY sampling_time desc ";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(open_id);
			params.add("2"); // 2已绑定 （用户在公众号上进行了绑定） 
			params.add("3"); // 3已寄出
			params.add("4"); // 4已接收（实验室接收到用户邮寄的样本）
			params.add("5"); // 5已报告
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);

			jsonback.put("success", true);
			jsonback.put("msg", list);

			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，通过openId获取该用户绑定的所有样本方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，通过openId获取该用户绑定的所有样本方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	/**
	 * 通过手机号获取该用户绑定的所有样本
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/getSamplePhone.hn")
	public void getSamplePhone(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入通过openId获取该用户绑定的所有样本方法start");
			String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();

			logger.info("getSampleList phone:" + phone);


			if (StringUtils.isBlank(phone)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，通过phone获取该用户绑定的所有样本方法end：" + jsonback.toString());
				return;
			}

			String sql = "SELECT c.reportUrl,c.sample_status,c.sample_id,date_format(c.report_time,'%Y-%m-%d') as reportTime,"
					+ "w.commodity_name  FROM care_userInfo c LEFT JOIN "
					+ "wx_commodity_info w on c.test_items=w.commodity_id where "
					+ "c.phone = ? and c.sample_status = ? ORDER BY sampling_time desc ";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(phone);
			params.add("5"); // 5已报告
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);

			String sqlForHealth = "SELECT c.reportUrl,'5' as sample_status,c.sample_id,date_format(c.report_time,'%Y-%m-%d') as reportTime,"
					+ "c.project_name as commodity_name  FROM care_userInfo c where "
					+ "c.phone = ? and c.reportUrl is not null ORDER BY sampling_time desc ";
			ArrayList<Object> paramsForHealth = new ArrayList<Object>();
			paramsForHealth.add(phone);
			List<HashMap<String, Object>> listForHealth = utilMethodService.getObjectList(sqlForHealth, paramsForHealth, HealthDB);
			
			list.addAll(listForHealth);
			//排序前 
	        for (HashMap<String, Object> map : list) {
	            System.out.println(map.get("reportTime"));
	            System.out.println(map.get("sample_id"));
	        }
			Collections.sort(list, new Comparator<HashMap<String, Object>>() {
	            public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
	                String name1 = o1.get("reportTime").toString() ;//name1是从你list里面拿出来的一个 
	                String name2 = o2.get("reportTime").toString() ; //name1是从你list里面拿出来的第二个name
	                return -name1.compareTo(name2);
	            }
	        });
			//排序后 
	        System.out.println("-------------------");
	        for (Map<String, Object> map : list) {
	            System.out.println(map.get("reportTime"));
	            System.out.println(map.get("sample_id"));
	        }
			/*for (int i = 0; i < list.size(); i++) {
				String sample_id = list.get(i).get("sample_id").toString();
				String sampling_wx_id = list.get(i).get("sampling_wx_id").toString();
				String url = Configure.getAppIp() + "api/showPdf.hn?sample_id=" + sample_id + "&open_id=" + sampling_wx_id;
				list.get(i).put("reportUrl", url);
			}*/

			if (list.size() == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "你还没有报告");
				jsonback.put("code", "101");

				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("用户"+phone+"报告获取失败");
				return;
				
			}
			jsonback.put("success", true);
			jsonback.put("msg", list);

			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，通过openId获取该用户绑定的所有样本方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，通过openId获取该用户绑定的所有样本方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}


	/**
	 * 保存用户填写的回寄运单号
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/saveSendNumber.hn")
	public void saveSendNumber(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入保存用户填写的回寄运单号方法start");
			String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
			String send_number = request.getParameter("send_number") == null ? "" : request.getParameter("send_number").trim();
			logger.info("saveSendNumber sample_id:" + sample_id);
			logger.info("saveSendNumber send_number:" + send_number);

			if (StringUtils.isBlank(sample_id) || StringUtils.isBlank(send_number)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
				return;
			}


			String sql = "UPDATE care_userInfo SET send_number = ?,sample_status = ? WHERE sample_id = ?";
			ArrayList<Object> params = new ArrayList<Object>();		
			params.add(send_number);
			params.add(3);
			params.add(sample_id);
			JSONObject result = JSONObject.fromObject(utilMethodService.updateObjectInfo(sql, DB, params));
			if(!result.getBoolean("success")){
				jsonback.put("success", false);
				jsonback.put("msg", "运单号设置失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
				return;
			}

			jsonback.put("success", true);
			jsonback.put("msg", "运单号设置成功");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	/**
	 * 保存用户填写的回寄运单号
	 * @param request
	 * @param response
	 * @return
	 */
/*	@RequestMapping("/saveSendNumber.hn")
	public void saveSendNumber(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入保存用户填写的回寄运单号方法start");
			String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
			String send_number = request.getParameter("send_number") == null ? "" : request.getParameter("send_number").trim();
			logger.info("saveSendNumber sample_id:" + sample_id);
			logger.info("saveSendNumber send_number:" + send_number);

			if (StringUtils.isBlank(sample_id) || StringUtils.isBlank(send_number)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
				return;
			}

			String sql = "UPDATE care_userInfo SET send_number = ? WHERE sample_id = ?";
			ArrayList<Object> params = new ArrayList<Object>();		
			params.add(send_number);
			params.add(sample_id);
			JSONObject result = JSONObject.fromObject(utilMethodService.updateObjectInfo(sql, DB, params));
			if(!result.getBoolean("success")){
				jsonback.put("success", false);
				jsonback.put("msg", "运单号设置失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
				return;
			}

			jsonback.put("success", true);
			jsonback.put("msg", "运单号设置成功");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，保存用户填写的回寄运单号方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}*/

	/**
	 * 查询自行送样回寄单号
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/selectSendNumber.hn")
	public void selectSendNumber(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入查询自行送样回寄单号方法start");
			

			String sql = "select * from inside_info ";
			ArrayList<Object> params = new ArrayList<Object>();		
			  List<HashMap<String, Object>> result = utilMethodService.getObjectList(sql, params, DB);
			

			jsonback.put("success", true);
			jsonback.put("msg", result);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，查询自行送样回寄单号方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，查询自行送样回寄单号方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	/**
	 * 查询报告
	 * @param request
	 * @param response
	 * @return
	 */
/*	@RequestMapping("/selectReport.hn")
	public void selectReport(HttpServletRequest request,HttpServletResponse response){
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入查询自行送样回寄单号方法start");
			
			String user_id = request.getParameter("user_id") == null ? "": request.getParameter("user_id").trim();

			String sql = "select * from inside_info ";
			ArrayList<Object> params = new ArrayList<Object>();	
			List<HashMap<String, Object>> result = utilMethodService.getObjectList(sql, params, DB);
			

			jsonback.put("success", true);
			jsonback.put("msg", result);
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，查询自行送样回寄单号方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，查询自行送样回寄单号方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}*/



	/**
	 * 根据code获取token
	 * @param code
	 * @return
	 */
	public JSONObject getAccessToken(String code) {
		JSONObject jsonObject = new JSONObject();
		String url = "https://api.weixin.qq.com/sns/oauth2/access_token";
		url = url + "?appid=" + Configure.getAppid() + "&secret="
				+ Configure.getAppSecrte() + "&code=" + code
				+ "&grant_type=authorization_code";
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(url);
		String result = "";
		try {
			httpClient.executeMethod(getMethod);
			result = getMethod.getResponseBodyAsString();
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		jsonObject = JSONObject.fromObject(result);
		jsonObject.put("success", true);
		if ("40029".equals(jsonObject.get("errcode"))) {
			jsonObject.put("success", false);
		}
		return jsonObject;
	}



	/**
	 * 展示pdf
	 * @param request
	 * @param response
	 */
	/*@RequestMapping("/showPdf.hn")
	public void showPdf(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入展示pdf方法start");
			String sample_id = request.getParameter("sample_id") == null ? "": request.getParameter("sample_id").trim();
			String open_id = request.getParameter("open_id") == null ? "": request.getParameter("open_id").trim();
			logger.info("showPdf sample_id:" + sample_id);
			logger.info("showPdf open_id:" + open_id);

			if (StringUtils.isBlank(open_id) || StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，展示pdf方法end：" + jsonback.toString());
				return;
			}


			String sql = "SELECT reportUrl FROM care_userInfo WHERE sampling_wx_id=? and sample_id=?";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(open_id);
			params.add(sample_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			if(list.size() == 0){
				jsonback.put("success", false);
				jsonback.put("msg", "信息获取失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，展示pdf方法end：" + jsonback.toString());
				return;
			}

			String reportUrl = list.get(0).get("reportUrl").toString();
			if(StringUtils.isBlank(reportUrl)){
				jsonback.put("success", false);
				jsonback.put("msg", "信息获取失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，展示pdf方法end：" + jsonback.toString());
				return;
			}

			InputStream in = null;
			OutputStream os = null;
			// 返回pdf流
			try {
				//创建url;
				logger.info("showPdf url" + reportUrl);
				URL url = new URL(reportUrl);
				//创建url连接;
				HttpURLConnection urlconn = (HttpURLConnection)url.openConnection();
				// 获取数据
				if(urlconn.getResponseCode()>=400){
					jsonback.put("success", false);
					jsonback.put("msg", "pdf加载失败");
					ResponseUtils.setToHttp(response, jsonback.toString());
					logger.info("care检测，展示pdf方法end：" + jsonback.toString());
					return;
				}else{
					response.setContentType("application/pdf");
					//链接远程服务器;
					urlconn.connect();
					in = urlconn.getInputStream();
					os = response.getOutputStream();
					byte[] b = new byte[512];
					while ((in.read(b)) != -1) {
						os.write(b);
					}
					os.flush();
					os.close();
					os.close();
					logger.info("pdf返回成功");

				}
			} catch (IOException e) {
				try {
					if (null != in) {
						in.close();
					}
					if (null != os) {
						os.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，展示pdf方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}
*/
	

	/**
	 * 展示pdf页面信息
	 * @param request
	 * @param response
	 */
	@RequestMapping("/showPdfDetails.hn")
	public void showPdfDetails(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入展示pdf方法start");
			String sample_id = request.getParameter("sample_id") == null ? "": request.getParameter("sample_id").trim();
			String open_id = request.getParameter("open_id") == null ? "": request.getParameter("open_id").trim();
			logger.info("showPdf sample_id:" + sample_id);
			logger.info("showPdf open_id:" + open_id);

			if ( StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，展示pdf方法end：" + jsonback.toString());
				return;
			}


			String sql = "SELECT c.sample_id,date_format( c.report_time, '%Y-%m-%d %H:%i:%s' ) AS report_time,"
					+ "date_format(c.sampling_time, '%Y-%m-%d %H:%i:%s' ) AS sampling_time,"
					+ "date_format(c.receive_time, '%Y-%m-%d %H:%i:%s' ) AS receive_time,"
					+ "c.NAME,c.age,c.gender,c.snpName,c.snpResult,w.commodity_name,c.reportUrl "
					+ "FROM care_userInfo c LEFT JOIN wx_commodity_info w ON c.test_items = w.commodity_id "
					+ "WHERE c.sample_id = ?";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(sample_id);
			JSONObject result = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
			if (result.getInt("total") == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "未查询到报告信息，请稍后重试");
				ResponseUtils.setToHttp(response, jsonback.toString());
			}else{
				jsonback.put("success", true);
				jsonback.put("msg", result.get("root"));
				ResponseUtils.setToHttp(response, jsonback.toString());
			}

			
		}catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，展示pdf方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	public static boolean dateFlag(String date) {
		Date date2 = new Date();
		Boolean flag = false;
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date expiration_time=df.parse(date);
			Date nowDate = new Date();
			flag = expiration_time.before(nowDate);
			System.out.println(flag);


		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flag;

	}

	
	public static void main(String[] args) {
		dateFlag("2021-03-09 21:56:22");
	}

	
}
