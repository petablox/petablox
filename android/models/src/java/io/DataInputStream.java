class DataInputStream
{
    @STAMP(flows = {@Flow(from="in",to="this")})
	public  DataInputStream(java.io.InputStream in) { super((java.io.InputStream)null); }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	private byte taintByte() { return (byte) 0; }

    @STAMP(flows = {@Flow(from="this",to="buffer")})
	public final  int read(byte[] buffer) throws java.io.IOException { 
	buffer[0] = taintByte(); 
	return 0;
    }

    @STAMP(flows = {@Flow(from="this",to="buffer")})
	public final  int read(byte[] buffer, int offset, int length) throws java.io.IOException {
	buffer[0] = taintByte(); 
	return 0;
    }
    
    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  boolean readBoolean() throws java.io.IOException { return true; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  byte readByte() throws java.io.IOException { return (byte) 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  char readChar() throws java.io.IOException { return (char) 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  double readDouble() throws java.io.IOException { return 0.0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  float readFloat() throws java.io.IOException { return 0.0f; }

    @STAMP(flows = {@Flow(from="this",to="dst")})
	public final  void readFully(byte[] dst) throws java.io.IOException { 
	dst[0] = taintByte();
	return; 
    }

    @STAMP(flows = {@Flow(from="this",to="dst")})
	public final  void readFully(byte[] dst, int offset, int byteCount) throws java.io.IOException { 
	dst[0] = taintByte();
	return; 
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  int readInt() throws java.io.IOException { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  java.lang.String readLine() throws java.io.IOException { return new String(); }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  long readLong() throws java.io.IOException { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  short readShort() throws java.io.IOException { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  int readUnsignedByte() throws java.io.IOException { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  int readUnsignedShort() throws java.io.IOException { return 0; }

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public final  java.lang.String readUTF() throws java.io.IOException { return new String(); }

    @STAMP(flows = {@Flow(from="in",to="@return")})
	public static final  java.lang.String readUTF(java.io.DataInput in) throws java.io.IOException { return new String();  }

    public final  int skipBytes(int count) throws java.io.IOException { throw new RuntimeException("Stub!"); }
}
