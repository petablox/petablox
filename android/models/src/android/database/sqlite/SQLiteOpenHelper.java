class SQLiteOpenHelper
{
	private SQLiteDatabase database;

	public  SQLiteOpenHelper(android.content.Context context, java.lang.String name, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version) { 
		this.database = SQLiteDatabase.openDatabase(name, factory, 0);
	}

	public  SQLiteOpenHelper(android.content.Context context, java.lang.String name, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version, android.database.DatabaseErrorHandler errorHandler) { 
		this.database = SQLiteDatabase.openDatabase(name, factory, 0, errorHandler);
	}
	
	public  android.database.sqlite.SQLiteDatabase getWritableDatabase() { 
		return this.database;
	}

	public  android.database.sqlite.SQLiteDatabase getReadableDatabase() { 
		return this.database;
	}

}