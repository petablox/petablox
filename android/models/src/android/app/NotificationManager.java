class NotificationManager
{
	public  void notify(int id, android.app.Notification notification) 
	{ 
		android.content.Context context = new android.test.mock.MockContext();
		notification.setLatestEventInfo(context, null, null, notification.contentIntent);

	}
	public  void notify(java.lang.String tag, int id, android.app.Notification notification) 
	{ 
		android.content.Context context = new android.test.mock.MockContext();
		notification.setLatestEventInfo(context, null, null, notification.contentIntent);
	}

}
