class String
{
    //public String() { }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data) { }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, int high) { }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, int start, int length) { }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, int high, int start, int length) {  }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, int start, int length, java.lang.String charsetName) throws java.io.UnsupportedEncodingException {  }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, java.lang.String charsetName) throws java.io.UnsupportedEncodingException {  }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, int start, int length, java.nio.charset.Charset charset) {  }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(byte[] data, java.nio.charset.Charset charset) {  }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(char[] data) {  }

	@STAMP(flows = {@Flow(from="data",to="this")})
    public  String(char[] data, int start, int length) {  }

	@STAMP(flows = {@Flow(from="toCopy",to="this")})
    public  String(java.lang.String toCopy) {  }

	@STAMP(flows = {@Flow(from="stringbuffer",to="this")})
    public  String(java.lang.StringBuffer stringbuffer) {  }

	@STAMP(flows = {@Flow(from="codePoints",to="this")})
    public  String(int[] codePoints, int offset, int count) {  }

	@STAMP(flows = {@Flow(from="sb",to="this")})
    public  String(java.lang.StringBuilder sb) {  }
	
	@STAMP(flows = {@Flow(from="this",to="@return"), @Flow(from="string",to="@return")})
	public  java.lang.String concat(java.lang.String string) { return new String(); }

	@STAMP(flows = {@Flow(from="data",to="@return")})
	public static java.lang.String copyValueOf(char[] data) { return new String(); }

	@STAMP(flows = {@Flow(from="data",to="@return")})
    public static  java.lang.String copyValueOf(char[] data, int start, int length) { return new String(); }

	@STAMP(flows = {@Flow(from="this",to="data")})
    public  void getBytes(int start, int end, byte[] data, int index) {  }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  byte[] getBytes() { return new byte[0]; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public  byte[] getBytes(java.lang.String charsetName) throws java.io.UnsupportedEncodingException { return new byte[0]; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public  byte[] getBytes(java.nio.charset.Charset charset) { return new byte[0]; }

	@STAMP(flows = {@Flow(from="this",to="buffer")})
    public  void getChars(int start, int end, char[] buffer, int index) {  }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public native  java.lang.String intern();

	@STAMP(flows = {@Flow(from="this",to="@return"), @Flow(from="newChar",to="@return")})
	public  java.lang.String replace(char oldChar, char newChar) { return new String(); }

	@STAMP(flows = {@Flow(from="this",to="@return"), @Flow(from="replacement",to="@return")})
	public  java.lang.String replace(java.lang.CharSequence target, java.lang.CharSequence replacement) { return new String(); }
	
    public  java.lang.String substring(int start) { return this; }

    public  java.lang.String substring(int start, int end) { return this; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  char[] toCharArray() { return new char[0]; }

    public  java.lang.String toLowerCase() { return this; }

    public  java.lang.String toLowerCase(java.util.Locale locale) { return this; }

	public  java.lang.String toString() { return this; }

	public  java.lang.String toUpperCase() { return this; }

    public  java.lang.String toUpperCase(java.util.Locale locale) { return this; }

	public  java.lang.String trim() { return this; }

	@STAMP(flows = {@Flow(from="data",to="@return")})
	public static  java.lang.String valueOf(char[] data) { return new String(); }

	@STAMP(flows = {@Flow(from="data",to="@return")})
	public static  java.lang.String valueOf(char[] data, int start, int length) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(char value) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(double value) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(float value) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(int value) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(long value) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(java.lang.Object value) { return new String(); }

	@STAMP(flows = {@Flow(from="value",to="@return")})
	public static  java.lang.String valueOf(boolean value) { return new String(); }

	@STAMP(flows = {@Flow(from="this",to="@return"), @Flow(from="replacement",to="@return")})
	public  java.lang.String replaceAll(java.lang.String regularExpression, java.lang.String replacement) { return new String(); }

	@STAMP(flows = {@Flow(from="this",to="@return"), @Flow(from="replacement",to="@return")})
	public  java.lang.String replaceFirst(java.lang.String regularExpression, java.lang.String replacement) { return new String(); }

    public  java.lang.String[] split(java.lang.String regularExpression) 
	{ 
		String[] ret = new String[1];
		ret[0] = this; 
		return ret;
	}

    public  java.lang.String[] split(java.lang.String regularExpression, int limit) 
	{ 
		String[] ret = new String[1];
		ret[0] = this;
		return ret;
	}

	public  java.lang.CharSequence subSequence(int start, int end) { return this; }

	@STAMP(flows = {@Flow(from="format",to="@return"), @Flow(from="args",to="@return")})
	public static  java.lang.String format(java.lang.String format, java.lang.Object... args) { return new String(); }

	@STAMP(flows = {@Flow(from="format",to="@return"), @Flow(from="args",to="@return")})
	public static  java.lang.String format(java.util.Locale locale, java.lang.String format, java.lang.Object... args) { return new String(); }

	@STAMP(flows = {@Flow(from="prefix",to="this")})
	public  boolean startsWith(java.lang.String prefix) 
	{ 
		return false;
	}

	@STAMP(flows = {@Flow(from="prefix",to="this")})
	public  boolean startsWith(java.lang.String prefix, int start) 
	{ 
		return false;
	}
}
