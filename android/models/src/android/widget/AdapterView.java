import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class AdapterView
{
	public  void setOnItemClickListener(final android.widget.AdapterView.OnItemClickListener listener) 
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						listener.onItemClick(AdapterView.this, null, 0, 0L);
					}
				});
	}
	
	public  void setOnItemLongClickListener(final android.widget.AdapterView.OnItemLongClickListener listener)
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						listener.onItemLongClick(AdapterView.this, null, 0, 0L);
					}
				});
	}

	public  void setOnItemSelectedListener(final android.widget.AdapterView.OnItemSelectedListener listener) 
	{ 
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						listener.onItemSelected(AdapterView.this, null, 0, 0L);
					}
				});

		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						listener.onNothingSelected(AdapterView.this);
					}
				});
	}

	
}