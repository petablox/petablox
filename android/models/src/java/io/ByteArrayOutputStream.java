class ByteArrayOutputStream
{
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public synchronized byte[] toByteArray() { 
		return new byte[1];
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public java.lang.String toString() { 
		return new String(); 
	}

	@STAMP(flows = {@Flow(from="this",to="@return"), @Flow(from="hibyte",to="@return")})
	public java.lang.String toString(int hibyte) { 
		return new String(); 
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public java.lang.String toString(java.lang.String enc) throws java.io.UnsupportedEncodingException {
		return new String(); 
	}

	@STAMP(flows = {@Flow(from="buffer",to="this"), @Flow(from="len",to="this")})
	public synchronized void write(byte[] buffer, int offset, int len) {
	}

	@STAMP(flows={@Flow(from="buffer",to="this")})
	public  void write(byte[] buffer) throws java.io.IOException { 
	}

	@STAMP(flows = {@Flow(from="oneByte",to="this")})
	public synchronized void write(int oneByte) {
	}

	public synchronized void writeTo(java.io.OutputStream out) throws java.io.IOException {
		out.write(toByteArray());
	}
}
