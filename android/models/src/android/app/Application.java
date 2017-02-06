import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class Application
{
	public Application()
	{
		super(null);
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						Application.this.onCreate();
						Application.this.onLowMemory();
						Application.this.onTerminate();
						Application.this.onTrimMemory(0);
					}
				});
	}
}