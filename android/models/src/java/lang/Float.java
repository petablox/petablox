class Float
{
    @STAMP(flows = {@Flow(from="value",to="this")})
	public  Float(float value) {  }

    @STAMP(flows = {@Flow(from="value",to="this")})
	public  Float(double value) {  }

    @STAMP(flows = {@Flow(from="string",to="this")})
	public  Float(java.lang.String string) throws java.lang.NumberFormatException {  }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  byte byteValue() { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  double doubleValue() { return 0.0; }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static native  int floatToIntBits(float value);

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static native  int floatToRawIntBits(float value);
 
    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  float floatValue() {  return 0.0f; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  int hashCode() { return 0; }

	@STAMP(flows = {@Flow(from="bits",to="@return")})
	public static native  float intBitsToFloat(int bits);

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  int intValue() { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  long longValue() { return 0L; }

	@STAMP(flows = {@Flow(from="string",to="@return")})
	public static  float parseFloat(java.lang.String string) throws java.lang.NumberFormatException { return 0.0f; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  short shortValue() { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.lang.String toString() { return new String(); }

    @STAMP(flows = {@Flow(from="f",to="@return")})
	public static  java.lang.String toString(float f) { return new String(); }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Float valueOf(java.lang.String string) throws java.lang.NumberFormatException { return new Float(0.0f); }

    @STAMP(flows = {@Flow(from="float1",to="@return"),@Flow(from="float2",to="@return")})
	public static  int compare(float float1, float float2) {  return 0; }

    @STAMP(flows = {@Flow(from="f",to="@return")})
	public static  java.lang.Float valueOf(float f) {  return new Float(0.0f); }

    @STAMP(flows = {@Flow(from="f",to="@return")})
	public static  java.lang.String toHexString(float f) { return new String();  }

}