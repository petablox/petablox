public class ListActivity
{
    public void callCallbacks()
	{
		super.callCallbacks();
		
		this.onListItemClick(null, null, 0, 0l);
		this.onRestoreInstanceState(null);
		this.onDestroy(); 
		this.onContentChanged(); 
	}

}
