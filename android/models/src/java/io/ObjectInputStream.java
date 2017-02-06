class ObjectInputStream
{
	@STAMP(flows = {@Flow(from="input",to="this")})
	public ObjectInputStream(java.io.InputStream input) throws java.io.StreamCorruptedException, java.io.IOException { }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  int read() throws java.io.IOException { return 0; }
	@STAMP(flows = {@Flow(from="this",to="buffer"), @Flow(from="length",to="this"), @Flow(from="length",to="@return")})
	public  int read(byte[] buffer, int offset, int length) throws java.io.IOException { return 0; }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  boolean readBoolean() throws java.io.IOException { return false; }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  byte readByte() throws java.io.IOException { return 0; }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  char readChar() throws java.io.IOException { return '\0'; }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  double readDouble() throws java.io.IOException { return 0.0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  float readFloat() throws java.io.IOException { return 0.0f; }
	@STAMP(flows = {@Flow(from="this",to="dst")})
	public  void readFully(byte[] dst) throws java.io.IOException { }
	@STAMP(flows = {@Flow(from="this",to="dst")})
	public  void readFully(byte[] dst, int offset, int byteCount) throws java.io.IOException { }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  int readInt() throws java.io.IOException { return 0; }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.lang.String readLine() throws java.io.IOException { return new String(); }
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  long readLong() throws java.io.IOException { return 0L; }


	@STAMP(flows = {@Flow(from="this",to="@return")})
	public final  java.lang.Object readObject() throws java.io.OptionalDataException, java.lang.ClassNotFoundException, java.io.IOException { return object; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.lang.Object readUnshared() throws java.io.IOException, java.lang.ClassNotFoundException { return new Object(); }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	protected  java.lang.Object readObjectOverride() throws java.io.OptionalDataException, java.lang.ClassNotFoundException, java.io.IOException { return new Object(); }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  short readShort() throws java.io.IOException { return 0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  int readUnsignedByte() throws java.io.IOException { return 0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  int readUnsignedShort() throws java.io.IOException { return 0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.lang.String readUTF() throws java.io.IOException { return new String(); }
	
	static Object object;
}
