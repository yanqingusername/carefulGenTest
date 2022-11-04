package com.org.framework.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.RequestMapping;


@RequestMapping("/msgapi/")
public class msgapi {
	private static String msgUrl = "http://cloud.coyotebio-lab.com/smsApi/sendsms.hn";
	
	
	
	
	public static void main(String[] args) throws ClientProtocolException, IOException {
		//发短信开始***********************************
		JSONObject jsonobject = new JSONObject();
		String customer_code = "108004007799";
		String phone = "18725723787";
		String text = "【卡尤迪】尊敬的用户，您的样本编号为"+customer_code+"的基因检测报告已经生成完毕，关注\"精准健康管家\"微信公众号:点击\"做检测->报告\"即可查看，祝您生活愉快！";
			
			CloseableHttpClient clientForMsg = HttpClients.createDefault();
            HttpPost postForMsg = new HttpPost(msgUrl);
            List<NameValuePair> listForMsg = new ArrayList<NameValuePair>();
            listForMsg.add(new BasicNameValuePair("phoneNumber", phone));
    		listForMsg.add(new BasicNameValuePair("contentCode", "SMS_213678955"));
    		listForMsg.add(new BasicNameValuePair("contentParam", text));
    		listForMsg.add(new BasicNameValuePair("signName", "卡尤迪"));
            
    	   /* 
    	    listForMsg.add(new BasicNameValuePair("apikey", apikey));
    	    listForMsg.add(new BasicNameValuePair("text", text));
    	    listForMsg.add(new BasicNameValuePair("mobile", "18725723787"));*/
            
    	    UrlEncodedFormEntity uefEntityForMsg = new UrlEncodedFormEntity(listForMsg,"UTF-8");
    	    postForMsg.setEntity(uefEntityForMsg);
    	    HttpResponse responseForMsg = clientForMsg.execute(postForMsg);
    	    if (responseForMsg.getStatusLine().getStatusCode() == 200) {
    	    	HttpEntity httpEntityForMsg = responseForMsg.getEntity();
    	    	String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
    	    	System.out.println("云片发送短信返回信息："+string);
    	    } else{
    	    	HttpEntity httpEntityForMsg = responseForMsg.getEntity();
    	    	String string = EntityUtils.toString(httpEntityForMsg, "UTF-8");
    	    	JSONObject fromObject = JSONObject.fromObject(string);
    	    	int Msgcode = fromObject.getInt("code");
    	    	String Msgmsg = fromObject.getString("msg");
    	    	String solve = "";
    	    	//无需处理返回码
    	    	if(Msgcode==8||Msgcode==9||Msgcode==17||Msgcode==22||Msgcode==33){
    	    		solve = "无需处理";
    	    	}else{
    	    		solve = "请联系技术支持";
    	    	}
    	    	
    	    	
    			jsonobject.put("code", 1);
    			jsonobject.put("success", true);
    			jsonobject.put("msg", "发短信失败：" + Msgmsg + solve);
    			String result = jsonobject.toString();
    	    }
    	    
    	   System.out.println(jsonobject);
       //发短信结束***********************************
	}
}
