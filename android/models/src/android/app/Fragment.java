import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class Fragment
{
	public  Fragment()
	{
		ApplicationDriver.getInstance().
			registerCallback(new Callback(){
					public void run() {
						Fragment.this.onHiddenChanged(false);
						Fragment.this.onActivityResult(0, 0, null);
						Fragment.this.onInflate(null, null);
						Fragment.this.onInflate(null, null, null);
						Fragment.this.onAttach(null);
						Fragment.this.onCreate(null);
						Fragment.this.onViewCreated(null, null);
						Fragment.this.onCreateView(null, null, null);
						Fragment.this.onActivityCreated(null);
						Fragment.this.onStart();
						Fragment.this.onResume();
						Fragment.this.onSaveInstanceState(null);
						Fragment.this.onConfigurationChanged(null);
						Fragment.this.onPause();
						Fragment.this.onStop();
						Fragment.this.onLowMemory();
						Fragment.this.onTrimMemory(0);
						Fragment.this.onDestroyView();
						Fragment.this.onDestroy();
						Fragment.this.onDetach();
						Fragment.this.onCreateOptionsMenu(null, null);
						Fragment.this.onPrepareOptionsMenu(null);
						Fragment.this.onDestroyOptionsMenu();
						Fragment.this.onOptionsItemSelected(null);
						Fragment.this.onOptionsMenuClosed(null);
						Fragment.this.onCreateContextMenu(null, null, null);
						Fragment.this.onContextItemSelected(null);
					}
				}); 

	}
}