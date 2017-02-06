class MockContentProvider
{

    @STAMP(flows = {@Flow(from="$CONTENT_PROVIDER",to="@return"), @Flow(from="uri",to="@return")})
    public  android.database.Cursor query(android.net.Uri uri, java.lang.String[] projection, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String sortOrder) 
    { 
		return new MockCursor();
    }
}
