class OutputStreamWriter
{
	@STAMP(flows = {@Flow(from="out",to="this")})
	public OutputStreamWriter(java.io.OutputStream out) {
    }

	@STAMP(flows = {@Flow(from="out",to="this")})
    public OutputStreamWriter(java.io.OutputStream out, java.lang.String enc) throws java.io.UnsupportedEncodingException {
    }

	@STAMP(flows = {@Flow(from="out",to="this")})
    public OutputStreamWriter(java.io.OutputStream out, java.nio.charset.Charset cs) {
    }

	@STAMP(flows = {@Flow(from="out",to="this")})
    public OutputStreamWriter(java.io.OutputStream out, java.nio.charset.CharsetEncoder enc) {
    }

	@STAMP(flows = {@Flow(from="buffer",to="!this")})
	public void write(char[] buffer, int offset, int count) throws java.io.IOException 
	{ 
		return; 
	}

	@STAMP(flows = {@Flow(from="oneChar",to="!this")})
	public void write(int oneChar) throws java.io.IOException {
		return; 
	}

	@STAMP(flows = {@Flow(from="str",to="!this")})
	public void write(java.lang.String str, int offset, int count) throws java.io.IOException {
		return; 
	}
}
