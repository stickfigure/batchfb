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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.googlecode.batchfb.test.util.TestBase;
import org.testng.annotations.Test;

import com.googlecode.batchfb.util.SplitterIterator;

/**
 * Tests of the utilities package
 * 
 * @author Jeff Schnitzer
 */
public class UtilsTest extends TestBase {
	
	/**
	 */
	@Test
	public void splitterIteratorTest() throws Exception {
		List<List<String>> master = new ArrayList<List<String>>();
		
		LinkedList<String> w1 = new LinkedList<String>();
		w1.add("a");
		w1.add("b");
		w1.add("c");
		master.add(w1);

		LinkedList<String> w2 = new LinkedList<String>();
		w2.add("d");
		w2.add("e");
		w2.add("f");
		master.add(w2);
		
		Iterator<List<String>> splitter = new SplitterIterator<String>(master, 2);
		assert splitter.hasNext();
		
		List<String> w1a = splitter.next();
		assert w1a.size() == 2;
		assert w1a.get(0).equals("a");
		assert w1a.get(1).equals("b");
		
		List<String> w1b = splitter.next();
		assert w1b.size() == 1;
		assert w1b.get(0).equals("c");
		
		List<String> w2a = splitter.next();
		assert w2a.size() == 2;
		assert w2a.get(0).equals("d");
		assert w2a.get(1).equals("e");
		splitter.remove();
		assert master.size() == 2;	// hasn't modified this yet
		assert master.get(1).size() == 1;	// the original structure was changed
		
		List<String> w2b = splitter.next();
		assert w2b.size() == 1;
		assert w2b.get(0).equals("f");
		splitter.remove();
		assert master.size() == 1;
		assert master.get(0) == w1;
	}
}