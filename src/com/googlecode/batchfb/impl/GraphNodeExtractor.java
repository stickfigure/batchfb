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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.batchfb.err.BrokenFacebookException;
import com.googlecode.batchfb.util.JSONUtils;
import com.googlecode.batchfb.util.LaterWrapper;

/**
 * <p>Knows how to get the JsonNode for a particular graph request out of a batchResult.
 * The batchResult must look like the result described here:
 * https://developers.facebook.com/docs/api/batch/</p>
 */
public class GraphNodeExtractor extends LaterWrapper<JsonNode, JsonNode>
{
	int index;
	ObjectMapper mapper;

	/** Force the input to be error detected so we always have a valid input */
	public GraphNodeExtractor(int index, ObjectMapper mapper, ErrorDetectingWrapper batchResult)
	{
		super(batchResult);
		
		this.index = index;
		this.mapper = mapper;
	}

	/** */
	@Override
	protected JsonNode convert(JsonNode data)
	{
		if (!(data instanceof ArrayNode))
			throw new IllegalStateException("Expected array node: " + data);

		JsonNode batchPart = ((ArrayNode)data).get(this.index);
		
		if (batchPart == null || batchPart.isNull())
			throw new BrokenFacebookException("Facebook returned an invalid batch response. There should not be a null at index " + index + " of this array: " + data);
		
		// This should be something like:
		// {
		//   "code": 200,
		//   "headers": [ { "name":"Content-Type", "value":"text/javascript; charset=UTF-8" } ],
		//   "body":"{\"id\":\"asdf\"}"
		// },
		
		JsonNode body = batchPart.get("body");
		if (body == null || body.isNull())
			return null;
		else
			return JSONUtils.toNode(body.textValue(), mapper);
	}
}