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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import com.googlecode.batchfb.util.RequestBuilder.BinaryAttachment;

/**
 * <p>Tool which writes multipart/form-data to a stream.</p>
 * 
 * <p>See <a href="http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4">http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4</a>.</p>
 * 
 * @author Jeff Schnitzer
 */
public class MultipartWriter {
	
	/** */
	private static final String MULTIPART_BOUNDARY = "**** an awful string which should never exist naturally ****" + Math.random();
	private static final String MULTIPART_BOUNDARY_SEPARATOR = "--" + MULTIPART_BOUNDARY;
	private static final String MULTIPART_BOUNDARY_END = MULTIPART_BOUNDARY_SEPARATOR + "--";
	
	/** */
	OutputStream out;
	
	/** */
	public MultipartWriter(RequestDefinition executor) throws IOException {
		executor.setHeader("Content-Type", "multipart/form-data; boundary=" + MULTIPART_BOUNDARY);
		this.out = executor.getContentOutputStream();
	}
	
	/**
	 * Write the params as multipart/form-data.  Params can include BinaryAttachemnt objects.
	 */
	public void write(Map<String, Object> params) throws IOException {
		LineWriter writer = new LineWriter(this.out);
		
		for (Map.Entry<String, Object> param: params.entrySet()) {
			writer.println(MULTIPART_BOUNDARY_SEPARATOR);
			
			if (param.getValue() instanceof BinaryAttachment) {
				BinaryAttachment ba = (BinaryAttachment)param.getValue();
				writer.println("Content-Disposition: form-data; name=\"" + StringUtils.urlEncode(param.getKey()) + "\"; filename=\""
						+ StringUtils.urlEncode(ba.filename) + "\"");
				writer.println("Content-Type: " + ba.contentType);
				writer.println("Content-Transfer-Encoding: binary");
				writer.println();
				writer.flush();
				// Now output the binary part to the raw stream
				int read;
				byte[] chunk = new byte[8192];
				while ((read = ba.data.read(chunk)) > 0)
					this.out.write(chunk, 0, read);
			} else {
				writer.println("Content-Disposition: form-data; name=\"" + StringUtils.urlEncode(param.getKey()) + "\"");
				writer.println();
				writer.println(StringUtils.urlEncode(param.getValue().toString()));
			}
		}
		
		writer.println(MULTIPART_BOUNDARY_END);
		writer.flush();
	}
}