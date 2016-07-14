class ContentProvider
{
    @STAMP(flows={@Flow(from="uri",to="!Content.Insert")})
    public abstract  android.net.Uri insert(android.net.Uri uri, android.content.ContentValues values);

    @STAMP(flows={@Flow(from="uri",to="!Content.Insert")})
    public  int bulkInsert(android.net.Uri uri, android.content.ContentValues[] values) { return 0; }

    @STAMP(flows={@Flow(from="uri",to="!Content.Delete")})
    public abstract  int delete(android.net.Uri uri, java.lang.String selection, java.lang.String[] selectionArgs);

    @STAMP(flows={@Flow(from="uri",to="!Content.Update")})
    public abstract  int update(android.net.Uri uri, android.content.ContentValues values, java.lang.String selection, java.lang.String[] selectionArgs);

}
