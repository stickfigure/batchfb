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

package com.googlecode.batchfb.test;

import org.codehaus.jackson.type.TypeReference;
import org.testng.annotations.Test;

import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.PagedLater;
import com.googlecode.batchfb.type.Paged;

/**
 * Testing out connections (paged stuff).  Note that these require an auth token
 * for an account that has a lot of stuff in the stream.
 * 
 * @author Jeff Schnitzer
 */
public class ConnectionTests extends TestBase {

  /**
   * Tests using the normal graph() call to get paged data.  Expects to find stuff
   * on your home.
   */
  @Test
  public void simpleRawPaged() throws Exception {
    Later<Paged<Object>> feed = this.authBatcher.graph("me/home", new TypeReference<Paged<Object>>(){});
    
    assert !feed.get().getData().isEmpty();
    assert feed.get().getPaging() != null;
  }

  /**
   * Uses the paged() method.
   */
  @Test
  public void pagedMethod() throws Exception {
    PagedLater<Object> feed = this.authBatcher.paged("me/home", Object.class);
    
    assert !feed.get().isEmpty();
    
    PagedLater<Object> previous = feed.previous();
    PagedLater<Object> next = feed.next();
    
    assert previous.get().isEmpty();
    assert !next.get().isEmpty();

    // Just for the hell of it
    assert !next.next().get().isEmpty();
}
}