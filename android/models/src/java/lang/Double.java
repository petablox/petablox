class Double
{
	@STAMP(flows={@Flow(from="value",to="this")})
	public  Double(double value) 
	{ 
	}

	@STAMP(flows={@Flow(from="string",to="this")})
	public  Double(java.lang.String string) throws java.lang.NumberFormatException 
	{ 
	}

	@STAMP(flows={@Flow(from="object",to="@return")})
	public  int compareTo(java.lang.Double object) 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  byte byteValue() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="value",to="@return")})
	public static native  long doubleToLongBits(double value);

	@STAMP(flows={@Flow(from="value",to="@return")})
	public static native  long doubleToRawLongBits(double value);

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  double doubleValue() 
	{
		return 0.0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  float floatValue() 
	{ 
		return 0.0f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int hashCode() 
	{ 
		return 0;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int intValue() 
	{ 
		return 0;
	}
	
	@STAMP(flows={@Flow(from="bits",to="@return")})
	public static native  double longBitsToDouble(long bits);
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  long longValue() 
	{ 
		return 0L;
	}

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  double parseDouble(java.lang.String string) throws java.lang.NumberFormatException 
	{ 
		return 0.0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  short shortValue() 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String toString() 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static  java.lang.String toString(double d) 
	{ 
		return new String();
	}

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Double valueOf(java.lang.String string) throws java.lang.NumberFormatException 
	{ 
		return new Double(0.0);
	}

	@STAMP(flows={@Flow(from="double1",to="@return"),@Flow(from="double2",to="@return")})
	public static  int compare(double double1, double double2) 
	{ 
		return 0;
	}

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static  java.lang.Double valueOf(double d) 
	{ 
		return new Double(d);
	}

	@STAMP(flows={@Flow(from="d",to="@return")})
	public static  java.lang.String toHexString(double d) 
	{ 
		return new String();
	}
}