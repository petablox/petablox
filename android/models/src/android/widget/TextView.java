class TextView
{
	public  TextView(android.content.Context context)
	{
		super(context);
	}		

	@STAMP(flows={@Flow(from="this",to="@return")})
	java.lang.CharSequence getText() 
	{ 
		return new String();
	}

	public  void setKeyListener(android.text.method.KeyListener input) 
	{ 
		input.onKeyDown(this, null, 0, null);
		input.onKeyOther(this, null, null);
		input.onKeyUp(this, null, 0, null);
	}

	public  void setOnEditorActionListener(android.widget.TextView.OnEditorActionListener l) 
	{ 
		l.onEditorAction(this, 0, null);
	}

	
}