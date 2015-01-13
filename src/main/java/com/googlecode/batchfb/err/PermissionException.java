package com.googlecode.batchfb.err;


/**
 * Thrown when trying to do something that produces a permission error.  Note that
 * this isn't a proper graph exception; we synthesize it by looking at the error code.
 */
public class PermissionException extends ErrorFacebookException
{
	private static final long serialVersionUID = 1L;
	
	/** Make GWT happy */
	PermissionException() {}
	
	/** */
	public PermissionException(String msg, String type, Integer code, Integer subcode, String userTitle, String userMsg) {
		super(msg, type, code, subcode, userTitle, userMsg);
	}
}
