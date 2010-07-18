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

package com.googlecode.batchfb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.logging.Logger;

import com.googlecode.batchfb.FacebookBatcher.GraphRequest;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;

/**
 * <p>
 * Manages the grouping of graph requests.  Groups requests that can be safely batched
 * together in a single Facebook call - that is, requests with the same method and params.
 * This allows us to batch graph calls into a single ids=123,456,789 type call.
 * </p>
 * <p>
 * Note also that connection requests (ie, me/feed) must always group by themselves.
 * </p>
 * 
 * @author Jeff Schnitzer
 */
class GraphRequestGrouper implements Iterable<LinkedList<GraphRequest<?>>> {
	
	/** */
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(GraphRequestGrouper.class.getName());
	
	/** Java threading rules allow us to atomically increment this */
	private static int uniqueValue = 0;

	/**
	 * Holds a queue of all graph requests to execute.  Note that these are all
	 * grouped by a stable key version of the params so that graph requests
	 * that have matching parameters can be batched together.  The key should
	 * be created with createStableKey().
	 */
	private LinkedHashMap<Object, LinkedList<GraphRequest<?>>> requests = new LinkedHashMap<Object, LinkedList<GraphRequest<?>>>();
	
	/** 
	 * Adds the graph request to the proper group based on method and params. 
	 */
	public void add(GraphRequest<?> request) {
		Object stableKey = this.createStableGroupKey(request);
		
		LinkedList<GraphRequest<?>> queue = this.requests.get(stableKey);
		if (queue == null) {
			queue = new LinkedList<GraphRequest<?>>();
			this.requests.put(stableKey, queue);
		}
		
		queue.add(request);
	}
	
	/**
	 * Provides an iterator across the groups.  The iterator can remove() whole groups.
	 */
	public Iterator<LinkedList<GraphRequest<?>>> iterator() {
		return this.requests.values().iterator();
	}
	
	/**
	 * <p>Creates a key which defines groupings of graph requests that can be
	 * batched together.  Graph requests can be grouped when they have the
	 * same parameters (order-independent).</p>
	 * 
	 * <p>Cannot be batched: Methods other than GET, and connection requests
	 * (eg me/feed). If facebook changes this, just modify this code.</p>
	 */
	private Object createStableGroupKey(GraphRequest<?> req) {
		
		if (req.method != HttpMethod.GET || req.object.contains("/")) {
			// This trick ensures the request will never group with any others.
			return uniqueValue++;
		} else {
			// A HashMap which includes the names/values of the parameters should work;
			// the contract for equals() and hashCode() are stable.
			HashMap<Object, Object> result = new HashMap<Object, Object>();
			
			for (Param param: req.params)
				result.put(param.name, param.value.toString());

			return result;
		}
	}
}