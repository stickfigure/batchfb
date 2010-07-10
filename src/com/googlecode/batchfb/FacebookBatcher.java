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
import com.googlecode.batchfb.err.QueryParseException;
import com.googlecode.batchfb.util.JSONUtils;
import com.googlecode.batchfb.util.RequestBuilder;
import com.googlecode.batchfb.util.StringUtils;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;

/**
 * <p>
 * Low-ish level class which interacts with the Facebook Graph API, batching requests into a minimal number of separate FB calls and using Jackson to
 * parse the result into user-friendly Java classes.
 * </p>
 * 
 * @author Jeff Schnitzer
 */
public class FacebookBatcher {
	
	/** */
	private static final Logger log = Logger.getLogger(FacebookBatcher.class.getName());
	
	/** */
	private static final JavaType JSON_NODE_TYPE = TypeFactory.type(JsonNode.class);
	
	/** The response will either be a valid value or an error */
	static class Response<T> {
		T result;
		RuntimeException error;
	}
	
	/** */
	class Command<T> implements Later<T> {
		JavaType resultType;
		Response<T> response;
		
		public Command(JavaType resultType) {
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
	class Request<T> extends Command<T> {
		String object;
		HttpMethod method;
		Param[] params;
		
		public Request(String object, JavaType resultType, Param[] params) {
			this(object, HttpMethod.GET, resultType, params);
		}
		
		public Request(String object, HttpMethod method, JavaType resultType, Param[] params) {
			super(resultType);
			this.object = object.startsWith("/") ? object : "/" + object;
			this.method = method;
			this.params = params;
		}
	}
	
	/** */
	class Query<T> extends Command<T> {
		String fql;
		String name;
		
		public Query(String fql, String name, JavaType resultType) {
			super(resultType);
			this.fql = fql;
			this.name = (name != null) ? name : "_q" + generatedQueryNameIndex++;
		}
	}
	
	/** */
	class OldRequest<T> extends Command<T> {
		String methodName;
		Param[] params;
		
		public OldRequest(String methodName, JavaType resultType, Param[] params) {
			super(resultType);
			this.methodName = methodName;
			this.params = params;
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
	 * Holds a queue of all requests to execute.
	 */
	private LinkedList<Request<?>> requests = new LinkedList<Request<?>>();
	
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
	 * Get the Jackson mapper which will be used to transform all JSON responses into objects. You can change the configuration of this mapper to
	 * alter the mapping.
	 */
	public ObjectMapper getMapper() {
		return this.mapper;
	}
	
	/**
	 * Enqueue a normal graph call. The result will be mapped into the specified class.
	 */
	public <T> Later<T> request(String object, Class<T> type, Param... params) {
		Request<T> req = new Request<T>(object, TypeFactory.type(type), params);
		this.requests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a normal graph call. The result will be mapped into the specified type, which can be a generic collection.
	 */
	public <T> Later<T> request(String object, TypeReference<T> type, Param... params) {
		Request<T> req = new Request<T>(object, TypeFactory.type(type), params);
		this.requests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a normal graph call. The result will be left as a raw Jackson node type and will not be interpreted as a Java class.
	 */
	public Later<JsonNode> request(String object, Param... params) {
		return this.request(object, JsonNode.class, params);
	}
	
	/**
	 * Enqueue an FQL call. All FQL calls will be grouped together into a single multiquery. The result will be interpreted as a list of the specified
	 * java class.
	 */
	public <T> Later<List<T>> query(String fql, Class<T> type) {
		return this.query(fql, type, null);
	}
	
	/**
	 * Enqueue an FQL call. All FQL calls will be grouped together into a single multiquery. The result will be left as a raw Jackson array node.
	 */
	public Later<ArrayNode> query(String fql) {
		return this.query(fql, (String)null);
	}
	
	/**
	 * Enqueue a named FQL call. The name can be used for processing in the FQL for further queries. See the Facebook documentation for multiquery for
	 * more information.
	 * 
	 * Note all FQL calls will be grouped together into a single multiquery.
	 */
	public <T> Later<List<T>> query(String fql, Class<T> type, String queryName) {
		Query<List<T>> q = new Query<List<T>>(fql, queryName, TypeFactory.collectionType(ArrayList.class, type));
		this.queries.add(q);
		return q;
	}
	
	/**
	 * Enqueue a named FQL call. The name can be used for processing in the FQL for further queries. See the Facebook documentation for multiquery for
	 * more information.
	 * 
	 * Note all FQL calls will be grouped together into a single multiquery.
	 */
	public Later<ArrayNode> query(String fql, String queryName) {
		Query<ArrayNode> q = new Query<ArrayNode>(fql, queryName, TypeFactory.type(ArrayNode.class));
		this.queries.add(q);
		return q;
	}
	
	/**
	 * Enqueue a delete call. Note that delete calls cannot be batched with other calls and will always result in a separate HTTP request.
	 */
	public Later<Boolean> delete(String object) {
		Request<Boolean> req = new Request<Boolean>(object, HttpMethod.DELETE, TypeFactory.type(Boolean.class), new Param[0]);
		this.requests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a post (publish) call. Note that post calls cannot be batched with other calls and will always result in a separate HTTP request.
	 */
	public Later<String> post(String object, Param... params) {
		Request<String> req = new Request<String>(object, HttpMethod.POST, TypeFactory.type(String.class), params);
		this.requests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a request to the old REST API.
	 */
	public <T> Later<T> oldApi(String methodName, Class<T> type, Param... params) {
		OldRequest<T> req = new OldRequest<T>(methodName, TypeFactory.type(type), params);
		this.oldRequests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a request to the old REST API.
	 */
	public <T> Later<T> oldApi(String methodName, TypeReference<T> type, Param... params) {
		OldRequest<T> req = new OldRequest<T>(methodName, TypeFactory.type(type), params);
		this.oldRequests.add(req);
		return req;
	}
	
	/**
	 * Enqueue a request to the old REST API.
	 */
	public Later<JsonNode> oldApi(String methodName, Param... params) {
		return this.oldApi(methodName, JsonNode.class, params);
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
		// TODO: all single-id requests with the same params set can be combined
		// TODO: execute all requests asynchronously in parallel
		
		// For now, handle graph requests one at a time
		while (!this.requests.isEmpty()) {
			Request<?> req = this.requests.removeFirst();
			this.execute(req);
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
					query.response.result = this.mapper.convertValue(queryResultNode, query.resultType);
				}
			}
			
			this.queries.clear();
		}
	}
	
	/**
	 * Executes the specified request and stores the result in itself.
	 */
	private void execute(Request<?> req) {
		RequestBuilder call = new RequestBuilder("https://graph.facebook.com" + req.object, req.method);
		
		this.addParams(call, req.params);
		
		req.response = this.fetchGraph(call, req.resultType);
	}
	
	/**
	 * Executes the specified old request
	 */
	private void execute(OldRequest<?> req) {
		
		RequestBuilder call = new RequestBuilder("https://api.facebook.com/method/" + req.methodName, HttpMethod.GET);
		
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
	private <T> Response<T> fetchGraph(RequestBuilder call, JavaType resultType) {
		Response<T> response = new Response<T>();
		
		try {
			if (log.isLoggable(Level.FINEST))
				log.finest("Fetching: " + call);
			
			HttpURLConnection conn = (HttpURLConnection)call.execute();
			
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
			{
				response.result = this.mapper.readValue(conn.getInputStream(), resultType);
			}
			else if (conn.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST)
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
			
			// We check to see if we have an exception that matches the type, otherwise
			// we simply throw the base FormalFacebookException
			String proposedExceptionType = this.getClass().getPackage().getName() + "." + type;
			FacebookException throwMe = null;
			
			try {
				Class<?> exceptionClass = Class.forName(proposedExceptionType);
				Constructor<?> ctor = exceptionClass.getConstructor(String.class);
				throwMe = (FacebookException)ctor.newInstance(msg);
			} catch (Exception e) {
				// Do nothing, throwMe will stay null
			}
			
			if (throwMe != null)
				return throwMe;
			else
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
					response.result = this.mapper.convertValue(node, resultType);
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
	 */
	private FacebookException checkForOldApiError(JsonNode root) {
		JsonNode errorCode = root.get("error_code");
		
		if (errorCode != null) {
			int code = errorCode.getIntValue();
			String msg = root.path("error_msg").getValueAsText();

			switch (code) {
				case 190: return new OAuthException(msg);
				case 601: return new QueryParseException(msg);
				default: return new FacebookException(msg);
			}
		}
		
		return null;
	}
}