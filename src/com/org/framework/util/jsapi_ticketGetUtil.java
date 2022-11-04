package com.org.framework.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
	获取jsapi_ticket工具类
	注意：urlStr的值修改一下
*/
public class jsapi_ticketGetUtil {

	@Autowired
	private static Logger logger = Logger.getLogger(jsapi_ticketGetUtil.class);
    
	/***
     * 获取jsapiTicket
     * @return
     */
    public synchronized static String getJSApiTicket(){
    	logger.info("进入获取jsapi_ticket方法，去配置文件寻找");

    	//保存jsapi_ticket文件名字
        String FileName = "../WxTokenUtil.properties";
        try{
        	// 从文件中获取jsapi_ticket值及时间
            Properties prop = new Properties();// 属性集合对象
             //获取文件流
            InputStream fis = AccessTokenUtil.class.getClassLoader().getResourceAsStream(FileName);
            
            prop.load(fis);// 将属性文件流装载到Properties对象中
            fis.close();// 关闭流
            //获取jsapi_ticket
            String jsapi_ticket = prop.getProperty("jsapi_ticket");
            String expires_in = prop.getProperty("expires_in");
            String last_time = prop.getProperty("last_time");
            
            logger.info("我是从配置文件中获取的init_jsapi_ticket:"+jsapi_ticket);
            logger.info("我是从配置文件中获取的init_last_time:"+last_time);
            logger.info("我是从配置文件中获取的init_expires_in:"+expires_in);
            
            int int_expires_in = 0;
            long long_last_time = 0;
  
            try {
                int_expires_in = Integer.parseInt(expires_in);
                long_last_time = Long.parseLong(last_time);
            } catch (Exception e) {
  
            }
            //得到当前时间
            long current_time = System.currentTimeMillis();
            logger.info("当前时间：" + current_time);
            logger.info("上次时间：" + long_last_time);
            logger.info("过期时间：" + int_expires_in);
            // 每次调用，判断expires_in是否过期，如果jsapi_ticket时间超时，重新获取，expires_in有效期为7200  
            if ((current_time - long_last_time) / 1000 >= int_expires_in){
            	logger.info("进行到这，说明jsapi_ticket超时，需要重新获取access_token");
            	//极端情况下，用户在某个页面停留7200秒
            	AccessTokenUtil.getAccessToken();
            	jsapi_ticket = getJSApiTicket();
            }
            return jsapi_ticket;
        }catch(Exception e){
        	return null;
        }

           
    }
   /***
    * 模拟get请求
    * @param url
    * @param charset
    * @param timeout
    * @return
    */
    public static String sendGet(String url, String charset, int timeout){
       String result = "";
       try{
           URL u = new URL(url);
           URLConnection conn = u.openConnection();
           conn.connect();
           conn.setConnectTimeout(timeout);
           BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset));
           String line="";
           while ((line = in.readLine()) != null){
               result = result + line;
           }
           in.close();
       }catch (Exception e){
         return result;
       }
       return result;
    }
}
