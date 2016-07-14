class Handler
{
	public final  boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
		this.handleMessage(null);
	    return true;
	}

	public final  boolean sendEmptyMessageDelayed(int what, long delayMillis) {
		this.handleMessage(null);
	    return true;
	}

	public final boolean sendEmptyMessage(int what) {
		this.handleMessage(null);
	    return true;
	}

	public final boolean sendMessage(final android.os.Message msg) {
		this.handleMessage(msg);
	    return true;
	}
	
	public final boolean sendMessageDelayed(final android.os.Message msg, final long delayMillis) {
		try {
			Thread.sleep(delayMillis);
		} catch(InterruptedException e) { }
		this.handleMessage(msg);
		return true;
	}

	public  boolean sendMessageAtTime(final android.os.Message msg, long uptimeMillis) {
		this.handleMessage(msg);
	    return true;
	}

	public final  boolean sendMessageAtFrontOfQueue(final android.os.Message msg) {
		this.handleMessage(msg);
	    return true;
	}

	public final  boolean post(final java.lang.Runnable r) {
		r.run();
	    return true;
	}
	
	public final  boolean postAtTime(final java.lang.Runnable r, long uptimeMillis) {
		r.run();
	    return true;
	}


	public final  boolean postAtTime(final java.lang.Runnable r, java.lang.Object token, long uptimeMillis) {
		r.run();
	    return true;
	}


	public final  boolean postDelayed(final java.lang.Runnable r, long delayMillis) {
		r.run();
	    return true;
	}

	public final  boolean postAtFrontOfQueue(final java.lang.Runnable r) {
		r.run();
	    return true;
	}

	public final  android.os.Message obtainMessage(int what, int arg1, int arg2, java.lang.Object obj) {
	    android.os.Message msg = new android.os.Message();
	    msg.what = what;
	    msg.arg1 = arg1;
	    msg.arg2 = arg2;
	    msg.obj = obj;
		msg.handler = this;
	    return msg;
	}
	
	public final  android.os.Message obtainMessage(int what, int arg1, int arg2) 
	{
	    android.os.Message msg = new android.os.Message();
	    msg.what = what;
	    msg.arg1 = arg1;
	    msg.arg2 = arg2;
		msg.handler = this;
	    return msg;
	}
	
	public final  android.os.Message obtainMessage(int what, java.lang.Object obj) 
	{
	    android.os.Message msg = new android.os.Message();
	    msg.what = what;
	    msg.obj = obj;
		msg.handler = this;
		return msg;
	}
	
	public final  android.os.Message obtainMessage(int what) 
	{
		android.os.Message msg = new android.os.Message();
	    msg.what = what;
		msg.handler = this;
	    return msg;
	}

	public final  android.os.Message obtainMessage() 
	{
		android.os.Message msg = new android.os.Message();
		msg.handler = this;
		return msg;
	}
}
