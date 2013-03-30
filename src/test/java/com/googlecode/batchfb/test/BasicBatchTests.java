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

import java.util.List;

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.batchfb.Later;

/**
 * Basic unit tests for the batching features
 * 
 * @author Jeff Schnitzer
 */
public class BasicBatchTests extends TestBase {
	
	/** */
	static class User {
		public String name;
	}
	
	/**
	 */
	@Test
	public void singleGraphRequestAsNode() throws Exception {
		Later<JsonNode> node = this.authBatcher.graph("/1047296661");
		assert "Robert Dobbs".equals(node.get().get("name").textValue());
	}
	
	/**
	 */
	@Test
	public void singleGraphRequestAsObject() throws Exception {
		Later<User> user = this.authBatcher.graph("/1047296661", User.class);
		assert "Robert Dobbs".equals(user.get().name);
	}

	/**
	 */
	@Test
	public void singleFqlAsNode() throws Exception {
		Later<ArrayNode> array = this.authBatcher.query("SELECT name FROM user WHERE uid = 1047296661");
		assert 1 == array.get().size();
		assert "Robert Dobbs".equals(array.get().get(0).get("name").textValue());
	}
	
	/**
	 */
	@Test
	public void singleFqlAsNodeUsingQueryFirst() throws Exception {
		Later<JsonNode> node = this.authBatcher.queryFirst("SELECT name FROM user WHERE uid = 1047296661");
		assert "Robert Dobbs".equals(node.get().get("name").textValue());
	}
	
	/**
	 */
	@Test
	public void singleFqlAsObject() throws Exception {
		Later<List<User>> array = this.authBatcher.query("SELECT name FROM user WHERE uid = 1047296661", User.class);
		assert 1 == array.get().size();
		assert "Robert Dobbs".equals(array.get().get(0).name);
	}
	
	/**
	 */
	@Test
	public void singleFqlAsObjectUsingQueryFirst() throws Exception {
		Later<User> array = this.authBatcher.queryFirst("SELECT name FROM user WHERE uid = 1047296661", User.class);
		assert "Robert Dobbs".equals(array.get().name);
	}
}