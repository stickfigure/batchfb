package com.googlecode.batchfb.err;



/**
 * Thrown when fetching an id which has been migrated to a new id.  Note that
 * this isn't a proper graph exception; we synthesize it by careful examination of
 * the error text.  It's error code 21.
 * 
 * The actual extraction of ids from the msg text (which relies on java regexes) has
 * been removed from this class so that it can be used in GWT. 
 */
public class PageMigratedException extends ErrorFacebookException
{
	private static final long serialVersionUID = 1L;
	
	/** Make GWT happy */
	PageMigratedException() {}
	
	/** The id that was migrated from */
	private long oldId;
	public long getOldId() { return this.oldId; }
	
	/** The id that was migrated to */
	private long newId;
	public long getNewId() { return this.newId; }
	
	/** */
	public PageMigratedException(String msg, Integer code, Integer subcode, String userTitle, String userMsg, long oldId, long newId)
	{
		super(msg, PageMigratedException.class.getSimpleName(), code, subcode, userTitle, userMsg);
		this.oldId = oldId;
		this.newId = newId;
	}
}
