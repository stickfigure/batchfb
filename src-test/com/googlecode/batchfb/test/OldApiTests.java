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
import org.junit.Assert;
import org.junit.Test;

import com.googlecode.batchfb.FacebookBatcher;
import com.googlecode.batchfb.FacebookException;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.Param;

/**
 * Basic unit tests for the batching features
 * 
 * @author Jeff Schnitzer
 */
public class OldApiTests {
	
	/**
	 */
	@Test
	public void simpleRequestAsNode() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher();

		Later<JsonNode> node = batcher.oldApi("fql.query", new Param("query", "SELECT uid FROM user WHERE uid = 1047296661"));
		Assert.assertTrue(node.get().isArray());
		Assert.assertEquals(1, node.get().size());
		Assert.assertEquals(1047296661, node.get().get(0).get("uid").getIntValue());
	}
	
	/**
	 */
	@Test
	public void twoRequestsBatched() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher();

		Later<JsonNode> bob = batcher.oldApi("fql.query", new Param("query", "SELECT uid FROM user WHERE uid = 1047296661"));
		
		// This method requires an API key so it will fail
		Later<JsonNode> bob2 = batcher.oldApi("friends.get", new Param("uid", 1047296661));

		// This should successfully return
		bob.get();
		
		// This should throw an exception
		try {
			bob2.get();
		} catch (FacebookException ex) {
			// Do nothing
		}
	}
}