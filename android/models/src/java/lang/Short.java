class Short
{
	@STAMP(flows={@Flow(from="string",to="this")})
	public  Short(java.lang.String string) throws java.lang.NumberFormatException {  }

	@STAMP(flows={@Flow(from="value",to="this")})
	public  Short(short value) {  }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  byte byteValue() { return (byte)0; }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Short decode(java.lang.String string) throws java.lang.NumberFormatException { return new Short((short)0); }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  double doubleValue() { return 0.0; }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  float floatValue() { return 0.0f; }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int intValue() { return 0; }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  long longValue() { return 0L; }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  short parseShort(java.lang.String string) throws java.lang.NumberFormatException { return (short)0; }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  short parseShort(java.lang.String string, int radix) throws java.lang.NumberFormatException { return (short) 0; }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  short shortValue() { return (short)0; }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String toString() { return new String(); }

	@STAMP(flows={@Flow(from="value",to="@return")})
	public static  java.lang.String toString(short value) { return new String(); }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Short valueOf(java.lang.String string) throws java.lang.NumberFormatException { return new Short((short)0); }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Short valueOf(java.lang.String string, int radix) throws java.lang.NumberFormatException { return new Short((short)0); }

	@STAMP(flows={@Flow(from="s",to="@return")})
	public static  short reverseBytes(short s) { return (short)0; }

	@STAMP(flows={@Flow(from="s",to="@return")})
	public static  java.lang.Short valueOf(short s) { return new Short((short)0); }

}