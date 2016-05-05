class StringBuilder
{
	@STAMP(flows = {@Flow(from="seq",to="this")})
    public  StringBuilder(java.lang.CharSequence seq) { }

	@STAMP(flows = {@Flow(from="str",to="this")})
    public StringBuilder(java.lang.String str) { }

	@STAMP(flows = {@Flow(from="b",to="this")})
    public  java.lang.StringBuilder append(boolean b) { return this; }

	@STAMP(flows = {@Flow(from="c",to="this")})
    public  java.lang.StringBuilder append(char c) { return this; }

	@STAMP(flows = {@Flow(from="i",to="this")})
    public  java.lang.StringBuilder append(int i) { return this; }

	@STAMP(flows = {@Flow(from="l",to="this")})
    public  java.lang.StringBuilder append(long l) { return this; }

	@STAMP(flows = {@Flow(from="f",to="this")})
    public  java.lang.StringBuilder append(float f) { return this; }

	@STAMP(flows = {@Flow(from="d",to="this")})
    public  java.lang.StringBuilder append(double d) { return this; }

	@STAMP(flows = {@Flow(from="obj",to="this")})
    public  java.lang.StringBuilder append(java.lang.Object obj) { return this; }

	@STAMP(flows = {@Flow(from="str",to="this")})
    public  java.lang.StringBuilder append(java.lang.String str) { return this; }

	@STAMP(flows = {@Flow(from="sb",to="this")})
    public  java.lang.StringBuilder append(java.lang.StringBuffer sb) { return this; }

	@STAMP(flows = {@Flow(from="chars",to="this")})
    public  java.lang.StringBuilder append(char[] chars) { return this; }

	@STAMP(flows = {@Flow(from="str",to="this")})
    public  java.lang.StringBuilder append(char[] str, int offset, int len) { return this; }

	@STAMP(flows = {@Flow(from="csq",to="this")})
    public  java.lang.StringBuilder append(java.lang.CharSequence csq) { return this; }

	@STAMP(flows = {@Flow(from="csq",to="this")})
    public  java.lang.StringBuilder append(java.lang.CharSequence csq, int start, int end) 	{ return this; }

	@STAMP(flows = {@Flow(from="codePoint",to="this")})
    public  java.lang.StringBuilder appendCodePoint(int codePoint) { return this; }

	@STAMP(flows = {@Flow(from="b",to="this")})
    public  java.lang.StringBuilder insert(int offset, boolean b) { return this; }

	@STAMP(flows = {@Flow(from="c",to="this")})
    public  java.lang.StringBuilder insert(int offset, char c) { return this; }

	@STAMP(flows = {@Flow(from="i",to="this")})
    public  java.lang.StringBuilder insert(int offset, int i) { return this; }

	@STAMP(flows = {@Flow(from="l",to="this")})
    public  java.lang.StringBuilder insert(int offset, long l) { return this; }

	@STAMP(flows = {@Flow(from="f",to="this")})
    public  java.lang.StringBuilder insert(int offset, float f) { return this; }

	@STAMP(flows = {@Flow(from="d",to="this")})
    public  java.lang.StringBuilder insert(int offset, double d) { return this; }

	@STAMP(flows = {@Flow(from="obj",to="this")})
    public  java.lang.StringBuilder insert(int offset, java.lang.Object obj) { return this; }

	@STAMP(flows = {@Flow(from="str",to="this")})
    public  java.lang.StringBuilder insert(int offset, java.lang.String str) { return this; }

	@STAMP(flows = {@Flow(from="ch",to="this")})
    public  java.lang.StringBuilder insert(int offset, char[] ch) { return this; }

	@STAMP(flows = {@Flow(from="str",to="this")})
    public  java.lang.StringBuilder insert(int offset, char[] str, int strOffset, int strLen) { return this; }

	@STAMP(flows = {@Flow(from="s",to="this")})
    public  java.lang.StringBuilder insert(int offset, java.lang.CharSequence s) { return this; }

	@STAMP(flows = {@Flow(from="s",to="this")})
    public  java.lang.StringBuilder insert(int offset, java.lang.CharSequence s, int start, int end) { return this; }

	@STAMP(flows = {@Flow(from="str",to="this")})
    public  java.lang.StringBuilder replace(int start, int end, java.lang.String str) { return this; }

    public  java.lang.StringBuilder reverse() { return this; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public  java.lang.String toString() { return new String(); }

    public java.lang.StringBuilder delete(int start, int end) {
		return this;
    }

    public java.lang.StringBuilder deleteCharAt(int index) {
		return this;
    }

}
