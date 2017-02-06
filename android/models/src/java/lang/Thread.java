class Thread
{
    private Runnable r;

    public  Thread() 
    { 
		this.r = this;
    }

    public  Thread(java.lang.Runnable runnable) 
    { 
		this.r = runnable;
    }

    public synchronized  void start() 
    { 
		r.run();
    }

    public static void setDefaultUncaughtExceptionHandler(final java.lang.Thread.UncaughtExceptionHandler handler) {
	edu.stanford.stamp.harness.ApplicationDriver.getInstance().
	    registerCallback(new edu.stanford.stamp.harness.Callback(){
		    public void run() {
			handler.uncaughtException(null, null);
                    }
		});
    }
}
