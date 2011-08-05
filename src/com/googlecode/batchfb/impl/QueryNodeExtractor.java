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

package com.googlecode.batchfb.impl;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.Request;
import com.googlecode.batchfb.util.LaterWrapper;

/**
 * <p>Knows how to get the JsonNode for a particular query request out of a multiquery.</p>
 * 
 * <p>Sadly, this is what a multiquery result looks like:</p>
<pre>
[
  {
    "name": "query1",
    "fql_result_set": [ { "uid": 503702723 } ]
  },
  {
    "name": "query2",
    "fql_result_set": [ { "uid": 503702723 } ]
  }
]
</pre>
 * <p>We'd like to use positional indexing but facebook reorders the results.  So we basically need to
 * scan through and look for our node.  LAME.</p>
 * 
 * <p>Because we need to know the name, the Request<?> (which holds the final name) must be set
 * after this object is constructed but before it is executed.  It can't be passed in on construction
 * because the QueryNodeExtractor gets passed in (through a chain of wrappers) to the request.
 * 
 * <p>More info available here:
 * https://developers.facebook.com/docs/reference/rest/fql.multiquery/
 * </p>
 */
public class QueryNodeExtractor extends LaterWrapper<JsonNode, JsonNode>
{
	Request<?> request;

	/**
	 * @param multiqueryResult should be the graph selection of a MultiqueryRequest
	 */
	public QueryNodeExtractor(Later<JsonNode> multiqueryResult) {
		super(multiqueryResult);
	}
	
	/**
	 * Sets the request object related to this query.  The request provides the name
	 * that we use to extract the result.  This creates a convoluted construction
	 * process because the Request will actually contain a reference (buried in a chain
	 * of wrappers) to this QueryNodeExtractor.  This method must be called before
	 * execution. 
	 */
	public void setRequest(Request<?> req) {
		this.request = req;
	}

	/** */
	@Override
	protected JsonNode convert(JsonNode data) {
		
		if (!(data instanceof ArrayNode))
			throw new IllegalStateException("Expected array node: " + data);
		
		// If you have an NPE here it means you didn't initialize the Request properly!
		String name = this.request.getName();

		for (int i=0; i<data.size(); i++) {
			JsonNode candidate = data.get(i);
			
			if (name.equals(candidate.path("name").getTextValue()))
				return candidate.get("fql_result_set");
		}
		
		throw new IllegalStateException("Didn't find query named '" + name + "' in query results");
	}
}