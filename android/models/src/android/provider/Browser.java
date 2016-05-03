public class Browser
{
    @STAMP(flows = {@Flow(from="$BROWSER",to="!BROWSER_HISTORY.MODIFY")})
    public static final  void deleteFromHistory(android.content.ContentResolver cr, java.lang.String url) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="$BROWSER",to="!BROWSER_SEARCHES.MODIFY")})
    public static final  void clearSearches(android.content.ContentResolver cr) { throw new RuntimeException("Stub!"); }
    
    @STAMP(flows = {@Flow(from="$BROWSER",to="!BROWSER_HISTORY.MODIFY")})
    public static final  void deleteHistoryTimeFrame(android.content.ContentResolver cr, long begin, long end) { throw new RuntimeException("Stub!"); }
    
    @STAMP(flows = {@Flow(from="$BROWSER",to="!BROWSER_HISTORY.MODIFY")})
    public static final  void truncateHistory(android.content.ContentResolver cr) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="$BROWSER",to="!BROWSER_HISTORY.MODIFY")})
    public static final  void clearHistory(android.content.ContentResolver cr) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="$BROWSER",to="!BROWSER_HISTORY.MODIFY")})
    public static final  void updateVisitedHistory(android.content.ContentResolver cr, java.lang.String url, boolean real) { throw new RuntimeException("Stub!"); }

	static {
		BOOKMARKS_URI = taintedBookmarks();
		SEARCHES_URI = taintedSearches();
	}

	@STAMP(flows = {@Flow(from="$Browser.BOOKMARKS",to="@return")})
	private static android.net.Uri taintedBookmarks()
	{
		return new android.net.StampUri("");
	}

	@STAMP(flows = {@Flow(from="$Browser.SEARCHES",to="@return")})
	private static android.net.Uri taintedSearches()
	{
		return new android.net.StampUri("");
	}


	@STAMP(flows = {@Flow(from="$Browser.BOOKMARKS",to="@return")})
	public static final  android.database.Cursor getAllBookmarks(android.content.ContentResolver cr) throws java.lang.IllegalStateException 
	{ 
		return new android.test.mock.MockCursor();
	}

	@STAMP(flows = {@Flow(from="$Browser.VisitedUrls",to="@return")})
	public static final  android.database.Cursor getAllVisitedUrls(android.content.ContentResolver cr) throws java.lang.IllegalStateException 
	{ 
		return new android.test.mock.MockCursor();
	}
	
}