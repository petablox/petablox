class AbstractThreadedSyncAdapter
{
	public AbstractThreadedSyncAdapter(android.content.Context context, boolean autoInitialize)  
	{
		this.onPerformSync(null, new android.os.Bundle(), new String(), null, null);
		this.onSyncCanceled();
		this.onSyncCanceled(null);
	}
}

