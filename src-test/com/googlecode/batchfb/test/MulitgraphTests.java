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

/**
 * Unit testing multiple graph calls batched together.
 * 
 * @author Jeff Schnitzer
 */
public class MulitgraphTests extends TestBase {
	
	/** */
	static class Like {
		public String id;
		public String name;
	}
	
	/**
	 */
	@Test
	public void multigraphAsNode() throws Exception {
		Later<JsonNode> mobcast = this.anonBatcher.graph("157841729726");
		Later<JsonNode> inception = this.anonBatcher.graph("110935752279118");
		assert "Mobcast".equals(mobcast.get().get("name").getTextValue());
		assert "Inception (2010)".equals(inception.get().get("name").getTextValue());
	}
	
	/**
	 */
	@Test
	public void multigraphAsObject() throws Exception {
		Later<Like> mobcast = this.anonBatcher.graph("157841729726", Like.class);
		Later<Like> inception = this.anonBatcher.graph("110935752279118", Like.class);
		assert "Mobcast".equals(mobcast.get().name);
		assert "Inception (2010)".equals(inception.get().name);
	}
}