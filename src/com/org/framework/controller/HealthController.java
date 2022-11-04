package com.org.framework.controller;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.org.framework.service.UtilMethodService;
import com.org.framework.util.ResponseUtils;

@Controller
@RequestMapping("/apihealth/")
public class HealthController {

	private static final String DB = "sys";

	@Autowired
	public UtilMethodService utilMethodService;
	private static Logger logger = Logger.getLogger(CommonApiController.class);

	
	
	/**
	 * 获取检测结果
	 * @param request
	 * @param response
	 */
	@RequestMapping("getSample_result.hn")
	public void getSample_result(HttpServletRequest request,HttpServletResponse response){
		logger.info("获取检测结果信息方法start");
		String sample_id = request.getParameter("sample_id")==null?"": request.getParameter("sample_id").trim();
		//String phone = request.getParameter("phone")==null?"": request.getParameter("phone").trim();
		JSONObject jsonback = new JSONObject();
		if(StringUtils.isBlank(sample_id)){
			jsonback.put("success", false);
			jsonback.put("msg", "必要参数样本号缺失");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		/*if(StringUtils.isBlank(phone)){
			jsonback.put("success", false);
			jsonback.put("msg", "必要参数手机号缺失");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}*/
		String sql = "SELECT sample_id,`name`,age,phone,snpName,snpResult FROM care_userInfo WHERE sample_id=? "
				+ " ORDER BY create_time DESC";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(sample_id);
		JSONObject js = new JSONObject();
		try {
			js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "获取信息失败");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if(js.getInt("total")==0){
			jsonback.put("success", false);
			jsonback.put("msg", "该条码没有人员信息和检测记录");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		JSONArray res = js.getJSONArray("root");
		js = JSONObject.fromObject(res.get(0));
		jsonback.put("success", true);
		jsonback.put("msg", "查询成功");
		jsonback.put("result", js);
		ResponseUtils.setToHttp(response, jsonback.toString());
		
		logger.info("获取检测结果信息方法end");
	}
	
	/**
     * 获取抑癌检测结果，并计算抑癌能力数据
     * @param request
     * @param response
     */
    @RequestMapping("getCancersuppressionResinfo.hn")
    public void getCancersuppressionResinfo(HttpServletRequest request,HttpServletResponse response) {
        logger.info("获取抑癌检测结果信息方法start");
        double[] OR1GG = {1,1,1,1,1,1,1,1,1};
        double[] OR1CG = {1.38,1.51,1,1.16,1,1.15,1.02,1.08,1.13};
        double[] OR1CC = {2.28,1.69,1.83,1.17,1.06,1.45,1.16,1.46,1.63};
        double[] OR2AA = {1,1,1,1,1,1,1,1,1};
        double[] OR2AG = {1.19,0.85,1.42,1.28,1.4,1.27,0.7,1.08,1.044};
        double[] OR2CG = {2.85,2.59,1.81,1.47,2.01,1.16,1.45,2.65,1.137};
        String sample_id = request.getParameter("sample_id")==null?"": request.getParameter("sample_id").trim();
        JSONObject jsonback = new JSONObject();
		
        if(StringUtils.isBlank(sample_id)){
            jsonback.put("success", false);
            jsonback.put("msg", "必要参数样本号缺失");
            ResponseUtils.setToHttp(response, jsonback.toString());
            return;
        }
        
        String sql = "SELECT sample_id,`name`,age,phone,snpName,snpResult FROM care_userInfo WHERE sample_id=?  "
                + " ORDER BY create_time DESC";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(sample_id);
      
        JSONObject js = new JSONObject();
        try {
            js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
        } catch (Exception e) {
            e.printStackTrace();
            jsonback.put("success", false);
            jsonback.put("msg", "获取信息失败");
            ResponseUtils.setToHttp(response, jsonback.toString());
            return;
        }
        if(js.getInt("total")==0){
            jsonback.put("success", false);
            jsonback.put("msg", "该条码没有人员信息和检测记录");
            ResponseUtils.setToHttp(response, jsonback.toString());
            return;
        }
        JSONArray res = js.getJSONArray("root");
        js = JSONObject.fromObject(res.get(0));
        jsonback.put("success", true);
        jsonback.put("msg", "查询成功");
        jsonback.put("result", js);
        String snpName = js.getString("snpName");
        String snpResult  = js.getString("snpResult");
        String[] snpResults = snpResult.split(";");
        String OR1 = snpResults[0];
        String OR2 = snpResults[1];
        
        double[] OR1res={};
        double[] OR2res={};
        if(OR1.equals("GG")){
            OR1res = OR1GG;
        }
        if(OR1.equals("CG")){
            OR1res = OR1CG;
        }
        if(OR1.equals("CC")){
            OR1res = OR1CC;
        }
        if(OR2.equals("AA")){
            OR2res = OR2AA;
        }
        if(OR2.equals("AG")){
            OR2res = OR2AG;
        }
        if(OR2.equals("GG")){
            OR2res = OR2CG;
        }
        
        //计算结果
        double lung;//肺癌
        double stomach;//胃癌
        double esophageal;//食道癌
        double Colorectal;//结直肠癌
        double liver;//肝癌
        double carcinoma;//子宫内膜癌
        double thyroid;//甲状腺癌
        double oophoroma;//卵巢癌
        double mammary;//乳腺癌
        
        lung = 100/(Math.pow(OR1res[0]*OR2res[0], 2));
        logger.info("lung值"+lung);
        if(lung >= 100){
        	lung = 100;
        }
        
        stomach = 100/(Math.pow(OR1res[1]*OR2res[1], 2));
        if(stomach >= 100){
        	stomach = 100;
        }        
    
        esophageal = 100/(Math.pow(OR1res[2]*OR2res[2], 2));
        if(esophageal >= 100){
        	esophageal = 100;
        }    
    
        Colorectal = 100/(Math.pow(OR1res[3]*OR2res[3], 2));
        if(Colorectal >= 100){
        	Colorectal = 100;
        }    
    
        liver = 100/(Math.pow(OR1res[4]*OR2res[4], 2));
        if(liver >= 100){
        	liver = 100;
        }    
    
        carcinoma = 100/(Math.pow(OR1res[5]*OR2res[5], 2));
        if(carcinoma >= 100){
        	carcinoma = 100;
        }   
    
        thyroid = 100/(Math.pow(OR1res[6]*OR2res[6], 2));
        if(thyroid >= 100){
        	thyroid = 100;
        }    
    
        oophoroma = 100/(Math.pow(OR1res[7]*OR2res[7], 2));
        if(oophoroma >= 100){
        	oophoroma = 100;
        }   
    
        mammary = 100/(Math.pow(OR1res[8]*OR2res[8], 2));
        if(mammary >= 100){
        	mammary = 100;
        }        
            
        
        
        jsonback.put("lung", String.format("%.0f", lung));
        jsonback.put("stomach", String.format("%.0f", stomach));
        jsonback.put("esophageal",String.format("%.0f", esophageal));
        jsonback.put("Colorectal",String.format("%.0f", Colorectal));
        jsonback.put("carcinoma", String.format("%.0f", carcinoma));
        jsonback.put("thyroid",String.format("%.0f", thyroid) );
        jsonback.put("oophoroma",String.format("%.0f", oophoroma));
        jsonback.put("mammary",String.format("%.0f", mammary) );
        jsonback.put("liver",String.format("%.0f", liver) );
        System.out.println(jsonback.toString());
        
        ResponseUtils.setToHttp(response, jsonback.toString());
        logger.info("获取检测结果信息方法end"+jsonback.toString());
    }	
    
    
    /**
     * 肖明瑞大健康首页获取结果接口方法
     */
    
    
    /**
	 * 查看报告
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("getcareindexReportInfo.hn")
	public void getcareindexReportInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("查询我的报告方法start");
		String searchinfo = request.getParameter("searchinfo") == null ? "" : request.getParameter("searchinfo").trim();
		JSONObject json = JSONObject.fromObject(request.getSession().getAttribute("phone"));
		System.out.println("获取的session内容：" + json);
		String phone = json.getString("phone");
		logger.info("查询报告信息:"+searchinfo);
		logger.info("手机号:"+phone);
		
		
		JSONObject msg_json = new JSONObject();
		msg_json.put("phone", phone);
		msg_json.put("createTime", System.currentTimeMillis());// 时间戳
		request.getSession().setAttribute("phone", msg_json);// 存到session
		

		JSONObject jsonback = new JSONObject();



		String sql = "select a.sample_id,a.name,b.commodity_name,date_format(a.report_time,'%Y-%m-%d') as report_time,sample_status,"
				+ "date_format(a.create_time,'%Y-%m-%d') as create_time,reportUrl,send_number from care_userInfo a,wx_commodity_info b "
				+ "where a.test_items=b.commodity_id and a.phone=? and a.sample_status='5'";
		logger.info("searchinfo:" + searchinfo);
	

		StringBuffer sb = new StringBuffer(sql);
		ArrayList<Object> params = new ArrayList<Object>();
		JSONObject jsonObject = new JSONObject();

		if (StringUtils.isBlank(phone)) {
			jsonback.put("success", false);
			jsonback.put("msg", "获取报告失败，请尝试切换账号");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("查询我的报告方法end");
			return;
		}
		params.add(phone);
		if (StringUtils.isNotBlank(searchinfo)) {
			searchinfo = "%" + searchinfo + "%";
			params.add(searchinfo);
			params.add(searchinfo);
			sb.append(" and (a.name like ? or a.sample_id like ?)");
		}
		sb.append(" order by a.create_time desc");
		sql = sb.toString();
		try {
			jsonObject = utilMethodService.showObjectInfo(sql, params, DB, 0, Integer.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "网络问题，请稍后重试");
			jsonback.put("result", "");
		}

		jsonback.put("success", true);
		jsonback.put("phone", phone.substring(0, 3) + "****" + phone.substring(7, 11));
		jsonback.put("msg", "查询成功");
		jsonback.put("result", jsonObject.getJSONArray("root"));
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("查询我的报告方法end" + jsonback.toString());
	}

    
    /**
     * 获取抑癌检测结果，并计算抑癌能力数据
     * @param request
     * @param response
     */
    @RequestMapping("getCancersuppressionResinfoforindex.hn")
    public void getCancersuppressionResinfoforindex(HttpServletRequest request,HttpServletResponse response) {
        logger.info("大健康首页获取抑癌检测结果信息方法start");
        double[] OR1GG = {1,1,1,1,1,1,1,1,1};
        double[] OR1CG = {1.38,1.51,1,1.16,1,1.15,1.02,1.08,1.13};
        double[] OR1CC = {2.28,1.69,1.83,1.17,1.06,1.45,1.16,1.46,1.63};
        double[] OR2AA = {1,1,1,1,1,1,1,1,1};
        double[] OR2AG = {1.19,0.85,1.42,1.28,1.4,1.27,0.7,1.08,1.044};
        double[] OR2CG = {2.85,2.59,1.81,1.47,2.01,1.16,1.45,2.65,1.137};
        String sample_id = request.getParameter("sample_id")==null?"": request.getParameter("sample_id").trim();
        JSONObject jsonback = new JSONObject();
		JSONObject json = JSONObject.fromObject(request.getSession().getAttribute("phone"));
		System.out.println("获取的session内容：" + json);
        logger.info("session是否为空"+json.isEmpty());
		if (json.isEmpty()) {
			
			jsonback.put("success", false);
			jsonback.put("msg", "请您重新登录");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("大健康首页获取检测结果信息方法end"+jsonback.toString());
			return;
		}
		String phone = json.getString("phone");
		
        
		if (json.isEmpty()) {
			jsonback.put("success", false);
			jsonback.put("msg", "请您重新登录");
			
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("大健康首页获取检测结果信息方法end"+jsonback.toString());
			return;
		}
        if(StringUtils.isBlank(sample_id)){
            jsonback.put("success", false);
            jsonback.put("msg", "必要参数样本号缺失");
            ResponseUtils.setToHttp(response, jsonback.toString());
            return;
        }
        
        String sql = "SELECT sample_id,`name`,age,phone,snpName,snpResult FROM care_userInfo WHERE sample_id=? and phone = ? "
                + " ORDER BY create_time DESC";
        ArrayList<Object> params = new ArrayList<Object>();
        params.add(sample_id);
        params.add(phone);
        JSONObject js = new JSONObject();
        try {
            js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
        } catch (Exception e) {
            e.printStackTrace();
            jsonback.put("success", false);
            jsonback.put("msg", "获取信息失败");
            ResponseUtils.setToHttp(response, jsonback.toString());
            return;
        }
        if(js.getInt("total")==0){
            jsonback.put("success", false);
            jsonback.put("msg", "该条码没有人员信息和检测记录");
            ResponseUtils.setToHttp(response, jsonback.toString());
            return;
        }
        JSONArray res = js.getJSONArray("root");
        js = JSONObject.fromObject(res.get(0));
        jsonback.put("success", true);
        jsonback.put("msg", "查询成功");
        jsonback.put("result", js);
        String snpName = js.getString("snpName");
        String snpResult  = js.getString("snpResult");
        String[] snpResults = snpResult.split(";");
        String OR1 = snpResults[0];
        String OR2 = snpResults[1];
        
        double[] OR1res={};
        double[] OR2res={};
        if(OR1.equals("GG")){
            OR1res = OR1GG;
        }
        if(OR1.equals("CG")){
            OR1res = OR1CG;
        }
        if(OR1.equals("CC")){
            OR1res = OR1CC;
        }
        if(OR2.equals("AA")){
            OR2res = OR2AA;
        }
        if(OR2.equals("AG")){
            OR2res = OR2AG;
        }
        if(OR2.equals("GG")){
            OR2res = OR2CG;
        }
        
        //计算结果
        double lung;//肺癌
        double stomach;//胃癌
        double esophageal;//食道癌
        double Colorectal;//结直肠癌
        double liver;//肝癌
        double carcinoma;//子宫内膜癌
        double thyroid;//甲状腺癌
        double oophoroma;//卵巢癌
        double mammary;//乳腺癌
        
        lung = 100/(Math.pow(OR1res[0]*OR2res[0], 2));
        logger.info("lung值"+lung);
        if(lung >= 100){
        	lung = 100;
        }
        
        stomach = 100/(Math.pow(OR1res[1]*OR2res[1], 2));
        if(stomach >= 100){
        	stomach = 100;
        }        
    
        esophageal = 100/(Math.pow(OR1res[2]*OR2res[2], 2));
        if(esophageal >= 100){
        	esophageal = 100;
        }    
    
        Colorectal = 100/(Math.pow(OR1res[3]*OR2res[3], 2));
        if(Colorectal >= 100){
        	Colorectal = 100;
        }    
    
        liver = 100/(Math.pow(OR1res[4]*OR2res[4], 2));
        if(liver >= 100){
        	liver = 100;
        }    
    
        carcinoma = 100/(Math.pow(OR1res[5]*OR2res[5], 2));
        if(carcinoma >= 100){
        	carcinoma = 100;
        }   
    
        thyroid = 100/(Math.pow(OR1res[6]*OR2res[6], 2));
        if(thyroid >= 100){
        	thyroid = 100;
        }    
    
        oophoroma = 100/(Math.pow(OR1res[7]*OR2res[7], 2));
        if(oophoroma >= 100){
        	oophoroma = 100;
        }   
    
        mammary = 100/(Math.pow(OR1res[8]*OR2res[8], 2));
        if(mammary >= 100){
        	mammary = 100;
        }        
        
        
        jsonback.put("lung", String.format("%.0f", lung));
        jsonback.put("stomach", String.format("%.0f", stomach));
        jsonback.put("esophageal",String.format("%.0f", esophageal));
        jsonback.put("Colorectal",String.format("%.0f", Colorectal));
        jsonback.put("carcinoma", String.format("%.0f", carcinoma));
        jsonback.put("thyroid",String.format("%.0f", thyroid) );
        jsonback.put("oophoroma",String.format("%.0f", oophoroma));
        jsonback.put("mammary",String.format("%.0f", mammary) );
        jsonback.put("liver",String.format("%.0f", liver) );
        System.out.println(jsonback.toString());
        
        ResponseUtils.setToHttp(response, jsonback.toString());
        logger.info("大健康首页获取检测结果信息方法end"+jsonback.toString());
    }	
    
    /**
     * 获取检测结果
     * @param request
     * @param response
     */
    @RequestMapping("getSampleresult.hn")
    public void getSampleresult(HttpServletRequest request,HttpServletResponse response){  logger.info("获取检测结果信息方法start");
    String sample_id = request.getParameter("sample_id")==null?"": request.getParameter("sample_id").trim();
    JSONObject jsonback = new JSONObject();
    JSONObject json = JSONObject.fromObject(request.getSession().getAttribute("phone"));
    System.out.println("获取的session内容：" + json);
    logger.info("session是否为空"+json.isEmpty());
    if (json.isEmpty()) {
        
        jsonback.put("success", false);
        jsonback.put("msg", "请您重新登录");
        ResponseUtils.setToHttp(response, jsonback.toString());
        logger.info("获取检测结果信息方法end"+jsonback.toString());
        return;
    }
    String phone = json.getString("phone");
    if(StringUtils.isBlank(sample_id)){
        jsonback.put("success", false);
        jsonback.put("msg", "必要参数样本号缺失");
        ResponseUtils.setToHttp(response, jsonback.toString());
        return;
    }
    if(StringUtils.isBlank(phone)){
        jsonback.put("success", false);
        jsonback.put("msg", "必要参数手机号缺失");
        ResponseUtils.setToHttp(response, jsonback.toString());
        return;
    }
    String sql = "SELECT sample_id,`name`,age,phone,snpName,snpResult,reportUrl FROM care_userInfo WHERE sample_id=? and phone=? "
            + "ORDER BY create_time DESC";
    ArrayList<Object> params = new ArrayList<Object>();
    params.add(sample_id);
    params.add(phone);
    JSONObject js = new JSONObject();
    try {
        js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
    } catch (Exception e) {
        e.printStackTrace();
        jsonback.put("success", false);
        jsonback.put("msg", "获取信息失败");
        ResponseUtils.setToHttp(response, jsonback.toString());
        return;
    }
    if(js.getInt("total")==0){
        jsonback.put("success", false);
        jsonback.put("msg", "该条码没有人员信息和检测记录");
        ResponseUtils.setToHttp(response, jsonback.toString());
        return;
    }
    JSONArray res = js.getJSONArray("root");
    js = JSONObject.fromObject(res.get(0));
    jsonback.put("success", true);
    jsonback.put("msg", "查询成功");
    jsonback.put("result", js);
    ResponseUtils.setToHttp(response, jsonback.toString());
    
    logger.info("获取检测结果信息方法end"+jsonback.toString());
    }
    
	
	
	/**
	 * 查看报告
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("getReportInfo.hn")
	public void getReportInfo(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("查询我的报告方法start");
		String searchinfo = request.getParameter("searchinfo") == null ? "" : request.getParameter("searchinfo").trim();
		String openid = request.getParameter("openid") == null ? "" : request.getParameter("openid").trim();
		/*
		 * int start = request.getParameter("start") == null ? 0 :
		 * Integer.parseInt(request.getParameter("start").trim()); int limit =
		 * request.getParameter("limit") == null ? 10 :
		 * Integer.parseInt(request.getParameter("limit").trim());
		 */

		JSONObject jsonback = new JSONObject();
		String sqlforphone = "select user_phone from wx_phone_for_report where openid = ?";
		ArrayList<Object> paramsforphone = new ArrayList<Object>();
		paramsforphone.add(openid);
		JSONObject jsonObjectforphone = utilMethodService.showObjectInfo(sqlforphone, paramsforphone, DB, 0, 1);
		if (jsonObjectforphone.getInt("total") == 0) {
			jsonback.put("success", false);
			jsonback.put("msg", "获取报告失败，请尝试切换账号");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("查询我的报告方法end");
			return;
		}
		String phone = jsonObjectforphone.getJSONArray("root").getJSONObject(0).getString("user_phone");

		String sql = "select a.sample_id,a.name,b.commodity_name,date_format(a.report_time,'%Y-%m-%d') as report_time,sample_status,"
				+ "date_format(a.create_time,'%Y-%m-%d') as create_time,reportUrl,send_number from care_userInfo a,wx_commodity_info b "
				+ "where a.test_items=b.commodity_id and a.phone=?";
		logger.info("searchinfo:" + searchinfo);
		/*
		 * logger.info("start:"+start); logger.info("limit:"+limit);
		 */

		StringBuffer sb = new StringBuffer(sql);
		ArrayList<Object> params = new ArrayList<Object>();
		JSONObject jsonObject = new JSONObject();

		if (StringUtils.isBlank(phone)) {
			jsonback.put("success", false);
			jsonback.put("msg", "获取报告失败，请尝试切换账号");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("查询我的报告方法end");
			return;
		}
		params.add(phone);
		if (StringUtils.isNotBlank(searchinfo)) {
			searchinfo = "%" + searchinfo + "%";
			params.add(searchinfo);
			params.add(searchinfo);
			sb.append(" and (a.name like ? or a.sample_id like ?)");
		}
		sb.append(" order by a.create_time desc");
		sql = sb.toString();
		try {
			jsonObject = utilMethodService.showObjectInfo(sql, params, DB, 0, Integer.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "网络问题，请稍后重试");
			jsonback.put("result", "");
		}

		jsonback.put("success", true);
		jsonback.put("phone", phone.substring(0, 3) + "****" + phone.substring(7, 11));
		jsonback.put("msg", "查询成功");
		jsonback.put("result", jsonObject.getJSONArray("root"));
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("查询我的报告方法end" + jsonback.toString());
	}

	/**
	 * 判断是否含有中文
	 * 
	 * @param checkStr
	 * @return
	 */
	public static boolean isCHinese(char c) {
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
				|| ub == Character.UnicodeBlock.GENERAL_PUNCTUATION // GENERAL_PUNCTUATION
																	// 判断中文的“号
				|| ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION // CJK_SYMBOLS_AND_PUNCTUATION
																			// 判断中文的。号
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS // HALFWIDTH_AND_FULLWIDTH_FORMS
																				// 判断中文的，号
		) {
			return true;
		}

		return false;
	}

	public static boolean isCHinese(String str) {
		char[] ch = str.toCharArray();
		for (char c : ch) {
			if (isCHinese(c))
				return true;
		}
		return false;
	}

	/**
	 * 查询详情页面接口
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping("getSmapledetial.hn")
	public void getSmapledetial(HttpServletRequest request, HttpServletResponse response) {
		logger.info("查询订单详情方法start");
		String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
		JSONObject jsonback = new JSONObject();
		if (StringUtils.isBlank(sample_id)) {
			jsonback.put("success", false);
			jsonback.put("msg", "参数为空");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("查询订单详情方法end");
			return;
		}
		String sql = "select a.sample_id,b.commodity_name,date_format(a.report_time,'%Y-%m-%d') as report_time,a.sample_status,"
				+ "date_format(a.create_time,'%Y-%m-%d') as create_time,a.reportUrl,"
				+ "date_format(a.receive_time,'%Y-%m-%d') as receive_time,"
				+ "date_format(a.back_send_time,'%Y-%m-%d') as back_send_time,"
				+ "date_format(date_add(a.receive_time,INTERVAL 5 DAY),'%Y-%m-%d') as estimate_time,a.send_number from care_userInfo a,"
				+ "wx_commodity_info b where a.test_items=b.commodity_id and a.sample_id=?";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(sample_id);
		JSONObject jsonObject = new JSONObject();

		try {
			jsonObject = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "网络问题，请稍后重试");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("查询订单详情方法end");
			return;
		}
		jsonback.put("success", true);
		jsonback.put("msg", "查询成功");
		jsonback.put("result", jsonObject.getJSONArray("root"));
		ResponseUtils.setToHttp(response, jsonback.toString());
		logger.info("查询订单详情方法end" + jsonback.toString());
	}

	/**
	 * 获取地区
	 * 
	 * @param request
	 * @param response
	 */
	@RequestMapping("getArea.hn")
	public void getArea(HttpServletRequest request, HttpServletResponse response) {
		logger.info("进入获取地区方法start");
		JSONObject jsonback = new JSONObject();
		JSONObject jsonObject1 = new JSONObject();
		JSONObject jsonObject2 = new JSONObject();
		JSONObject jsonObject3 = new JSONObject();
		String sql1 = "select id,name,parentid,type from area_info where type='1'";
		String sql2 = "select id,name,parentid,type from area_info where type='2'";
		String sql3 = "select id,name,parentid,type from area_info where type='3'";
		try {
			jsonObject1 = utilMethodService.showObjectInfo(sql1, null, DB, 0, Integer.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "网络问题，请稍后重试");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("进入获取地区方法end");
			return;
		}
		try {
			jsonObject2 = utilMethodService.showObjectInfo(sql2, null, DB, 0, Integer.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "网络问题，请稍后重试");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("进入获取地区方法end");
			return;
		}
		try {
			jsonObject3 = utilMethodService.showObjectInfo(sql3, null, DB, 0, Integer.MAX_VALUE);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "网络问题，请稍后重试");
			jsonback.put("result", "");
			ResponseUtils.setToHttp(response, jsonback.toString());
			logger.info("进入获取地区方法end");
			return;
		}
		JSONArray res1 = new JSONArray();
		JSONArray res2 = new JSONArray();
		JSONArray res3 = new JSONArray();
		JSONArray result = new JSONArray();
		res1 = jsonObject1.getJSONArray("root");
		res2 = jsonObject2.getJSONArray("root");
		res3 = jsonObject3.getJSONArray("root");
		JSONObject js_province = new JSONObject();
		for (int i = 0; i < res1.size(); i++) {
			JSONObject js = JSONObject.fromObject(res1.get(i));

			JSONArray res_1 = new JSONArray();
			js_province.put("id", i);
			js_province.put("value", js.getJSONObject("name"));
			for (int j = 0; j < res2.size(); j++) {
				JSONArray res_2 = new JSONArray();
				JSONObject js1 = JSONObject.fromObject(res2.get(j));
				JSONObject js_city = new JSONObject();
				js_city.put("id", j);
				js_city.put("value", js1.getJSONObject("name"));
				for (int k = 0; k < res3.size(); j++) {
					JSONObject js2 = JSONObject.fromObject(res2.get(j));
					JSONObject js_dic = new JSONObject();
					js_dic.put("id", k);
					js_dic.put("value", js2.getJSONObject("name"));
					res_2.add(js_dic);// 第三层

				}
				js_city.put("childs", res_2);
				res_1.add(js_city);
			}
			js_province.put("childs", res_1);
		}
		result.add(js_province);

		System.out.println(result);
		logger.info("进入获取地区方法end");
	}

	/**
	 * 获取报告链接
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("getReportUrl.hn")
	public void getReportUrl(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("进入获取报告链接方法start");
		String sample_id = request.getParameter("sample_id") == null ? "" : request.getParameter("sample_id").trim();
		String openid = request.getParameter("openid") == null ? "" : request.getParameter("openid").trim();
		logger.info("sample_id:" + sample_id);
		logger.info("openid:" + openid);
		JSONObject result = new JSONObject();
		String reportUrl = "";
		if (StringUtils.isBlank(sample_id)) {
			result.put("success", false);
			result.put("msg", "样本号为空");
			ResponseUtils.setToHttp(response, result.toString());
			logger.info("进入获取报告链接方法end");
			return;
		}

		ArrayList<Object> params = new ArrayList<Object>();
		String sql = "select reportUrl from care_userInfo where sample_id=?";
		params.add(sample_id);
		JSONObject js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		if (js.getInt("total") != 0) {
			JSONArray res = js.getJSONArray("root");
			js = JSONObject.fromObject(res.get(0));
			reportUrl = js.getString("reportUrl");
		}
		result.put("success", true);
		// PdfToimage(js.getString("reportUrl"));
		result.put("reportUrl", reportUrl);
		ResponseUtils.setToHttp(response, result.toString());

		logger.info("进入获取报告链接方法end");
	}

	/**
	 * 根据openid获取手机号
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("getPhone.hn")
	public void getPhone(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("进入获取当前手机号方法start");
		String openid = request.getParameter("openid") == null ? "" : request.getParameter("openid").trim();
		String sql = "select user_phone from wx_phone_for_report where openid=?";
		ArrayList<Object> params = new ArrayList<Object>();
		JSONObject result = new JSONObject();
		params.add(openid);
		JSONObject js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		if (js.getInt("total") == 1) {
			JSONArray res = js.getJSONArray("root");
			js = JSONObject.fromObject(res.get(0));
			result.put("success", true);
			result.put("result", js.getString("user_phone"));
		} else {
			result.put("success", false);
			result.put("result", "");
		}
		ResponseUtils.setToHttp(response, result.toString());
		logger.info("进入获取当前手机号方法end");
	}

	/**
	 * 下载pdf接口
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	@RequestMapping("download.hn")
	public void getpdf(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("通用报告系统，进入下载pdf接口start");
		String filename = request.getParameter("url") == null ? "" : request.getParameter("url").trim();
		logger.info("通用报告查询系统：getpdf  filename:" + filename);
		filename = new String(filename.getBytes("ISO8859-1"), "UTF-8");

		String filename2 = filename.substring(filename.lastIndexOf("/") + 1);
		URL url = new URL(filename);
		HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
		urlconn.connect();
		InputStream inputStream = urlconn.getInputStream();
		BufferedInputStream fis = new BufferedInputStream(inputStream);
		ServletOutputStream os = response.getOutputStream();
		byte b[] = new byte[2048];
		/*
		 * response.setContentType("multipart/form-data");
		 * response.setHeader("Content-Disposition",
		 * "attachment;fileName="+filename2);
		 */
		response.setHeader("Content-disposition", "inline;filename=" + filename2);
		response.setContentType("application/octet-stream exe;charset=utf-8");
		int size;
		while ((size = fis.read(b, 0, b.length)) != -1) {
			os.write(b, 0, size);
		}
		os.close();
		fis.close();
		os.flush();
		logger.info("通用报告查询系统，下载pdf接口结束end");
	}
	
	/**
	 * 展示pdf页面信息
	 * @param request
	 * @param response
	 */
	@RequestMapping("/getSampleType.hn")
	public void getSampleType(HttpServletRequest request,HttpServletResponse response) {
		JSONObject jsonback = new JSONObject();
		try {
			logger.info("care检测，进入获取拭子类型方法start");
			String sample_id = request.getParameter("sample_id") == null ? "": request.getParameter("sample_id").trim();
			logger.info("getSampleType sample_id:" + sample_id);

			if ( StringUtils.isBlank(sample_id)) {
				jsonback.put("success", false);
				jsonback.put("msg", "参数不全");
				ResponseUtils.setToHttp(response, jsonback.toString());
				logger.info("care检测，进入获取拭子类型方法end：" + jsonback.toString());
				return;
			}


			String sql = "SELECT sample_type "
					+ "FROM test_box_warehouse WHERE sample_id = ?";
			ArrayList<Object> params = new ArrayList<Object>();
			params.add(sample_id);
			JSONObject result = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
			if (result.getInt("total") == 0) {
				jsonback.put("success", false);
				jsonback.put("msg", "无");
				ResponseUtils.setToHttp(response, jsonback.toString());
			}else{
				jsonback.put("success", true);
				jsonback.put("msg", result.getJSONArray("root").getJSONObject(0).getString("sample_type"));
				ResponseUtils.setToHttp(response, jsonback.toString());
			}
			logger.info("care检测，进入获取拭子类型方法end：" + jsonback.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "系统异常，请联系技术支持");
			logger.info("care检测，进入获取拭子类型方法end：" + jsonback.toString());
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
	}
	
	public static void main(String[] args) {
	
		double[] OR1GG = {1,1,1,1,1,1,1,1,1};
		double[] OR2AA = {1,1,1,1,1,1,1,1,1};
		double[] OR1CG = {2.14,1.51,0.96,1.01,0.932,1.15,1.02,1.08,1.29};
		double[] OR1CC = {2.68,1.69,1.28,1.04,1.152,1.45,1.16,1.46,0.63};
		double[] OR2AG = {2.32,0.87,1.42,1.18,1.4,1.27,0.7,1.08,1.18};
		double[] OR2CG = {0.78,1.27,1.81,1.83,2.01,1.16,1.45,2.65,1.95};
		JSONObject jsonback = new JSONObject();
		/*String sample_id = request.getParameter("sample_id")==null?"": request.getParameter("sample_id").trim();
		JSONObject jsonback = new JSONObject();
		if(StringUtils.isBlank(sample_id)){
			jsonback.put("success", false);
			jsonback.put("msg", "必要参数样本号缺失");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		
		String sql = "SELECT sample_id,`name`,age,phone,snpName,snpResult FROM care_userInfo WHERE sample_id=? "
				+ " ORDER BY create_time DESC";
		ArrayList<Object> params = new ArrayList<Object>();
		params.add(sample_id);
		
		JSONObject js = new JSONObject();
		try {
			js = utilMethodService.showObjectInfo(sql, params, DB, 0, 1);
		} catch (Exception e) {
			e.printStackTrace();
			jsonback.put("success", false);
			jsonback.put("msg", "获取信息失败");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		if(js.getInt("total")==0){
			jsonback.put("success", false);
			jsonback.put("msg", "该条码没有人员信息和检测记录");
			ResponseUtils.setToHttp(response, jsonback.toString());
			return;
		}
		JSONArray res = js.getJSONArray("root");
		js = JSONObject.fromObject(res.get(0));
		jsonback.put("success", true);
		jsonback.put("msg", "查询成功");
		jsonback.put("result", js);
		String snpName = js.getString("snpName");
		String snpResult  = js.getString("snpResult");
		String[] snpResults = snpResult.split(";");*/
		/*String OR1 = snpResults[0];
		String OR2 = snpResults[1];*/
		String OR1 = "CG";
		String OR2 = "GG";
		
		double[] OR1res = {};
		double[] OR2res = {};
		/*if(OR1.equals("GG")){
			for (int i = 0; i < OR1GG.length; i++) {
				OR1res[i] = OR1GG[i];
			}
		}
		if(OR1.equals("CG")){
			for (int i = 0; i < OR1CG.length; i++) {
				OR1res[i] = OR1CG[i];
			}
		}
		if(OR1.equals("CC")){
			for (int i = 0; i < OR1CC.length; i++) {
				OR1res[i] = OR1CC[i];
			}
		}
		if(OR2.equals("AA")){
			for (int i = 0; i < OR2AA.length; i++) {
				OR2res[i] = OR2AA[i];
			}
		}
		if(OR2.equals("AG")){
			for (int i = 0; i < OR2AG.length; i++) {
				OR2res[i] = OR2AG[i];
			}
		}
		if(OR2.equals("CG")){
			for (int i = 0; i < OR2CG.length; i++) {
				OR2res[i] = OR2CG[i];
			}
		}*/
		if(OR1.equals("GG")){
			OR1res = OR1GG;
		}
		if(OR1.equals("CG")){
			OR1res = OR1CG;
		}
		if(OR1.equals("CC")){
			OR1res = OR1CC;
		}
		if(OR2.equals("AA")){
			OR2res = OR2AA;
		}
		if(OR2.equals("AG")){
			OR2res = OR2AG;
		}
		if(OR2.equals("GG")){
			OR2res = OR2CG;
		}
		
		//计算结果
		double lung;//肺癌
		double stomach;//胃癌
		double esophageal;//食道癌
		double Colorectal;//结直肠癌
		double liver;//肝癌
		double carcinoma;//子宫内膜癌
		double thyroid;//甲状腺癌
		double oophoroma;//卵巢癌
		double mammary;//乳腺癌
		
		if(OR1res[0]*OR2res[0]<=1){
			lung = 100/(OR1res[0]*OR2res[0]);
		}else{
			lung = 100/(Math.pow((OR1res[0]*OR2res[0]), 0.3));
		}
		if(OR1res[1]*OR2res[1]<=1){
			stomach = 100/(OR1res[1]*OR2res[1]);
		}else{
			stomach = 100/(Math.pow((OR1res[1]*OR2res[1]), 0.9));
		}
		if(OR1res[2]*OR2res[2]<=1){
			esophageal = 100/(OR1res[2]*OR2res[2]);
		}else{
			esophageal = 100/(Math.pow((OR1res[2]*OR2res[2]), 1.1));
		}
		if(OR1res[3]*OR2res[3]<=1){
			Colorectal = 100/(OR1res[3]*OR2res[3]);
		}else{
			Colorectal = 100/(Math.pow((OR1res[3]*OR2res[3]), 2));
		}
		if(OR1res[4]*OR2res[4]<=1){
			liver = 100/(OR1res[4]*OR2res[4]);
		}else{
			liver = 100/(Math.pow((OR1res[4]*OR2res[4]), 1.1));
		}
		if(OR1res[5]*OR2res[5]<=1){
			carcinoma = 100/(OR1res[5]*OR2res[5]);
		}else{
			carcinoma = 100/(Math.pow((OR1res[5]*OR2res[5]), 1.2));
		}
		if(OR1res[6]*OR2res[6]<=1){
			thyroid = 100/(OR1res[6]*OR2res[6]);
		}else{
			thyroid = 100/(Math.pow((OR1res[6]*OR2res[6]), 1.8));
		}
		if(OR1res[7]*OR2res[7]<=1){
			oophoroma = 100/(OR1res[7]*OR2res[7]);
		}else{
			oophoroma = 100/(Math.pow((OR1res[7]*OR2res[7]), 0.5));
		}
		if(OR1res[8]*OR2res[8]<=1){
			mammary = 100/(OR1res[8]*OR2res[8]);
		}else{
			mammary = 100/(Math.pow((OR1res[8]*OR2res[8]), 1.1));
		}
		String result = String.format("%.0f", lung);  //四舍五入
		System.out.println(result);
		jsonback.put("lung", String.format("%.0f", lung));
		jsonback.put("stomach", String.format("%.0f", stomach));
		jsonback.put("esophageal",String.format("%.0f", esophageal));
		jsonback.put("Colorectal",String.format("%.0f", Colorectal));
		jsonback.put("carcinoma", String.format("%.0f", carcinoma));
		jsonback.put("thyroid",String.format("%.0f", thyroid) );
		jsonback.put("oophoroma",String.format("%.0f", oophoroma));
		jsonback.put("mammary",String.format("%.0f", mammary) );
		jsonback.put("liver",String.format("%.0f", liver) );
		System.out.println(jsonback.toString());
	}
	
	
}
