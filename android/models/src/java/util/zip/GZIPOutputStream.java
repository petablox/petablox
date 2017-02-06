class GZIPOutputStream
{
	public GZIPOutputStream(java.io.OutputStream os) throws java.io.IOException 
	{
		super((java.io.OutputStream) null, (java.util.zip.Deflater) null, 0);
		this.out = os;
	}

	public  void write(byte[] buffer, int offset, int length) throws java.io.IOException 
	{ 
		out.write(buffer, offset, length);
	}

	public  void write(int oneByte) throws java.io.IOException 
	{ 
		out.write(oneByte);
	}
	
	public  void write(byte[] buffer) throws java.io.IOException 
	{ 
		out.write(buffer);
	}
}