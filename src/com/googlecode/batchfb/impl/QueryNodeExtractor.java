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
import com.googlecode.batchfb.util.LaterWrapper;

/**
 * <p>Knows how to get the JsonNode for a particular query request out of a multiquery.</p>
 * <p>Sadly, this is what a multiquery result looks like - so it's best to use positional indexing:</p>
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
 * <p>More info available here:
 * https://developers.facebook.com/docs/reference/rest/fql.multiquery/
 * </p>
 */
public class QueryNodeExtractor extends LaterWrapper<JsonNode, JsonNode>
{
	int index;

	/**
	 * @param multiqueryResult should be the graph selection of a MultiqueryRequest
	 */
	public QueryNodeExtractor(int index, Later<JsonNode> multiqueryResult)
	{
		super(multiqueryResult);
		
		this.index = index;
	}

	/** */
	@Override
	protected JsonNode convert(JsonNode data)
	{
		if (!(data instanceof ArrayNode))
			throw new IllegalStateException("Expected array node: " + data);

		JsonNode queryPart = data.get(this.index);
		
//		if (!name.equals(queryPart.get("name").getTextValue()))
//			throw new IllegalStateException("Expected to find name of '" + name + "' in " + queryPart);
		
		return queryPart.get("fql_result_set");
	}
}