import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class BaseAdapter
{
	public  BaseAdapter() 
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						BaseAdapter.this.getCount();
						BaseAdapter.this.getView(0, null, null);
						BaseAdapter.this.getItem(0);
						BaseAdapter.this.getItemId(0);
						BaseAdapter.this.getItemViewType(0);
						BaseAdapter.this.getViewTypeCount();
					}
				});
	}
}