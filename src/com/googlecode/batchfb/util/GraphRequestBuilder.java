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
import java.net.HttpURLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>There is a problem with batching too many things; you start to hit infrastructure limits
 * on the length of a URL.  There's no official documented limit on a URL length, but some
 * poorly behaved pieces of software (including Google App Engine) break when the length
 * exceeds some arbitrary limit.</p>
 * 
 * <p>This class converts excessively-long URLs to a POST request using the special
 * method=GET parameter.</p>
 * 
 * @author Jeff Schnitzer
 */
public class GraphRequestBuilder extends RequestBuilder {
	/** */
	private static final Logger log = Logger.getLogger(GraphRequestBuilder.class.getName());
	
	/** Arbitrarily chosen value of 2000 seems safe; actual appengine limit is reported to be 2072. */
	public static final int DEFAULT_CUTOFF = 2000;
	
	/** Limit to when we should convert to a POST */
	int cutoff = DEFAULT_CUTOFF;
	
	/**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
	public GraphRequestBuilder(String url, HttpMethod method) {
		super(url, method);
	}
	
	/**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
	public GraphRequestBuilder(String url, HttpMethod method, int timeout) {
		super(url, method, timeout);
	}
	
	/**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
	public GraphRequestBuilder(String url, HttpMethod method, int timeout, int retries) {
		super(url, method, timeout, retries);
	}
	
	/**
	 * Construct from a base URL like http://api.facebook.com/. It should not have any query parameters or a ?.
	 */
	public GraphRequestBuilder(String url, HttpMethod method, int timeout, int retries, int cutoff) {
		this(url, method, timeout);
		this.cutoff = cutoff;
	}
	
	/**
	 * Replace excessively-long GET requests with a POST.
	 */
	@Override
	public HttpURLConnection execute() throws IOException
	{
		if (this.method == HttpMethod.GET) {
			String url = this.toString();
			if (url.length() > this.cutoff) {
				if (log.isLoggable(Level.FINER))
					log.finer("URL length " + url.length() + " too long, converting GET to POST: " + url);
				
				String rebase = this.baseURL + "?method=GET";
				return this.execute(HttpMethod.POST, rebase);
			}
		}

		return super.execute();
	}
}