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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;

import org.codehaus.jackson.map.ObjectMapper;

import com.googlecode.batchfb.BinaryParam;
import com.googlecode.batchfb.Param;

/**
 * Some string handling utilities
 */
public final class StringUtils
{
	/**
	 * Masks the useless checked exception from URLEncoder.encode()
	 */
	public static String urlEncode(String string) {
		try {
			return URLEncoder.encode(string, "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Masks the useless checked exception from URLDecoder.decode()
	 */
	public static String urlDecode(String string) {
		try {
			return URLDecoder.decode(string, "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Stringify the parameter value in an appropriate way. Note that Facebook fucks up dates by using unix time-since-epoch
	 * some places and ISO-8601 others. However, maybe unix times always work as parameters?
	 */
	public static String stringifyValue(Param param, ObjectMapper mapper) {
		assert !(param instanceof BinaryParam);
		
		if (param.value instanceof String)
			return (String)param.value;
		if (param.value instanceof Date)
			return Long.toString(((Date)param.value).getTime() / 1000);
		else if (param.value instanceof Number)
			return param.value.toString();
		else
			return JSONUtils.toJSON(param.value, mapper);
	}

	/**
	 * Reads an input stream into a String, encoding with UTF-8
	 */
	public static String read(InputStream input) {
		try {
			StringBuilder bld = new StringBuilder();
			Reader reader = new InputStreamReader(input, "utf-8");
			int ch;
			while ((ch = reader.read()) >= 0)
				bld.append((char)ch);
			
			return bld.toString();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}