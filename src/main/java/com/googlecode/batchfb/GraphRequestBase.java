package com.googlecode.batchfb;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;
import com.googlecode.batchfb.util.StringUtils;

/**
 * <p>Common behavior for the requests that get batched at the top level.</p>
 * 
 * <p>A little trick: This object is also serialized out to JSON as the array of batch parameters
 * to the Facebook call.  Its getters return the appropriate data.
 */
abstract public class GraphRequestBase<T> extends Request<T> {
	private String object;
	private HttpMethod method;
	
	/** This is an oddity because if missing it is by default true */
	private Boolean omitResponseOnSuccess;
	
	@JsonIgnore
	protected ObjectMapper mapper;
	
	/** Strips off any leading / from object */
	public GraphRequestBase(String object, HttpMethod method, ObjectMapper mapper, Later<T> source) {
		super(source);
		
		this.object = object.startsWith("/") ? object.substring(1) : object;
		this.method = method;
		
		this.mapper = mapper;
	}
	
	/**
	 * Concrete subclasses should override this to provide params that will go into the construction of the relative url.
	 */
	abstract protected Param[] getParams();
	
	/** Obnoxiously, if you don't set this false, the default is true */
	public void setOmitResponseOnSuccess(boolean value) {
		this.omitResponseOnSuccess = value;
	}
	
	/** If null, this property is true - silly facebook */
	@JsonProperty("omit_response_on_success")
	public Boolean getOmitResponseOnSuccess() {
		return this.omitResponseOnSuccess;
	}
	
	/** Jackson does the right thing with this */
	public HttpMethod getMethod() {
		return this.method;
	}
	
	/** What Facebook uses to define the url in a batch */
	@JsonProperty("relative_url")
	public String getRelativeURL() {
		StringBuilder bld = new StringBuilder();
		bld.append(this.object);
		
		Param[] params = this.getParams();
		
		if (params != null && params.length > 0) {
			bld.append('?');
			boolean afterFirst = false;
			
			for (Param param: params) {
				if (afterFirst)
					bld.append('&');
				else
					afterFirst = true;
				
				if (param instanceof BinaryParam) {
					//call.addParam(param.name, (InputStream)param.value, ((BinaryParam)param).contentType, "irrelevant");
					throw new UnsupportedOperationException("Not quite sure what to do with BinaryParam yet");
				} else {
					String paramValue = StringUtils.stringifyValue(param, this.mapper);
					bld.append(StringUtils.urlEncode(param.name));
					bld.append('=');
					bld.append(StringUtils.urlEncode(paramValue));
				}
			}
		}
		
		return bld.toString();
	}
}