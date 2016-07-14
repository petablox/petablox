class Fragment
{
	public android.view.View view;
	public android.view.StampLayoutInflater stamp_inflater;

	public  Fragment()
	{
	}
	
	public void callCallbacks()
	{
		this.onHiddenChanged(false);
		this.onActivityResult(0, 0, null);
		this.onInflate(null, null);
		this.onInflate(null, null, null);
		this.onAttach(null);
		this.onCreate(null);
		this.view = this.onCreateView(null, null, null);
		this.onViewCreated(this.view, null);
		this.onActivityCreated(null);
		this.onStart();
		this.onResume();
		this.onSaveInstanceState(null);
		this.onConfigurationChanged(null);
		this.onPause();
		this.onStop();
		this.onLowMemory();
		this.onTrimMemory(0);
		this.onDestroyView();
		this.onDestroy();
		this.onDetach();
		this.onCreateOptionsMenu(null, null);
		this.onPrepareOptionsMenu(null);
		this.onDestroyOptionsMenu();
		this.onOptionsItemSelected(null);
		this.onOptionsMenuClosed(null);
		this.onCreateContextMenu(null, null, null);
		this.onContextItemSelected(null);
	}
	
	public  android.view.View getView() { 
		return this.view;
	}
	
}