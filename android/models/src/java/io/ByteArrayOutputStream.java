class ByteArrayOutputStream
{
	public ByteArrayOutputStream() {
        this.buf = new byte[1];
    }

    public ByteArrayOutputStream(int size) {
        this.buf = new byte[size];
    }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public synchronized byte[] toByteArray() { 
		return new byte[1];
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public java.lang.String toString() { 
		return new String(); 
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public java.lang.String toString(int hibyte) { 
		return new String(); 
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public java.lang.String toString(java.lang.String enc) throws java.io.UnsupportedEncodingException {
		return new String(); 
	}

	@STAMP(flows = {@Flow(from="buffer",to="this")})
	public synchronized void write(byte[] buffer, int offset, int len) {
		this.buf[0] = buffer[0];
	}

	@STAMP(flows={@Flow(from="buffer",to="this")})
	public  void write(byte[] buffer) throws java.io.IOException { 
		this.buf[0] = buffer[0];
	}

	@STAMP(flows = {@Flow(from="oneByte",to="this")})
	public synchronized void write(int oneByte) {
		this.buf[0] = (byte) oneByte;
	}

	public synchronized void writeTo(java.io.OutputStream out) throws java.io.IOException {
		out.write(this.buf);
	}
}
