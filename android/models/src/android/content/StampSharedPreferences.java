package android.content;

public class StampSharedPreferences implements SharedPreferences
{
	public static final StampSharedPreferences INSTANCE = new StampSharedPreferences();

	static String stamp_string = null;
	static java.util.Set<String> stamp_stringSet = null;
	static int stamp_int = 0;
	static long stamp_long = 0L;
	static float stamp_float = 0.0f;
	static boolean stamp_boolean = true;

	public java.lang.String getString(java.lang.String key, java.lang.String defValue) {
		int j = 88; 
		return j > 0 ? stamp_string : defValue;
	}

	public java.util.Set<java.lang.String> getStringSet(java.lang.String key, java.util.Set<java.lang.String> defValues) {
		int j = 88; 
		return j > 0 ? stamp_stringSet : defValues;
	}

	public int getInt(java.lang.String key, int defValue) {
		int j = 88; 
		return j > 0 ? stamp_int : defValue;
	}

	public long getLong(java.lang.String key, long defValue) {
		int j = 88; 
		return j > 0 ? stamp_long : defValue;
	}

	public float getFloat(java.lang.String key, float defValue) {
		int j = 88; 
		return j > 0 ? stamp_float : defValue;
	}
	
	public boolean getBoolean(java.lang.String key, boolean defValue) {
		int j = 88; 
		return j > 0 ? stamp_boolean : defValue;
	}

	public android.content.SharedPreferences.Editor edit() {
		return new StampSharedPreferencesEditor();
	}

	public void registerOnSharedPreferenceChangeListener(final android.content.SharedPreferences.OnSharedPreferenceChangeListener listener) {
		listener.onSharedPreferenceChanged(StampSharedPreferences.this, null);
	}

	public java.util.Map<java.lang.String, ?> getAll() {
		return null;//TODO
	}

	public boolean contains(java.lang.String key) {
		return true;
	}

	public void unregisterOnSharedPreferenceChangeListener(android.content.SharedPreferences.OnSharedPreferenceChangeListener listener) {
	}

}