package com.org.framework.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;

import com.org.framework.dao.UtilMethodDao;
import com.org.framework.service.UtilMethodService;

import net.sf.json.JSONObject;

public class UtilMethodServiceImpl implements UtilMethodService {
	@Resource
	private UtilMethodDao utilMethodDao;

	/**
	 * 删除对象信息
	 */
	public String deleteObjectInfo(String SQL, String DB, ArrayList<Object> params) throws Exception {
		int result = utilMethodDao.deleteObjectInfo(SQL, DB, params);
		JSONObject json = new JSONObject();
		if (result > 0) {
			json.put("success", true);
			json.put("msg", "成功");
			json.put("count", result);
		} else {
			json.put("success", false);
			json.put("msg", "失败");
		}
		return json.toString();
	}

	/**
	 * 查询对象信息
	 */
	public JSONObject showObjectInfo(String SQL, ArrayList<Object> params, String DB, int start, int limit) throws Exception {
		JSONObject json = utilMethodDao.showObjectInfo(SQL, params, DB, start, limit);
		return json;
	}

	public JSONObject addObjectInfo(String SQL, String DB, List<Object> fields) throws Exception {
		int result = utilMethodDao.addObjectInfo(SQL, DB, fields);
		JSONObject json = new JSONObject();
		if (result > 0) {
			json.put("success", true);
			json.put("msg", "成功");
		} else {
			json.put("success", false);
			json.put("msg", "失败");
			json.put("result", result);
		}
		return json;
	}

	public String updateObjectInfo(String SQL, String DB, List<Object> fields) throws Exception {
		int result = utilMethodDao.updateObjectInfo(SQL, DB, fields);
		JSONObject json = new JSONObject();
		if (result > 0) {
			json.put("success", true);
			json.put("msg", "成功");
		} else {
			json.put("success", false);
			json.put("msg", "失败");
			json.put("result", result);
		}
		return json.toString();
	}

	public List<HashMap<String, Object>> getObjectList(String SQL, ArrayList<Object> params, String DB) throws Exception {
		return utilMethodDao.getObjectList(SQL, params, DB);
	}

	/**
	 * 回滚对象
	 */
	public String rollbackObjectInfo(ArrayList<Object> SQL,ArrayList<ArrayList<Object>> params, String DB) throws Exception {
		int result = utilMethodDao.rollbackObjectInfo(SQL,params,DB);
		JSONObject json = new JSONObject(); 
		if(result>0){
			json.put("success", true);
			json.put("msg", "成功");
		}else{
			json.put("success", false);
			json.put("msg", "失败");
			json.put("result", result);
		}
		return json.toString();
	}

}
