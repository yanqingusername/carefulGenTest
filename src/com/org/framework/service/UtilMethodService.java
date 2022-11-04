package com.org.framework.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.json.JSONObject;

public interface UtilMethodService {

	/**
	 * 删除对象信息
	 * 
	 * @param ids
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	String deleteObjectInfo(String SQL, String DB, ArrayList<Object> params) throws Exception;

	/**
	 * 显示对象信息
	 * 
	 * @param searchName
	 * @param tableName
	 * @param start
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	JSONObject showObjectInfo(String SQL, ArrayList<Object> params, String DB, int start, int limit) throws Exception;

	/**
	 * 添加对象信息
	 * 
	 * @param SQL
	 * @param DB
	 * @param fields
	 * @return
	 * @throws Exception
	 */
	JSONObject addObjectInfo(String SQL, String DB, List<Object> fields) throws Exception;

	/**
	 * 修改对象信息
	 * 
	 * @param SQL
	 * @param DB
	 * @param fields
	 * @return
	 * @throws Exception
	 */
	String updateObjectInfo(String SQL, String DB, List<Object> fields) throws Exception;

	/**
	 * 获取列表数据
	 * 
	 * @param SQL
	 * @param params
	 * @param DB
	 * @return
	 * @throws Exception
	 */
	List<HashMap<String, Object>> getObjectList(String SQL, ArrayList<Object> params, String DB) throws Exception;
	
	String rollbackObjectInfo(ArrayList<Object> SQL, ArrayList<ArrayList<Object>> params,String DB)throws Exception;;

}
