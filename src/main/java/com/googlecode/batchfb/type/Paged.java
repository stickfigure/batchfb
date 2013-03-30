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

package com.googlecode.batchfb.type;

import java.util.List;

/**
 * <p>A simple structure that helps deal with Facebook graph results that
 * are paged.  Most connections (like https://graph.facebook.com/me/home)
 * have this structure.</p>
 * 
 * <p>Here is an example of how to use this class, assuming you have already
 * created your own Event class:</p>
 * 
 * <pre>
 * {@code
 * Later<Paged<Event>> paged = batcher.graph("me/events", new TypeReference<Paged<Event>>(){});
 * for (Event ev: paged.getData() {
 *     ...
 * }
 * }
 * </pre>
 * @author Jeff Schnitzer
 */
public class Paged<T> {
	
	public static class Paging {
		String previous;
		public String getPrevious() { return this.previous; }
		public void setPrevious(String value) { this.previous = value; }
		
		String next;
		public String getNext() { return this.next; }
		public void setNext(String value) { this.next = value; }
	}
	
	List<T> data;
	public List<T> getData() { return this.data; }
	public void setData(List<T> value) { this.data = value; }
	
	Paging paging;
	public Paging getPaging() { return this.paging; }
	public void setPaging(Paging value) { this.paging = value; }
}