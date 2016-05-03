class View
{
	public  View(android.content.Context context) 
	{ 
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						View.this.onFinishInflate();

						View.this.onMeasure(0,0);
						View.this.onLayout(false, 0, 0, 0, 0);
						View.this.onSizeChanged(0, 0, 0, 0);

						View.this.onDraw(null);
						
						View.this.onTouchEvent(null);
						View.this.onTrackballEvent(null);
						View.this.onKeyUp(0, null);
						View.this.onKeyDown(0, null);
						
						View.this.onFocusChanged(false, 0, null);
						View.this.onWindowFocusChanged(false);
						
						View.this.onAttachedToWindow();
						View.this.onDetachedFromWindow();
						View.this.onWindowVisibilityChanged(0);
					}
				});
	}

	public final android.view.View findViewById(int id) {
        return new android.view.View(null);
    }

    public  void setOnFocusChangeListener(final android.view.View.OnFocusChangeListener l) 
	{ 
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						l.onFocusChange(View.this, false);
					}
				});
	}
	
    // Callback classes and callback setter methods                                                                                                                            
    public  void setOnClickListener(final android.view.View.OnClickListener l) 
    { 
        edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						l.onClick(View.this);
					}
				});
    }
	
    public  void setOnLongClickListener(final android.view.View.OnLongClickListener l) 
    { 
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						l.onLongClick(View.this);
					}
				});
    }
	
    public  void setOnCreateContextMenuListener(final android.view.View.OnCreateContextMenuListener l) 
	{ 
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						l.onCreateContextMenu(null, View.this, null);
					}
				});
	}

    public  void setOnKeyListener(final android.view.View.OnKeyListener l) 
	{ 
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						l.onKey(View.this, 0, null);
					}
				});
	}
    public  void setOnTouchListener(final android.view.View.OnTouchListener l) 
	{ 
		edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						l.onTouch(View.this, null);
					}
				});
	}
}
