class Application
{
	public Application()
	{
		super(null);
	}
	
	public void callCallbacks()
	{
		this.onCreate();
		this.onLowMemory();
		this.onTerminate();
		this.onTrimMemory(0);
	}
}