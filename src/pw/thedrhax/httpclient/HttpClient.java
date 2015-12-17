package pw.thedrhax.httpclient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpClient {
	private String cookies;
	private URL referer;
	private String userAgent;
	private int timeout = 5000;
	private boolean ignoreSSL = false;
	private int retries = 0;
	
	/*
	 * Set initial request headers
	 */
	
	public HttpClient setCookies (String cookies) {
		this.cookies = cookies; return this;
	}
	public HttpClient setReferer (URL referer) {
		this.referer = referer; return this;
	}
	public HttpClient setReferer (String referer) throws MalformedURLException {
		return setReferer(new URL(referer));
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
	public HttpClient setMaxRetries (int retries) {
		this.retries = retries; return this;
	}
	
	/*
	 * Methods
	 */
	
	private HttpRequest createRequest(URL url, String params) throws IOException {
		HttpRequest request = new HttpRequest(url, params);
		
		if (cookies != null)	request.setCookies(cookies);
		if (referer != null)	request.setReferer(referer);
		if (userAgent != null)	request.setUserAgent(userAgent);
		if (ignoreSSL)			request.setIgnoreSSL();
		request.setTimeout(timeout);
		
		return request;
	}
	
	public HttpRequest navigate (URL url, String params) throws IOException {
		HttpRequest request = createRequest(url, params);
		
		for (int i = 0; i <= retries; i++) {
			try {
				request.connect();
				break;
			} catch (IOException ex) {
				if (i == retries) {
					throw(ex);
				} else {
					request = createRequest(url, params);
				}
			}
		}
		
		cookies += request.getCookies();
		referer = url;
		
		return request;
	}
	
	public HttpRequest navigate (URL url) throws IOException {
		return navigate(url, null);
	}
	
	public HttpRequest navigate (String address, String params) throws IOException {
		return navigate(new URL(address), params);
	}
	
	public HttpRequest navigate (String address) throws IOException {
		return navigate(address, null);
	}	
}
