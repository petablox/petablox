class IntentService
{
	public  IntentService(java.lang.String name) 
	{ 
		super();
	}
	
	public void callCallbacks()
	{
		super.callCallbacks();
		this.onHandleIntent(new android.content.Intent());
	}

}
