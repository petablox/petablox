class ContextThemeWrapper
{
	public ContextThemeWrapper(android.content.Context base, int themeres) 
	{
        super(base);
    }

	protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(newBase);
	}

	public java.lang.Object getSystemService(java.lang.String name) 
	{
		return mBase.getSystemService(name);
	}
}