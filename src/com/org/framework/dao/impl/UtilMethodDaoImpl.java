package com.org.framework.dao.impl;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import net.sf.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.org.framework.dao.UtilMethodDao;
import com.org.framework.util.DataSourceBase;

@Repository
public class UtilMethodDaoImpl implements UtilMethodDao {
	@Autowired
	private DataSourceBase dataSourceBase;

	/**
	 * 删除对象信息
	 */
	public int deleteObjectInfo(String SQL, String DB, ArrayList<Object> params) throws Exception {
		return dataSourceBase.buildDelete(DB, SQL, params);
	}

	public JSONObject showObjectInfo(String SQL, ArrayList<Object> params, String DB, int start, int limit) throws Exception {
		return dataSourceBase.buildPageJSON(DB, SQL, params, start, limit);
	}

	public int addObjectInfo(String SQL, String DB, List<Object> fields) throws Exception {
		ArrayList<Object> params = new ArrayList<Object>();
		Iterator<Object> iter = fields.iterator();
		while (iter.hasNext()) {
			params.add(iter.next());
		}
		return dataSourceBase.buildInsert(DB, SQL, params);
	}

	public int updateObjectInfo(String SQL, String DB, List<Object> fields) throws Exception {

		ArrayList<Object> params = new ArrayList<Object>();
		Iterator<Object> iter = fields.iterator();
		while (iter.hasNext()) {
			Object object = iter.next();
			params.add(object);
		}
		return dataSourceBase.buildUpdate(DB, SQL, params);
	}

	public List<HashMap<String, Object>> getObjectList(String SQL, ArrayList<Object> params, String DB) throws Exception {
		return dataSourceBase.buildSelect(DB, SQL, params);
	}
	
	public int rollbackObjectInfo(ArrayList<Object> SQL,ArrayList<ArrayList<Object>> params, String DB) throws Exception {
		return dataSourceBase.rollbackObjectInfo(SQL, params,DB);
	}
}
