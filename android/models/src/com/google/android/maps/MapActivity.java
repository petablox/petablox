import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

public abstract class MapActivity
{

  public  MapActivity()
  {
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						MapActivity.this.onGetMapDataSource();
					}
				}); 
  }


}

