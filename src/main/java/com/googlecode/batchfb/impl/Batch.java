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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.batchfb.Batcher;
import com.googlecode.batchfb.BinaryParam;
import com.googlecode.batchfb.FacebookBatcher;
import com.googlecode.batchfb.GraphRequest;
import com.googlecode.batchfb.GraphRequestBase;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.PagedLater;
import com.googlecode.batchfb.Param;
import com.googlecode.batchfb.QueryRequest;
import com.googlecode.batchfb.err.FacebookException;
import com.googlecode.batchfb.err.IOFacebookException;
import com.googlecode.batchfb.type.Paged;
import com.googlecode.batchfb.util.FirstElementLater;
import com.googlecode.batchfb.util.FirstNodeLater;
import com.googlecode.batchfb.util.GraphRequestBuilder;
import com.googlecode.batchfb.util.JSONUtils;
import com.googlecode.batchfb.util.LaterWrapper;
import com.googlecode.batchfb.util.RequestBuilder;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;
import com.googlecode.batchfb.util.RequestBuilder.HttpResponse;
import com.googlecode.batchfb.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Everything that can be done in a single Batch request.</p>
 * 
 * <p>There are three states to a batch:</p>
 * <ul>
 * <li>It can be waiting for additional requests to be added</li>
 * <li>It can be fetching asynchronously (no more requests allowed)</li>
 * <li>It can have all the data available</li>
 * </ul>
 * 
 * @author Jeff Schnitzer
 */
public class Batch implements Batcher, Later<JsonNode> {
	
	/** */
	private static final Logger log = Logger.getLogger(Batch.class.getName());
	
	/**
	 * Required facebook access token
	 */
	private String accessToken;

	/**
	 * Facebook api version, eg "v2.0". If null, submits a versionless request.
	 * See https://developers.facebook.com/docs/apps/upgrading/
	 */
	private String apiVersion;

	/**
	 * Jackson mapper used to translate all JSON to java classes.
	 */
	private ObjectMapper mapper;
	
	/**
	 * Executed whenever we execute so that the master knows to kick off other batches
	 * and remove us from consideration for further work.  Also a place we can issue
	 * fresh requests post-execution to make paging work.
	 */
	private Batcher master;
	
	/**
	 * Holds (and groups properly) all the graph requests.
	 */
	private LinkedList<GraphRequestBase<?>> graphRequests = new LinkedList<GraphRequestBase<?>>();
	
	/**
	 * Holds all queries to execute.  Will be null if there are none, and when created, this
	 * gets added to the graphRequests collection as well.
	 */
	private MultiqueryRequest multiqueryRequest;
	
	/**
	 * When generating query names, use this as an index.
	 */
	int generatedQueryNameIndex;
	
	/**
	 * Connection and read timeout for http connections, 0 for no timeout
	 */
	private int timeout = 0;	
	
	/**
	 * Number of retries to execute when a timeout occurs.
	 */
	private int retries = 0;
	
	/**
	 * When the query is launched, this holds the entire result of the batch call.
	 * If this batch is still pending, this will be null.
	 */
	private Later<JsonNode> rawBatchResult;
	
	/**
	 * Construct a batch with the specified facebook access token.
	 * 
	 * @param master is our parent batcher, probably the FacebookBatcher
	 * @param accessToken can be null to make unauthenticated FB requests
	 */
	public Batch(Batcher master, ObjectMapper mapper, String accessToken, String apiVersion, int timeout, int retries) {
		this.master = master;
		this.mapper = mapper;
		this.accessToken = accessToken;
		this.apiVersion = apiVersion;
		this.timeout = timeout;
		this.retries = retries;
	}
	
	/**
	 * @return the number of graph calls currently enqueued.
	 */
	public int graphSize() {
		return this.graphRequests.size();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#graph(java.lang.String, java.lang.Class, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> GraphRequest<T> graph(String object, Class<T> type, Param... params) {
		return this.graph(object, mapper.getTypeFactory().constructType(type), params);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#graph(java.lang.String, org.codehaus.jackson.type.TypeReference, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> GraphRequest<T> graph(String object, TypeReference<T> type, Param... params) {
		return this.graph(object, mapper.getTypeFactory().constructType(type), params);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#graph(java.lang.String, com.googlecode.batchfb.Param[])
	 */
	@Override
	public GraphRequest<JsonNode> graph(String object, Param... params) {
		return this.graph(object, JsonNode.class, params);
	}
	
	/**
	 * The actual implementation of this, after we've converted to proper Jackson JavaType
	 */
	private <T> GraphRequest<T> graph(String object, JavaType type, Param... params) {
		this.checkForBatchExecution();
		
		// The data is transformed through a chain of wrappers
		GraphRequest<T> req =
			new GraphRequest<T>(object, params, this.mapper, this.<T>createMappingChain(type));
		
		this.graphRequests.add(req);
		return req;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#paged(java.lang.String, java.lang.Class, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> PagedLater<T> paged(String object, Class<T> type, Param... params) {
		if (!object.contains("/"))
			throw new IllegalArgumentException("You can only use paged() for connection requests, eg me/friends");

		// For example if type is User.class, this will produce Paged<User>
		JavaType pagedType = mapper.getTypeFactory().constructParametricType(Paged.class, mapper.getTypeFactory().constructType(type));
			
		GraphRequest<Paged<T>> req = this.graph(object, pagedType, params);
			
		return new PagedLaterAdapter<T>(this.master, req, type);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#query(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> QueryRequest<List<T>> query(String fql, Class<T> type) {
		return this.query(fql, mapper.getTypeFactory().constructCollectionType(ArrayList.class, type));
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#query(java.lang.String)
	 */
	@Override
	public QueryRequest<ArrayNode> query(String fql) {
		return this.query(fql, mapper.getTypeFactory().constructType(ArrayNode.class));
	}
	
	/**
	 * Implementation now that we have chosen a Jackson JavaType for the return value
	 */
	private <T> QueryRequest<T> query(String fql, JavaType type) {
		this.checkForBatchExecution();
		
		if (this.multiqueryRequest == null) {
			this.multiqueryRequest = new MultiqueryRequest(mapper, this.createUnmappedChain());
			this.graphRequests.add(this.multiqueryRequest);
		}
		
		// There is a circular reference between the extractor and request, so construction of the chain
		// is a little complicated
		QueryNodeExtractor extractor = new QueryNodeExtractor(this.multiqueryRequest);
		
		String name = "__q" + this.generatedQueryNameIndex++;
		QueryRequest<T> q =
			new QueryRequest<T>(fql, name,
				new MapperWrapper<T>(type, this.mapper,
						extractor));
		
		extractor.setRequest(q);
		
		this.multiqueryRequest.addQuery(q);
		return q;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#queryFirst(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> Later<T> queryFirst(String fql, Class<T> type) {
		Later<List<T>> q = this.query(fql, type);
		return new FirstElementLater<T>(q);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#queryFirst(java.lang.String)
	 */
	@Override
	public Later<JsonNode> queryFirst(String fql) {
		Later<ArrayNode> q = this.query(fql);
		return new FirstNodeLater(q);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#delete(java.lang.String)
	 */
	@Override
	public Later<Boolean> delete(String object) {
		this.checkForBatchExecution();
		
		// Something is fucked up with java's ability to perform DELETE.  FB's servers always return
		// 400 Bad Request even though the code is correct.  We will switch all deletes to posts.
		//GraphRequest<Boolean> req = new GraphRequest<Boolean>(object, HttpMethod.DELETE, mapper.getTypeFactory().constructType(Boolean.class), new Param[0]);

		GraphRequest<Boolean> req =
			new GraphRequest<Boolean>(object, HttpMethod.POST, new Param[] { new Param("method", "DELETE") }, this.mapper,
				this.<Boolean>createMappingChain(mapper.getTypeFactory().constructType(Boolean.class)));
		
		this.graphRequests.add(req);
		return req;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#post(java.lang.String, com.googlecode.batchfb.Param[])
	 */
	@Override
	public Later<String> post(String object, Param... params) {
		this.checkForBatchExecution();
		
		final GraphRequest<JsonNode> req =
			new GraphRequest<JsonNode>(object, HttpMethod.POST, params, this.mapper, this.<JsonNode>createMappingChain(mapper.constructType(JsonNode.class)));

		this.graphRequests.add(req);
		return new Later<String>() {
			@Override
			public String get() throws FacebookException
			{
				return req.get().path("id").asText();
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#post(java.lang.String, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> GraphRequest<T> post(String object, Class<T> type, Param... params) {
		this.checkForBatchExecution();

		// The data is transformed through a chain of wrappers
		GraphRequest<T> req =
			new GraphRequest<T>(object, HttpMethod.POST, params, this.mapper, this.<T>createMappingChain(mapper.getTypeFactory().constructType(type)));
		
		this.graphRequests.add(req);
		return req;
	}
	
	/**
	 * Adds mapping to the basic unmapped chain.
	 */
	private <T> MapperWrapper<T> createMappingChain(JavaType type) {
		return new MapperWrapper<T>(type, this.mapper, this.createUnmappedChain());
	}
	
	/**
	 * Creates the common chain of wrappers that will select out one graph request
	 * from the batch and error check it both at the batch level and at the individual
	 * request level.  Result will be an unmapped JsonNode.
	 */
	private ErrorDetectingWrapper createUnmappedChain() {
		int nextIndex = this.graphRequests.size();

		return
			new ErrorDetectingWrapper(
				new GraphNodeExtractor(nextIndex, this.mapper,
					new ErrorDetectingWrapper(this)));
	}
	
	/**
	 * @throws IllegalStateException if the batch has already been executed
	 */
	private void checkForBatchExecution() {
		if (this.rawBatchResult != null)
			throw new IllegalStateException("You cannot add requests to a batch that has been executed");
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#execute()
	 */
	@Override
	public void execute() {
		if (!this.graphRequests.isEmpty())
			this.getRawBatchResult();
	}
	
	/**
	 * Get the batch result, firing it off if necessary
	 */
	private Later<JsonNode> getRawBatchResult() {
		if (this.rawBatchResult == null) {
			// Use LaterWrapper to cache the result so we don't fetch over and over
			this.rawBatchResult = new LaterWrapper<JsonNode, JsonNode>(this.createFetcher());
			
			// Also let the master know it's time to kick off any other batches and
			// remove us as a valid batch to add to.
			// This must be called *after* the rawBatchResult is set otherwise we
			// will have endless recursion when the master tries to execute us.
			this.master.execute();
		}
		
		return this.rawBatchResult;
	}
	
	/**
	 * The Batch itself is a Later<JsonNode> that will return the raw batch result.  We hide
	 * the actual batching behind this method.
	 */
	@Override
	public JsonNode get() throws FacebookException {
		return this.getRawBatchResult().get();
	}
	
	/**
	 * Constructs the batch query and executes it, possibly asynchronously.
	 * @return an asynchronous handle to the raw batch result, whatever it may be.
	 */
	private Later<JsonNode> createFetcher() {
		final RequestBuilder call = new GraphRequestBuilder(getGraphEndpoint(), HttpMethod.POST, this.timeout, this.retries);
		
		// This actually creates the correct JSON structure as an array
		String batchValue = JSONUtils.toJSON(this.graphRequests, this.mapper);
		if (log.isLoggable(Level.FINEST))
			log.finest("Batch request is: " + batchValue);

		this.addParams(call, new Param[] { new Param("batch", batchValue) });
		
		final HttpResponse response;
		try {
			response = call.execute();
		} catch (IOException ex) {
			throw new IOFacebookException(ex);
		}
		
		return new Later<JsonNode>() {
			@Override
			public JsonNode get() throws FacebookException
			{
				try {
					if (response.getResponseCode() == HttpURLConnection.HTTP_OK
							|| response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST
							|| response.getResponseCode() == HttpURLConnection.HTTP_UNAUTHORIZED) {
						
						// If it was an error, we will recognize it in the content later.
						// It's possible we should capture all 4XX codes here.
						JsonNode result = mapper.readTree(response.getContentStream());
						
						if (log.isLoggable(Level.FINEST))
							log.finest("Response is: " + result);
						
						return result;
					} else {
						throw new IOFacebookException(
								"Unrecognized error " + response.getResponseCode() + " from "
								+ call + " :: " + StringUtils.read(response.getContentStream()));
					}
				} catch (IOException e) {
					throw new IOFacebookException("Error calling " + call, e);
				}
			}
		};
	}

	/**
	 * Adds the appropriate parameters to the call, including boilerplate ones
	 * (access token, format).
	 * @param params can be null or empty
	 */
	private void addParams(RequestBuilder call, Param[] params) {
		
		// Once upon a time this was necessary, now it isn't
		//call.addParam("format", "json");
		
		if (this.accessToken != null)
			call.addParam("access_token", this.accessToken);

		if (params != null) {
			for (Param param: params) {
				if (param instanceof BinaryParam) {
					call.addParam(param.name, (InputStream)param.value, ((BinaryParam)param).contentType, "irrelevant");
				} else {
					String paramValue = StringUtils.stringifyValue(param, this.mapper);
					call.addParam(param.name, paramValue);
				}
			}
		}
	}

	/**
	 * @return the facebook graph endpoint base, with the optional api version.
	 */
	private String getGraphEndpoint() {
		if (apiVersion == null)
			return FacebookBatcher.GRAPH_ENDPOINT;
		else
			return FacebookBatcher.GRAPH_ENDPOINT + apiVersion + "/";
	}
}