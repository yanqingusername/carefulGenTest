package com.org.framework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import net.sf.json.JSONObject;  
 
/** 
* 获取微信APPID和secret工具类 
*/  
 
public  class AccessTokenUtil {
	@Autowired
	private static Logger logger = Logger.getLogger(AccessTokenUtil.class);

   //实用<span style="font-family:Consolas;">synchronized static可以防止同时被多实例化</span>  
   public synchronized static String getAccessToken() {
       //保存access_token文件名字
       String FileName = "../WxTokenUtil.properties";
       try {
           // 从文件中获取token值及时间
           Properties prop = new Properties();// 属性集合对象
            //获取文件流
           InputStream fis = AccessTokenUtil.class.getClassLoader().getResourceAsStream(FileName);
           
           prop.load(fis);// 将属性文件流装载到Properties对象中
           fis.close();// 关闭流
           //获取Appid，APPsecret
           String APPID = Configure.getAppid();
           String APPSECRET = Configure.getAppSecrte(); 
           // 获取accesstoken，初始值为空，第一次调用之后会把值写入文件
           String access_token = prop.getProperty("access_token");
           String jsapi_ticket = prop.getProperty("jsapi_ticket");
           String expires_in = prop.getProperty("expires_in");
           String last_time = prop.getProperty("last_time");
 
           logger.info("我是从配置文件中获取的init_APPID:"+APPID);
           logger.info("我是从配置文件中获取的init_APPSECRET:"+APPSECRET);
           logger.info("我是从配置文件中获取的init_access_token:"+access_token);
           logger.info("我是从配置文件中获取的init_jsapi_ticket:"+jsapi_ticket);
           logger.info("我是从配置文件中获取的init_expires_in:"+expires_in);
           logger.info("我是从配置文件中获取的init_last_time:"+last_time);
           
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
           // 每次调用，判断expires_in是否过期，如果token时间超时，重新获取，expires_in有效期为7200  
           if ((current_time - long_last_time) / 1000 >= int_expires_in) {
        	   logger.info("进行到这说明expires_in过期，重新获取access_token");
               //获取token url
               String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="  
                       + APPID + "&secret=" + APPSECRET;

               logger.info("token_url:"+url);

               //发送http请求得到json流
               JSONObject jobject = httpRequest(url);
               logger.info("我是从微信端获取到的json："+jobject);
               //从json流中获取access_token
               String j_access_token = jobject.getString("access_token");
               Integer j_expires_in = jobject.getInt("expires_in");
               logger.info("urlReturn_access_token:"+j_access_token);
               logger.info("urlReturn_expires_in:"+j_expires_in);
               
               //通过access_token获取ticket
               logger.info("通过access_token获取ticket");
               String urlStr = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token="+j_access_token+"&type=jsapi";  
               String backData=jsapi_ticketGetUtil.sendGet(urlStr, "utf-8", 10000);
               jsapi_ticket = (String) JSONObject.fromObject(backData).get("ticket");
               logger.info("urlReturn_jsapi_ticket:"+jsapi_ticket);
               
               //保存access_token
               if (j_access_token != null && j_expires_in != null && jsapi_ticket != null) {
                   prop.setProperty("access_token", j_access_token);
                   prop.setProperty("jsapi_ticket", jsapi_ticket);
                   prop.setProperty("expires_in", ""+j_expires_in);
                   prop.setProperty("last_time", System.currentTimeMillis() + "");
 
                   URL url_ = AccessTokenUtil.class.getClassLoader().getResource(FileName);
                   FileOutputStream fos = new FileOutputStream(new File(url_.toURI()));
                   prop.store(fos, null);
                   fos.close();// 关闭流
               }
               //如果已经过期返回获取到的access_token
               return j_access_token;
           } else {
        	   logger.info("进行到这说明expires_in没有过期");
               //如果没有过期，返回从文件中读取的access_token
               return access_token;
           }
       } catch (Exception e) {
           return null;
       }
   }
 
   // 获取accesstoken  
   public synchronized static JSONObject httpRequest(String requestUrl) {
       JSONObject jsonObject = null;
       StringBuffer buffer = new StringBuffer();
       try {
           URL url = new URL(requestUrl);
           HttpsURLConnection httpUrlConn = (HttpsURLConnection) url.openConnection();
 
           httpUrlConn.setDoOutput(true);
           httpUrlConn.setDoInput(true);
           httpUrlConn.setUseCaches(false);
           // 设置请求方式（GET/POST）
           httpUrlConn.setRequestMethod("GET");
           httpUrlConn.connect();
           // 将返回的输入流转换成字符串
           InputStream inputStream = httpUrlConn.getInputStream();
           InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
           BufferedReader bufferedReader = new BufferedReader(inputStreamReader);  
 
           String str = null;
           while ((str = bufferedReader.readLine()) != null) {
               buffer.append(str);
           }  
           bufferedReader.close();
           inputStreamReader.close();
           // 释放资源
           inputStream.close();
           inputStream = null;
           httpUrlConn.disconnect();
           jsonObject = JSONObject.fromObject(buffer.toString());

       } catch (Exception e) {
           e.printStackTrace();
       }

       return jsonObject;
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
