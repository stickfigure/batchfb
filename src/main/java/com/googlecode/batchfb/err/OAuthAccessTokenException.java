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
 * <p>Indicates something is wrong with the access token you have (or should have)
 * passed to Facebook.  However, this is only thrown in a few cases - many other
 * token problems produce the base OAuthException.  Because Facebook is erratic
 * about this, you should catch the base OAuthException instead of directly trying
 * to catch this exception.</p>
 * 
 * <p>Note:  Facebook seems to have stopped producing this error, using OAuthException
 * instead.</p>
 * 
 * @see OAuthException
 * @author Jeff Schnitzer
 */
public class OAuthAccessTokenException extends OAuthException {
	private static final long serialVersionUID = 1L;
	
	/** Make GWT happy */
	OAuthAccessTokenException() {}

	public OAuthAccessTokenException(String message, int code, int subcode) {
		super(message, code, subcode);
	}
}