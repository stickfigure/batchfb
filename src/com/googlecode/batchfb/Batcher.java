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

import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.TypeReference;

/**
 * <p>
 * Interface to the Facebook APIs which allows you to define multiple requests
 * (graph and fql) in advance and execute them in an optimal set of actual
 * http calls to Facebook.  Normal result values are mapped using Jackson; error
 * results are unified into a standard exception hierarchy.
 * </p>
 * 
 * <p>Two types of batching are currently supported:</p>
 * 
 * <ul>
 * <li>Graph requests are batched using the Graph Batch mechanism.</li>
 * <li>FQL calls are batched into a single multiquery and then merged with the graph batch (if any).</li>
 * </ul>
 * 
 * <p>Batches are limited to groups of 20.  Any requests that exceed this
 * number are executed in multiple requests.  If your environment supports it (eg appengine),
 * these requests will be executed concurrently.</p> 
 * 
 * <p>See the <a href="http://code.google.com/p/batchfb/wiki/UserGuide">User Guide</a>
 * for more information about how to use this class.</p>
 * 
 * @author Jeff Schnitzer
 */
public interface Batcher {
	
	/**
	 * Enqueue a Graph API call. The result will be mapped into the specified class.
	 * 
	 * @param object is the object to request, eg "me" or "1234". Doesn't need to start with "/".
	 * @param type is the type to map the result to
	 * @param params are optional parameters to pass to the method.
	 */
	public <T> GraphRequest<T> graph(String object, Class<T> type, Param... params);
	
	/**
	 * Enqueue a Graph API call. The result will be mapped into the specified type, which can be a generic collection.
	 * 
	 * @param object is the object to request, eg "me" or "1234". Doesn't need to start with "/".
	 * @param type is the Jackson type reference to map the result to (see the BatchFB UserGuide).
	 * @param params are optional parameters to pass to the method.
	 */
	public <T> GraphRequest<T> graph(String object, TypeReference<T> type, Param... params);
	
	/**
	 * Enqueue a Graph API call. The result will be left as a raw Jackson node type and will not be interpreted as a Java class.
	 * 
	 * @param object is the object to request, ie "me" or "1234". Doesn't need to start with "/".
	 * @param params are optional parameters to pass to the method.
	 */
	public GraphRequest<JsonNode> graph(String object, Param... params);
	
	/**
	 * <p>Enqueue a Graph API call to an endpoint that results in paginated data.  Any of the
	 * Facebook "connections" fit this pattern; the results look like:</p>
	 * 
	 * {@code
	 * { data:[{...},{...}], paging:{previous:"http://blahblah", next:"http://blahblah"} }
	 * }
	 * 
	 * The PagedLater<?> returned from this method can be used to navigate forwards and
	 * backwards in the pagination.  For example, you could have a PagedLater<User> that
	 * provides the list of User objects plus additional PagedLater<User> for the next
	 * and previous pages.
	 * 
	 * @param object is the connection object to request, eg "me/friends" or "1234/feed". Doesn't need to start with "/".
	 * @param type is the type of the element that will be paged across
	 * @param params are optional parameters to pass to the method.
	 */
	public <T> PagedLater<T> paged(String object, Class<T> type, Param... params);
	
	/**
	 * Enqueue a FQL call. The result will be interpreted as a list of the specified java class.
	 * 
	 * @param fql is the query to run, which can include previously named query results
	 * @param type is what the contents of the resulting list will be mapped to
	 */
	public <T> QueryRequest<List<T>> query(String fql, Class<T> type);
	
	/**
	 * Enqueue a FQL call. The result will be left as a raw Jackson array node.
	 * 
	 * @param fql is the query to run, which can include previously named query results
	 */
	public QueryRequest<ArrayNode> query(String fql);
	
	/**
	 * Just like query(), but retrieves the first value from the result set.  If the result set
	 * is empty, the Later<?>.get() value will be null.
	 * 
	 * @return a later upon which you cannot set a name
	 */
	public <T> Later<T> queryFirst(String fql, Class<T> type);
	
	/**
	 * Just like query(), but retrieves the first value from the result set.  If the result set
	 * is empty, the Later<?>.get() value will be null.
	 * 
	 * @return a later upon which you cannot set a name
	 */
	public Later<JsonNode> queryFirst(String fql);
	
	/**
	 * Enqueue a delete call to the Graph API.
	 */
	public Later<Boolean> delete(String object);
	
	/**
	 * Enqueue a post (publish) call to the Graph API.
	 * 
	 * @param params can include a BinaryParam to post binary objects.
	 */
	public Later<String> post(String object, Param... params);
	
	/**
	 * Immediately start execution of the batch, asynchronously if possible.
	 * Normally there is no need to call this method explicitly; execution is
	 * triggered automatically when the first Later<?>.get() call is made.
	 * 
	 * If no calls are queued, nothing happens.
	 * If batch was already executed, nothing happens.
	 */
	public void execute();
}