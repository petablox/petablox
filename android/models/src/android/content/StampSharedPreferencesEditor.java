package android.content;

class StampSharedPreferencesEditor implements SharedPreferences.Editor
{
	public  android.content.SharedPreferences.Editor putString(java.lang.String key, java.lang.String value) {
		StampSharedPreferences.stamp_string = value;
		return this;
	}

	public  android.content.SharedPreferences.Editor putStringSet(java.lang.String key, java.util.Set<java.lang.String> values) {
		StampSharedPreferences.stamp_stringSet = values;
		return this;
	}
	
	public  android.content.SharedPreferences.Editor putInt(java.lang.String key, int value) {
		StampSharedPreferences.stamp_int = value;
		return this;
	}

	public  android.content.SharedPreferences.Editor putLong(java.lang.String key, long value) {
		StampSharedPreferences.stamp_long = value;
		return this;
	}

	public  android.content.SharedPreferences.Editor putFloat(java.lang.String key, float value) {
		StampSharedPreferences.stamp_float = value;
		return this;
	}

	public  android.content.SharedPreferences.Editor putBoolean(java.lang.String key, boolean value) {
		StampSharedPreferences.stamp_boolean = value;
		return this;
	}

	public  android.content.SharedPreferences.Editor remove(java.lang.String key) {
		return this;
	}

	public  android.content.SharedPreferences.Editor clear() {
		return this;
	}

	public  boolean commit() {
		return true;
	}
	
	public  void apply() {
		return;
	}

}