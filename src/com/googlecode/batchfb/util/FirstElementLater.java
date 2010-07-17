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

import java.util.List;

import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.err.FacebookException;

/**
 * <p>Adapter which pulls out the first element of a list.  Returns null
 * if the list does not have a first element.</p>
 * 
 * @author Jeff Schnitzer
 */
public class FirstElementLater<T> implements Later<T> {
	
	/** */
	Later<List<T>> base;
	
	/** */
	public FirstElementLater(Later<List<T>> base) {
		this.base = base;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.batchfb.Later#get()
	 */
	@Override
	public T get() throws FacebookException {
		if (base.get().isEmpty())
			return null;
		else
			return base.get().get(0);
	}
}