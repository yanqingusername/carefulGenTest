package com.org.framework.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.json.JSONObject;

public interface UtilMethodDao {
	/**
	 * 删除对象信息
	 * 
	 * @param ids
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	int deleteObjectInfo(String SQL, String DB, ArrayList<Object> params) throws Exception;

	/**
	 * 查询对象信息
	 * 
	 * @param searchName
	 * @param tableName
	 * @param start
	 * @param limit
	 * @return
	 * @throws Exception
	 */
	JSONObject showObjectInfo(String SQL, ArrayList<Object> params, String DB, int start, int limit) throws Exception;

	int addObjectInfo(String SQL, String DB, List<Object> fields) throws Exception;

	int updateObjectInfo(String SQL, String DB, List<Object> fields) throws Exception;

	/**
	 * 获取对象列表
	 * 
	 * @param SQL
	 * @param params
	 * @param DB
	 * @return
	 * @throws Exception
	 */
	List<HashMap<String, Object>> getObjectList(String SQL, ArrayList<Object> params, String DB) throws Exception;

	/**
	 * 回滚对象
	 * @param SQL
	 * @param params
	 * @param DB
	 * @return
	 * @throws Exception
	 */
	int rollbackObjectInfo(ArrayList<Object> SQL, ArrayList<ArrayList<Object>> params,String DB)throws Exception;

}
