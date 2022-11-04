package com.org.framework.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.org.framework.bean.UserBean;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class DataSourceBase {
	public static Log logger = LogFactory.getLog(DataSourceBase.class);
	@Autowired
	private DataSource sysDataSource;
	@Autowired
	private DataSource healthDataSource;

	public Connection getConnection(String dataSource) {
		Connection conn = null;
		if (dataSource.equals("sys")) {
			try {
				conn = sysDataSource.getConnection();
			} catch (SQLException e) {
				logger.error("获取数据库连接异常。。。。。。。");
				e.printStackTrace();
			}
		} else if (dataSource.equals("health")) {
			try {
				conn = healthDataSource.getConnection();
			} catch (SQLException e) {
				logger.error("获取数据库连接异常。。。。。。。");
				e.printStackTrace();
			}
		} else{
			logger.error("没有这个数据库。。。。。。。");
			conn = null;
		}
		return conn;
	}

	public void close(Connection conn, PreparedStatement ps, ResultSet rs) {

		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				logger.error("数据库结果集关闭异常。。。。  rs==" + rs);
				e.printStackTrace();
			}
		}
		if (ps != null) {
			try {
				ps.close();
			} catch (SQLException e) {
				logger.error("数据库ps关闭异常。。。。  ps==" + ps);
				e.printStackTrace();
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				logger.error("数据库连接关闭异常。。。。  conn==" + conn);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 普通查询
	 * 
	 * @param dataSource
	 * @param sql
	 * @param params
	 * @return
	 */
	public List<HashMap<String, Object>> buildSelect(String dataSource, String sql, List<Object> params) {
		List<HashMap<String, Object>> list = new ArrayList<HashMap<String, Object>>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// 获取对不同数据库的连接
			conn = this.getConnection(dataSource);
			ps = conn.prepareStatement(sql);
			// PreparedStatement 设入
			setPreparedStatementParams(ps, params);
			rs = ps.executeQuery();
			HashMap<String, String> columnNameMap = null;
			try {
				columnNameMap = ColumnNameToHashMap(rs);
			} catch (Exception e) {
				e.printStackTrace();
			}
			while (rs.next()) {
				list.add(resultSetToHashMap(rs, columnNameMap));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(conn, ps, rs);
		}
		return list;
	}

	/**
	 * 插入一条数据
	 * 
	 * @param dataSource
	 * @param sql
	 * @param params
	 * @return
	 */
	public int buildInsert(String dataSource, String sql, List<Object> params) {
		Connection conn = null;
		PreparedStatement ps = null;
		int rs = 0;
		try {
			// 获取对不同数据库的连接
			conn = this.getConnection(dataSource);
			ps = conn.prepareStatement(sql);
			// PreparedStatement 设入
			setPreparedStatementParams(ps, params);
			rs = ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(conn, ps, null);
		}
		return rs;
	}

	/**
	 * 更新一条数据
	 * 
	 * @param dataSource
	 * @param sql
	 * @param params
	 * @return
	 */
	public int buildUpdate(String dataSource, String sql, List<Object> params) {
		Connection conn = null;
		PreparedStatement ps = null;
		int rs = 0;
		try {
			// 获取对不同数据库的连接
			conn = this.getConnection(dataSource);
			ps = conn.prepareStatement(sql);
			// PreparedStatement 设入
			setPreparedStatementParams(ps, params);
			rs = ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(conn, ps, null);
		}
		return rs;
	}

	private void setPreparedStatementParams(PreparedStatement ps, List<Object> params) {
		if (params != null && params.size() > 0) {
			// logger.error("params的个数=="+params.size());
			// logger.error("params=="+params);
			System.out.println("=========params的个数==" + params.size());
			System.out.println("=========params==" + params);
			int index = 0;
			Iterator<Object> iter = params.iterator();
			while (iter.hasNext()) {
				try {
					ps.setObject(++index, iter.next());
				} catch (SQLException e) {
					// logger.error("在ps设置setObject()查询条件时出错。。。。");
					e.printStackTrace();
				}
			}
		}

	}

	private HashMap<String, String> ColumnNameToHashMap(ResultSet rs) throws Exception {
		HashMap<String, String> hashMap = new HashMap<String, String>();
		ResultSetMetaData rsmd = rs.getMetaData();
		for (int i = 0; i < rsmd.getColumnCount(); i++) {
			String columnName = rsmd.getColumnName(i + 1);
			String fileName = columnName.toLowerCase();
			hashMap.put(columnName, fileName);
		}
		return hashMap;
	}

	private HashMap<String, Object> resultSetToHashMap(ResultSet rs, HashMap<String, String> columnNameMap) {
		HashMap<String, Object> hashMap = new HashMap<String, Object>();
		Iterator<String> iter = columnNameMap.keySet().iterator();
		while (iter.hasNext()) {
			try {
				String columnName = iter.next();
				Object columnValue = rs.getObject(columnName);
				hashMap.put(columnName, columnValue == null ? "" : columnValue);
			} catch (SQLException e) {
				logger.info("数据库 没有这个字段的值");
				e.printStackTrace();
			}
		}
		return hashMap;
	}

	public JSONObject buildPageJSON(String dataSource, String sql, List<Object> params, int start, int limit) {
		JSONObject json = new JSONObject();
		List<HashMap<?, ?>> list = new ArrayList<HashMap<?, ?>>();
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		int total = 0;
		try {
			// 获取对不同数据库的连接
			conn = this.getConnection(dataSource);
			// ps =
			// conn.prepareStatement("select * from ("+sql+")t where rowindex>? and rowindex<=?");
			ps = conn.prepareStatement("select * from (" + sql + ")t LIMIT ?,?");
			System.out.println("==============================select * from (" + sql + ")t LIMIT ?,?");
			System.out.println("==============================" + start);
			System.out.println("==============================" + limit);
			// PreparedStatement 设入
			setPreparedStatementParams(ps, params);
			int paramCount = params == null ? 0 : params.size();
			ps.setObject(++paramCount, start);
			ps.setObject(++paramCount, limit);
			rs = ps.executeQuery();
			HashMap<String, String> columnNameMap = null;
			try {
				columnNameMap = ColumnNameToHashMap(rs);
			} catch (Exception e) {
				e.printStackTrace();
			}
			while (rs.next()) {
				list.add(resultSetToHashMap(rs, columnNameMap));
			}
			// ps = conn.prepareStatement("select count(*) c from ("+sql+") t");
			// setPreparedStatementParams(ps,params);
			// ---------------------------------以上注释的替换代码：以下为替换代码的开始-------------------------------------------
			ps = conn.prepareStatement("select count(*) AS c from (select t.* from (" + sql + ") t LIMIT ?,?) s");
			setPreparedStatementParams(ps, params);
			paramCount = params == null ? 0 : params.size();
			ps.setObject(++paramCount, start);
			ps.setObject(++paramCount, limit);
			// ---------------------------------注释的替换代码：以上为替换代码的结束-------------------------------------------
			rs = ps.executeQuery();
			while (rs.next()) {
				total = Integer.parseInt(rs.getString("c"));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(conn, ps, rs);
		}
		json.put("total", total);
		System.out.println("==============================" + list.size());
		JSONArray arr = JSONArray.fromObject(list);
		System.out.println("==================arr.size()============" + arr.size());
		json.put("root", arr);
		return json;

	}

	/**
	 * 删除数据
	 * 
	 * @param dataSource
	 * @param sql
	 * @param params
	 * @return
	 */
	public int buildDelete(String dataSource, String sql, List<Object> params) {
		Connection conn = null;
		PreparedStatement ps = null;
		int rs = 0;
		try {
			conn = this.getConnection(dataSource);
			ps = conn.prepareStatement(sql);
			// logger.info(sql);
			// logger.info(params.size());
			setPreparedStatementParams(ps, params);
			rs = ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			close(conn, ps, null);
		}
		System.out.println("-------------------------------------------------"
				+ rs);
		return rs;
	}

	/**
	 * 回滚事物
	 * @param SQL
	 * @param params
	 * @param DB
	 * @return
	 */
	public int rollbackObjectInfo(ArrayList<Object> SQL,ArrayList<ArrayList<Object>> params, String DB) {
		System.out.println("========rollbackObjectInfo============");
		System.out.println("========DB============"+DB);
		System.out.println("========params============"+params.size());
		System.out.println("========SQL============"+SQL.size());			
		Connection conn = null;
		PreparedStatement ps = null;
        int rs = 0;
		try {
			// 获取对不同数据库的连接
        	conn = this.getConnection(DB);
        	conn.setAutoCommit(false);
        	for(int i = 0;i<SQL.size();i++){
        		ps = conn.prepareStatement(SQL.get(i).toString());
    			// PreparedStatement 设入
        		ArrayList<Object> tempParams = params.get(i);
    			setPreparedStatementParams(ps,tempParams);
//    			rs = ps.executeUpdate();
    			//modify by wusj有缺陷
    			ps.executeUpdate();
        	}
        	conn.commit();
        	rs=1;//modify by wusj
        	//UserBean UserBean = (UserBean) WebContext.getCurrentRequest().getSession().getAttribute("userInfo");
        	//logger.info("insert：sql----"+SQL+">>>>>params----"+params+">>>>>UserBean----"+UserBean.getUserName());
		} catch (SQLException e) {
			try {
				if(conn!=null){
					System.out.println("事务执行失败，进行回滚！"); 
					conn.rollback();
				}			
			} catch (SQLException e1) {
				e1.printStackTrace();
			}				        
		}finally{
			close(conn,ps,null);
        }
		return rs;
	}
}
