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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.logging.Logger;

import com.googlecode.batchfb.util.RequestBuilder.HttpMethod;
import com.googlecode.batchfb.util.RequestBuilder.HttpResponse;

/**
 * <p>Uses the default HttpURLConnection in the JDK.  Does not support parallel fetching.</p>
 * 
 * @author Jeff Schnitzer
 */
public class DefaultRequestExecutor extends RequestExecutor {
	/** */
	private static final Logger log = Logger.getLogger(DefaultRequestExecutor.class.getName());
	
	/** */
	private class DefaultRequestDefinition implements RequestDefinition {
		
		HttpURLConnection conn;
		
		@Override
		public void init(HttpMethod meth, String url) throws IOException {
			this.conn = (HttpURLConnection)new URL(url).openConnection();
			this.conn.setRequestMethod(meth.name());
		}

		@Override
		public void setHeader(String name, String value) {
			this.conn.setRequestProperty(name, value);
		}

		@Override
		public OutputStream getContentOutputStream() throws IOException {
			this.conn.setDoOutput(true);
			return this.conn.getOutputStream();
		}

		@Override
		public void setContent(byte[] content) throws IOException {
			this.getContentOutputStream().write(content);
		}

		@Override
		public void setTimeout(int millis) {
			conn.setConnectTimeout(millis);
			conn.setReadTimeout(millis);
		}

		public HttpResponse execute() throws IOException {
			return new HttpResponse() {
				@Override
				public int getResponseCode() throws IOException {
					return conn.getResponseCode();
				}

				@Override
				public InputStream getContentStream() throws IOException {
					InputStream errStream = conn.getErrorStream(); 
					if (errStream != null)
						return errStream;
					else
						return conn.getInputStream();
				}
			};
		}
	}

	/** */
	@Override
	public HttpResponse execute(int retries, RequestSetup setup) throws IOException {
		
		if (retries == 0) {
			return this.executeOnce(setup);
		} else {
			for (int i=0; i<=retries; i++) {
				try {
					return this.executeOnce(setup);
				} catch (IOException ex) {
					// This should just be a check for SocketTimeoutException, but GAE is not
					// throwing the right exception - it's just IOException with "Timeout while fetching..."
					if (i < retries && (ex instanceof SocketTimeoutException || ex.getMessage().startsWith("Timeout"))) {
						log.warning("Timeout error, retrying");
					} else {
						throw ex;
					}
				}
			}
			
			// Logically unreachable code, but the compiler doesn't know that
			return null;
		}
	}
	
	/**
	 * Execute given the specified http method once, throwing any exceptions as they come
	 */
	private HttpResponse executeOnce(RequestSetup setup) throws IOException {
		DefaultRequestDefinition req = new DefaultRequestDefinition();
		setup.setup(req);
		
		HttpResponse response = req.execute();
		
		// This will force the request to complete, causing any timeout exceptions to happen here
		response.getResponseCode();
		
		return response;
	}
}