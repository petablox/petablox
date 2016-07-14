public class SpannableStringBuilder
{
	@STAMP(flows={@Flow(from="text",to="this")})
	public  SpannableStringBuilder(java.lang.CharSequence text) 
	{ 
	}

	@STAMP(flows={@Flow(from="text",to="this")})
	public  SpannableStringBuilder(java.lang.CharSequence text, int start, int end) 
	{ 
	}

	@STAMP(flows={@Flow(from="source",to="@return")})
	public static  android.text.SpannableStringBuilder valueOf(java.lang.CharSequence source) 
	{ 
		return new SpannableStringBuilder();
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  char charAt(int where) 
	{ 
		return 'a';
	}

	@STAMP(flows={@Flow(from="tb",to="this")})
	public android.text.SpannableStringBuilder insert(int where, java.lang.CharSequence tb, int start, int end) 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="tb",to="this")})
	public  android.text.SpannableStringBuilder insert(int where, java.lang.CharSequence tb) 
	{ 
		return this;
	}

	public  android.text.SpannableStringBuilder delete(int start, int end) 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="text",to="this")})
	public  android.text.SpannableStringBuilder append(java.lang.CharSequence text) 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="text",to="this")})
	public  android.text.SpannableStringBuilder append(java.lang.CharSequence text, int start, int end) 
	{ 
		return this;
	}
	
	@STAMP(flows={@Flow(from="text",to="this")})
	public  android.text.SpannableStringBuilder append(char text) 
	{ 
		return this;
	}

	public  android.text.SpannableStringBuilder replace(int start, int end, java.lang.CharSequence tb) 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="tb",to="this")})
	public  android.text.SpannableStringBuilder replace(int start, int end, java.lang.CharSequence tb, int tbstart, int tbend) 
	{ 
		return this;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.CharSequence subSequence(int start, int end) 
	{ 
		return new java.lang.String();
	}
	
	@STAMP(flows={@Flow(from="this",to="dest")})
	public  void getChars(int start, int end, char[] dest, int destoff) 
	{ 
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.String toString()
	{
		return new java.lang.String();
	}
}
