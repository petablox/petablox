class Context
{
	@STAMP(flows={@Flow(from="$Context.getString",to="@return")})
	public final  java.lang.String getString(int resId) 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="$Context.getString",to="@return")})
	public final  java.lang.String getString(int resId, java.lang.Object... formatArgs)
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="$Context.getText",to="@return")})
	public final  java.lang.CharSequence getText(int resId) 
	{
		return new String();
	}
}