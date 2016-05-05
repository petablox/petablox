package edu.stanford.stamp.testapps.stringcast;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity
{

	public final static Object field = "TEST_STRING_CAST";	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}
