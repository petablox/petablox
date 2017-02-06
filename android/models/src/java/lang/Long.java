class Long
{
	@STAMP(flows={@Flow(from="value",to="this")})
	public  Long(long value) { }

	@STAMP(flows={@Flow(from="string",to="this")})
	public  Long(java.lang.String string) throws java.lang.NumberFormatException { }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  byte byteValue() { return (byte)0; }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Long decode(java.lang.String string) throws java.lang.NumberFormatException { return new Long(0L); }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  double doubleValue() { return 0.0; }
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  float floatValue() { return 0.0f; }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Long getLong(java.lang.String string) { return new Long(0L); }

	@STAMP(flows={@Flow(from="string",to="@return"),@Flow(from="defaultValue",to="@return")})
	public static  java.lang.Long getLong(java.lang.String string, long defaultValue) { return new Long(0L); }

	@STAMP(flows={@Flow(from="string",to="@return"),@Flow(from="defaultValue",to="@return")})
	public static  java.lang.Long getLong(java.lang.String string, java.lang.Long defaultValue) { return new Long(0L); }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int intValue() { return 0; }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  long longValue() { return 0L; }

	@STAMP(flows={@Flow(from="string",to="@return")}) 
	public static  long parseLong(java.lang.String string) throws java.lang.NumberFormatException { return new Long(0L); }

	@STAMP(flows={@Flow(from="string",to="@return")}) 
	public static  long parseLong(java.lang.String string, int radix) throws java.lang.NumberFormatException { return new Long(0L); }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  short shortValue() { return (short)0; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  java.lang.String toBinaryString(long v) { return new String(); }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  java.lang.String toHexString(long v) { return new String(); }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  java.lang.String toOctalString(long v) { return new String(); }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.lang.String toString() { return new String(); }
	
	@STAMP(flows={@Flow(from="n",to="@return")})
	public static  java.lang.String toString(long n) { return new String(); }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  java.lang.String toString(long v, int radix) { return new String(); }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Long valueOf(java.lang.String string) throws java.lang.NumberFormatException { return new Long(0L); }

	@STAMP(flows={@Flow(from="string",to="@return")})
	public static  java.lang.Long valueOf(java.lang.String string, int radix) throws java.lang.NumberFormatException { return new Long(0L); }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  long highestOneBit(long v) { return 0L; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  long lowestOneBit(long v) { return 0L; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  int numberOfLeadingZeros(long v) { return 0; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  int numberOfTrailingZeros(long v) { return 0; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  int bitCount(long v) { return 0; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  long rotateLeft(long v, int distance) { return 0L; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  long rotateRight(long v, int distance) { return 0L; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  long reverseBytes(long v) { return 0L; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  long reverse(long v) { return 0L; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  int signum(long v) { return 0; }

	@STAMP(flows={@Flow(from="v",to="@return")})
	public static  java.lang.Long valueOf(long v) { return new Long(0L); }
}
