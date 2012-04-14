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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.util.LaterWrapper;

/**
 * Wrapper that converts from a JsonNode to an actual Java object.
 */
public class MapperWrapper<T> extends LaterWrapper<JsonNode, T> {
	JavaType resultType;
	ObjectMapper mapper;
	
	/**
	 * @param base is assumed to produce the real deal, not an error node
	 */
	public MapperWrapper(JavaType resultType, ObjectMapper mapper, Later<JsonNode> base) {
		super(base);
		this.resultType = resultType;
		this.mapper = mapper;
	}
	
	/** Use Jackson to map from JsonNode to the type */
	@Override
	@SuppressWarnings("unchecked")
	protected T convert(JsonNode data) {
		// The (T) cast prevents the commandline javac from choking "no unique maximal instance"
		return (T)this.mapper.convertValue(data, this.resultType);
	}	
}