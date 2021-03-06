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

package com.googlecode.batchfb.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.batchfb.Batcher;
import com.googlecode.batchfb.Later;
import com.googlecode.batchfb.err.ErrorFacebookException;
import com.googlecode.batchfb.err.FacebookException;
import com.googlecode.batchfb.err.OAuthException;
import com.googlecode.batchfb.err.PageMigratedException;
import com.googlecode.batchfb.err.PermissionException;
import com.googlecode.batchfb.util.LaterWrapper;

import java.lang.reflect.Constructor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Detects a Facebook error in the JSON result from a Graph API request and throws
 * the correct kind of exception, whatever that happens to be.  It tries to match the
 * type of the exception with an actual exception class of the correct name.</p>
 * 
 * <p>In addition, this detects the REALLY WEIRD cases where Facebook just spits back
 * "false".  We translate that into a null.</p>
 * 
 * <p>If there was no error, this wrapper just passes through normally.</p>
 * 
 * <p>Facebook coughs up errors in at least three different formats.  This
 * detects them all.</p>
 */
public class ErrorDetectingWrapper extends LaterWrapper<JsonNode, JsonNode>
{
	public ErrorDetectingWrapper(Later<JsonNode> orig) {
		super(orig);
	}

	/** */
	@Override
	protected JsonNode convert(JsonNode node) {
		// Hopefully a simple "false" at the top level is never a legitimate value... it seems that it should be mapped
		// to null.  It happens (among other times) when fetching multiple items and you don't have permission on one of them.
		if (node == null || node.isBoolean() && !node.booleanValue())
			return null;
		
		this.checkForStandardGraphError(node);
		this.checkForBatchError(node);
		this.checkForOldRestStyleError(node);
		
		return node;
	}
	
	/**
	 * The basic graph error looks like this:
<pre>
{
  error: {
    type: "OAuthException"
    message: "Error validating application."
  }
}
</pre>
	 */
	protected void checkForStandardGraphError(JsonNode node) {
		JsonNode errorNode = node.get("error");
		if (errorNode != null) {
			// If we're missing type or message, it must be some other kind of error
			String type = errorNode.path("type").textValue();
			if (type == null)
				return;
			
			String msg = errorNode.path("message").textValue();
			if (msg == null)
				return;

			JsonNode codeNode = errorNode.get("code");
			Integer code = codeNode == null ? null : codeNode.intValue();

			JsonNode subcodeNode = errorNode.get("error_subcode");
			Integer subcode = subcodeNode == null ? null : subcodeNode.intValue();

			String userTitle = errorNode.path("error_user_title").textValue();
			String userMsg = errorNode.path("error_user_msg").textValue();

			if (code != null) {
				// Special case, migration exceptions are poorly structured
				if (code == 21)
					this.throwPageMigratedException(msg, code, subcode, userTitle, userMsg);

				// Documented here: https://developers.facebook.com/docs/graph-api/using-graph-api
				if (code == 10 || (code >= 200 && code <= 299))
					throw new PermissionException(msg, type, code, subcode, userTitle, userMsg);
			}

			// We check to see if we have an exception that matches the type, otherwise
			// we simply throw the base FacebookException
			String proposedExceptionType = Batcher.class.getPackage().getName() + ".err." + type;
			
			try {
				Class<?> exceptionClass = Class.forName(proposedExceptionType);
				Constructor<?> ctor = exceptionClass.getConstructor(String.class, String.class, Integer.TYPE, Integer.TYPE);
				throw (FacebookException)ctor.newInstance(msg, type, code, subcode);
			} catch (FacebookException e) {
				throw e;
			} catch (Exception e) {
				throw new ErrorFacebookException(type + ": " + msg, type, code, subcode, userTitle, userMsg);
			}
		}
	}

	/** Matches IDs in the error msg */
	private static final Pattern ID_PATTERN = Pattern.compile("ID [0-9]+");

	/**
	 * Builds the proper exception and throws it.
	 * @throws PageMigratedException always
	 */
	private void throwPageMigratedException(String msg, int code, int subcode, String userTitle, String userMsg) {
		// This SUCKS ASS.  Messages look like:
		// (#21) Page ID 114267748588304 was migrated to page ID 111013272313096.  Please update your API calls to the new ID

		Matcher matcher = ID_PATTERN.matcher(msg);

		long oldId = this.extractNextId(matcher, msg);
		long newId = this.extractNextId(matcher, msg);

		throw new PageMigratedException(msg, code, subcode, userTitle, userMsg, oldId, newId);
	}

	/**
	 * Gets the next id out of the matcher
	 */
	private long extractNextId(Matcher matcher, String msg) {
		if (!matcher.find())
			throw new IllegalStateException("Facebook changed the error msg for page migration to something unfamiliar. The new msg is: " + msg);

		String idStr = matcher.group().substring("ID ".length());
		return Long.parseLong(idStr);
	}

	/**
	 * The batch call itself seems to have a funky error format:
	 * 
	 * {"error":190,"error_description":"Invalid OAuth access token signature."}
	 */
	protected void checkForBatchError(JsonNode root) {
		JsonNode errorCode = root.get("error");
		if (errorCode != null) {
			
			JsonNode errorDescription = root.get("error_description");
			if (errorDescription != null) {
				
				int code = errorCode.intValue();
				String msg = errorDescription.asText();

				this.throwCodeAndMessage(code, msg);
			}
		}
	}

	/**
	 * Old-style calls, including multiquery, has its own wacky error format:
<pre>
{
  "error_code": 602,
  "error_msg": "bogus is not a member of the user table.",
  "request_args": [
    {
      "key": "queries",
      "value": "{"query1":"SELECT uid FROM user WHERE uid=503702723",
"query2":"SELECT uid FROM user WHERE bogus=503702723"}"
    },
    {
      "key": "method",
      "value": "fql.multiquery"
    },
    {
      "key": "access_token",
      "value": "blahblahblah"
    },
    {
      "key": "format",
      "value": "json"
    }
  ]
}
</pre>
	 *
	 * The code interpretations rely heavily on http://wiki.developers.facebook.com/index.php/Error_codes
	 * The wayback machine: https://web.archive.org/web/20091223080550/http://wiki.developers.facebook.com/index.php/Error_codes
	 */
	protected void checkForOldRestStyleError(JsonNode node) {
		JsonNode errorCode = node.get("error_code");
		
		if (errorCode != null) {
			int code = errorCode.intValue();
			String msg = node.path("error_msg").asText();

			this.throwCodeAndMessage(code, msg);
		}
	}
	
	/**
	 * Throw the appropriate exception for the given legacy code and message.
	 * Always throws, never returns.
	 */
	protected void throwCodeAndMessage(int code, String msg) {
		switch (code) {
			case 0:
			case 101:
			case 102:
			case 190: throw new OAuthException(msg, "OAuthException", code, null, null, null);
			
			default: throw new ErrorFacebookException(msg + " (code " + code +")", null, code, null, null, null);
		}
	}
}