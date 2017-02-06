import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class Service
{
	public  Service() 
	{ 
		super((android.content.Context)null); 
		
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						Service.this.onCreate();
						Service.this.onStart(new android.content.Intent(), 0);
						Service.this.onStartCommand(new android.content.Intent(), 0, 0);
						Service.this.onDestroy();
						Service.this.onConfigurationChanged(null);
						Service.this.onLowMemory();
						Service.this.onUnbind(new android.content.Intent());
						Service.this.onRebind(new android.content.Intent());
						Service.this.onBind(new android.content.Intent());
					}
				});
	}		
}
