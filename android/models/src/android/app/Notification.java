class Notification
{

	public  void setLatestEventInfo(android.content.Context context, java.lang.CharSequence contentTitle, java.lang.CharSequence contentText, android.app.PendingIntent contentIntent) 
	{ 
		android.app.StampPendingIntent t = contentIntent.pi;
		android.content.Intent i0 = ((android.app.StampActivityPendingIntent) t).intent;
		context.startActivity(i0);

		android.content.Intent i1 = ((android.app.StampServicePendingIntent) t).intent;
		context.startService(i1);

        android.content.Intent i2 = ((android.app.StampBroadcastPendingIntent) t).intent;
		context.sendBroadcast(i2);

	}
}
