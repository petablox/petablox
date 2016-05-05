package com.example;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import edu.stanford.stamp.annotation.Flow;
import edu.stanford.stamp.annotation.STAMP;

abstract class AbstractClass {
	@STAMP(flows={@Flow(from="x",to="@return")})
	abstract String foo(String x);
}

class ConcreteClass extends AbstractClass {
	@Override
	String foo(String x) {
		return new String();
	}
}

public class AnnotationInheritance extends Activity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		TelephonyManager tm =
			(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String IMEI = tm.getDeviceId();
		AbstractClass o = new ConcreteClass();
		String msg = o.foo(IMEI);
		Log.v("", msg);
	}
}
