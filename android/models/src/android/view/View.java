class View
{
	public android.view.StampLayoutInflater stamp_inflater;

	public  View(android.content.Context context) 
	{ 
	}
	
	public void callCallbacks()
	{
		onFinishInflate();

		onMeasure(0,0);
		onLayout(false, 0, 0, 0, 0);
		onSizeChanged(0, 0, 0, 0);
		
		onDraw(null);
						
		onTouchEvent(null);
		onTrackballEvent(null);

		onKeyPreIme(0, null);
		onKeyDown(0, null);
		onKeyLongPress(0, null);
		onKeyUp(0, null);
		onKeyMultiple(0, 0, null);
		onKeyShortcut(0, null);

		onCheckIsTextEditor();
		
		onFocusChanged(false, 0, null);
		onWindowFocusChanged(false);
		
		onAttachedToWindow();
		onDetachedFromWindow();
		onWindowVisibilityChanged(0);
	}

    public  void setOnFocusChangeListener(final android.view.View.OnFocusChangeListener l) 
	{ 
		l.onFocusChange(View.this, false);
	}
	
    // Callback classes and callback setter methods                                                                                                                            
    public  void setOnClickListener(final android.view.View.OnClickListener l) 
    { 
		l.onClick(View.this);
    }
	
    public  void setOnLongClickListener(final android.view.View.OnLongClickListener l) 
    { 
		l.onLongClick(View.this);
    }
	
    public  void setOnCreateContextMenuListener(final android.view.View.OnCreateContextMenuListener l) 
	{ 
		l.onCreateContextMenu(null, View.this, null);
	}

    public  void setOnKeyListener(final android.view.View.OnKeyListener l) 
	{ 
		l.onKey(View.this, 0, null);
	}

    public  void setOnTouchListener(final android.view.View.OnTouchListener l) 
	{ 
		l.onTouch(View.this, null);
	}

	public final  android.content.Context getContext() 
	{
		return this.stamp_inflater.context;
	}

	public  void setId(int id) 
	{
		//dont remove it. Othewise it would become a stub, but the
		//analysis uses it
	}
}
