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
import java.io.OutputStream;

import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;

/**
 * <p>Interface which executors must provide</p>
 * 
 * @author Jeff Schnitzer
 */
public interface RequestDefinition {
	
	/**
	 * Must be called first, defines the basic params of the request.
	 */
	void init(HttpMethod meth, String url) throws IOException;

	/**
	 * Set a request header
	 */
	void setHeader(String name, String value);
	
	/**
	 * Gets an output stream into which you can write the content body.
	 * Use this *or* setContent().
	 */
	OutputStream getContentOutputStream() throws IOException;
	
	/**
	 * Use this *or* getContentOutputStream().
	 */
	void setContent(byte[] content) throws IOException;
	
	/**
	 * Sets the number of milliseconds to timeout if no response obtained.
	 */
	void setTimeout(int millis);
}