class InputStreamReader
{
	@STAMP(flows = {@Flow(from="in",to="this")})
	public  InputStreamReader(java.io.InputStream in) {}

	@STAMP(flows = {@Flow(from="in",to="this"), @Flow(from="enc",to="this")})
	public  InputStreamReader(java.io.InputStream in, java.lang.String enc) throws java.io.UnsupportedEncodingException {}

	@STAMP(flows = {@Flow(from="in",to="this"), @Flow(from="dec",to="this")})
	public  InputStreamReader(java.io.InputStream in, java.nio.charset.CharsetDecoder dec) {}

	@STAMP(flows = {@Flow(from="in",to="this"), @Flow(from="charset",to="this")})
	public  InputStreamReader(java.io.InputStream in, java.nio.charset.Charset charset) {}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  int read() throws java.io.IOException { return 0; }

	@STAMP(flows = {@Flow(from="this",to="buffer"), @Flow(from="length",to="@return")})
	public  int read(char[] buffer, int offset, int length) throws java.io.IOException { return 0; }
}
