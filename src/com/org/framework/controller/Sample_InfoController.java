package com.org.framework.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
import com.org.framework.util.Configure;
import com.org.framework.util.ResponseUtils;
import com.sun.istack.internal.logging.Logger;

@Controller
@RequestMapping("/bind/")
public class Sample_InfoController {
	public static String DB = "sys";
	@Autowired
	UtilMethodService utilMethodService;
	public static Logger logger = Logger.getLogger(Sample_InfoController.class);

	@RequestMapping("bind_sampleId.hn")
	public void bind_sampleId(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("大健康绑定样本号方法start");
		String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id");
		String open_id = request.getParameter("open_id") == null ? "" : request.getParameter("open_id");
		logger.info("bind_sampleId sample_id:" + sample_id);
		logger.info("bind_sampleId open_id:" + open_id);
		JSONObject jsonback = new JSONObject();
		String user_phone = "";
		// 判断参数格式
		if (sample_id.length() != 11 && sample_id.length() != 12) {
			jsonback.put("success", false);
			jsonback.put("msg", "样本编号格式不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		String check_sampleid = "select sample_id from care_userInfo where sample_id=?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(sample_id);
		JSONObject js = utilMethodService.showObjectInfo(check_sampleid, params, DB, 0, Integer.MAX_VALUE);
		if (js.getInt("total") != 0) {
			jsonback.put("success", false);
			jsonback.put("msg", "此样本编号已被绑定");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		String sqlforwarehouse = "select test_items,status from test_box_warehouse where sample_id=?";
		ArrayList<Object> paramsforwarehouse = new ArrayList<Object>();
		paramsforwarehouse.add(sample_id);
		JSONObject jsforwarehouse = utilMethodService.showObjectInfo(sqlforwarehouse, paramsforwarehouse, DB, 0, 1);
		if (jsforwarehouse.getInt("total") == 0) {
			jsonback.put("success", false);
			jsonback.put("msg", "样本编号无效，请联系客服");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		if (!"0".equals(jsforwarehouse.getJSONArray("root").getJSONObject(0).getString("status"))) {
			jsonback.put("success", false);
			jsonback.put("msg", "此样本编号为不可使用状态，请联系客服");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		logger.info("openid:==========" + open_id);
		String sqlforphone = "select user_phone from wx_phone_for_report where openid = ?";
		ArrayList<Object> paramsforphone = new ArrayList<Object>();
		paramsforphone.add(open_id);
		JSONObject showObjectInfo = utilMethodService.showObjectInfo(sqlforphone, paramsforphone, DB, 0, 1);
		int total = showObjectInfo.getInt("total");
		if (total > 0) {
			logger.info("showObjectInfo:" + showObjectInfo);
			user_phone = showObjectInfo.getJSONArray("root").getJSONObject(0).getString("user_phone");
		} else {
			jsonback.put("success", false);
			jsonback.put("msg", "当前帐号未绑定手机号，请重新登录");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		/*
		 * String sql =
		 * "insert into care_userInfo(sample_id,phone,test_items,sampling_wx_id,sampling_time,create_time,sample_status) values(?,?,?,?,?,?,?)"
		 * ; params.clear(); params.add(sample_id); params.add(user_phone);
		 * params.add(jsforwarehouse.getJSONArray("root").getJSONObject(0).
		 * getString("test_items")); params.add(open_id);
		 * params.add(create_time); params.add(create_time); params.add("1"); js
		 * = utilMethodService.addObjectInfo(sql, DB, params);
		 * jsonback.put("success", js.getString("success"));
		 * if(js.getBoolean("success")){ jsonback.put("msg", "绑定条码成功"); }else{
		 * jsonback.put("msg", "系统故障，绑定失败"); }
		 */
		jsonback.put("success", true);
		jsonback.put("msg", "条码可使用");
		ResponseUtils.setToHttp(response, jsonback.toString());
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("大健康绑定样本号方法end" + jsonback.toString());
	}



	@RequestMapping("bind_sampleInfo.hn")
	public void bind_sampleInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("大健康绑定样本号方法start");
		String open_id = request.getParameter("open_id") == null ? "" : request.getParameter("open_id");
		String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id");
		String name = request.getParameter("name") == null ? "" : request.getParameter("name");
		String gender = request.getParameter("gender") == null ? "" : request.getParameter("gender");
		String age = request.getParameter("age") == null ? "" : request.getParameter("age");

		String height = request.getParameter("height") == null ? "" : request.getParameter("height");
		String weight = request.getParameter("weight") == null ? "" : request.getParameter("weight");
		String national = request.getParameter("national") == null ? "" : request.getParameter("national");
		String questionnaire = request.getParameter("questionnaire") == null ? "" : request.getParameter("questionnaire");
		logger.info("bind_sampleInfo open_id:" + open_id);
		logger.info("bind_sampleInfo sample_id:" + sample_id);
		logger.info("bind_sampleInfo name:" + name);
		logger.info("bind_sampleInfo gender:" + gender);
		logger.info("bind_sampleInfo age:" + age);

		logger.info("bind_sampleInfo height:" + height);
		logger.info("bind_sampleInfo weight:" + weight);
		logger.info("bind_sampleInfo national:" + national);
		logger.info("bind_sampleInfo questionnaire:" + questionnaire);
		String user_phone = "";
		JSONObject jsonback = new JSONObject();

		String sqlforphone = "select user_phone from wx_phone_for_report where openid = ?";
		ArrayList<Object> paramsforphone = new ArrayList<Object>();
		paramsforphone.add(open_id);
		JSONObject showObjectInfo = utilMethodService.showObjectInfo(sqlforphone, paramsforphone, DB, 0, 1);
		int total = showObjectInfo.getInt("total");
		if (total > 0) {
			logger.info("showObjectInfo:" + showObjectInfo);
			user_phone = showObjectInfo.getJSONArray("root").getJSONObject(0).getString("user_phone");
		} else {
			jsonback.put("success", false);
			jsonback.put("msg", "当前帐号未绑定手机号，请重新登录");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		// 判断参数格式
		if (sample_id.length()!=11 && sample_id.length()!=12) {
			jsonback.put("success", false);
			jsonback.put("msg", "样本号格式不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if (StringUtils.isBlank(name)) {
			jsonback.put("success", false);
			jsonback.put("msg", "姓名不能为空");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		boolean result = age.matches("[0-9]+");// 判断年龄是否是纯数字
		if (StringUtils.isBlank(age) || !result) {
			jsonback.put("success", false);
			jsonback.put("msg", "年龄格式不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if (!gender.equals("女") && !gender.equals("男")) {
			jsonback.put("success", false);
			jsonback.put("msg", "性别格式不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		String create_time = sdf.format(cal.getTime());
		String check_sampleid = "select sample_id from care_userInfo where sample_id=?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(sample_id);
		JSONObject js = utilMethodService.showObjectInfo(check_sampleid, params, DB, 0, Integer.MAX_VALUE);
		if(js.getInt("total")!=0){
			jsonback.put("success", false);
			jsonback.put("msg", "此样本编号已被绑定");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		String sqlforwarehouse = "select test_items,sample_type,status,stock_up_channel from test_box_warehouse where sample_id=?";
		ArrayList<Object> paramsforwarehouse = new ArrayList<Object>();
		paramsforwarehouse.add(sample_id);
		JSONObject jsforwarehouse = utilMethodService.showObjectInfo(sqlforwarehouse, paramsforwarehouse, DB, 0, 1);
		if (jsforwarehouse.getInt("total") == 0) {
			jsonback.put("success", false);
			jsonback.put("msg", "样本编号无效，请联系客服");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		if (!"0".equals(jsforwarehouse.getJSONArray("root").getJSONObject(0).getString("status"))) {
			jsonback.put("success", false);
			jsonback.put("msg", "此样本编号为不可使用状态，请联系客服");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}

		String test_items = jsforwarehouse.getJSONArray("root").getJSONObject(0).getString("test_items");
		String sample_type = jsforwarehouse.getJSONArray("root").getJSONObject(0).getString("sample_type");
		String stock_up_channel = jsforwarehouse.getJSONArray("root").getJSONObject(0).getString("stock_up_channel");

		ArrayList<Object> sqlList = new ArrayList<Object>();
		ArrayList<ArrayList<Object>> paramsList = new ArrayList<ArrayList<Object>>();

		String sql = "insert into care_userInfo(phone,sampling_wx_id,sample_id,name,age,gender,create_time,sampling_time,sample_status,test_items,sample_type,sales_channel,questionnaire,height,weight,national) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		params.clear();
		params.add(user_phone);
		params.add(open_id);
		params.add(sample_id);
		params.add(name);
		params.add(age);
		params.add(gender);
		params.add(create_time);
		params.add(create_time);
		params.add("2");
		params.add(test_items);
		params.add(sample_type);
		params.add(stock_up_channel);
		params.add(questionnaire);
		params.add(height);
		params.add(weight);
		params.add(national);

		sqlList.add(sql);
		paramsList.add(params);

		String sql1 = "update test_box_warehouse set status=? where sample_id=?";
		ArrayList<Object> params1 = new ArrayList<Object>();
		params1.add(1);
		params1.add(sample_id);
		sqlList.add(sql1);
		paramsList.add(params1);
		JSONObject rollbackObj = JSONObject.fromObject(utilMethodService.rollbackObjectInfo(sqlList, paramsList, DB));
		logger.info("回滚返回的信息" + rollbackObj);

		//js = utilMethodService.addObjectInfo(sql, DB, params);
		//jsonback.put("success", js.getString("success"));
		if (rollbackObj.getBoolean("success")) {
			jsonback.put("success", true);
			jsonback.put("msg", "绑定信息成功");

			String sqlfortest_name = "select commodity_name from wx_commodity_info where commodity_id=?";
			ArrayList<Object> paramsfortest_name = new ArrayList<Object>();
			paramsfortest_name.add(test_items);
			JSONObject jsfortest_name = utilMethodService.showObjectInfo(sqlfortest_name, paramsfortest_name, DB, 0, 1);
			String test_name = jsfortest_name.getJSONArray("root").getJSONObject(0).getString("commodity_name");

			String sqlforservice_tel = "select service_tel from service_tel_info ";
			ArrayList<Object> paramsforservice_tel = new ArrayList<Object>();
			JSONObject jsforservice_tel = utilMethodService.showObjectInfo(sqlforservice_tel, paramsforservice_tel, DB, 0, 1);
			String service_tel = jsforservice_tel.getJSONArray("root").getJSONObject(0).getString("service_tel");

			// 绑定成功，给用户发短信
			bindSuccessToSMS(user_phone,name,sample_id,test_name,service_tel);
		} else {
			jsonback.put("success", false);
			jsonback.put("msg", "系统故障，绑定失败");
		}
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("大健康绑定样本号方法end" + jsonback.toString());
	}


	/**
	 * 绑定成功后发送的短信消息
	 * @param
	 * @param
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
	public static void bindSuccessToSMS(String phone,String name,String sample_id,String test_name,String service_tel) throws ClientProtocolException, IOException{
		logger.info("***********进入绑定成功后发送的短信消息方法*****************");
		logger.info("接收到的参数，phone=" + phone);
		JSONObject jsonback = new JSONObject();

		/*CloseableHttpClient clientForMsg = HttpClients.createDefault();
        HttpPost postForMsg = new HttpPost(Configure.getUrlForSMS());
        List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();

        String contentCode = Configure.getCodeForCodeSuccessfullyBinding2();
        //短信参数
		JSONObject contentParam = new JSONObject();
		contentParam.put("name", name);
		contentParam.put("sample_id", sample_id);
		contentParam.put("test_name", test_name);
		contentParam.put("service_tel", service_tel);

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
	
	@RequestMapping("showQuestionnaire.hn")
	public void showQuestionnaire(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("大健康展示问卷方法start");
		String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id");
		logger.info("showQuestionnaire sample_id:" + sample_id);
		JSONObject jsonback = new JSONObject();
		
		// 判断参数格式
		if (sample_id.length() != 11 && sample_id.length() != 12) {
			jsonback.put("success", false);
			jsonback.put("msg", "样本编号格式不正确");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		String check_sampleid = "select wx.questionnaire from test_box_warehouse tw left join wx_commodity_info wx "
				+ "on tw.test_items = wx.commodity_id where tw.sample_id=?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(sample_id);
		JSONObject js = utilMethodService.showObjectInfo(check_sampleid, params, DB, 0, 1);
		if (js.getInt("total") == 0) {
			jsonback.put("success", false);
			jsonback.put("msg", "无样本编号");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		
		if ("".equals(js.getJSONArray("root").getJSONObject(0).getString("questionnaire"))) {
			jsonback.put("success", false);
			jsonback.put("msg", "无问卷");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		 
		JSONArray datas =  new JSONArray();
		String questionnaire =  js.getJSONArray("root").getJSONObject(0).getString("questionnaire");
		String questionnaireList[] = questionnaire.split("#\\$#");
		logger.info("questionnaireList[0]:"+questionnaireList[0]);
		logger.info("questionnaireList[1]:"+questionnaireList[1]);
		logger.info("questionnaireList[2]:"+questionnaireList[2]);
		for(int i = 0;i<questionnaireList.length;i++){
			String qlist[] = questionnaireList[i].split("#&#");
			JSONObject data = new JSONObject();
			JSONArray d = new JSONArray();
			for(int j = 1;j<qlist.length;j++){
				d.add(qlist[j]);
			}
			
			data.put("target",qlist[0]);
			data.put("select",d);
			datas.add(data);
		}
		jsonback.put("success", true);
		jsonback.put("msg", datas);
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("showQuestionnaire end" + jsonback.toString());
	}
}
