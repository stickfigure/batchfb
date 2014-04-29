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

package com.googlecode.batchfb.test;

import com.googlecode.batchfb.test.util.TestBase;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.batchfb.FacebookBatcher;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.err.OAuthException;
import com.googlecode.batchfb.err.PageMigratedException;

/**
 * Tests of the exceptions generated.
 * 
 * @author Jeff Schnitzer
 */
public class ExceptionTest extends TestBase {
	/**
	 * Use an invalid token to generate an OAuthException
	 */
	@Test(expectedExceptions=OAuthException.class)
	public void makeOAuthException() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher("asdf");

		Later<JsonNode> node = batcher.graph("/me");
		node.get();
	}

//	/**
//	 * Make a call to /me without a token
//	 */
//	@Test(expectedExceptions=OAuthException.class)
//	public void makeQueryParseException() throws Exception {
//		Later<JsonNode> node = this.anonBatcher.graph("/me");
//		node.get();
//	}
//
//	/**
//	 * Make a token-less call to something that requires a token.
//	 */
//	@Test(expectedExceptions=OAuthException.class)
//	public void makeOAuthAccessTokenException() throws Exception {
//		Later<JsonNode> node = this.anonBatcher.graph("/markzuckerberg/friends");
//		node.get();
//	}

	/**
	 * The Swimming page migrated to a different id
	 */
	@Test
	public void makePageMigratedException() throws Exception {
		try {
			Later<JsonNode> node = this.authBatcher.graph("/114267748588304");
			node.get();
			assert false;	// should never get here
		} catch (PageMigratedException ex) {
			assert ex.getOldId() == 114267748588304L;
			assert ex.getNewId() == 111013272313096L;
		}
	}
}