import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class AbstractThreadedSyncAdapter
{
	public AbstractThreadedSyncAdapter(android.content.Context context, boolean autoInitialize)  
	{
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						AbstractThreadedSyncAdapter.this.onPerformSync(null, new android.os.Bundle(), new String(), null, null);
					}
				});

		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						AbstractThreadedSyncAdapter.this.onSyncCanceled();
					}
				});

		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						AbstractThreadedSyncAdapter.this.onSyncCanceled(null);
					}
				});
	}
}

