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

package com.googlecode.batchfb.err;

/**
 * Exception generated when Facebook returns a well-formed error to us. Includes the type,
 * code, and subcode. Note that the codes may not exist.
 *
 * @author Jeff Schnitzer
 */
public class ErrorFacebookException extends FacebookException {

	private static final long serialVersionUID = 1L;

	/** Facebook's 'type' */
	String type;

	/** Facebook's 'code', possibly null */
	Integer code;

	/** Facebook's 'error_subcode', possibly null */
	Integer subcode;

	/** Make GWT happy */
	ErrorFacebookException() {}

	/**
	 */
	public ErrorFacebookException(String message, String type, Integer code, Integer subcode) {
		super(message);
		this.code = code;
		this.subcode = subcode;
	}

	/** */
	public String getType() { return type; }

	/** */
	public Integer getCode() { return code; }

	/** */
	public Integer getSubcode() { return subcode; }
}