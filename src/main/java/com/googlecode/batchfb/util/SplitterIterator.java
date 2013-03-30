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

package com.googlecode.batchfb.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Iterator over a series of Lists that ensures that none of the lists returned
 * by the iterator contains more than a certain number of elements.  If one of the
 * lists is too large, it is split into multiple lists.  The remove() method works,
 * removing entries from the original list.</p>
 * 
 * @author Jeff Schnitzer
 */
public class SplitterIterator<T> implements Iterator<List<T>> {
	
	/** */
	int maxSize;
	Iterator<List<T>> original;
	
	/** These are only non-null if we are processing a too-big piece */
	List<T> whole;
	int offset;
	List<T> part;
	
	/** */
	public SplitterIterator(Collection<List<T>> coll, int maxSize) {
		this.maxSize = maxSize;
		this.original = coll.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return this.original.hasNext() || this.whole != null;
	}

	@Override
	public List<T> next() {
		if (this.whole == null)
			this.whole = this.original.next();
		
		if (this.whole.size() - this.offset <= this.maxSize) {
			List<T> result = this.whole.subList(this.offset, this.whole.size());
			this.whole = null;
			this.part = null;
			this.offset = 0;
			return result;
		} else {
			this.part = this.whole.subList(this.offset, this.maxSize);
			this.offset += this.maxSize;
			return this.part;
		}
	}

	@Override
	public void remove() {
		if (this.part != null) {
			this.offset -= this.part.size();
			this.part.clear();
		} else {
			this.original.remove();
		}
	}
}