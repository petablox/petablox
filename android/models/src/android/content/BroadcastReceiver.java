import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class BroadcastReceiver
{
	public  BroadcastReceiver() 
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						BroadcastReceiver.this.onReceive(null, new android.content.Intent());
					}
				}); 
	}
}