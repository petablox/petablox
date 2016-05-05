class DeflaterOutputStream
{
	public  DeflaterOutputStream(java.io.OutputStream os, java.util.zip.Deflater def) 
	{ 
        super((java.io.OutputStream) null);
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