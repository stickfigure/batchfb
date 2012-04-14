package com.googlecode.batchfb.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.batchfb.GraphRequestBase;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.Param;
import com.googlecode.batchfb.QueryRequest;
import com.googlecode.batchfb.util.JSONUtils;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;

/**
 * <p>Aggregates a set of queries into a single multiquery.  This is a graph request,
 * but different from the ones that users will create.</p>
 */
public class MultiqueryRequest extends GraphRequestBase<JsonNode> {
	
	/** */
	List<QueryRequest<?>> queryRequests = new ArrayList<QueryRequest<?>>();
	
	/** */
	public MultiqueryRequest(ObjectMapper mapper, Later<JsonNode> source) {
		super("method/fql.multiquery", HttpMethod.GET, mapper, source);
	}
	
	/** */
	public void addQuery(QueryRequest<?> req) {
		this.queryRequests.add(req);
	}
	
	/** Generate the parameters approprate to a multiquery from the registered queries */
	@Override
	@JsonIgnore
	protected Param[] getParams() {
		Map<String, String> queries = new LinkedHashMap<String, String>();
		for (QueryRequest<?> req: this.queryRequests)
			queries.put(req.getName(), req.getFQL());
		
		String json = JSONUtils.toJSON(queries, this.mapper);
		
		return new Param[] { new Param("queries", json) };
	}

	/** @return the current number of queries registered */
	public int numQueries()
	{
		return this.queryRequests.size();
	}
}