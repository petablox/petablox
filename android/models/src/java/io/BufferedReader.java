class BufferedReader
{
	@STAMP(flows = {@Flow(from="in",to="this")})
	public BufferedReader(java.io.Reader in) {}

	@STAMP(flows = {@Flow(from="in",to="this")})
	public BufferedReader(java.io.Reader in, int size) {}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public int read() throws java.io.IOException { return 0; }

	@STAMP(flows = {@Flow(from="this",to="buffer"), @Flow(from="length",to="buffer"), @Flow(from="length",to="this"), @Flow(from="length",to="@return")})
	public int read(char[] buffer, int offset, int length) throws java.io.IOException { return 0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public java.lang.String readLine() throws java.io.IOException { return new String(); }
}
