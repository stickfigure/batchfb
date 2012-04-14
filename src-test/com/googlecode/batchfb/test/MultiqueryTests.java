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

import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.QueryRequest;

/**
 * Testing out batching of multiqueries.
 * 
 * @author Jeff Schnitzer
 */
public class MultiqueryTests extends TestBase {

	/**
	 */
	@Test
	public void basicMultiquery() throws Exception {
		Later<ArrayNode> firstNameArray = this.authBatcher.query("SELECT first_name FROM user WHERE uid = 1047296661");
		Later<ArrayNode> lastNameArray = this.authBatcher.query("SELECT last_name FROM user WHERE uid = 1047296661");
		
		assert 1 == firstNameArray.get().size();
		assert 1 == lastNameArray.get().size();
		assert "Robert".equals(firstNameArray.get().get(0).get("first_name").textValue());
		assert "Dobbs".equals(lastNameArray.get().get(0).get("last_name").textValue());
	}

	/**
	 * What happened was the order went q1, q10, q11, q2, q3 and thus fucked it up.  This should
	 * now work based on proper query name lookup.
	 */
	@Test
	public void moreThanTenQueries() throws Exception {
		Later<JsonNode> firstName = this.authBatcher.queryFirst("SELECT first_name FROM user WHERE uid = 1047296661");
		Later<JsonNode> lastName = this.authBatcher.queryFirst("SELECT last_name FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		this.authBatcher.queryFirst("SELECT pic_square FROM user WHERE uid = 1047296661");
		
		assert "Robert".equals(firstName.get().get("first_name").textValue());
		assert "Dobbs".equals(lastName.get().get("last_name").textValue());
	}

	/**
	 * Make sure we can explicitly name queries
	 */
	@Test
	public void namedQueries() throws Exception {
		QueryRequest<ArrayNode> firstNameArray = this.authBatcher.query("SELECT first_name FROM user WHERE uid = 1047296661");
		firstNameArray.setName("foo");
		QueryRequest<ArrayNode> lastNameArray = this.authBatcher.query("SELECT last_name FROM user WHERE uid = 1047296661");
		lastNameArray.setName("bar");
		
		assert 1 == firstNameArray.get().size();
		assert 1 == lastNameArray.get().size();
		assert "Robert".equals(firstNameArray.get().get(0).get("first_name").textValue());
		assert "Dobbs".equals(lastNameArray.get().get(0).get("last_name").textValue());
	}
}