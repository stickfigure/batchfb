package com.googlecode.batchfb;


/**
 * <p>Represents one queued FQL query.</p>
 */
public class QueryRequest<T> extends Request<T> {
	String fql;
	public String getFQL() { return this.fql; }
	
	/** */
	public QueryRequest(String fql, String name, Later<T> source) {
		super(source);
		
		this.fql = fql;
		this.setName(name);
	}
}