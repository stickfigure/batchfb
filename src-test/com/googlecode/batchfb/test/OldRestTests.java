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

import org.codehaus.jackson.JsonNode;
import org.testng.annotations.Test;

import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.Param;

/**
 * Basic unit tests for the batching features
 * 
 * @author Jeff Schnitzer
 */
public class OldRestTests extends TestBase {
	
	/**
	 */
	@Test
	public void simpleRequestAsNode() throws Exception {
		Later<JsonNode> node = this.authBatcher.oldRest("fql.query", new Param("query", "SELECT uid FROM user WHERE uid = 1047296661"));
		assert node.get().isArray();
		assert 1 == node.get().size();
		assert 1047296661 == node.get().get(0).get("uid").getIntValue();
	}
	
	/**
	 */
	@Test(expectedExceptions=IllegalStateException.class)
	public void tryWithoutAccessToken() throws Exception {
		this.anonBatcher.oldRest("fql.query", new Param("query", "SELECT uid FROM user WHERE uid = 1047296661"));
	}
	
	/**
	 * Ensure that it works.
	 */
	@Test
	public void twoRequestsBatched() throws Exception {
		Later<JsonNode> bob = this.authBatcher.oldRest("fql.query", new Param("query", "SELECT uid FROM user WHERE uid = 1047296661"));
		
		// This method requires an API key so it will fail
		Later<JsonNode> jeff = this.authBatcher.oldRest("friends.get", new Param("uid", 503702723));

		// This should successfully return
		assert bob.get().get(0).get("uid").getValueAsText().equals("1047296661");
		assert jeff.get().isArray();
		assert jeff.get().size() > 0;
	}
}