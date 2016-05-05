class Integer
{

    @STAMP(flows = {@Flow(from="value",to="this")})
	public  Integer(int value) { }

    @STAMP(flows = {@Flow(from="string",to="this")})
	public  Integer(java.lang.String string) throws java.lang.NumberFormatException { }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  byte byteValue() {return (byte) 0; }

    public  int compareTo(java.lang.Integer object) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Integer decode(java.lang.String string) throws java.lang.NumberFormatException {return new Integer(0); }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  double doubleValue() { return 0.0; }

    public  boolean equals(java.lang.Object o) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  float floatValue() { return 0.0f; }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Integer getInteger(java.lang.String string) { return new Integer(0); }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Integer getInteger(java.lang.String string, int defaultValue) { return new Integer(0); }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Integer getInteger(java.lang.String string, java.lang.Integer defaultValue) { return new Integer(0); }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  int hashCode() { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  int intValue() { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  long longValue() { return 0;}

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  int parseInt(java.lang.String string) throws java.lang.NumberFormatException {return 0; }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  int parseInt(java.lang.String string, int radix) throws java.lang.NumberFormatException { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  short shortValue() {return 0; }

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  java.lang.String toBinaryString(int i) { return new String(); }
    
    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  java.lang.String toHexString(int i) { return new String(); }
    
    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  java.lang.String toOctalString(int i) { return new String(); }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.lang.String toString() {return new String(); }

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  java.lang.String toString(int i) { return new String(); }

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  java.lang.String toString(int i, int radix) { return new String(); }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Integer valueOf(java.lang.String string) throws java.lang.NumberFormatException {return new Integer(0); }

    @STAMP(flows = {@Flow(from="string",to="@return")})
	public static  java.lang.Integer valueOf(java.lang.String string, int radix) throws java.lang.NumberFormatException { return new Integer(0); }

    public static  int highestOneBit(int i) { throw new RuntimeException("Stub!"); }
    public static  int lowestOneBit(int i) { throw new RuntimeException("Stub!"); }
    public static  int numberOfLeadingZeros(int i) { throw new RuntimeException("Stub!"); }
    public static  int numberOfTrailingZeros(int i) { throw new RuntimeException("Stub!"); }
    public static  int bitCount(int i) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  int rotateLeft(int i, int distance) { return 0;}

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  int rotateRight(int i, int distance) { return 0;}

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  int reverseBytes(int i) { return 0; }

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  int reverse(int i) { return 0; }
    
    public static  int signum(int i) { throw new RuntimeException("Stub!"); }

    @STAMP(flows = {@Flow(from="i",to="@return")})
	public static  java.lang.Integer valueOf(int i) {return new Integer(0); }
}
