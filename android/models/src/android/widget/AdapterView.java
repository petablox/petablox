class AdapterView
{
    public android.widget.Adapter adapter;

    public  void setOnItemClickListener(final android.widget.AdapterView.OnItemClickListener listener)
    {
        android.view.View view = adapter.getView(0, null, this);
		this.child = view;
		listener.onItemClick(this, view, 0, 0L);
    }

    public  void setOnItemLongClickListener(final android.widget.AdapterView.OnItemLongClickListener listener)
    {
		android.view.View view = adapter.getView(0, null, this);
		this.child = view;
		listener.onItemLongClick(this, view, 0, 0L);
    }

    public  void setOnItemSelectedListener(final android.widget.AdapterView.OnItemSelectedListener listener)
    {
		android.view.View view = adapter.getView(0, null, this);
		this.child = view;
		listener.onItemSelected(this, view, 0, 0L);

		listener.onNothingSelected(this);
    }

    public void setOnClickListener(android.view.View.OnClickListener l) 
	{
		l.onClick(this);
    }
}