package ru.thedrhax.httpclient;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

public class HttpRequest {
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
		this.params = params;
		
		isHttps = url.getProtocol().equals("https");
		
		connection = url.openConnection();
	}
	
	public HttpRequest (URL  url) throws IOException {
		this(url, null);
	}
	
	// Request sequence
	public HttpRequest connect() throws IOException {
		// Send POST data if defined
		if (params != null) {
			try{
				((HttpURLConnection)connection).setRequestMethod("POST");
			} catch (ProtocolException ignored) {}
			
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
			} catch (ProtocolException ignored) {}
		}
			
		// Read server answer
		InputStream stream;
		int responseCode;

		try {
			responseCode = ((HttpURLConnection) connection).getResponseCode();
		} catch (Exception ex) {
			responseCode = ((HttpURLConnection) connection).getResponseCode();
		}

		if (responseCode < 400) {
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
		StringBuilder buffer = new StringBuilder();
		
		try {
			while ((input = reader.readLine()) != null) {
				buffer.append(input);
				buffer.append("\r\n");
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
		
		StringBuilder buffer = new StringBuilder();
		
		for (String cookie : cookies) {
			buffer.append(cookie.substring(0, cookie.indexOf(';')));
			buffer.append("; ");
		}
		
		return buffer.toString();
	}
}
