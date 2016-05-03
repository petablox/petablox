class SQLiteDatabase
{
	
    @STAMP(flows = {@Flow(from="$Database",to="@return"), @Flow(from="!Database",to="@return")})
	public static android.database.sqlite.SQLiteDatabase openDatabase(java.lang.String path, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int flags) {
		return new SQLiteDatabase();
    }

    @STAMP(flows = {@Flow(from="$Database",to="@return"), @Flow(from="!Database",to="@return")})
    public static android.database.sqlite.SQLiteDatabase openDatabase(java.lang.String path, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int flags, android.database.DatabaseErrorHandler errorHandler) {
		return new SQLiteDatabase();
    }

    @STAMP(flows = {@Flow(from="$Database",to="@return"), @Flow(from="!Database",to="@return")})
    public static android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.io.File file, android.database.sqlite.SQLiteDatabase.CursorFactory factory) {
		return new SQLiteDatabase();
    }

    @STAMP(flows = {@Flow(from="$Database",to="@return"), @Flow(from="!Database",to="@return")})
    public static android.database.sqlite.SQLiteDatabase openOrCreateDatabase(java.lang.String path, android.database.sqlite.SQLiteDatabase.CursorFactory factory) {
		return new SQLiteDatabase();
    }

   @STAMP(flows = {@Flow(from="values",to="!this")})
    public long insert(java.lang.String table, java.lang.String nullColumnHack, android.content.ContentValues values) {
		return 0L;
    }

    @STAMP(flows = {@Flow(from="values",to="!this")})
    public long insertOrThrow(java.lang.String table, java.lang.String nullColumnHack, android.content.ContentValues values) throws android.database.SQLException {
		return 0L;
    }

    @STAMP(flows = {@Flow(from="initialValues",to="!this")})
    public long insertWithOnConflict(java.lang.String table, java.lang.String nullColumnHack, android.content.ContentValues initialValues, int conflictAlgorithm) {
		return 0L;
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor query(boolean distinct, java.lang.String table, java.lang.String[] columns, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String groupBy, java.lang.String having, java.lang.String orderBy, java.lang.String limit) {
	return new android.test.mock.MockCursor();
    }

    /*@STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor query(boolean distinct, java.lang.String table, java.lang.String[] columns, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String groupBy, java.lang.String having, java.lang.String orderBy, java.lang.String limit, android.os.CancellationSignal cancellationSignal) {
	return new android.test.mock.MockCursor();
    }*/
    
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor queryWithFactory(android.database.sqlite.SQLiteDatabase.CursorFactory cursorFactory, boolean distinct, java.lang.String table, java.lang.String[] columns, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String groupBy, java.lang.String having, java.lang.String orderBy, java.lang.String limit) {
	return new android.test.mock.MockCursor();
    }

    /*
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor queryWithFactory(android.database.sqlite.SQLiteDatabase.CursorFactory cursorFactory, boolean distinct, java.lang.String table, java.lang.String[] columns, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String groupBy, java.lang.String having, java.lang.String orderBy, java.lang.String limit, android.os.CancellationSignal cancellationSignal) {
	return new android.test.mock.MockCursor();
	}*/

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor query(java.lang.String table, java.lang.String[] columns, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String groupBy, java.lang.String having, java.lang.String orderBy) {
	return new android.test.mock.MockCursor();
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor query(java.lang.String table, java.lang.String[] columns, java.lang.String selection, java.lang.String[] selectionArgs, java.lang.String groupBy, java.lang.String having, java.lang.String orderBy, java.lang.String limit) {
	return new android.test.mock.MockCursor();
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor rawQuery(java.lang.String sql, java.lang.String[] selectionArgs) {
	return new android.test.mock.MockCursor();
    }

    /*
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor rawQuery(java.lang.String sql, java.lang.String[] selectionArgs, android.os.CancellationSignal cancellationSignal) {
	return new android.test.mock.MockCursor();
    }*]

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase.CursorFactory cursorFactory, java.lang.String sql, java.lang.String[] selectionArgs, java.lang.String editTable) {
	return new android.test.mock.MockCursor();
    }
 
    /*
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase.CursorFactory cursorFactory, java.lang.String sql, java.lang.String[] selectionArgs, java.lang.String editTable, android.os.CancellationSignal cancellationSignal) {
	return new android.test.mock.MockCursor();
	}*/
}


      
