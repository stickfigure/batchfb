package com.googlecode.batchfb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;

/**
 * <p>Represents one graph request queued by the user.</p>
 */
public class GraphRequest<T> extends GraphRequestBase<T> {
	@JsonIgnore
	Param[] params;
	
	/** Strips off any leading / from object */
	public GraphRequest(String object, Param[] params, ObjectMapper mapper, Later<T> source) {
		this(object, HttpMethod.GET, params, mapper, source);
	}
	
	/** Strips off any leading / from object */
	public GraphRequest(String object, HttpMethod method, Param[] params, ObjectMapper mapper, Later<T> source) {
		super(object, method, mapper, source);
		
		this.params = params;
	}

	@Override
	@JsonIgnore
	protected Param[] getParams() {
		return this.params;
	}
}