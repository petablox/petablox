import java.io.IOException;

class BackupAgent
{
	public  BackupAgent() 
	{
		super((android.content.Context)null); 
	}

	public void callCallbacks()
	{
		try{
			this.onCreate();
			this.onDestroy();
			this.onBackup(null, null, null);
			this.onRestore(null, 0, null);
			this.onFullBackup(null);
			this.onRestoreFile(null, 0L, null, 0, 0L, 0L);
		}catch(IOException e){
		}
	}
}