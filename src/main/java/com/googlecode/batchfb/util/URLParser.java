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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import com.googlecode.batchfb.Param;

/**
 * <p>Parses a URL into its constituent parts, including the path and the parameters.
 * This is really just a slightly smarter wrapper for java.net.URL.  It assumes
 * the URL is valid, which should be safe for Facebook's paging urls.</p>
 * 
 * @author Jeff Schnitzer
 */
public class URLParser {
	
	/** 
	 * Breaks down a standard query string (ie "param1=foo&param2=bar")
	 * @return an empty map if the query is empty or null 
	 */
	public static Map<String, String> parseQuery(String query) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		
		if (query != null) {
			for (String keyValue: query.split("&")) {
				String[] pair = keyValue.split("=");
				result.put(StringUtils.urlDecode(pair[0]), StringUtils.urlDecode(pair[1]));
			}
		}
		
		return result;
	}
	
	/** */
	URL parsed;
	Map<String, String> params;
	
	/**
	 * @param url is assumed to be a valid url with properly encoded key=value pairs, nothing exotic
	 */
	public URLParser(String url) {
		try {
			this.parsed = new URL(url);
			this.params = parseQuery(this.parsed.getQuery());
			
		} catch (MalformedURLException e) { throw new RuntimeException(e); }
	}

	/**
	 * Get the params, or an empty map if there are none
	 */
	public Map<String, String> getParams() { return this.params; }
	
	/**
	 * Gets the current state of the params as a Param[] array
	 */
	public Param[] getParamsAsArray() {
		Param[] result = new Param[this.params.size()];
		
		int i = 0;
		for (Map.Entry<String, String> entry: this.params.entrySet())
			result[i++] = new Param(entry.getKey(), entry.getValue());
		
		return result;
	}
	
	/**
	 * Get the path, or an empty string if there is none
	 */
	public String getPath() {
		if (this.parsed.getPath() == null)
			return "";
		else
			return this.parsed.getPath();
	}
}