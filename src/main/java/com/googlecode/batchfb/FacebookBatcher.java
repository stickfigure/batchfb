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

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker.Std;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.googlecode.batchfb.err.IOFacebookException;
import com.googlecode.batchfb.impl.Batch;
import com.googlecode.batchfb.impl.ErrorDetectingWrapper;
import com.googlecode.batchfb.util.CryptoUtils;
import com.googlecode.batchfb.util.Now;
import com.googlecode.batchfb.util.RequestBuilder;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;
import com.googlecode.batchfb.util.RequestBuilder.HttpResponse;
import com.googlecode.batchfb.util.StringUtils;
import com.googlecode.batchfb.util.URLParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Primary implementation of the Batcher interface.
 * 
 * @author Jeff Schnitzer
 */
public class FacebookBatcher implements Batcher {
	
	/** */
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(FacebookBatcher.class.getName());
	
	/** Base URL for the graph api */
	public static final String GRAPH_ENDPOINT = "https://graph.facebook.com/";
	
	/**
	 * Get the app access token from Facebook.
	 * 
	 * see https://developers.facebook.com/docs/authentication/
	 */
	public static String getAppAccessToken(String clientId, String clientSecret) {
		return getAccessToken(clientId, clientSecret, null, null);
	}
	
	/**
	 * Get a user access token from Facebook.  Normally you obtain this from the client-side SDK (javascript, iphone, etc)
	 * but if you are driving the OAuth flow manually, this method is the last step.
	 * 
	 * see https://developers.facebook.com/docs/authentication/
	 */
	public static String getAccessToken(String clientId, String clientSecret, String code, String redirectUri) {
		RequestBuilder call = new RequestBuilder(GRAPH_ENDPOINT + "oauth/access_token", HttpMethod.GET);
		call.setTimeout(10 * 1000);	// this is a somewhat crude hack but seems reasonable right now
		call.addParam("client_id", clientId);
		call.addParam("client_secret", clientSecret);
		if (code != null || redirectUri != null) {
			call.addParam("code", code);
			call.addParam("redirect_uri", redirectUri);
		} else
			call.addParam("grant_type", "client_credentials");
		
		try {
			HttpResponse response = call.execute();
			
			// Yet more Facebook API stupidity; if the response is OK then we parse as urlencoded params,
			// otherwise we must parse as JSON and run through the error detector.
			if (response.getResponseCode() == 200) {
				return URLParser.parseQuery(StringUtils.read(response.getContentStream())).get("access_token");
			} else {
				Later<JsonNode> json = new Now<JsonNode>(new ObjectMapper().readTree(response.getContentStream()));
				new ErrorDetectingWrapper(json).get();	// This should throw an exception
				throw new IllegalStateException("Impossible, this should have been detected as an error: " + json);
			}
		} catch (IOException ex) {
			throw new IOFacebookException(ex);
		}
	}
	
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
	 * The proof that can be passed to FB; null means don't pass it. It is calculated
	 * per https://developers.facebook.com/docs/graph-api/securing-requests
	 * hash_hmac('sha256', $access_token, $app_secret);
	 */
	private String appSecretProof;
	
	/**
	 * Jackson mapper used to translate all JSON to java classes.
	 */
	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * Connection and read timeout for http connections, 0 for no timeout
	 */
	private int timeout = 0;	
	
	/**
	 * Number of retries to execute when a timeout occurs.
	 */
	private int retries = 0;
	
	/**
	 * Maximum size of a single batch.  Facebook's limit is currently 50.
	 */
	private int maxBatchSize = 50;
	
	/**
	 * Active batches
	 */
	private List<Batch> batches = new ArrayList<Batch>();
	
	/**
	 * If we have issued any fql queries they will be on this batch.  It will be one of the batches in
	 * the batches collection.
	 */
	private Batch queryBatch;

//	/**
//	 * Construct a batcher without an access token. All requests will be unauthenticated.
//	 * JMS: This doesn't work because FB requires a token for Batch requests.  Maybe we
//	 * will allow single calls someday, but we will have to optimize out the batch request.
//	 */
//	public FacebookBatcher() {
//		this(null);
//	}

	/**
	 * Construct a batcher with the specified facebook access token. The api version will
	 * be unspecified to facebook.
	 *
	 * @param accessToken is required; you cannot make unauthenticated batch FB requests
	 */
	public FacebookBatcher(String accessToken) {
		this(accessToken, null);
	}

	/**
	 * Construct a batcher with the specified facebook access token and api version.
	 *
	 * @param accessToken is required; you cannot make unauthenticated batch FB requests.
	 * @param apiVersion is the full version string, eg "v2.0". null results in versionless requests.
	 */
	public FacebookBatcher(String accessToken, String apiVersion) {
		this(accessToken, null, apiVersion);
	}

	/**
	 * Construct a batcher with the specified facebook access token and api version.
	 * 
	 * @param accessToken is required; you cannot make unauthenticated batch FB requests.
	 * @param appSecret is your app secret; if present, appsecret_proof will be included with every request. Can be null.
	 * @param apiVersion is the full version string, eg "v2.0". null results in versionless requests.
	 */
	public FacebookBatcher(String accessToken, String appSecret, String apiVersion) {
		this.accessToken = accessToken;
		this.apiVersion = apiVersion;

		if (appSecret != null)
			this.appSecretProof = CryptoUtils.makeAppSecretProof(appSecret, accessToken);
		
		// This allows us to deserialize private fields
		this.mapper.setVisibilityChecker(Std.defaultInstance().withFieldVisibility(Visibility.NON_PRIVATE));
		
		// Shouldn't force users to create classes with all fields
		this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		// We don't want to send null values to FB for things like omit_response_on_success
		this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

		// Facebook uses underscores, not camelcase
		this.mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
	}
	
	/**
	 * Get the Jackson mapper which will be used to transform all JSON responses into objects.
	 * You can change the configuration of this mapper to alter the mapping.
	 */
	public ObjectMapper getMapper() {
		return this.mapper;
	}
	
	/**
	 * Sets the connection timeout in milliseconds.  0 means no timeout.
	 */
	public void setTimeout(int millis) {
		if (!this.batches.isEmpty())
			throw new IllegalStateException("Can't set timeout after batches have been created");
		
		this.timeout = millis;
	}
	
	/**
	 * Gets the connection/read timeout in milliseconds, or 0 for "no timeout".
	 */
	public int getTimeout() {
		return this.timeout;
	}
	
	/**
	 * Sets the number of retries to execute when a timeout occurs.
	 */
	public void setRetries(int count) {
		if (!this.batches.isEmpty())
			throw new IllegalStateException("Can't set retries after batches have been created");
		
		this.retries = count;
	}
	
	/**
	 * Gets the number of retries to execute when a timeout occurs.
	 */
	public int getRetries() {
		return this.retries;
	}
	
	/**
	 * <p>Maximum number of graph requests to put in a single batch.  As you add more things to
	 * a single batch, the time FB takes to return it gets longer.  You must balance
	 * this with timeout and retries to obtain optimum performance and reliability.</p>
	 * 
	 * <p>Default value is the Facebook max, 20.</p>
	 * 
	 * <p>Note that you can have virtually unlimited FQL calls.</p>
	 */
	public void setMaxBatchSize(int max) {
		if (!this.batches.isEmpty())
			throw new IllegalStateException("Can't set max batch size after batches have been created");
		
		this.maxBatchSize = max;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#graph(java.lang.String, java.lang.Class, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> GraphRequest<T> graph(String object, Class<T> type, Param... params) {
		return this.getBatchForGraph().graph(object, type, params);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#graph(java.lang.String, org.codehaus.jackson.type.TypeReference, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> GraphRequest<T> graph(String object, TypeReference<T> type, Param... params) {
		return this.getBatchForGraph().graph(object, type, params);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#graph(java.lang.String, com.googlecode.batchfb.Param[])
	 */
	@Override
	public GraphRequest<JsonNode> graph(String object, Param... params) {
		return this.getBatchForGraph().graph(object, params);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#paged(java.lang.String, java.lang.Class, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> PagedLater<T> paged(String object, Class<T> type, Param... params) {
		return this.getBatchForGraph().paged(object, type, params);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#query(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> QueryRequest<List<T>> query(String fql, Class<T> type)
	{
		return this.getBatchForQuery().query(fql, type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#query(java.lang.String)
	 */
	@Override
	public QueryRequest<ArrayNode> query(String fql)
	{
		return this.getBatchForQuery().query(fql);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#queryFirst(java.lang.String, java.lang.Class)
	 */
	@Override
	public <T> Later<T> queryFirst(String fql, Class<T> type)
	{
		return this.getBatchForQuery().queryFirst(fql, type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#queryFirst(java.lang.String)
	 */
	@Override
	public Later<JsonNode> queryFirst(String fql)
	{
		return this.getBatchForQuery().queryFirst(fql);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#delete(java.lang.String)
	 */
	@Override
	public Later<Boolean> delete(String object)
	{
		return this.getBatchForGraph().delete(object);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#post(java.lang.String, com.googlecode.batchfb.Param[])
	 */
	@Override
	public Later<String> post(String object, Param... params)
	{
		return this.getBatchForGraph().post(object, params);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Batcher#post(java.lang.String, java.lang.Class, com.googlecode.batchfb.Param[])
	 */
	@Override
	public <T> GraphRequest<T> post(String object, Class<T> type, Param... params)
	{
		return this.getBatchForGraph().post(object, type, params);
	}
	
	/**
	 * Executes all existing batches and removes them from consideration for further batching.
	 */
	@Override
	public void execute() {
		if (this.batches.isEmpty())
			return;
		
		// Reset the collection before making the call to eliminate callback chatter when
		// the batches call back to the master.
		List<Batch> old = this.batches;
		
		// Reset our batches
		this.batches = new ArrayList<Batch>();
		this.queryBatch = null;
		
		for (Batch batch: old) {
			batch.execute();
		}
	}
	
	/**
	 * Get an appropriate Batch for issuing a new graph call.  Will construct a new one
	 * if all existing batches are full.
	 */
	private Batch getBatchForGraph() {
		Batch lastValidBatch = this.batches.isEmpty() ? null : this.batches.get(this.batches.size()-1);
		
		if (lastValidBatch != null && lastValidBatch.graphSize() < this.maxBatchSize)
			return lastValidBatch;
		else {
			Batch next = new Batch(this, this.mapper, this.accessToken, this.apiVersion, this.timeout, this.retries);
			this.batches.add(next);
			return next;
		}
	}

	/**
	 * Get an appropriate Batch for issuing a new FQL call.  Will construct a new one
	 * if all existing batches are full. Note that all query() calls occur on the same batch.
	 */
	private Batch getBatchForQuery() {
		if (this.queryBatch == null)
			this.queryBatch = this.getBatchForGraph();
		
		return this.queryBatch;
	}
}