class ContextWrapper
{
	protected Context mBase;
	
	public ContextWrapper(android.content.Context base) {
		mBase = base;
	}

    protected void attachBaseContext(android.content.Context base) {
        mBase = base;
    }

    private android.content.ContentResolver contentResolver = new android.test.mock.MockContentResolver();

    public  android.content.ContentResolver getContentResolver() 
    { 
		return contentResolver;
    }
	
	public  android.content.Intent registerReceiver(android.content.BroadcastReceiver receiver, android.content.IntentFilter filter) { 
		final BroadcastReceiver r = receiver;
		r.onReceive(ContextWrapper.this, new Intent());
		return new Intent();
	}

	public  android.content.Intent registerReceiver(android.content.BroadcastReceiver receiver, android.content.IntentFilter filter, java.lang.String broadcastPermission, android.os.Handler scheduler) 
	{ 
		final BroadcastReceiver r = receiver;
		r.onReceive(ContextWrapper.this, new Intent());
		return new Intent();
	}

	@STAMP(flows = {@Flow(from="service",to="!Service")})
    public boolean bindService(android.content.Intent service, android.content.ServiceConnection conn, int flags) 
	{
		final android.content.ServiceConnection c = conn;
		c.onServiceConnected(null, null);
		c.onServiceDisconnected(null);
		return true;
    }

	public  android.content.Context getApplicationContext() 
	{ 
		return this;
	}

	@STAMP(flows = {@Flow(from="intent",to="!Activity"),@Flow(from="options",to="!Activity")})
	public void startActivity(android.content.Intent intent, android.os.Bundle options) {
    }

	@STAMP(flows = {@Flow(from="intent",to="!Activity")})
	public void startActivity(android.content.Intent intent) {
    }

	@STAMP(flows = {@Flow(from="intents",to="!Activity"),@Flow(from="options",to="!Activity")})
	public void startActivities(android.content.Intent[] intents, android.os.Bundle options) {
	}

	@STAMP(flows = {@Flow(from="intents",to="!Activity")})
    public void startActivities(android.content.Intent[] intents) {
    }

	@STAMP(flows = {@Flow(from="service",to="!Service")})
    public android.content.ComponentName startService(android.content.Intent service) {
		return null;
    }

	@STAMP(flows = {@Flow(from="intent",to="!Broadcast")})
	public void sendBroadcast(android.content.Intent intent) {
    }

	@STAMP(flows = {@Flow(from="intent",to="!Broadcast")})
    public void sendBroadcast(android.content.Intent intent, java.lang.String receiverPermission) {
    }

	@STAMP(flows = {@Flow(from="intent",to="!Broadcast")})
    public void sendOrderedBroadcast(android.content.Intent intent, java.lang.String receiverPermission) {
    }

	@STAMP(flows = {@Flow(from="intent",to="!Broadcast"),@Flow(from="initialExtras",to="!Broadcast")})
    public void sendOrderedBroadcast(android.content.Intent intent, java.lang.String receiverPermission, android.content.BroadcastReceiver resultReceiver, android.os.Handler scheduler, int initialCode, java.lang.String initialData, android.os.Bundle initialExtras) {

    }

	@STAMP(flows = {@Flow(from="intent",to="!Broadcast")})
    public void sendStickyBroadcast(android.content.Intent intent) {
    }

    public android.content.pm.PackageManager getPackageManager() {
		return new android.test.mock.MockPackageManager();
    }        

	public  android.content.SharedPreferences getSharedPreferences(java.lang.String name, int mode) { 
		return android.content.StampSharedPreferences.INSTANCE;
	}

    public java.io.FileInputStream openFileInput(java.lang.String name) throws java.io.FileNotFoundException {
		return new java.io.FileInputStream(name);
    }

    public java.io.FileOutputStream openFileOutput(java.lang.String name, int mode) throws java.io.FileNotFoundException {
		return new java.io.FileOutputStream(name);
    }

	public  java.lang.Object getSystemService(java.lang.String name) 
	{
		return mBase.getSystemService(name);
	}
}
