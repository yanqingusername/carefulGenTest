package com.org.framework.util;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/** 
 * 证书信任管理器（用于https请求）
 */  
public class MyX509TrustManager implements X509TrustManager {

    public void checkClientTrusted(X509Certificate[] chain, String authType){
    }
  
    public void checkServerTrusted(X509Certificate[] chain, String authType){
    }
  
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}