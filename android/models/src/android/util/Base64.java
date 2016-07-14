class Base64
{
	@STAMP(flows = {@Flow(from="str",to="@return")})
	public static  byte[] decode(java.lang.String str, int flags) { return new byte[0]; }

	@STAMP(flows = {@Flow(from="input",to="@return")})
	public static  byte[] decode(byte[] input, int flags) { return new byte[0]; }

	@STAMP(flows = {@Flow(from="input",to="@return")})
	public static  byte[] decode(byte[] input, int offset, int len, int flags) { return new byte[0]; }

	@STAMP(flows = {@Flow(from="input",to="@return")})
	public static  java.lang.String encodeToString(byte[] input, int flags) { return new String(); }

	@STAMP(flows = {@Flow(from="input",to="@return")})
	public static  java.lang.String encodeToString(byte[] input, int offset, int len, int flags) { return new String(); }

	@STAMP(flows = {@Flow(from="input",to="@return")})
	public static  byte[] encode(byte[] input, int flags) { return new byte[0]; }

	@STAMP(flows = {@Flow(from="input",to="@return")})
	public static  byte[] encode(byte[] input, int offset, int len, int flags) { return new byte[0]; }
}
