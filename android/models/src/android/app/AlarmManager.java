class AlarmManager
{
    public  void set(int type, long triggerAtMillis, android.app.PendingIntent operation) 
    {
		android.app.StampPendingIntent t = operation.pi;
		android.content.Intent i0 = ((android.app.StampActivityPendingIntent) t).intent;
		new android.content.ContextWrapper(null).startActivity(i0);
		
		android.content.Intent i1 = ((android.app.StampServicePendingIntent) t).intent;
		new android.content.ContextWrapper(null).startService(i1);

        android.content.Intent i2 = ((android.app.StampBroadcastPendingIntent) t).intent;
		new android.content.ContextWrapper(null).sendBroadcast(i2);


    }

    public  void setRepeating(int type, long triggerAtMillis, long intervalMillis, android.app.PendingIntent operation) 
    { 
        android.app.StampPendingIntent t = operation.pi;
		android.content.Intent i0 = ((android.app.StampActivityPendingIntent) t).intent;
		new android.content.ContextWrapper(null).startActivity(i0);
		
		android.content.Intent i1 = ((android.app.StampServicePendingIntent) t).intent;
		new android.content.ContextWrapper(null).startService(i1);

        android.content.Intent i2 = ((android.app.StampBroadcastPendingIntent) t).intent;
		new android.content.ContextWrapper(null).sendBroadcast(i2);


    }

    public  void setInexactRepeating(int type, long triggerAtMillis, long intervalMillis, android.app.PendingIntent operation) 
    { 
        android.app.StampPendingIntent t = operation.pi;
		android.content.Intent i0 = ((android.app.StampActivityPendingIntent) t).intent;
		new android.content.ContextWrapper(null).startActivity(i0);
		
		android.content.Intent i1 = ((android.app.StampServicePendingIntent) t).intent;
		new android.content.ContextWrapper(null).startService(i1);

        android.content.Intent i2 = ((android.app.StampBroadcastPendingIntent) t).intent;
		new android.content.ContextWrapper(null).sendBroadcast(i2);

 
    }

    public  void cancel(android.app.PendingIntent operation) 
    { 
        android.app.StampPendingIntent t = operation.pi;
		android.content.Intent i0 = ((android.app.StampActivityPendingIntent) t).intent;
		new android.content.ContextWrapper(null).startActivity(i0);
		
		android.content.Intent i1 = ((android.app.StampServicePendingIntent) t).intent;
		new android.content.ContextWrapper(null).startService(i1);

        android.content.Intent i2 = ((android.app.StampBroadcastPendingIntent) t).intent;
		new android.content.ContextWrapper(null).sendBroadcast(i2);


    }

}
