class PreferenceManager
{
	public static  android.content.SharedPreferences getDefaultSharedPreferences(android.content.Context context) { 
		return android.content.StampSharedPreferences.INSTANCE;
	}

	public  android.content.SharedPreferences getSharedPreferences() { 
		return android.content.StampSharedPreferences.INSTANCE;
	}
	
}