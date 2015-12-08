package ru.thedrhax.httpclient;

public class HttpClient {
	private String cookies = "";
	private String referer = "";
	private String userAgent;
	private boolean ignoreSSL = false;
	private int timeout = 1000;
	
	/*
	 * Set initial request headers
	 */
	
	public HttpClient setCookies (String cookies) {
		this.cookies = cookies; return this;
	}
	public HttpClient setReferer (String referer) {
		this.referer = referer; return this;
	}
	public HttpClient setUserAgent (String userAgent) {
		this.userAgent = userAgent; return this;
	}
	
	/*
	 * Set connection parameters
	 */
	
	public HttpClient setIgnoreSSL (boolean ignoreSSL) {
		this.ignoreSSL = ignoreSSL; return this;
	}
	public HttpClient setTimeout(int timeout) {
		this.timeout = timeout; return this;
	}
	
	
	/*
	 * Methods
	 */
	
	public HttpRequest navigate (String address, String params) {
		HttpRequest request = new HttpRequest(address, params);
		
		request.setCookies(cookies);
		request.setReferer(referer);
		request.setUserAgent(userAgent);
		if (ignoreSSL) request.setIgnoreSSL();
		if (timeout != 1000) request.setTimeout(timeout);
		
		request.connect();
		
		cookies += request.getCookies();
		referer = address;
		
		return request;
	}
	
	public HttpRequest navigate (String address) {
		return navigate(address, null);
	}
}
