class LayoutInflater
{
	public android.content.Context context;

	protected  LayoutInflater(android.content.Context context) { 
		this.context = context;
	}

	public static  android.view.LayoutInflater from(android.content.Context context) { 
		return new StampLayoutInflater(context);
	}
	
	public  android.content.Context getContext() { return context; }
}