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
 * <p>Indicates something has failed in the authorization of your application.
 * Unfortunately Facebook is somewhat erratic about the errors it produces:</p>
 * 
 * <ul>
 * <li>Calling http://graph.facebook.com/me/friends without a token produces QueryParseException</li>
 * <li>Calling http://graph.facebook.com/markzuckerberg/friends without a token produces OAuthAccessTokenException</li>
 * <li>Calling any graph method with a malformed access token produces OAuthException</li>
 * <li>Calling any graph method with an expired access token produces OAuthException</li>
 * </ul>
 * 
 * <p>In general, you should be wary of catching exceptions more specific than OAuthException.</p> 
 * 
 * <p>While the name of this exception is derived from the error produced by the Graph API,
 * BatchFB will throw this exception when the Old REST API produces an "equivalent" error.</p>
 * 
 * @author Jeff Schnitzer
 */
public class OAuthException extends FacebookException {
	private static final long serialVersionUID = 1L;
	
	/** Make GWT happy */
	OAuthException() {}

	public OAuthException(String message) {
		super(message);
	}
}