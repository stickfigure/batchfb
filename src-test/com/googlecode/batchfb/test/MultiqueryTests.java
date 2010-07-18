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

import org.codehaus.jackson.node.ArrayNode;
import org.junit.Assert;
import org.junit.Test;

import com.googlecode.batchfb.Later;

/**
 * Testing out batching of multiqueries.
 * 
 * @author Jeff Schnitzer
 */
public class MultiqueryTests extends TestBase {

  /**
   */
  @Test
  public void basicMultiquery() throws Exception {
    Later<ArrayNode> firstNameArray = this.anonBatcher.query("SELECT first_name FROM user WHERE uid = 1047296661");
    Later<ArrayNode> lastNameArray = this.anonBatcher.query("SELECT last_name FROM user WHERE uid = 1047296661");
    
    Assert.assertEquals(1, firstNameArray.get().size());
    Assert.assertEquals(1, lastNameArray.get().size());
    Assert.assertEquals("Robert", firstNameArray.get().get(0).get("first_name").getTextValue());
    Assert.assertEquals("Dobbs", lastNameArray.get().get(0).get("last_name").getTextValue());
  }
}