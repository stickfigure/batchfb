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

import com.googlecode.batchfb.err.FacebookException;

/**
 * <p>Similar to java.concurrent.Future; allows a return value to be defined sometime in
 * the future. This provides the ability to separate the request from execution, and allows
 * the backend to optimize the actual collection of data.</p>
 * 
 * @author Jeff Schnitzer
 */
public interface Later<T> {
	/**
	 * <p>Get the value, triggering execution of the batch if necessary.  Once the batch
	 * has been executed, this method can be called repeatedly without incurring further
	 * calls to Facebook or triggering the execution of any subsequently created batches.
	 * It is as efficient as a simple value getter.</p>
	 * 
	 * <p>If the Facebook call produced an error, repeated calls to this method will produce
	 * the same exception.  BatchFB will *not* retry a Facebook call; you must create a
	 * new Later<?> object from the FacebookBatcher class.</p>
	 * 
	 * @throws FacebookException if anything went wrong with the Facebook interaction
	 */
	T get() throws FacebookException;
}