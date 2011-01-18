/*
 * Copyright (c) 2010 Jeff Schnitzer.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.googlecode.batchfb.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>For building and executing requests.</p>
 * 
 * @author Jeff Schnitzer
 */
public class RequestBuilder {
	
	/** Supported methods */
	public static enum HttpMethod {
		GET, POST, DELETE;
	}
	
	/** Used as a param value when user submits binary attachments */
	private static class BinaryAttachment {
		InputStream data;
		String contentType;
		String filename;
		
		BinaryAttachment(InputStream data, String contentType, String filename) {
			this.data = data;
			this.contentType = contentType;
			this.filename = filename;
		}
	}
	
	/** */
	private static final String MULTIPART_BOUNDARY = "**** an awful string which should never exist naturally ****" + Math.random();
	private static final String MULTIPART_BOUNDARY_SEPARATOR = "--" + MULTIPART_BOUNDARY;
	private static final String MULTIPART_BOUNDARY_END = MULTIPART_BOUNDARY_SEPARATOR + "--";
	
	/** */
	String baseURL;
	Map<String, Object> params = new LinkedHashMap<String, Object>(); // value will be either String or BinaryAttachment
	HttpMethod method;
	boolean hasBinaryAttachments;
	int timeout;	// 0 for no timeout
	
	/**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
	public RequestBuilder(String url, HttpMethod method) {
		this(url, method, 0);
	}
	
	/**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
	public RequestBuilder(String url, HttpMethod method, int timeout) {
		this.baseURL = url;
		this.method = method;
		this.timeout = timeout;
	}
	
	/**
	 * Adds a parameter, urlencoding both the name and value
	 */
	public void addParam(String name, String value) {
		this.params.put(name, value);
	}
	
	/**
	 * Adds a binary attachment. Request method must be POST; causes the type to be multipart/form-data
	 */
	public void addParam(String name, InputStream stream, String contentType, String filename) {
		if (this.method != HttpMethod.POST)
			throw new IllegalStateException("May only add binary attachment to POST, not to " + this.method);
		
		this.params.put(name, new BinaryAttachment(stream, contentType, filename));
		this.hasBinaryAttachments = true;
	}
	
	/**
	 * Set a connection/read timeout, or 0 for no timeout.
	 */
	public void setTimeout(int millis) {
		this.timeout = millis;
	}
	
	/**
	 * @return the URL + the queryString as if for a GET request
	 */
	public String toString() {
		if (this.params.isEmpty())
			return this.baseURL;
		else
			return this.baseURL + '?' + this.createQueryString();
	}
	
	/**
	 * Execute the request, providing the result in the connection object.
	 */
	public HttpURLConnection execute() throws IOException {
		switch (this.method) {
			case POST: {
				HttpURLConnection conn = this.createConnection(this.baseURL);
				if (!this.params.isEmpty()) {
					conn.setDoOutput(true);
					
					if (!this.hasBinaryAttachments) {
						// This is more efficient if we don't have any binary attachments
						conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						conn.getOutputStream().write(this.createQueryString().getBytes("utf-8"));
					} else {
						// Binary attachments requires more complicated multipart/form-data format
						this.writeMultipart(conn);
					}
				}
				return conn;
			}
				
			default: {
				HttpURLConnection conn = this.createConnection(this.toString());
				return conn;
			}
		}
	}
	
	/**
	 * Create the connection and set the method.
	 */
	protected HttpURLConnection createConnection(String url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
		conn.setRequestMethod(this.method.name());
		
		if (this.timeout > 0) {
			conn.setConnectTimeout(this.timeout);
			conn.setReadTimeout(this.timeout);
		}
		
		return conn;
	}
	
	/**
	 * Creates a string representing the current query string, or an empty string if there are no parameters. Will not work if there are binary
	 * attachments!
	 */
	protected String createQueryString() {
		assert !this.hasBinaryAttachments;
		
		if (this.params.isEmpty())
			return "";
		
		StringBuilder bld = null;
		
		for (Map.Entry<String, Object> param: this.params.entrySet()) {
			if (bld == null)
				bld = new StringBuilder();
			else
				bld.append('&');
			
			bld.append(StringUtils.urlEncode(param.getKey()));
			bld.append('=');
			bld.append(StringUtils.urlEncode(param.getValue().toString()));
		}
		
		return bld.toString();
	}
	
	/**
	 * Write out params as multipart/form-data, including any binary attachments.
	 * 
	 * See <a href="http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4">http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</a>.
	 */
	protected void writeMultipart(HttpURLConnection conn) throws IOException {
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY);
		
		OutputStream out = conn.getOutputStream();
		LineWriter writer = new LineWriter(out);
		
		for (Map.Entry<String, Object> param: this.params.entrySet()) {
			writer.println(MULTIPART_BOUNDARY_SEPARATOR);
			
			if (param.getValue() instanceof BinaryAttachment) {
				BinaryAttachment ba = (BinaryAttachment)param.getValue();
				writer.println("Content-Disposition: form-data; name=\"" + StringUtils.urlEncode(param.getKey()) + "\"; filename=\""
						+ StringUtils.urlEncode(ba.filename) + "\"");
				writer.println("Content-Type: " + ba.contentType);
				writer.println("Content-Transfer-Encoding: binary");
				writer.println();
				writer.flush();
				// Now output the binary part to the raw stream
				int read;
				byte[] chunk = new byte[8192];
				while ((read = ba.data.read(chunk)) > 0)
					out.write(chunk, 0, read);
			} else {
				writer.println("Content-Disposition: form-data; name=\"" + StringUtils.urlEncode(param.getKey()) + "\"");
				writer.println();
				writer.println(StringUtils.urlEncode(param.getValue().toString()));
			}
		}
		
		writer.println(MULTIPART_BOUNDARY_END);
		writer.flush();
	}
}