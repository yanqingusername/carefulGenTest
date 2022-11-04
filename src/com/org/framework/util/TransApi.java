package com.org.framework.util;

import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

public class TransApi {
    private static final String TRANS_API_HOST = "http://api.fanyi.baidu.com/api/trans/vip/translate";

    /*private static String appid = "20200608000489876";
    private static String securityKey = "k1cvdTZ0e5KmkyZMovhP";*/
    private static String appid = "20200812000540859";
    private static String securityKey = "yZF9bXtkZ5L_7eb29DJX";
    private static String CHINA = "zh";
    private static String ENGLISH = "en";
    private static String AUTO = "auto";
    
    public static String trans(String text, String language) {
    	Map<String, String> params;
    	String dst = "";
    	if("cn".equals(language)){ //cn
    		params = buildParams(text, AUTO, CHINA);
    	}else{ // en
    		params = buildParams(text, AUTO, ENGLISH);
    	}
    	String result = HttpGet.get(TRANS_API_HOST, params);
        JSONObject json = JSONObject.fromObject(result);
        if(json.has("error_code")){
        	// 如果报错，返回原语言
        	System.out.println("----------翻译失败------------");
        	System.out.println(json);
        	dst = text;
        }else{
        	dst = json.getJSONArray("trans_result").getJSONObject(0).getString("dst");
        }
        return dst;
    }

    private static Map<String, String> buildParams(String query, String from, String to) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("q", query);
        params.put("from", from);
        params.put("to", to);

        params.put("appid", appid);

        // 随机数
        String salt = String.valueOf(System.currentTimeMillis());
        params.put("salt", salt);

        // 签名
        String src = appid + query + salt + securityKey; // 加密前的原文
        params.put("sign", MD5Util.MD5(src));
        return params;
    }
}
