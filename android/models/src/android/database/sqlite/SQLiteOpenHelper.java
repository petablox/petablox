package android.database.sqlite;
class SQLiteOpenHelper
{
	private static android.database.sqlite.SQLiteDatabase db = new android.database.sqlite.SQLiteDatabase();
    public  android.database.sqlite.SQLiteDatabase getWritableDatabase() { return db;}
    public  android.database.sqlite.SQLiteDatabase getReadableDatabase() { return db; }
}
