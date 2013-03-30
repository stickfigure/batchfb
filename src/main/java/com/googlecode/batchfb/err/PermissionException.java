package com.googlecode.batchfb.err;


/**
 * Thrown when trying to do something that produces a permission error.  Note that
 * this isn't a proper graph exception; we synthesize it by careful examination of
 * the error text from both the Graph API and Old REST API.  It's error 200, if you're
 * wondering.
 */
public class PermissionException extends FacebookException
{
	private static final long serialVersionUID = 1L;
	
	/** Make GWT happy */
	PermissionException() {}
	
	/** */
	public PermissionException(String msg)
	{
		super(msg);
	}
}
