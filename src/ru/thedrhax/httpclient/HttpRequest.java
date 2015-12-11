package ru.thedrhax.httpclient;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class HttpRequest {
	private URL url;
	private String params;
	
	private URLConnection connection;
	private boolean isHttps;

	private Map<String,List<String>> header;	
	private String content;
	
	/*
	 * Set optional request headers
	 */
	
	public HttpRequest setCookies (String cookies) {
		connection.addRequestProperty("Cookie", cookies);
		return this;
	}
	public HttpRequest setReferer (URL referer) {
		connection.addRequestProperty("Referer", referer.toString());
		return this;
	}
	public HttpRequest setUserAgent (String userAgent) {
		connection.addRequestProperty("User-Agent", userAgent);
		return this;
	}
	public HttpRequest setTimeout (int timeout) {
		connection.setConnectTimeout(timeout);
		connection.setReadTimeout(timeout);
		return this;
	}
	public HttpRequest setIgnoreSSL () {
		if (!isHttps) return this;
		
		TrustManager[] trustAllCerts = new TrustManager[] {
			new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {return null;}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {}
			}
		};
		
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (Exception ex) {
			return this;
		}

		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {return true;}
		};

        ((HttpsURLConnection) connection).setSSLSocketFactory(sc.getSocketFactory());
        ((HttpsURLConnection) connection).setHostnameVerifier(allHostsValid);
        
		return this;
	}
	
	/*
	 *  Public constructors
	 */
	
	public HttpRequest (URL url, String params) throws IOException {
		this.url = url;
		this.params = params;
		
		isHttps = url.getProtocol().equals("https");
		
		connection = url.openConnection();
	}
	
	public HttpRequest (URL  url) throws IOException {
		this(url, null);
	}
	
	// Request sequence
	public HttpRequest connect() throws IOException, SSLHandshakeException {
		// Send POST data if defined
		if (params != null) {
			try{
				((HttpURLConnection)connection).setRequestMethod("POST");
			} catch (ProtocolException ex) {}
			
			connection.setDoOutput(true);

			OutputStreamWriter writer = new OutputStreamWriter (
				connection.getOutputStream()
			);
		
			writer.write(params);
			writer.flush();
			writer.close();
		} else {
			try {
				((HttpURLConnection)connection).setRequestMethod("GET");
			} catch (ProtocolException ex) {}
		}
			
		// Read server answer
		InputStream stream;
		if (((HttpURLConnection)connection).getResponseCode() < 400) {
			stream = connection
				.getInputStream();
		} else {
			stream = ((HttpURLConnection)connection)
				.getErrorStream();
		}
		
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(stream)
		);
		
		String input;
		StringBuffer buffer = new StringBuffer();
		
		// TODO: ??????, ??? ???, ????, ??????????
		try {
			while ((input = reader.readLine()) != null) {
				buffer.append(input + "\r\n");
			}
		} catch (IOException ex) {
			return this;
		}
		
		reader.close();
		
		content = buffer.toString();
		header = connection.getHeaderFields();
		
		return this;
	}
	
	/*
	 * Get request results 
	 */
	
	public String getContent() {
		return content;
	}
	
	public String getCookies() {
		if (header == null) return "";
		List<String> cookies = header.get("Set-Cookie");
		if (cookies == null) return "";
		
		StringBuffer buffer = new StringBuffer();
		
		for (String cookie : cookies) {
			buffer.append(cookie.substring(0, cookie.indexOf(';')) + "; ");
		}
		
		return buffer.toString();
	}
}
