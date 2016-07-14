class Preference
{
	public  void setOnPreferenceChangeListener(final android.preference.Preference.OnPreferenceChangeListener onPreferenceChangeListener) { 
		onPreferenceChangeListener.onPreferenceChange(Preference.this, null);
	}


	public  void setOnPreferenceClickListener(final android.preference.Preference.OnPreferenceClickListener onPreferenceClickListener) { 
		onPreferenceClickListener.onPreferenceClick(Preference.this);
	}

}