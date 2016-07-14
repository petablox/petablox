class AppWidgetProvider
{
    public  AppWidgetProvider() 
	{ 
		android.content.Context context = new android.test.mock.MockContext();

		this.onReceive(context, new android.content.Intent());
		this.onUpdate(context, null, new int[1]);
		//AppWidgetProvider.this.onAppWidgetOptionsChanged(null, null, 1, null);
		this.onDeleted(context, new int[2]);
		this.onEnabled(context);
		this.onDisabled(context);
    }
}
