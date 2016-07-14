class ListView
{
    public  void setAdapter(android.widget.ListAdapter adapter)
    {
        this.adapter = adapter;
    }

	void addHeaderView(android.view.View v, java.lang.Object data, boolean isSelectable)
	{
		this.child = v;
	}
	
	void addHeaderView(android.view.View v) {
		this.child = v;
	}
}