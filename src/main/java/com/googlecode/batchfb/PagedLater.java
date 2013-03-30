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

import java.util.List;

import com.googlecode.batchfb.err.FacebookException;


/**
 * <p>Adds the ability to enqueue the previous and next pages of Facebook's
 * paginated data structures.</p>
 * 
 * @see FacebookBatcher#paged(String, Class, Param...)
 * @author Jeff Schnitzer
 */
public interface PagedLater<T> extends Later<List<T>> {
	
	/**
	 * Executes the current batch (if necessary) and enqueues a request for the previous page of data.
	 * If there is no previous page of data, this method will return null.
	 * 
	 * @throws FacebookException if there was an error executing the original request.
	 */
	PagedLater<T> previous() throws FacebookException;
	
	/**
	 * Executes the current batch (if necessary) and enqueues a request for the next page of data.
	 * If there is no next page of data, this method will return null.
	 * 
	 * @throws FacebookException if there was an error executing the original request.
	 */
	PagedLater<T> next() throws FacebookException;
}