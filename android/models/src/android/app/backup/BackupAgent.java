import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;
import java.io.IOException;

class BackupAgent
{
	public  BackupAgent() 
	{
		super((android.content.Context)null); 

		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						try{
							BackupAgent.this.onCreate();
							BackupAgent.this.onDestroy();
							BackupAgent.this.onBackup(null, null, null);
							BackupAgent.this.onRestore(null, 0, null);
							BackupAgent.this.onFullBackup(null);
							BackupAgent.this.onRestoreFile(null, 0L, null, 0, 0L, 0L);
						}catch(IOException e){
						}
					}
				}); 
	}
}