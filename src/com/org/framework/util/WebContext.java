package com.org.framework.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public  final class WebContext {
	private static ThreadLocal<HttpServletRequest> requestThreadLocal=new ThreadLocal<HttpServletRequest>();
	private static ThreadLocal<HttpServletResponse> responseThreadLocal=new ThreadLocal<HttpServletResponse>();
	
	public static HttpServletRequest getCurrentRequest(){
		return requestThreadLocal.get();
	}
	
	public static HttpServletResponse getCurrentResponse(){
		return responseThreadLocal.get();
	}
	
	public static void setCurrentRequest(HttpServletRequest request){
		requestThreadLocal.set(request);
	}
	
	public static void setCurrentResponse(HttpServletResponse response){
		responseThreadLocal.set(response);
	}
	
	public static void destory(){
		requestThreadLocal.set(null);
		responseThreadLocal.set(null);
	}
	
}
