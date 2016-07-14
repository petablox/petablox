class ViewGroup
{
	public android.view.View child;

	public  void addView(android.view.View child) { 
		this.child = child; 
		this.stamp_inflater = child.stamp_inflater;
	}

	public  void addView(android.view.View child, int index) { 
		this.child = child; 
		this.stamp_inflater = child.stamp_inflater;
	}

	public  void addView(android.view.View child, int width, int height) { 
		this.child = child; 
		this.stamp_inflater = child.stamp_inflater;
	}

	public  void addView(android.view.View child, android.view.ViewGroup.LayoutParams params) { 
		this.child = child; 
		this.stamp_inflater = child.stamp_inflater;
	}

	public  void addView(android.view.View child, int index, android.view.ViewGroup.LayoutParams params) { 
		this.child = child; 
		this.stamp_inflater = child.stamp_inflater;
	}
	
	protected  void attachViewToParent(android.view.View child, int index, android.view.ViewGroup.LayoutParams params)
	{
		this.child = child; 
		this.stamp_inflater = child.stamp_inflater;		
	}
	
	public  android.view.View getChildAt(int index) 
	{
		return this.child;
	}
	
	public  android.view.View getFocusedChild()
	{
		return this.child;
	}
}