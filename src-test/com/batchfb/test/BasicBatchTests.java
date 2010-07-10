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

package com.batchfb.test;

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Assert;
import org.junit.Test;

import com.googlecode.batchfb.FacebookBatcher;
import com.googlecode.batchfb.Later;

/**
 * Basic unit tests for the batching features
 * 
 * @author Jeff Schnitzer
 */
public class BasicBatchTests {
	
	/** */
	static class User {
		public String name;
	}
	
	/**
	 */
	@Test
	public void singleFqlAsNode() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher();
		
		Later<ArrayNode> array = batcher.query("SELECT name FROM user WHERE uid = 1047296661");
		Assert.assertEquals(1, array.get().size());
		Assert.assertEquals("Robert Dobbs", array.get().get(0).get("name").getTextValue());
	}
	
	/**
	 */
	@Test
	public void singleFqlAsObject() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher();
		
		Later<List<User>> array = batcher.query("SELECT name FROM user WHERE uid = 1047296661", User.class);
		Assert.assertEquals(1, array.get().size());
		Assert.assertEquals("Robert Dobbs", array.get().get(0).name);
	}
	
	/**
	 */
	@Test
	public void singleRequestAsNode() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher();
		
		Later<JsonNode> node = batcher.request("/1047296661");
		Assert.assertEquals("Robert Dobbs", node.get().get("name").getTextValue());
	}
	
	/**
	 */
	@Test
	public void singleRequestAsObject() throws Exception {
		FacebookBatcher batcher = new FacebookBatcher();
		
		Later<User> user = batcher.request("/1047296661", User.class);
		Assert.assertEquals("Robert Dobbs", user.get().name);
	}
}