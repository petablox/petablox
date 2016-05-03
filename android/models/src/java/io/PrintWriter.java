class PrintWriter
{
	@STAMP(flows = {@Flow(from="out",to="this")})
	public  PrintWriter(java.io.OutputStream out) {  }

	@STAMP(flows = {@Flow(from="out",to="this"), @Flow(from="autoFlush",to="this")})
	public  PrintWriter(java.io.OutputStream out, boolean autoFlush) {  }

	@STAMP(flows = {@Flow(from="wr",to="this")})
	public  PrintWriter(java.io.Writer wr) {  }

	@STAMP(flows = {@Flow(from="wr",to="this"), @Flow(from="autoFlush",to="this")})
	public  PrintWriter(java.io.Writer wr, boolean autoFlush) {  }

	@STAMP(flows = {@Flow(from="file",to="this")})
	public  PrintWriter(java.io.File file) throws java.io.FileNotFoundException {  }

	@STAMP(flows = {@Flow(from="file",to="this")})
	public  PrintWriter(java.io.File file, java.lang.String csn) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException {  }

	@STAMP(flows = {@Flow(from="fileName",to="this")})
	public  PrintWriter(java.lang.String fileName) throws java.io.FileNotFoundException {  }

	@STAMP(flows = {@Flow(from="fileName",to="this")})
	public  PrintWriter(java.lang.String fileName, java.lang.String csn) throws java.io.FileNotFoundException, java.io.UnsupportedEncodingException {  }

	@STAMP(flows = {@Flow(from="charArray",to="!this")})
	public  void print(char[] charArray) {  }
	
	@STAMP(flows = {@Flow(from="ch",to="!this")})
	public  void print(char ch) {  }

	@STAMP(flows = {@Flow(from="dnum",to="!this")})
	public  void print(double dnum) {  }

	@STAMP(flows = {@Flow(from="fnum",to="!this")})
	public  void print(float fnum) {  }

	@STAMP(flows = {@Flow(from="inum",to="!this")})
	public  void print(int inum) {  }

	@STAMP(flows = {@Flow(from="lnum",to="!this")})
	public  void print(long lnum) {  }

	@STAMP(flows = {@Flow(from="obj",to="!this")})
	public  void print(java.lang.Object obj) {  }

	@STAMP(flows = {@Flow(from="str",to="!this")})
	public  void print(java.lang.String str) {  }

	@STAMP(flows = {@Flow(from="bool",to="!this")})
	public  void print(boolean bool) {  }
	
	@STAMP(flows = {@Flow(from="chars",to="!this")})
	public  void println(char[] chars) {  }

	@STAMP(flows = {@Flow(from="c",to="!this")})
	public  void println(char c) {  }

	@STAMP(flows = {@Flow(from="d",to="!this")})
	public  void println(double d) {  }

	@STAMP(flows = {@Flow(from="f",to="!this")})
	public  void println(float f) {  }

	@STAMP(flows = {@Flow(from="i",to="!this")})
	public  void println(int i) {  }

	@STAMP(flows = {@Flow(from="l",to="!this")})
	public  void println(long l) {  }

	@STAMP(flows = {@Flow(from="obj",to="!this")})
	public  void println(java.lang.Object obj) {  }

	@STAMP(flows = {@Flow(from="str",to="!this")})
	public  void println(java.lang.String str) {  }

	@STAMP(flows = {@Flow(from="b",to="!this")})
	public  void println(boolean b) {  }

	@STAMP(flows = {@Flow(from="buf",to="!this")})
	public  void write(char[] buf) {  }

	@STAMP(flows = {@Flow(from="buf",to="!this")})
	public  void write(char[] buf, int offset, int count) {  }

	@STAMP(flows = {@Flow(from="oneChar",to="!this")})
	public  void write(int oneChar) {  }

	@STAMP(flows = {@Flow(from="str",to="!this")})
	public  void write(java.lang.String str) {  }

	@STAMP(flows = {@Flow(from="str",to="!this")})
	public  void write(java.lang.String str, int offset, int count) {  }

	@STAMP(flows = {@Flow(from="c",to="!this")})
	public  java.io.PrintWriter append(char c) {  return this; }

	@STAMP(flows = {@Flow(from="csq",to="!this")})
	public  java.io.PrintWriter append(java.lang.CharSequence csq) { return this; }

	@STAMP(flows = {@Flow(from="csq",to="!this")})
	public  java.io.PrintWriter append(java.lang.CharSequence csq, int start, int end) { return this;  }
}
