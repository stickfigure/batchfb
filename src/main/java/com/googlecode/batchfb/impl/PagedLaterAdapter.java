package com.googlecode.batchfb.impl;

import com.googlecode.batchfb.Batcher;
import com.googlecode.batchfb.GraphRequest;
import com.googlecode.batchfb.PagedLater;
import com.googlecode.batchfb.err.FacebookException;
import com.googlecode.batchfb.type.Paged;
import com.googlecode.batchfb.util.URLParser;

import java.util.List;

/** Provides paging ability */
public class PagedLaterAdapter<T> implements PagedLater<T> {
	Batcher batcher;

	GraphRequest<Paged<T>> request;
	
	/** The type of T **/
	Class<T> type;

	/**
	 * @param batcher must be the master FacebookBatcher or something capable of creating fresh requests
	 * @param req is the request to wrap
	 * @param type is the type of T, the thing we are paging across (ie not Paged<T>)
	 */
	public PagedLaterAdapter(Batcher batcher, GraphRequest<Paged<T>> req, Class<T> type) {
		this.batcher = batcher;
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
		if (this.request.get().getPaging() == null || this.request.get().getPaging().getNext() == null)
			return null;
		else
			return this.createRequest(this.request.get().getPaging().getNext());
	}

	@Override
	public PagedLater<T> previous()
	{
		if (this.request.get().getPaging() == null || this.request.get().getPaging().getPrevious() == null)
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
		
		return this.batcher.paged(parser.getPath(), this.type, parser.getParamsAsArray());
	}
}