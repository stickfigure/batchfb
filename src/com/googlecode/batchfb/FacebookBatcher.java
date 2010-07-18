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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.DeserializationConfig.Feature;
import org.codehaus.jackson.map.introspect.VisibilityChecker.Std;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import com.googlecode.batchfb.err.FacebookException;
import com.googlecode.batchfb.err.IOFacebookException;
import com.googlecode.batchfb.err.OAuthException;
import com.googlecode.batchfb.err.PermissionException;
import com.googlecode.batchfb.err.QueryParseException;
import com.googlecode.batchfb.type.Paged;
import com.googlecode.batchfb.util.FirstElementLater;
import com.googlecode.batchfb.util.FirstNodeLater;
import com.googlecode.batchfb.util.JSONUtils;
import com.googlecode.batchfb.util.RequestBuilder;
import com.googlecode.batchfb.util.StringUtils;
import com.googlecode.batchfb.util.URLParser;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;

/**
 * <p>
 * Interface to the Facebook APIs which allows you to define all of your requests
 * (graph, fql, and old rest) in advance and execute them in an optimal set of actual
 * http calls to Facebook.  Normal result values are mapped using Jackson; error
 * results are unified into a standard exception hierarchy.
 * </p>
 * 
 * <p>Three types of batching are currently supported:</p>
 * 
 * <ul>
 * <li>Graph API calls with a common http method (ie GET vs POST) and identical parameters
 * are batched into a single graph call with ids=id1,id2,id3.</li>
 * <li>FQL calls are batched into a single multiquery.</li>
 * <li>Old REST API calls (including the query/multiquery) are batched into a single
 * batch.run call.</li>
 * </ul>
 * 
 * <p>See the <a href="http://code.google.com/p/batchfb/wiki/UserGuide">User Guide</a>
 * for more information about how to use this class.</p>
 * 
 * @author Jeff Schnitzer
 */
public class FacebookBatcher {
	
	/** */
	private static final Logger log = Logger.getLogger(FacebookBatcher.class.getName());
	
	/** */
	public static final String GRAPH_ENDPOINT = "https://graph.facebook.com/";
	public static final String OLD_REST_ENDPOINT = "https://api.facebook.com/method/";
	
	/** */
	private static final JavaType JSON_NODE_TYPE = TypeFactory.type(JsonNode.class);
	
	/** The response will either be a valid value or an error */
	static class Response<T> {
		T result;
		RuntimeException error;
	}
	
	/** */
	class Request<T> implements Later<T> {
		JavaType resultType;
		Response<T> response;
		
		public Request(JavaType resultType) {
			this.resultType = resultType;
		}
		
		public T get() throws FacebookException {
			if (this.response == null)
				execute();
			
			if (this.response.error != null)
				throw this.response.error;
			else
				return this.response.result;
		}
	}
	
	/** */
	class GraphRequest<T> extends Request<T> {
		String object;
		HttpMethod method;
		Param[] params;
		
		public GraphRequest(String object, JavaType resultType, Param[] params) {
			this(object, HttpMethod.GET, resultType, params);
		}
		
		/** Strips off any leading / from object */
		public GraphRequest(String object, HttpMethod method, JavaType resultType, Param[] params) {
			super(resultType);
			this.object = object.startsWith("/") ? object.substring(1) : object;
			this.method = method;
			this.params = params;
		}
	}
	
	/** */
	class Query<T> extends Request<T> {
		String fql;
		String name;
		
		public Query(String fql, String name, JavaType resultType) {
			super(resultType);
			this.fql = fql;
			this.name = (name != null) ? name : "_q" + generatedQueryNameIndex++;
		}
	}
	
	/** */
	class OldRequest<T> extends Request<T> {
		String methodName;
		Param[] params;
		
		public OldRequest(String methodName, JavaType resultType, Param[] params) {
			super(resultType);
			this.methodName = methodName;
			this.params = params;
		}
	}
	
	/** Provides paging ability */
	class PagedLaterAdapter<T> implements PagedLater<T> {
		GraphRequest<Paged<T>> request;
		
		/** The type of T **/
		JavaType type;

		/**
		 * @param req is the request to wrap
		 * @param type is the type of T, the thing we are paging across (ie not Paged<T>)
		 */
		public PagedLaterAdapter(GraphRequest<Paged<T>> req, JavaType type) {
			this.request = req;
			this.type = type;
		}
		
		@Override
		public List<T> get() throws FacebookException {
			return this.request.get().getData();
		}

		@Override
		public PagedLater<T> next()
		{
			if (this.request.get().getPaging() == null)
				return null;
			else
				return this.createRequest(this.request.get().getPaging().getNext());
		}

		@Override
		public PagedLater<T> previous()
		{
			if (this.request.get().getPaging() == null)
				return null;
			else
				return this.createRequest(this.request.get().getPaging().getPrevious());
		}
		
		/**
		 * Unfortunately we need to parse the url to create a new GraphRequest<?>.
		 * It's not strictly necessary; we could create a new type of GraphRequest that
		 * merely issues the http request as-is, but this would eliminate any future
		 * option of grouping these requests.  You can't group connection requests
		 * right now but maybe that will change in the future.  Also, this keeps the
		 * rest of the code much simpler.
		 * 
		 * @param pagedUrl is a paging url, either next or previous
		 */
		private PagedLater<T> createRequest(String pagedUrl)
		{
			// Parse the url to create a new GraphRequest<Paged<T>>.
			URLParser parser = new URLParser(pagedUrl);
			
			// Need to remove the access token, that gets added back later and isn't
			// relevant for grouping.
			parser.getParams().remove("access_token");
			
			return paged(parser.getPath(), this.type, parser.getParamsAsArray());
		}
	}
	
	/** */
	/**
	 * Optional facebook access token
	 */
	private String accessToken;
	
	/**
	 * Jackson mapper used to translate all JSON to java classes.
	 */
	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * Holds a queue of all queries to execute.
	 */
	private LinkedList<Query<?>> queries = new LinkedList<Query<?>>();

	/**
	 * Holds (and groups properly) all the graph requests.
	 */
	private GraphRequestGrouper graphRequests = new GraphRequestGrouper();
	
	/**
	 * Holds a queue of the old rest api requests.
	 */
	private LinkedList<OldRequest<?>> oldRequests = new LinkedList<OldRequest<?>>();
	
	/**
	 * When generating query names, use this as an index.
	 */
	private int generatedQueryNameIndex;
	
	/**
	 * Construct a batcher without an access token. All requests will be unauthenticated.
	 */
	public FacebookBatcher() {
		this(null);
	}
	
	/**
	 * Construct a batcher with the specified facebook access token.
	 * 
	 * @param accessToken can be null to make unauthenticated FB requests
	 */
	public FacebookBatcher(String accessToken) {
		this.accessToken = accessToken;
		
		// This allows us to deserialize private fields
		this.mapper.setVisibilityChecker(Std.defaultInstance().withFieldVisibility(Visibility.ANY));
		
		// Shouldn't force users to create classes with all fields
		this.mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	
	/**
	 * Get the Jackson mapper which will be used to transform all JSON responses into objects.
	 * You can change the configuration of this mapper to alter the mapping.
	 */
	public ObjectMapper getMapper() {
		return this.mapper;
	}
	
	/**
	 * Enqueue a Graph API call. The result will be mapped into the specified class.
	 * 
	 * @param object is the object to request, eg "me" or "1234". Doesn't need to start with "/".
	 * @param type is the type to map the result to
	 * @param params are optional parameters to pass to the method.
	 */
	public <T> Later<T> graph(String object, Class<T> type, Param... params) {
		return this.graph(object, TypeFactory.type(type), params);
	}
	
	/**
	 * Enqueue a Graph API call. The result will be mapped into the specified type, which can be a generic collection.
	 * 
	 * @param object is the object to request, eg "me" or "1234". Doesn't need to start with "/".
	 * @param type is the Jackson type reference to map the result to (see the BatchFB UserGuide).
	 * @param params are optional parameters to pass to the method.
	 */
	public <T> Later<T> graph(String object, TypeReference<T> type, Param... params) {
		return this.graph(object, TypeFactory.type(type), params);
	}
	
	/**
	 * Enqueue a Graph API call. The result will be left as a raw Jackson node type and will not be interpreted as a Java class.
	 * 
	 * @param object is the object to request, ie "me" or "1234". Doesn't need to start with "/".
	 * @param params are optional parameters to pass to the method.
	 */
	public Later<JsonNode> graph(String object, Param... params) {
		return this.graph(object, JSON_NODE_TYPE, params);
	}
	
	/**
	 * The actual implementation of this, after we've converted to proper Jackson JavaType
	 */
	private <T> Later<T> graph(String object, JavaType type, Param... params) {
		GraphRequest<T> req = new GraphRequest<T>(object, type, params);
		this.graphRequests.add(req);
		return req;
	}
	
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
	public <T> PagedLater<T> paged(String object, Class<T> type, Param... params) {
		return this.paged(object, TypeFactory.type(type), params);
	}
	
	/**
	 * Implementation of this after we have T converted to a proper Jackson JavaType.
	 */
	private <T> PagedLater<T> paged(String object, JavaType type, Param... params) {
		if (!object.contains("/"))
			throw new IllegalArgumentException("You can only use paged() for connection requests, eg me/friends");

		// For example if type is User.class, this will produce Paged<User>
		JavaType pagedType = TypeFactory.parametricType(Paged.class, type);
			
		GraphRequest<Paged<T>> req = new GraphRequest<Paged<T>>(object, pagedType, params);
		this.graphRequests.add(req);
		return new PagedLaterAdapter<T>(req, type);
	}
	
	/**
	 * Enqueue a FQL call. The result will be interpreted as a list of the specified java class.
	 * 
	 * @param fql is the query to run, which can include previously named query results
	 * @param type is what the contents of the resulting list will be mapped to
	 */
	public <T> Later<List<T>> query(String fql, Class<T> type) {
		return this.query(fql, type, null);
	}
	
	/**
	 * Enqueue a FQL call. The result will be left as a raw Jackson array node.
	 * 
	 * @param fql is the query to run, which can include previously named query results
	 */
	public Later<ArrayNode> query(String fql) {
		return this.query(fql, (String)null);
	}
	
	/**
	 * Enqueue a named FQL call. The name allows query results to be used as a parameter to
	 * further queries as described in the Facebook documentation for
	 * <a href="http://developers.facebook.com/docs/reference/rest/fql.multiquery">fql.multiquery</a>
	 * 
	 * Note: The name can only be used in further queries that are part of the same batch.
	 * 
	 * @param fql is the query to run, which can include previously named query results
	 * @param type is what the contents of the resulting list will be mapped to
	 * @param queryName must be a unique name for the batch, used as a reference for other
	 *  queries in the batch.
	 */
	public <T> Later<List<T>> query(String fql, Class<T> type, String queryName) {
		return this.query(fql, queryName, TypeFactory.collectionType(ArrayList.class, type));
	}
	
	/**
	 * Enqueue a named FQL call. The name allows query results to be used as a parameter to
	 * further queries as described in the Facebook documentation for
	 * <a href="http://developers.facebook.com/docs/reference/rest/fql.multiquery">fql.multiquery</a>
	 * The result will be left as a Jackson array node.
	 * 
	 * Note: The name can only be used in further queries that are part of the same batch.
	 * 
	 * @param fql is the query to run, which can include previously named query results
	 * @param queryName must be a unique name for the batch, used as a reference for other
	 *  queries in the batch.
	 */
	public Later<ArrayNode> query(String fql, String queryName) {
		return this.query(fql, queryName, TypeFactory.type(ArrayNode.class));
	}
	
	/**
	 * Implementation now that we have chosen a Jackson JavaType for the return value
	 */
	private <T> Later<T> query(String fql, String queryName, JavaType type) {
		Query<T> q = new Query<T>(fql, queryName, type);
		this.queries.add(q);
		return q;
	}
	
	/**
	 * Just like query(), but retrieves the first value from the result set.  If the result set
	 * is empty, the Later<?>.get() value will be null.
	 */
	public <T> Later<T> queryFirst(String fql, Class<T> type) {
		Later<List<T>> q = this.query(fql, type);
		return new FirstElementLater<T>(q);
	}
	
	/**
	 * Just like query(), but retrieves the first value from the result set.  If the result set
	 * is empty, the Later<?>.get() value will be null.
	 */
	public Later<JsonNode> queryFirst(String fql) {
		Later<ArrayNode> q = this.query(fql);
		return new FirstNodeLater(q);
	}
	
	/**
	 * Enqueue a delete call to the Graph API. Note that delete calls cannot be batched with other calls
	 * and will always result in a separate HTTP request.
	 */
	public Later<Boolean> delete(String object) {
		GraphRequest<Boolean> req = new GraphRequest<Boolean>(object, HttpMethod.DELETE, TypeFactory.type(Boolean.class), new Param[0]);
		this.graphRequests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a post (publish) call to the Graph API. Note that post calls cannot be batched with other
	 * calls and will always result in a separate HTTP request.
	 * 
	 * @param params can include a BinaryParam to post binary objects.
	 */
	public Later<String> post(String object, Param... params) {
		GraphRequest<String> req = new GraphRequest<String>(object, HttpMethod.POST, TypeFactory.type(String.class), params);
		this.graphRequests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a call to the old REST API.
	 * 
	 * @param methodName is the name of the method, eg "status.get"
	 */
	public <T> Later<T> oldRest(String methodName, Class<T> type, Param... params) {
		return this.oldRest(methodName, TypeFactory.type(type), params);
	}
	
	/**
	 * Enqueue a call to the old REST API.
	 * 
	 * @param methodName is the name of the method, eg "status.get"
	 */
	public <T> Later<T> oldRest(String methodName, TypeReference<T> type, Param... params) {
		return this.oldRest(methodName, TypeFactory.type(type), params);
	}
	
	/**
	 * Enqueue a call to the old REST API.
	 * 
	 * @param methodName is the name of the method, eg "status.get"
	 */
	public Later<JsonNode> oldRest(String methodName, Param... params) {
		return this.oldRest(methodName, JSON_NODE_TYPE, params);
	}
	
	/**
	 * Implementation after we have discovered Jackson JavaType.
	 */
	private <T> Later<T> oldRest(String methodName, JavaType type, Param... params) {
		OldRequest<T> req = new OldRequest<T>(methodName, type, params);
		this.oldRequests.add(req);
		return req;
	}

	/**
	 * Immediately executes all queued calls with as much batching as possible.
	 * Normally there is no need to call this method explicitly; execution is
	 * triggered automatically when the first Later<?>.get() call is made.
	 * 
	 * If no calls are queued, nothing happens.
	 */
	@SuppressWarnings("unchecked")
	public void execute() {
		
		// There is a lot of room for optimization and parallelization here.
		// All queries are combined into a single multiquery.
		// All legacy queries (including fql) are combined in a single batch.run
		// All graph requests with common params and http method can be combined
		// TODO: execute all requests asynchronously in parallel
		
		// Handle graph requests in groups
		Iterator<LinkedList<GraphRequest<?>>> graphRequestGroupIt = this.graphRequests.iterator();
		while (graphRequestGroupIt.hasNext()) {
			LinkedList<GraphRequest<?>> group = graphRequestGroupIt.next();
			this.executeGraphGroup(group);
			graphRequestGroupIt.remove();
		}
		
		// If we have any queries, we create a fake OldRequest to process in batch with any real OldRequests
		OldRequest<?> queryRequest = null;
		
		if (this.queries.size() == 1) {
			Query<?> query = this.queries.getFirst();
			queryRequest = new OldRequest<Object>("fql.query", query.resultType, new Param[] { new Param("query", query.fql) });
		}
		else if (this.queries.size() > 1) {
			// First we need to generate the query name/fql map
			Map<String, String> multi = new LinkedHashMap<String, String>();
			for (Query<?> query: this.queries)
				multi.put(query.name, query.fql);
			
			String queries = JSONUtils.toJSON(multi, this.mapper);
			
			queryRequest = new OldRequest<Object>("fql.multiquery", JSON_NODE_TYPE, new Param[] { new Param("queries", queries) });
		}
		
		// Now execute any OldRequests, including the fake queryRequest (if one exists)
		if (queryRequest != null)
			this.oldRequests.add(queryRequest);
		
		if (!this.oldRequests.isEmpty()) {
			this.execute(this.oldRequests);
			
			// We're totally done with these so we can remove them
			this.oldRequests.clear();
		}
		
		// Last thing we need to do is map the queryResult back onto the original query objects
		if (queryRequest != null) {
			if (queryRequest.response.error != null) {
				
				for (Query<?> query: this.queries) {
					// Java generics suck
					((Query<Object>)query).response = (Response<Object>)queryRequest.response;
				}
				
			} else if (this.queries.size() == 1) {
				
				Query<Object> query = (Query<Object>)this.queries.getFirst();
				query.response = ((OldRequest<Object>)queryRequest).response;
				
			} else if (this.queries.size() > 1) {
				
				// The result is an incredibly stupid format, so we must change it. It looks like this:
				// [{"name":"_q0","fql_result_set":[{"first_name":"Robert"}]},{"name":"_q1","fql_result_set":[{"last_name":"Dobbs"}]}]
				Map<String, JsonNode> resultMap = new HashMap<String, JsonNode>();
				for (JsonNode entryNode: (JsonNode)queryRequest.response.result) {
					String name = entryNode.path("name").getTextValue();
					JsonNode namedResult = entryNode.path("fql_result_set");
					resultMap.put(name, namedResult);
				}
				
				// Now we match up the queries with their results
				Iterator<Query<?>> queryIt = this.queries.iterator();
				while (queryIt.hasNext()) {
					Query<?> query = queryIt.next();
					JsonNode queryResultNode = resultMap.get(query.name);
					((Query<Object>)query).response = new Response<Object>();
					((Response<Object>)query.response).result = this.mapper.convertValue(queryResultNode, query.resultType);
				}
			}
			
			this.queries.clear();
		}
	}
	
	/**
	 * Executes a properly grouped set of graph requests as a single facebook call.
	 * @param group must all have the same http method and params 
	 */
	@SuppressWarnings("unchecked")
	private void executeGraphGroup(LinkedList<GraphRequest<?>> group) {
		if (group.size() == 1) {
			// This is easy enough we should just special-case it and remove the
			// overhead of generating the intermediate JsonNode graph.
			this.executeSingle(group.getFirst());
		} else {
			// The http method and params will be the same for all, so use the first
			GraphRequest<?> first = group.getFirst();
			
			RequestBuilder call = new RequestBuilder(GRAPH_ENDPOINT, first.method);
			
			// We add the generated ids first because of the case where the user chose
			// to specify all the ids as a Param explicitly.  If that happens, the
			// generated ids field will be bogus, but it will be overwritten when the
			// explicit params are added.  It is a little odd and it seems like allowing
			// users to specify ids as a param is a bad idea, but it all works out so WTH.
			call.addParam("ids", this.createIdsString(group));
			this.addParams(call, first.params);
			
			Response<JsonNode> response = this.fetchGraph(call, JSON_NODE_TYPE);
			if (response.error != null) {
				for (GraphRequest<?> req: group)
					((GraphRequest)req).response = response;
			} else {
				for (GraphRequest<?> req: group) {
					((GraphRequest)req).response = new Response<Object>();
					((GraphRequest)req).response.result = this.mapper.convertValue(response.result, req.resultType);
				}
			}
		}
	}
	
	/**
	 * Creates the ids= parameter for a group of graph requests.
	 */
	private String createIdsString(Iterable<GraphRequest<?>> group) {
		StringBuilder bld = null;
		
		for (GraphRequest<?> req: group) {
			if (bld == null)
				bld = new StringBuilder();
			else
				bld.append(',');
			
			bld.append(req.object);
		}
		
		return bld.toString();
	}
	
	/**
	 * Executes a single graph request as a standalone request and stores the result in itself.
	 */
	private void executeSingle(GraphRequest<?> req) {
		RequestBuilder call = new RequestBuilder(GRAPH_ENDPOINT + req.object, req.method);
		this.addParams(call, req.params);
		req.response = this.fetchGraph(call, req.resultType);
	}
	
	/**
	 * Executes the specified old request
	 */
	private void execute(OldRequest<?> req) {
		
		RequestBuilder call = new RequestBuilder(OLD_REST_ENDPOINT + req.methodName, HttpMethod.GET);
		
		this.addParams(call, req.params);
		
		req.response = this.fetchOld(call, req.resultType);
	}
	
	/**
	 * Executes the specified old requests, using batch.run if there are more than one
	 */
	@SuppressWarnings("unchecked")
	private void execute(List<OldRequest<?>> requests) {
		if (requests.size() == 1) {
			this.execute(requests.get(0));
		} else {
			// Make this into a batch.run
			List<String> encoded = new ArrayList<String>(requests.size());
			
			for (OldRequest<?> req: requests) {
				StringBuilder bld = new StringBuilder();
				bld.append("method=").append(req.methodName);
				
				for (Param param: req.params) {
					bld.append('&');
					bld.append(StringUtils.urlEncode(param.name));
					bld.append('=');
					bld.append(StringUtils.urlEncode(param.value.toString()));
				}
				
				encoded.add(bld.toString());
			}
			
			String jsonArray = JSONUtils.toJSON(encoded, this.mapper);
			
			OldRequest<List<String>> batchRequest = new OldRequest<List<String>>("batch.run", TypeFactory.type(List.class), new Param[] { new Param("method_feed", jsonArray)});
			this.execute(batchRequest);
			
			if (batchRequest.response.error != null) {
				// Give the same error response to all
				Response<Object> resp = new Response<Object>();
				resp.error = batchRequest.response.error;
				for (OldRequest<?> request: requests) {
					((OldRequest<Object>)request).response = resp;
				}
			} else {
				// Now we need to extract the data, it comes as a set of strings (yes, JSON in strings)
				// in the same order as the requests.  This is what a friends.get looks like:
				// [ "[212730,431332,710904]" ]
				// Note also that errors can show up here, so they must be checked.
				
				Iterator<OldRequest<?>> requestIt = requests.iterator();
				Iterator<String> responseIt = batchRequest.response.result.iterator();
				
				while (requestIt.hasNext()) {
					OldRequest<Object> req = (OldRequest<Object>)requestIt.next();
					String responseString = responseIt.next();
					
					req.response = new Response<Object>();
					
					try {
						JsonNode root = this.mapper.readTree(responseString);
						req.response.error = this.checkForOldApiError(root);
						if (req.response.error != null)
							req.response.result = this.mapper.convertValue(root, req.resultType);
						
					} catch (IOException e) {
						req.response.error = new IOFacebookException(e);
					}
				}
			}
		}
	}
	
	/**
	 * Stringify the parameter value in an appropriate way. Note that Facebook fucks up dates by using unix time-since-epoch some places and ISO-8601
	 * others. However, maybe unix times always work as parameters?
	 */
	private String stringifyValue(Param param) {
		assert !(param instanceof BinaryParam);
		
		if (param.value instanceof String)
			return (String)param.value;
		if (param.value instanceof Date)
			return Long.toString(((Date)param.value).getTime() / 1000);
		else if (param.value instanceof Number)
			return param.value.toString();
		else
			return JSONUtils.toJSON(param.value, this.mapper);
	}
	
	/**
	 * Adds the appropriate parameters to the call, including boilerplate ones
	 * (access token, format).
	 * @param params can be null or empty
	 */
	private void addParams(RequestBuilder call, Param[] params) {
		
		call.addParam("format", "json");
		
		if (this.accessToken != null)
			call.addParam("access_token", this.accessToken);

		if (params != null) {
			for (Param param: params) {
				if (param instanceof BinaryParam) {
					call.addParam(param.name, (InputStream)param.value, ((BinaryParam)param).contentType, "irrelevant");
				} else {
					String paramValue = this.stringifyValue(param);
					call.addParam(param.name, paramValue);
				}
			}
		}
	}
	
	/**
	 * Performs a fetch to the facebook graph API, converting the result into the expected type.
	 * If an error occurs, an exception will be thrown.  Not to be used with calls to the old
	 * REST API, which has a different error handling mechanism.
	 */
	@SuppressWarnings("unchecked")
	private <T> Response<T> fetchGraph(RequestBuilder call, JavaType resultType) {
		Response<T> response = new Response<T>();
		
		try {
			if (log.isLoggable(Level.FINEST))
				log.finest("Fetching: " + call);
			
			HttpURLConnection conn = (HttpURLConnection)call.execute();
			
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				// Weird, javac (from ant) needs this cast but Eclipse doesn't. 
				response.result = (T)this.mapper.readValue(conn.getInputStream(), resultType);
			}
			else if (conn.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST
					|| conn.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
			{
				JsonNode node = this.mapper.readTree(conn.getErrorStream());
				response.error = this.createGraphException(node);
			}
			else
			{
				response.error = new IOFacebookException("Got error " + conn.getResponseCode() + " '" + conn.getResponseMessage() + "' from " + call);
			}
			
		} catch (IOException e) {
			response.error = new IOFacebookException("Error calling " + call, e);
		}
		
		return response;
	}
	
	/**
	 * Takes a JSON error result from a Graph API request and returns the correct kind of error,
	 * whatever that happens to be. It tries to match the type of the exception with an actual
	 * exception class of the correct name.
	 * 
	 * If the node doesn't have a normal "error" field, an IOFacebookException is retrned.  Something
	 * unexpected is wrong.
	 */
	private FacebookException createGraphException(JsonNode node) {
		JsonNode errorNode = node.get("error");
		if (errorNode == null) {
			return new IOFacebookException("Incomprehensible error response " + node.toString());
		} else {
			String type = errorNode.path("type").getValueAsText();
			String msg = errorNode.path("message").getValueAsText();
			
			// Special case, permission exceptions are poorly structured
			if (type.equals("Exception") && msg.startsWith("(#200)"))
				return new PermissionException(msg);
			
			// We check to see if we have an exception that matches the type, otherwise
			// we simply throw the base FormalFacebookException
			String proposedExceptionType = this.getClass().getPackage().getName() + ".err." + type;
			
			try {
				Class<?> exceptionClass = Class.forName(proposedExceptionType);
				Constructor<?> ctor = exceptionClass.getConstructor(String.class);
				return (FacebookException)ctor.newInstance(msg);
			} catch (Exception e) {
				// Do nothing, throwMe will stay null
			}
			
			return new FacebookException(msg);
		}
	}

	/**
	 * Performs a fetch to the Old REST API, converting the result into the expected type.
	 * If an error occurs, an exception matching the graph API exceptions will be thrown.
	 * This mapping to graph exceptions is more art than science.
	 * 
	 * This method must not be used with Graph API requests, which have a different error system.
	 */
	@SuppressWarnings("unchecked")
	private <T> Response<T> fetchOld(RequestBuilder call, JavaType resultType) {
		Response<T> response = new Response<T>();
		
		try {
			if (log.isLoggable(Level.FINEST))
				log.finest("Fetching: " + call);
			
			HttpURLConnection conn = (HttpURLConnection)call.execute();
			
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				// We must examine the result for an error node since there is no other clue
				JsonNode node = this.mapper.readTree(conn.getInputStream());
				
				response.error = this.checkForOldApiError(node);
				if (response.error == null)
					response.result = (T)this.mapper.convertValue(node, resultType);
			} else {
				response.error = new IOFacebookException("Got error " + conn.getResponseCode() + " '" + conn.getResponseMessage() + "' from " + call);
			}
			
		} catch (IOException e) {
			response.error = new IOFacebookException("Error calling " + call, e);
		}
		
		return response;
	}

	/**
	 * Checks the tree of an old API call for errors, returning an appropriately mapped
	 * exception if one is found.  Returns null if the node is not an error node.
	 * 
	 * This relies heavily on http://wiki.developers.facebook.com/index.php/Error_codes
	 */
	private FacebookException checkForOldApiError(JsonNode root) {
		JsonNode errorCode = root.get("error_code");
		
		if (errorCode != null) {
			int code = errorCode.getIntValue();
			String msg = root.path("error_msg").getValueAsText();

			switch (code) {
				case 101:
				case 102:
				case 190: return new OAuthException(msg);
				
				default:
					if (code >= 200 && code < 300)
						return new PermissionException(msg);
					else if (code >= 600 && code < 700)
						return new QueryParseException(msg);
					else
						return new FacebookException(msg);
			}
		}
		
		return null;
	}
}