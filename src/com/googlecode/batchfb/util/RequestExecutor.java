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

import com.googlecode.batchfb.util.RequestBuilder.HttpResponse;


/**
 * <p>Allows requests to be executed given the character of a particular platform or http library.</p>
 * 
 * @author Jeff Schnitzer
 */
abstract public class RequestExecutor {

	/** */
	private static RequestExecutor current;
	
	/** Gets the current factory */
	public static RequestExecutor instance() { return current; }

	/**
	 * Sets the current factory used, overriding the default discovery process.
	 */
	public static void setInstance(RequestExecutor value) { current = value; }
	
	/**
	 * The discovery process
	 */
	static {
		if (System.getProperty("com.google.appengine.runtime.environment") != null)
			current = new AppengineRequestExecutor();
		else
			current = new DefaultRequestExecutor();
	}

	/**
	 * Executes the specified request, up to the number of retries allowed.
	 */
	abstract public HttpResponse execute(int retries, RequestSetup setup) throws IOException;
}