package com.org.framework.bean;

/**
 * 
* Title: Ship
* Description: 存储物流返回信息
* Version:1.0.0  
* @author mateJay
* @date 2019年4月15日
 */
public class Ship {
	/**
	 * 快递公司编号
	 */
	private String Express;
	/**
	 * 物流单号
	 */
	private String ExpressCode;
	/**
	 * 物流状态
	 *  0-无轨迹
	 *  1-已揽收
     *  2-在途中
     *  201-到达派件城市
     *  3-签收
     *  4-问题件
	 */
	private String status;
	/**
	 * 轨迹发生时间
	 */
	private String acceptTime;
	/**
	 * 轨迹描述
	 */
	private String acceptStation;
	
	
	public String getAcceptStation() {
		return acceptStation;
	}
	public void setAcceptStation(String acceptStation) {
		this.acceptStation = acceptStation;
	}
	public String getExpress() {
		return Express;
	}
	public void setExpress(String express) {
		Express = express;
	}
	public String getExpressCode() {
		return ExpressCode;
	}
	public void setExpressCode(String expressCode) {
		ExpressCode = expressCode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getAcceptTime() {
		return acceptTime;
	}
	public void setAcceptTime(String acceptTime) {
		this.acceptTime = acceptTime;
	}
	@Override
	public String toString() {
		return "Ship [Express=" + Express + ", ExpressCode=" + ExpressCode
				+ ", status=" + status + ", acceptTime=" + acceptTime
				+ ", acceptStation=" + acceptStation + "]";
	}
	
	
	
	
}
