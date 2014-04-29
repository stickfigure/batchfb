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

import java.net.URL;
import java.net.URLEncoder;

import com.googlecode.batchfb.test.util.TestBase;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.batchfb.Later;

/**
 * Testing a report that sometimes query results return the wrong size.
 * 
 * @author Jeff Schnitzer
 */
public class QueryResultSizeTest extends TestBase {

	/**
	 */
	@Test
	public void basicQuery() throws Exception {
		this.ensureQueryIsCorrectSize("SELECT aid FROM album WHERE owner = me()");
	}
	
	/**
	 * This query takes a while to run
	 */
	@Test
	public void biggerQuery() throws Exception {
		this.ensureQueryIsCorrectSize("SELECT aid, owner, cover_pid, created, name, description, size, type FROM album WHERE owner IN (SELECT uid2 FROM friend WHERE uid1 = me()) OR owner = me()");
	}
	
	/**
	 * Run the query through BatchFB and by hand and ensure the result set is same size.
	 */
	private void ensureQueryIsCorrectSize(String query) throws Exception {
		String url = "https://api.facebook.com/method/fql.query?format=json&query=" + URLEncoder.encode(query, "utf-8")
				+ "&access_token=" + ACCESS_TOKEN;
		URL manual = new URL(url);
		System.out.println("Manual URL is: " + url);
		JsonNode manualNodes = new ObjectMapper().readTree(manual.openStream());
		assert manualNodes instanceof ArrayNode;
		
		Later<ArrayNode> nodes = this.authBatcher.query(query);
		System.out.println("Query obtained " + nodes.get().size() + " items");
		
		assert manualNodes.size() == nodes.get().size();
	}
}