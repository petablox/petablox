class DataOutputStream
{

	@STAMP(flows={@Flow(from="out",to="this")})	
	public  DataOutputStream(java.io.OutputStream out) 
	{ 
		super((java.io.OutputStream)null); 
	}
	

	@STAMP(flows={@Flow(from="buffer",to="!this")})	
	public  void write(byte[] buffer, int offset, int count) throws java.io.IOException 
	{ 
	}
	
	@STAMP(flows={@Flow(from="oneByte",to="!this")})	
	public  void write(int oneByte) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeBoolean(boolean val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeByte(int val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="str",to="!this")})	
	public final  void writeBytes(java.lang.String str) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeChar(int val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="str",to="!this")})	
	public final  void writeChars(java.lang.String str) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeDouble(double val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeFloat(float val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeInt(int val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeLong(long val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="val",to="!this")})	
	public final  void writeShort(int val) throws java.io.IOException 
	{ 
	}

	@STAMP(flows={@Flow(from="str",to="!this")})	
	public final  void writeUTF(java.lang.String str) throws java.io.IOException 
	{ 
	}
}