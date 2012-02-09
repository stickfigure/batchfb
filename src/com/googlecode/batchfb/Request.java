package com.googlecode.batchfb;

import com.googlecode.batchfb.util.LaterWrapper;

/** 
 * Adds common characteristics for all types of batcher requests.
 */
public class Request<T> extends LaterWrapper<T, T> {
	
	private String name;
	
	public Request(Later<T> source) {
		super(source);
	}

	/**
	 * A name that might be referenced later in other requests in the same batch.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Set an explicit name that can be referenced later in other requests in the same batch.
	 * This works within multiquery and within graph calls, but you can't use names across
	 * the two types of batches.
	 * 
	 * This method can be chained.
	 */
	public Request<T> setName(String value) {
		this.name = value;
		return this;
	}
}