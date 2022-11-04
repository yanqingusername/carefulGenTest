package com.org.framework.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.org.framework.util.Configure;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.org.framework.service.UtilMethodService;
import com.org.framework.util.ResponseUtils;
import com.sun.istack.internal.logging.Logger;


@Controller
@RequestMapping("/login")
public class CareLoginController {
	@Autowired
	UtilMethodService utilMethodService;
	public static Logger logger = Logger.getLogger(CareLoginController.class);
	public static String DB = "sys";
	private static String msgUrl = "http://cloud.coyotebio-lab.com/smsApi/sendsms.hn";

	/**
	 * 发送验证码
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	@RequestMapping("/Verification.hn")
	public void Verification(HttpServletRequest request, HttpServletResponse response) throws ClientProtocolException, IOException {
		logger.info("大健康手机号登录发送验证码方法start");
		String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();
		// 校验手机号
		JSONObject jsonback = new JSONObject();
		if (phone.equals("")) {
			jsonback.put("success", false);
			jsonback.put("msg", "手机号为空");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		Boolean check_phone = CheckPhone(phone);
		if (!check_phone) {
			jsonback.put("success", false);
			jsonback.put("msg", "手机号格式不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		logger.info("手机号" + phone);
		String str = "";// 验证码
		Random ran = new Random();
		for (int i = 0; i < 4; i++) {
			int number = ran.nextInt(10);
			str += String.valueOf(number);
		}
		System.out.println("验证码：" + str);


//		CloseableHttpClient httpclient = HttpClients.createDefault();
//		HttpPost postForMsg = new HttpPost(msgUrl);
//		JSONObject contentParam = new JSONObject();
//		contentParam.put("code", str);
//		List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();
//		listForMsg.add(new BasicNameValuePair("phoneNumber", phone));
//		listForMsg.add(new BasicNameValuePair("contentCode", "SMS_213678955"));
//		listForMsg.add(new BasicNameValuePair("contentParam", contentParam.toString()));
//		listForMsg.add(new BasicNameValuePair("signName", "卡尤迪"));
//		UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg, "utf-8");
//		postForMsg.setEntity(uefEntityForMsg);
//		HttpResponse responseForMsg = httpclient.execute(postForMsg);// 接收响应
//		responseForMsg.getEntity();
//		int code = responseForMsg.getStatusLine().getStatusCode();
//		String back_msg = null;
//		try {
//			back_msg = EntityUtils.toString(uefEntityForMsg, "utf-8");
//		} catch (ParseException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}


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

		// 将验证码存到session中,同时存入创建时间
		// 以json存放，这里使用的是阿里的fastjson

		JSONObject msg_json = new JSONObject();
		msg_json.put("phone", phone);
		msg_json.put("verifyCode", str);// 验证码
		msg_json.put("createTime", System.currentTimeMillis());// 时间戳
		//jsonback.put("success", true);
		//jsonback.put("code", code);
		request.getSession().setAttribute(phone, msg_json);// 存到session
		ResponseUtils.setToHttp(response, jsonback.toString());
	}

	/**
	 * 校验手机号
	 * 
	 * @param phone
	 * @return
	 */
	public static boolean CheckPhone(String phoneNum) {
		String regex = "^(1[1-9]\\d{9}$)";
		if (phoneNum.length() == 11) {
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(phoneNum);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 登录
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/login.hn")
	public void login(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("大健康手机号登录方法start");
		JSONObject jsonback = new JSONObject();
		String phone = request.getParameter("phone") == null ? "" : request.getParameter("phone").trim();
		String open_id = request.getParameter("open_id") == null ? "" : request.getParameter("open_id").trim();
		String verCode = request.getParameter("verCode") == null ? "" : request.getParameter("verCode").trim();
		JSONObject json = JSONObject.fromObject(request.getSession().getAttribute(phone));
		System.out.println("获取的session内容：" + json);
		if (StringUtils.isBlank(verCode)) {
			jsonback.put("success", false);
			jsonback.put("msg", "验证码为空");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if (StringUtils.isBlank(phone)) {
			jsonback.put("success", false);
			jsonback.put("msg", "手机号为空");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if (StringUtils.isBlank(open_id)) {
			jsonback.put("success", false);
			jsonback.put("msg", "openid为空");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if (json.isEmpty()) {
			jsonback.put("success", false);
			jsonback.put("msg", "请获取验证码");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		String phone_session = json.getString("phone");// session中的phone
		if (!phone.equals(phone_session)) {
			jsonback.put("success", false);
			jsonback.put("msg", "手机号不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		String verifyCode_session = json.getString("verifyCode");

		if (!verCode.equals(verifyCode_session)) {
			jsonback.put("success", false);
			jsonback.put("msg", "验证码不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if ((System.currentTimeMillis() - json.getLong("createTime")) > 1000 * 60 * 5) {
			jsonback.put("success", false);
			jsonback.put("msg", "验证码超时");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		String time = sdf.format(cal.getTime());

		String sql = "select openid from wx_phone_for_report where openid = ?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(open_id);
		JSONObject res = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		int sum = res.getInt("total");
		logger.info("sum===="+sum);
		JSONObject result = new JSONObject();
		String SQL = "";// 更新数据库sql
		if (sum == 0) {
			params.add(phone);
			params.add(time);
			SQL = "insert into wx_phone_for_report(openid,user_phone,create_time) values(?,?,?)";
			result = utilMethodService.addObjectInfo(SQL, DB, params);
		} else {
			params.clear();
			params.add(phone);
			params.add(time);
			params.add(open_id);
			SQL = "update wx_phone_for_report set user_phone=?,update_time=? where openid = ?";
			result = JSONObject.fromObject(utilMethodService.updateObjectInfo(SQL, DB, params));
		}
		logger.info("success===="+result.getBoolean("success"));
		if (result.getBoolean("success")) {
			jsonback.put("success", true);
			jsonback.put("msg", "登陆成功");
			ResponseUtils.setToHttp(response, jsonback.toString());
		} else {
			jsonback.put("success", false);
			jsonback.put("msg", "登陆失败");
			ResponseUtils.setToHttp(response, jsonback.toString());
		}
		logger.info("大健康手机号登录方法end"+jsonback.toString());
	}

	 /**
		 * 自动登陆
		 */
	    @RequestMapping("autoLogin.hn")
		public void autoLogin(HttpServletRequest request,HttpServletResponse response){
			logger.info("获取openid方法start*******************************");
			String code = request.getParameter("code") == null ? "" : request.getParameter("code").trim();
			logger.info("code:"+code);
			JSONObject jsonback = new JSONObject();
			String openid = (String)request.getSession().getAttribute("openid");
			if ("".equals(openid) || "undefined".equals(openid)||openid==null||"0".equals(openid)) {
				if ("".equals(code) || "undefined".equals(code)) {
					jsonback.put("success", false);
					jsonback.put("error_code", "1000");
					jsonback.put("error", "没对公众号授权！");
					ResponseUtils.setToHttp(response, jsonback.toString());
					logger.info(code+"获取openid方法  jsonback:"+jsonback);
					return;
				}
				JSONObject accessToken = getAccessToken(code);
				logger.info("accessToken:"+accessToken);
				openid = accessToken.getString("openid");
				request.getSession().setAttribute("openid", openid);
				logger.info("openid:"+openid);
			}
			
			JSONArray loginuser = new JSONArray();
			try {
				String SQL = "select * from wx_phone_for_report where openid = ?";
				ArrayList<Object> params = new ArrayList<Object>();
				params.add(openid);
				loginuser = JSONArray.fromObject(utilMethodService.getObjectList(SQL, params, DB));
			} catch (Exception e) {
				jsonback.put("success", false);
				jsonback.put("error_code", "1001");
				jsonback.put("error", "系统异常");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info(code+"获取openid方法  jsonback:"+jsonback);
				return;
			}
			
			if(loginuser.size()>0){//可以直接登录
				JSONObject temp = loginuser.getJSONObject(0);
				logger.info("temp:" + temp);
				jsonback.put("success", true);
				jsonback.put("msg", "自动登陆成功");
				jsonback.put("code", 100);
				jsonback.put("user_phone", temp.getString("user_phone"));
				jsonback.put("openid", openid);
			}else{
				jsonback.put("success", true);
				jsonback.put("code", 101);
				jsonback.put("msg", "自动登录失败，需手动登录");
				jsonback.put("openid", openid);
			}
			
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info(code+"自动登陆  jsonback：" + jsonback.toString());
		}
	    
	/**
	 * 自动登录
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/getOpenid.hn")
	public void getOpenid(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("获取openid方法start*******************************");
		String code = request.getParameter("code") == null ? "" : request.getParameter("code").trim();
		logger.info("code:" + code);
		JSONObject jsonback = new JSONObject();
		if ("".equals(code) || "undefined".equals(code)) {
			jsonback.put("success", false);
			jsonback.put("error_code", "1000");
			jsonback.put("error", "没对公众号授权！");
			logger.info("enterExam  jsonback:" + jsonback);
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		HttpSession session = request.getSession();
		String openid = (String)session.getAttribute("openid") == null ? "":(String)session.getAttribute("openid");
		if(openid==""||openid=="0"){
			logger.info("session内无openid");
			JSONObject accessToken = getAccessToken(code);
			openid = accessToken.getString("openid");
			logger.info("openid:==========" + openid);
		}
		
		logger.info("openid:==========" + openid);
		String SQL = "select * from wx_phone_for_report where openid = ?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(openid);
		JSONObject showObjectInfo = utilMethodService.showObjectInfo(SQL, params, DB, 0, 1);
		int total = showObjectInfo.getInt("total");

		if (total > 0) {// 可以直接登录
			JSONObject jsonObject = showObjectInfo.getJSONArray("root").getJSONObject(0);
			logger.info("jsonObject:" + jsonObject);
			String user_phone = jsonObject.getString("user_phone");
			logger.info("user_phone:" + user_phone);
			jsonback.put("success", true);
			jsonback.put("msg", "自动登陆成功");
			jsonback.put("code", 100);
			jsonback.put("user_phone", user_phone);
			jsonback.put("openid", openid);

			// 根据openid 进行操作
			logger.info("获取的openid**********************" + openid);
			request.getSession().setAttribute("openid", openid);
		} else {
			jsonback.put("success", true);
			jsonback.put("code", 101);
			jsonback.put("msg", "自动登录失败，需手动登录");
			jsonback.put("openid", openid);
		}

		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("新冠报告查询系统,获取openid方法，返回结果：" + jsonback.toString());
	}

	// 获取code
	public static JSONObject getAccessToken(String code) {
		JSONObject jsonObject = new JSONObject();
		String url = "https://api.weixin.qq.com/sns/oauth2/access_token";
		url = url + "?appid=" + com.org.framework.util.Configure.getAppid() + "&secret=" + com.org.framework.util.Configure.getAppSecrte()+ "&code=" + code + "&grant_type=authorization_code";
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(url);
		String result = "";
		try {
			httpClient.executeMethod(getMethod);
			result = getMethod.getResponseBodyAsString();
			logger.info("=========返回结果：" + result);
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

	//注销接口
  	@RequestMapping("/logout.hn")
  	public void logout(HttpServletRequest request, HttpServletResponse response) {
  		JSONObject jsonback = new JSONObject();
  		try {
  			logger.info("新冠报告查询系统,，注销方法");
  			String openid = request.getParameter("openid") == null ? "" : request.getParameter("openid").trim();
  			logger.info("logout openid：" + openid);
  			if (StringUtils.isBlank(openid)) {
  				jsonback.put("success", false);
  				jsonback.put("msg", "未获取用户信息");
  				ResponseUtils.setToHttp(response, jsonback.toString());
  				logger.info("新冠报告查询系统,，注销方法end：" + jsonback.toString());
  				return;
  			}
  			request.getSession().setAttribute("openid", "0");
  			String sql = "delete from wx_phone_for_report where openid=? ";
  			ArrayList<Object> params = new ArrayList<Object>();
  			params.add(openid);
  			JSONObject result = JSONObject.fromObject(utilMethodService.deleteObjectInfo(sql, DB, params));
  			if(result.getBoolean("success")){
  				jsonback.put("success", true);
  				jsonback.put("msg", "注销成功");
  			}else{
  				jsonback.put("success", false);
  				jsonback.put("msg", "注销失败");
  			}
  		} catch (Exception e) {
  			e.printStackTrace();
  			jsonback.put("success", false);
  			jsonback.put("msg", "系统异常，请联系技术支持");
  		}
  		
  		ResponseUtils.setToHttp(response, jsonback.toString());
  		logger.info("新冠报告查询系统,，注销方法end：" + jsonback.toString());
  	}
  	
  	public static void main(String[] args) {
		System.out.println(CheckPhone("17864239212"));
		boolean result = "12sd".matches("[0-9]+");// 判断年龄是否是纯数字
		System.out.println(result);
	}
}
