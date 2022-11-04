package com.org.framework.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.org.framework.bean.Ship;
import com.org.framework.exception.ControllerException;
import com.org.framework.service.UtilMethodService;
import com.org.framework.util.AccessTokenUtil;
import com.org.framework.util.Configure;
import com.org.framework.util.ResponseUtils;
import com.org.framework.util.TransApi;
import com.sf.csim.express.service.EspServiceCode;
import com.sf.csim.express.service.HttpClientUtil;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sun.misc.BASE64Encoder;

/**
 * 快递
 */
@RequestMapping("/express")
@Controller
public class KuaiDiNiaoController {

	private static final String DB = "sys";

	// 物流公司
	private static String ShipperCodeSF = "SF";
	private static final String CLIENT_CODE = "KYDSWP";  //此处替换为您在丰桥平台获取的顾客编码
    private static final String CHECK_WORD = "ezFy3UABwUSwtxjKcj63sowLiYAHTtTY";//此处替换为您在丰桥平台获取的校验码
    //沙箱环境的地址ַ
    //private static final String CALL_URL_BOX = "https://sfapi-sbox.sf-express.com/std/service";
    //生产环境的地址 ַ
    private static final String CALL_URL_PROD = "https://sfapi.sf-express.com/std/service";
    
	private static Logger logger = Logger.getLogger(KuaiDiNiaoController.class);

	// private static final String updateExpressModelId =
	// "6UrSpNOXo7m4EHfu7m0hOwrJ_uEmgvd0-BrW43MbhyM";//物流信息通知
	private static final String updateExpressModelId = "6UZerHRw4gsVxidXqi7B0yTR3vk1L-Evrzs-4Lf5j6I";// 物流信息通知
																										// 正式
	// private static final String callbackUrl =
	// "http://m.kuaidi100.com/index_all.html?type=shunfeng&postid=";
	//private static final String callbackUrl = "http://syrdev.coyotebio-lab.com/carefulGenTest/test/getSFTrack.html?";// 大健康测试
	private static final String callbackUrl = "http://store.coyotebio-lab.com/carefulGenTest/test/getSFTrack.html?";//大健康正式
	private static String MonthCode = "0100004314";// 顺丰月结号

	@Autowired
	public UtilMethodService utilMethodService;

	@RequestMapping(value = "/trace.hn", method = RequestMethod.POST)
	public void tuisong(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("***********进入接收快递鸟推送方法*******");
		String RequestData = request.getParameter("RequestData") == null ? ""
				: request.getParameter("RequestData").trim();
		String RequestType = request.getParameter("RequestType") == null ? ""
				: request.getParameter("RequestType").trim();
		String DataSign = request.getParameter("DataSign") == null ? "" : request.getParameter("DataSign").trim();

		logger.info("接收到的参数：RequestData=" + RequestData);
		logger.info("接收到的参数：RequestType=" + RequestType);

		Map<String, Object> result = new HashMap<String, Object>();

		// 判断是从快递鸟进入
		if (!(RequestType.equals("102") && !RequestType.equals("101"))) {
			result.put("Success", false);
			result.put("Reason", "不是快递鸟推送来的数据。");
			logger.info("fail");
			return;
		}

		JSONObject jsonObj = new JSONObject();
		jsonObj = JSONObject.fromObject(RequestData);

		result.put("EBusinessID", jsonObj.getString("EBusinessID"));
		result.put("UpdateTime", jsonObj.getString("PushTime"));
		// 先返回，再做逻辑处理
		result.put("Success", true);
		result.put("Reason", "");
		logger.info("返回结果：" + result.toString());
		JSONObject jsonBack = JSONObject.fromObject(result);
		setToHttp(response, jsonBack.toString());

		// TODO 获取回传的样本编号，暂时先根据物流单号获取样本编号
		// String Callback = jsonObj.getString("Callback")==null ?
		// "用户自定义回传字段不存在" : jsonObj.getString("Callback");
		// logger.info("自定义字段(这里是样本编号) Callback=" + Callback);

		// 推送轨迹的轨迹数据集合
		JSONArray jsonArray = jsonObj.getJSONArray("Data");
		List<Ship> shipList = new ArrayList<Ship>();
		Ship ship = null;
		for (int i = 0; i < jsonArray.size(); i++) {
			jsonObj = (JSONObject) jsonArray.get(i);
			if (!jsonObj.getBoolean("Success")) {
				continue;
			}

			ship = new Ship();
			ship.setExpress(jsonObj.getString("ShipperCode"));
			ship.setExpressCode(jsonObj.getString("LogisticCode"));
			ship.setStatus(jsonObj.getString("State"));

			logger.info("物流状态：" + jsonObj.getString("State"));

			if (!jsonObj.getString("State").equals("0")) {
				JSONArray array = jsonObj.getJSONArray("Traces");
				JSONObject obj = array.getJSONObject(array.size() - 1);
				String time = obj.getString("AcceptTime");
				String acceptStation = obj.getString("AcceptStation");
				ship.setAcceptTime(time);
				ship.setAcceptStation(acceptStation);
			}
			logger.info("ship:" + ship.toString());
			shipList.add(ship);
		}

		logger.info("shipList:" + shipList.toString());

		logger.info("开始更新物流状态");

		Ship ship2 = null;
		logger.info("循环更新开始,size=" + shipList.size());
		for (int i = 0; i < shipList.size(); i++) {
			ship2 = shipList.get(i);
			String logisticsNumber = ship2.getExpressCode();
			String logisticsStatus = ship2.getStatus();
			String acceptStation = ship2.getAcceptStation();
			String acceptTime = ship2.getAcceptTime();
			logger.info("物流单号logisticsNumber=" + logisticsNumber);
			logger.info("物流状态logisticsStatus=" + logisticsStatus);
			logger.info("最新物流信息 acceptStation=" + acceptStation);
			logger.info("轨迹发生时间 acceptTime=" + acceptTime);

			if (logisticsStatus.equals("0")) {
				logisticsStatus = "正在出库";
			}
			if (logisticsStatus.equals("1")) {
				logisticsStatus = "正在配送中";
			}
			if (logisticsStatus.equals("2")) {
				logisticsStatus = "正在派送";
			}
			if (logisticsStatus.equals("3")) {
				logisticsStatus = "已签收";
			}
			if (logisticsStatus.equals("4")) {
				logisticsStatus = "问题件";
			}
			logger.info("物流状态logisticsStatus=" + logisticsStatus);

			int get_logisticsNumber = 0;// 0:care_userInfo表内无对应的寄出或回寄的物流单号
										// 1:寄出单号 2:回寄单号
			String openId = "";
			String phone = "";
			logger.info("进入根据物流单号获取相关信息");
			String sql1 = "SELECT * FROM care_userInfo WHERE logistics_num = ?";// 寄出更新状态
			ArrayList<Object> params1 = new ArrayList<Object>();
			params1.add(logisticsNumber);
			List<HashMap<String, Object>> list1 = utilMethodService.getObjectList(sql1, params1, DB);
			logger.info("list1:" + list1);
			if (list1.size() > 0) {
				openId = list1.get(0).get("pay_wx_id").toString();// 发货时需要使用此id发送模板消息
				phone = list1.get(0).get("addressee_phone").toString();// 发货时需要使用此手机号
				get_logisticsNumber = 1;
				logger.info("用户openId=" + openId);
				if (logisticsStatus.equals("正在配送中")) {
					String sqlForUpdate1 = "update care_userInfo set sample_status = 1 where logistics_num = ?";
					ArrayList<Object> paramsForUpdate1 = new ArrayList<Object>();
					paramsForUpdate1.add(logisticsNumber);
					String result1 = utilMethodService.updateObjectInfo(sqlForUpdate1, DB, paramsForUpdate1);
					logger.info("快递鸟发货更新结果：" + result1);
				} else if (logisticsStatus.equals("已签收")) {
					String sqlForUpdate2 = "update care_userInfo set sign_time=? where logistics_num = ?";
					ArrayList<Object> paramsForUpdate2 = new ArrayList<Object>();
					paramsForUpdate2.add(acceptTime);
					paramsForUpdate2.add(logisticsNumber);
					String result1 = utilMethodService.updateObjectInfo(sqlForUpdate2, DB, paramsForUpdate2);
					logger.info("快递鸟发货更新结果：" + result1);
				}
			}

			String sql2 = "SELECT * FROM care_userInfo WHERE send_number = ?";// 回寄更新状态
			ArrayList<Object> params2 = new ArrayList<Object>();
			params2.add(logisticsNumber);
			List<HashMap<String, Object>> list2 = utilMethodService.getObjectList(sql2, params2, DB);
			logger.info("list2:" + list2);
			if (list2.size() > 0) {
				openId = list2.get(0).get("sampling_wx_id").toString();// 绑定成功、收样成功、出报告成功使用此id发送模板消息
				phone = list2.get(0).get("phone").toString();// 绑定成功、收样成功、出报告成功使用此手机号
				get_logisticsNumber = 2;
				logger.info("用户openId=" + openId);
				if (logisticsStatus.equals("正在配送中")) {
					String sqlForUpdate2 = "update care_userInfo set sample_status = 3,back_send_time=? where send_number = ?";
					ArrayList<Object> paramsForUpdate2 = new ArrayList<Object>();
					paramsForUpdate2.add(acceptTime);
					paramsForUpdate2.add(logisticsNumber);
					String result1 = utilMethodService.updateObjectInfo(sqlForUpdate2, DB, paramsForUpdate2);
					logger.info("快递鸟回寄更新结果：" + result1);
				} else if (logisticsStatus.equals("已签收")) {
					String sqlForUpdate2 = "update care_userInfo set back_sign_time=? where send_number = ?";
					ArrayList<Object> paramsForUpdate2 = new ArrayList<Object>();
					paramsForUpdate2.add(acceptTime);
					paramsForUpdate2.add(logisticsNumber);
					String result1 = utilMethodService.updateObjectInfo(sqlForUpdate2, DB, paramsForUpdate2);
					logger.info("快递鸟回寄更新结果：" + result1);
				}
			}

			if (get_logisticsNumber == 0) {
				logger.info("根据物流单号获取样本号失败！");
				return;
			}

			// 发送模板消息
			// 下为 careFulGen通知 暂时取消 ，之后需根据sales_channel字段判断
			//logisticsInfo(ship2, openId, phone);
		}

		logger.info("***********离开接收快递鸟推送方法*******");
	}

	/**
	 * 物流状态改变时模板消息发送物流信息
	 * 
	 * @param ship
	 * @param openId
	 * @param sampleNumber
	 */
	public static void logisticsInfo(Ship ship, String openId, String phone) {
		logger.info("***********进入更新物流信息后发送的模板消息方法*****************");
		logger.info("接收到的参数，ship=" + ship);
		logger.info("接收到的参数，openid=" + openId);

		String logisticsStatus = ship.getStatus();
		if (logisticsStatus.equals("0")) {
			logisticsStatus = "正在出库";
		}
		if (logisticsStatus.equals("1")) {
			logisticsStatus = "正在配送中";
		}
		if (logisticsStatus.equals("2")) {
			logisticsStatus = "正在派送";
		}
		if (logisticsStatus.equals("3")) {
			logisticsStatus = "已签收";
		}
		if (logisticsStatus.equals("4")) {
			logisticsStatus = "问题件";
		}
		logger.info("物流状态logisticsStatus=" + logisticsStatus);

		String express = ship.getExpress();
		if ("SF".equals(express)) {
			express = "顺丰";
		}

		// 获取token
		String accessToken = AccessTokenUtil.getAccessToken();
		// api
		String msgUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
		// post请求
		JSONObject big = new JSONObject();
		big.put("touser", openId);
		big.put("template_id", updateExpressModelId);
		big.put("url", callbackUrl + "order_serial=" + ship.getExpressCode() + "&phone=" + phone);

		JSONObject first = new JSONObject();
		JSONObject middle = new JSONObject();
		first.put("value", "您的订单有物流更新，当前状态:" + logisticsStatus);
		first.put("color", "#173177");
		middle.put("first", first);

		JSONObject keyword1 = new JSONObject();
		keyword1.put("value", express);
		keyword1.put("color", "#173177");
		middle.put("keyword1", keyword1);

		JSONObject keyword2 = new JSONObject();
		keyword2.put("value", ship.getExpressCode());
		keyword2.put("color", "#173177");
		middle.put("keyword2", keyword2);

		/**
		 * JSONObject keyword5 = new JSONObject(); keyword5.put("value",
		 * ship.getAcceptStation()); keyword5.put("color", "#173177");
		 * middle.put("keyword5", keyword5);
		 **/

		JSONObject remark = new JSONObject();
		remark.put("value", ship.getAcceptStation());
		remark.put("color", "#173177");
		middle.put("remark", remark);
		big.put("data", middle);

		String post = post(big, msgUrl);
		logger.info("post请求返回的结果：" + post);
		JSONObject backResult = JSONObject.fromObject(post);
		logger.info("发送模板消息返回结果：" + backResult.getString("errmsg"));

		logger.info("***********离开样本绑定成功后发送的模板消息方法*****************");
	}

	public static void setToHttp(HttpServletResponse response, String jsonObject) {
		PrintWriter writer = null;
		try {
			response.setCharacterEncoding("utf-8");
			response.setContentType("application/json;charset=utf-8");
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Access-Control-Allow-Origin", "*");
			writer = response.getWriter();
			if (jsonObject != null) {
				writer.write(jsonObject.toString());
			}
			writer.flush();
		} catch (IOException e) {
			throw new ControllerException("发生输出IO异常！", e);
		} catch (Exception e) {
			throw new ControllerException("发生调用异常！", e);
		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	public static String post(JSONObject json, String URL) {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(URL);
		post.setHeader("Content-Type", "application/json");
		post.addHeader("Authorization", "Basic YWRtaW46");
		String result = "";

		try {
			StringEntity s = new StringEntity(json.toString(), "utf-8");
			s.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
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
	 * 预约快递员上门
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/appointmentExpress.hn")
	public void appointmentExpress(HttpServletRequest request, HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入预约快递员上门方法start");
			String province = request.getParameter("province") == null ? "" : request.getParameter("province").trim();
			String city = request.getParameter("city") == null ? "" : request.getParameter("city").trim();
			String county = request.getParameter("county") == null ? "" : request.getParameter("county").trim();
			String address = request.getParameter("address") == null ? "" : request.getParameter("address").trim();
			String sample_id = request.getParameter("sample_id") == null ? ""
					: request.getParameter("sample_id").trim();
			logger.info("appointmentExpress province:" + province);
			logger.info("appointmentExpress city:" + city);
			logger.info("appointmentExpress county:" + county);
			logger.info("appointmentExpress address:" + address);
			logger.info("appointmentExpress sample_id:" + sample_id);

			if (StringUtils.isBlank(province) || StringUtils.isBlank(city) || StringUtils.isBlank(county)
					|| StringUtils.isBlank(address) || StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			String sql = "SELECT sample_status,phone,name,send_number FROM care_userInfo where sample_id = ? ";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(sample_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			if (list.size() == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "未查询到样本信息，预约失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			String sample_status = list.get(0).get("sample_status").toString();
			String phone = list.get(0).get("phone").toString();
			String name = list.get(0).get("name").toString();
			String send_number = list.get(0).get("send_number").toString();
			/*
			 * 样本状态 0已配货 1已发货 （已邮寄给客户） 2已绑定 （用户在公众号上进行了绑定） 3已接收（实验室接收到用户邮寄的样本）
			 * 4已报告
			 */
			if ("3".equals(sample_status) || "4".equals(sample_status) || "5".equals(sample_status)) {
				jsonback.put("success", false);
				jsonback.put("msg", "样本已接收，无需邮寄");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}
			if ("0".equals(sample_status) || "1".equals(sample_status)) {
				jsonback.put("success", false);
				jsonback.put("msg", "样本未绑定，无法邮寄");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			if (StringUtils.isNotBlank(send_number)) {
				jsonback.put("success", false);
				jsonback.put("msg", "样本已邮寄，运单号：" + send_number);
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			// 获取收件地址信息
			String sqlForReceipt = "SELECT province,city,area,address,company,name,phone FROM coyote_receipt_info LIMIT 1";
			ArrayList<Object> paramsForReceipt = new ArrayList<Object>();
			List<HashMap<String, Object>> listForReceipt = utilMethodService.getObjectList(sqlForReceipt,
					paramsForReceipt, DB);
			if (listForReceipt.size() == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "收件信息获取失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			JSONObject receiptInfo = JSONObject.fromObject(listForReceipt.get(0));

			// 订单序列号
			String serialNumber = createSerialNumber();
			JSONObject json = new JSONObject();
			json.put("OrderCode", serialNumber);
			json.put("Callback", sample_id);
			json.put("ShipperCode", ShipperCodeSF);
			json.put("PayType", 2);
			json.put("ExpType", 1);

			JSONObject Sender = new JSONObject();
			Sender.put("Name", name);
			Sender.put("Mobile", phone);
			Sender.put("ProvinceName", province);
			Sender.put("CityName", city);
			Sender.put("ExpAreaName", county);
			Sender.put("Address", address);
			json.put("Sender", Sender);

			JSONObject Receiver = new JSONObject();
			Receiver.put("Company", receiptInfo.getString("company"));
			Receiver.put("Name", receiptInfo.getString("name"));
			Receiver.put("Mobile", receiptInfo.getString("phone"));
			Receiver.put("ProvinceName", receiptInfo.getString("province"));
			Receiver.put("CityName", receiptInfo.getString("city"));
			Receiver.put("ExpAreaName", receiptInfo.getString("area"));
			Receiver.put("Address", receiptInfo.getString("address"));
			json.put("Receiver", Receiver);

			json.put("Quantity", 1);
			json.put("IsNotice", 0);
			json.put("IsSendMessage", 0);
			json.put("IsSubscribe", 1);
			json.put("IsReturnPrintTemplate", 1);
			json.put("DeliveryMethod", 2);
			json.put("CurrencyCode", "CNY");

			JSONArray Commodity = new JSONArray();
			JSONObject GoodsName = new JSONObject();
			GoodsName.put("GoodsName", "采样器");
			Commodity.add(GoodsName);
			json.put("Commodity", Commodity);

			json.put("Remark", "小心轻放");
			String requestData = json.toString();

			Map<String, String> express_params = new HashMap<String, String>();
			express_params.put("RequestData", urlEncoder(requestData, "UTF-8"));
			express_params.put("EBusinessID", Configure.getKDNEBusinessID());
			express_params.put("RequestType", "1007");
			String dataSign = encrypt(requestData, Configure.getKDNAppKey(), "UTF-8");
			express_params.put("DataSign", urlEncoder(dataSign, "UTF-8"));
			express_params.put("DataType", "2");

			JSONObject result = JSONObject.fromObject(sendPost(Configure.getKDNReqURL(), express_params));
			logger.info("快递鸟电子面单返回结果:" + result.toString());

			if (!result.getBoolean("Success")) {
				jsonback.put("success", false);
				jsonback.put("msg", result.getString("Reason"));
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			JSONObject jsonForOrder = result.getJSONObject("Order");
			String LogisticCode = jsonForOrder.getString("LogisticCode");
			logger.info("电子面单获取的快递单号：" + LogisticCode);

			String sqlForUpdate = "UPDATE care_userInfo SET send_number = ?  WHERE sample_id = ?";
			ArrayList<Object> paramsForUpdate = new ArrayList<Object>();
			paramsForUpdate.add(LogisticCode);
			paramsForUpdate.add(sample_id);
			JSONObject resultForUpdate = JSONObject
					.fromObject(utilMethodService.updateObjectInfo(sqlForUpdate, DB, paramsForUpdate));
			if (!resultForUpdate.getBoolean("success")) {
				jsonback.put("success", false);
				jsonback.put("msg", "预约成功，但物流号填充失败，请手动填充");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			jsonback.put("success", true);
			jsonback.put("msg", "预约成功");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}

	private static String createSerialNumber() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMdd");
		String serialNumber = dateFormat.format(Calendar.getInstance().getTime());
		String nano = String.valueOf(System.nanoTime());
		serialNumber += nano.substring(nano.length() / 2) + String.format("%04d", (int) (Math.random() * 9999));
		return serialNumber;
	}

	private String urlEncoder(String str, String charset) throws UnsupportedEncodingException {
		String result = URLEncoder.encode(str, charset);
		return result;
	}

	/**
	 * 电商Sign签名生成
	 * 
	 * @param content
	 *            内容
	 * @param keyValue
	 *            Appkey
	 * @param charset
	 *            编码方式
	 * @throws UnsupportedEncodingException
	 *             ,Exception
	 * @return DataSign签名
	 */
	private String encrypt(String content, String keyValue, String charset)
			throws UnsupportedEncodingException, Exception {
		if (keyValue != null) {
			return base64(MD5(content + keyValue, charset), charset);
		}
		return base64(MD5(content, charset), charset);
	}

	/**
	 * MD5加密
	 * 
	 * @param str
	 *            内容
	 * @param charset
	 *            编码方式
	 * @throws Exception
	 */
	private String MD5(String str, String charset) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(str.getBytes(charset));
		byte[] result = md.digest();
		StringBuffer sb = new StringBuffer(32);
		for (int i = 0; i < result.length; i++) {
			int val = result[i] & 0xff;
			if (val <= 0xf) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(val));
		}
		return sb.toString().toLowerCase();
	}

	/**
	 * base64编码
	 * 
	 * @param str
	 *            内容
	 * @param charset
	 *            编码方式
	 * @throws UnsupportedEncodingException
	 */
	private String base64(String str, String charset) throws UnsupportedEncodingException {
		String encoded = Base64.encode(str.getBytes(charset));
		return encoded;
	}

	/**
	 * 向指定 URL 发送POST方法的请求
	 * 
	 * @param url
	 *            发送请求的 URL
	 * @param params
	 *            请求的参数集合
	 * @return 远程资源的响应结果
	 */
	private String sendPost(String url, Map<String, String> params) {
		OutputStreamWriter out = null;
		BufferedReader in = null;
		StringBuilder result = new StringBuilder();
		try {
			URL realUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			// 发送POST请求必须设置如下两行
			conn.setDoOutput(true);
			conn.setDoInput(true);
			// POST方法
			conn.setRequestMethod("POST");
			// 设置通用的请求属性
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
			conn.connect();
			// 获取URLConnection对象对应的输出流
			out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
			// 发送请求参数
			if (params != null) {
				StringBuilder param = new StringBuilder();
				for (Map.Entry<String, String> entry : params.entrySet()) {
					if (param.length() > 0) {
						param.append("&");
					}
					param.append(entry.getKey());
					param.append("=");
					param.append(entry.getValue());
					System.out.println(entry.getKey() + ":" + entry.getValue());
				}
				System.out.println("param:" + param.toString());
				out.write(param.toString());
			}
			// flush输出流的缓冲
			out.flush();
			// 定义BufferedReader输入流来读取URL的响应
			in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line;
			while ((line = in.readLine()) != null) {
				result.append(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 使用finally块来关闭输出流、输入流
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result.toString();
	}

	/**
	 * 预约快递员上门 于光良
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/appointmentExpress1.hn")
	public void appointmentExpress1(HttpServletRequest request, HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入预约快递员上门方法start");
			String area = request.getParameter("area") == null ? "" : request.getParameter("area").trim();
			String address = request.getParameter("address") == null ? "" : request.getParameter("address").trim();
			String sample_id = request.getParameter("sample_id") == null ? ""
					: request.getParameter("sample_id").trim();
			String time = request.getParameter("time") == null ? "" : request.getParameter("time").trim();

			if (StringUtils.isBlank(area) || StringUtils.isBlank(address) || StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}
			String province = area.split(" ")[0];
			String city = area.split(" ")[1];
			String county = area.split(" ")[2];
			String start_time = null;
			String end_time = null;
			String day = time.split(" ")[0];
			String hour = time.split(" ")[1];
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar cal = Calendar.getInstance();
			if (day.equals("今天")) {
				day = sdf.format(cal.getTime());
				start_time = day + " " + hour.split("-")[0] + ":00";
				end_time = day + " " + hour.split("-")[1] + ":00";
			}
			if (day.equals("明天")) {
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + 1);// 一天
				day = sdf.format(cal.getTime());
				start_time = day + " " + hour.split("-")[0] + ":00";
				end_time = day + " " + hour.split("-")[1] + ":00";
			}
			if (day.equals("后天")) {
				cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + 2);// 一天
				day = sdf.format(cal.getTime());
				start_time = day + " " + hour.split("-")[0] + ":00";
				end_time = day + " " + hour.split("-")[1] + ":00";
			}
			logger.info("appointmentExpress province:" + province);
			logger.info("appointmentExpress city:" + city);
			logger.info("appointmentExpress county:" + county);
			logger.info("appointmentExpress address:" + address);
			logger.info("appointmentExpress sample_id:" + sample_id);
			logger.info("appointmentExpress time:" + time);

			String sql = "SELECT sample_status,phone,name,send_number FROM care_userInfo where sample_id = ? ";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(sample_id);
			List<HashMap<String, Object>> list = utilMethodService.getObjectList(sql, params, DB);
			if (list.size() == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "未查询到样本信息，预约失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			String sample_status = list.get(0).get("sample_status").toString();
			String phone = list.get(0).get("phone").toString();
			String name = list.get(0).get("name").toString();
			String send_number = list.get(0).get("send_number").toString();
			/*
			 * 样本状态 0已配货 1已发货（已邮寄给客户）2已绑定 3已回寄 4已接收 5已报告
			 */
			if (StringUtils.isNotBlank(send_number)) {
				jsonback.put("success", false);
				jsonback.put("msg", "样本已邮寄，运单号：" + send_number);
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}
			if ("3".equals(sample_status) || "4".equals(sample_status) || "5".equals(sample_status)) {
				jsonback.put("success", false);
				jsonback.put("msg", "样本已接收，无需邮寄");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}
			if ("0".equals(sample_status) || "1".equals(sample_status)) {
				jsonback.put("success", false);
				jsonback.put("msg", "样本未绑定，无法邮寄");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			// 获取收件地址信息
			String sqlForReceipt = "SELECT province,city,area,address,company,name,phone FROM coyote_receipt_info LIMIT 1";
			ArrayList<Object> paramsForReceipt = new ArrayList<Object>();
			List<HashMap<String, Object>> listForReceipt = utilMethodService.getObjectList(sqlForReceipt,
					paramsForReceipt, DB);
			if (listForReceipt.size() == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "收件信息获取失败");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			JSONObject receiptInfo = JSONObject.fromObject(listForReceipt.get(0));

			// 订单序列号
			String serialNumber = createSerialNumber();
			JSONObject json = new JSONObject();
			json.put("OrderCode", serialNumber);
			json.put("Callback", sample_id);
			json.put("ShipperCode", ShipperCodeSF);
			json.put("PayType", 3);
			json.put("ExpType", 1);
			json.put("MonthCode", MonthCode);

			JSONObject Sender = new JSONObject();
			Sender.put("Name", name);
			Sender.put("Mobile", phone);
			Sender.put("ProvinceName", province);
			Sender.put("CityName", city);
			Sender.put("ExpAreaName", county);
			Sender.put("Address", address);
			json.put("Sender", Sender);

			JSONObject Receiver = new JSONObject();
			Receiver.put("Company", receiptInfo.getString("company"));
			Receiver.put("Name", receiptInfo.getString("name"));
			Receiver.put("Mobile", receiptInfo.getString("phone"));
			Receiver.put("ProvinceName", receiptInfo.getString("province"));
			Receiver.put("CityName", receiptInfo.getString("city"));
			Receiver.put("ExpAreaName", receiptInfo.getString("area"));
			Receiver.put("Address", receiptInfo.getString("address"));
			json.put("Receiver", Receiver);

			json.put("StartDate", start_time);
			json.put("EndDate", end_time);
			json.put("Quantity", 1);
			json.put("IsNotice", 0);
			json.put("IsSendMessage", 0);
			json.put("IsSubscribe", 1);
			json.put("IsReturnPrintTemplate", 1);
			json.put("DeliveryMethod", 2);
			json.put("CurrencyCode", "CNY");

			JSONArray Commodity = new JSONArray();
			JSONObject GoodsName = new JSONObject();
			GoodsName.put("GoodsName", "采样器");
			Commodity.add(GoodsName);
			json.put("Commodity", Commodity);

			json.put("Remark", "小心轻放");
			String requestData = json.toString();

			Map<String, String> express_params = new HashMap<String, String>();
			express_params.put("RequestData", urlEncoder(requestData, "UTF-8"));
			express_params.put("EBusinessID", Configure.getKDNEBusinessID());
			express_params.put("RequestType", "1007");
			String dataSign = encrypt(requestData, Configure.getKDNAppKey(), "UTF-8");
			express_params.put("DataSign", urlEncoder(dataSign, "UTF-8"));
			express_params.put("DataType", "2");

			JSONObject result = JSONObject.fromObject(sendPost(Configure.getKDNReqURL(), express_params));
			logger.info("快递鸟电子面单返回结果:" + result.toString());

			if (!result.getBoolean("Success")) {
				jsonback.put("success", false);
				jsonback.put("msg", result.getString("Reason"));
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			JSONObject jsonForOrder = result.getJSONObject("Order");
			String LogisticCode = jsonForOrder.getString("LogisticCode");
			logger.info("电子面单获取的快递单号：" + LogisticCode);

			String sqlForUpdate = "UPDATE care_userInfo SET send_number = ?  WHERE sample_id = ?";
			ArrayList<Object> paramsForUpdate = new ArrayList<Object>();
			paramsForUpdate.add(LogisticCode);
			paramsForUpdate.add(sample_id);
			JSONObject resultForUpdate = JSONObject
					.fromObject(utilMethodService.updateObjectInfo(sqlForUpdate, DB, paramsForUpdate));
			if (!resultForUpdate.getBoolean("success")) {
				jsonback.put("success", false);
				jsonback.put("msg", "预约成功，但物流号填充失败，请手动填充");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
				return;
			}

			jsonback.put("success", true);
			jsonback.put("msg", "预约成功");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，预约快递员上门方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}
	
	/**
	 * 查询物流信息
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/getSFTrackingSingleInfo_Phone.hn")
	public void getSFTrackingSingleInfo_Phone(HttpServletRequest request, HttpServletResponse response) throws Exception{
		String tracking_number = request.getParameter("order_serial")==null?"":request.getParameter("order_serial").trim();
		String lang = request.getParameter("lang") == null ? "0" : request.getParameter("lang").trim();
		String last4number = request.getParameter("last4number") == null ? "" : request.getParameter("last4number").trim();
		
		System.out.println("=======列出单个运单号物流信息=======");
		System.out.println("=======物流单号=======" + tracking_number);
		System.out.println("=======返回语言类型=======" + lang);
		System.out.println("=======收货人手机号后四位=======" + last4number);
		
		EspServiceCode testService = EspServiceCode.EXP_RECE_SEARCH_ROUTES;
		Map<String, String> params = new HashMap<String, String>();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        JSONObject js = new JSONObject();
        JSONArray a = new JSONArray();
        a.add(tracking_number);
        //language 0：中文 1：英文 2：繁体
        js.put("language", lang);
        js.put("trackingType", "3");
        js.put("trackingNumber", a.toString());
        js.put("methodType", "1");
        js.put("checkPhoneNo", last4number);
        
        //将业务报文+时间戳+校验码组合成需加密的字符串(注意顺序)
		String toVerifyText = js.toString()+timeStamp+CHECK_WORD;
		//因业务报文中可能包含加号、空格等特殊字符，需要urlEnCode处理
		toVerifyText = URLEncoder.encode(toVerifyText,"UTF-8");    
		//进行Md5加密        
		MessageDigest  md5 = MessageDigest.getInstance("MD5");
		md5.update(toVerifyText.getBytes("UTF-8"));
		byte[] md = md5.digest();
		//通过BASE64生成数字签名
		String msgDigest = new String(new BASE64Encoder().encode(md));
		System.out.println(msgDigest);
        
        params.put("partnerID", CLIENT_CODE);  // 顾客编码 ，对应丰桥上获取的clientCode
        params.put("requestID", UUID.randomUUID().toString().replace("-", ""));
        params.put("serviceCode",testService.getCode());// 接口服务码
        params.put("timestamp", timeStamp);    
        params.put("msgData", js.toString());
        params.put("msgDigest", msgDigest);
        
		String result = HttpClientUtil.post(CALL_URL_PROD, params);
		JSONObject returnJson = new JSONObject();
		
		if("".equals(result)){
			returnJson.put("success", false);
			returnJson.put("msg", "获取信息失败");
			System.out.println("return======="+returnJson);
			
			logger.info("Care商城后台管理系统，查询物流信息方法结束end："+returnJson);
			ResponseUtils.setToHttp(response, returnJson.toString().trim());
			return;
		}
		
		JSONObject json = JSONObject.fromObject(result);
		System.out.println("!============"+json);
		String code = json.getString("apiResultCode");
		//String message = json.getJSONObject("meta").getString("message");
		System.out.println("code============"+code);
		if(code.equals("A1000")){
			JSONObject apiResultDataObject = JSONObject.fromObject(json.getString("apiResultData"));
			JSONObject msgDataObject = JSONObject.fromObject(apiResultDataObject).getJSONObject("msgData");
			JSONArray routeRespsjsonArray = JSONObject.fromObject(msgDataObject).getJSONArray("routeResps");
			JSONObject routeRespsObject = routeRespsjsonArray.getJSONObject(0);
			JSONArray routesjsonArray = JSONObject.fromObject(routeRespsObject).getJSONArray("routes");
			logger.info("routesjsonArray:"+routesjsonArray);
			if(routesjsonArray.isEmpty()){
				returnJson.put("success", false);
				returnJson.put("msg", "暂无物流信息");
				System.out.println("return======="+returnJson);
				
				logger.info("Care商城后台管理系统，查询物流信息方法结束end："+returnJson);
				ResponseUtils.setToHttp(response, returnJson.toString().trim());
				return;
			}
			
			JSONArray r = new JSONArray();
			Map<String,Object> map = new HashMap<String,Object>();
			for (int i = routesjsonArray.size()-1; i > -1; i--) {
				//将节点时间放入Date
				String opCode = routesjsonArray.getJSONObject(i).getString("opCode");
				String acceptTime = routesjsonArray.getJSONObject(i).getString("acceptTime");
				String StatusDescription = routesjsonArray.getJSONObject(i).getString("remark");
				String Details = routesjsonArray.getJSONObject(i).getString("acceptAddress");
				if(!opCode.equals("8000")){
					if("0".equals(lang)){
						StatusDescription = routesjsonArray.getJSONObject(i).getString("remark");
						Details = routesjsonArray.getJSONObject(i).getString("acceptAddress");
						map.put("Date", acceptTime);
						map.put("StatusDescription", StatusDescription);
						map.put("Details", Details);
						r.add(map);
					} else {
						StatusDescription = TransApi.trans(routesjsonArray.getJSONObject(i).getString("remark"), "en");
						Details = TransApi.trans(routesjsonArray.getJSONObject(i).getString("acceptAddress"), "en");
						map.put("Date", acceptTime);
						map.put("StatusDescription", StatusDescription);
						map.put("Details", Details);
						r.add(map);
					}
				}
			}
			returnJson.put("success", true);
			returnJson.put("msg", "统一接入平台校验成功");
			returnJson.put("trackinfo", r);
		}
		else{
			returnJson.put("success", false);
			returnJson.put("msg", "统一接入平台校验失败");
		}
		System.out.println("return======="+returnJson);
		logger.info("Care商城后台管理系统，查询物流信息方法结束end："+returnJson);
		ResponseUtils.setToHttp(response, returnJson.toString().trim());
		return;
    }
	
	/**
	 * 查询存在 “已预约快递但未取件状态”的快递
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/IsPostTogether.hn")
	public void IsPostTogether(HttpServletRequest request, HttpServletResponse response) throws Exception{
		String send_phone = request.getParameter("send_phone")==null?"":request.getParameter("send_phone").trim();
		
		System.out.println("查询存在 “已预约快递但未取件状态”的快递 =======手机号=======" + send_phone);
		
		JSONObject jsonback = new JSONObject();

		String sql = "SELECT * FROM care_userInfo WHERE phone=? and send_number is not null and sample_status=? ORDER BY sampling_time DESC";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(send_phone);
		params.add(2);
		JSONObject res = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		int sum = res.getInt("total");
		logger.info("sum===="+sum);
		if (sum > 0) {//
			jsonback.put("success", true);
			jsonback.put("msg", "存在已预约快递但未取件状态的快递");
			jsonback.put("send_number", res.getJSONArray("root").getJSONObject(0).getString("send_number"));
			ResponseUtils.setToHttp(response, jsonback.toString());
		} else {
			jsonback.put("success", false);
			jsonback.put("msg", "不存在已预约快递但未取件状态的快递");
			ResponseUtils.setToHttp(response, jsonback.toString());
		}
		logger.info("查询存在 “已预约快递但未取件状态”的快递方法end"+jsonback.toString());
    }
	
	/**
	 * 选择一起邮寄时更新信息
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("/UpdateSameBatch.hn")
	public void UpdateSameBatch(HttpServletRequest request, HttpServletResponse response) throws Exception{
		String send_number = request.getParameter("send_number")==null?"":request.getParameter("send_number").trim();
		String sample_id = request.getParameter("sample_id")==null?"":request.getParameter("sample_id").trim();
		
		System.out.println("send_number=====" + send_number);
		System.out.println("sample_id=======" + sample_id);
		
		JSONObject jsonback = new JSONObject();

		String sqlForUpdate = "UPDATE care_userInfo SET send_number = ? WHERE sample_id = ?";
		ArrayList<Object> paramsForUpdate = new ArrayList<Object>();
		paramsForUpdate.add(send_number);
		paramsForUpdate.add(sample_id);
		JSONObject resultForUpdate = JSONObject
				.fromObject(utilMethodService.updateObjectInfo(sqlForUpdate, DB, paramsForUpdate));
		if (!resultForUpdate.getBoolean("success")) {
			jsonback.put("success", false);
			jsonback.put("msg", "物流号填充失败，请手动填充");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("选择一起邮寄时更新信息方法end：" + jsonback.toString());
			return;
		}

		jsonback.put("success", true);
		jsonback.put("msg", "合并成功");
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("选择一起邮寄时更新信息方法end：" + jsonback.toString());
    }
}
