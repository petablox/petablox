class ByteBuffer
{
    public ByteBuffer() {}

    public static java.nio.ByteBuffer allocate(int capacity) {
		return new StampByteBuffer();
    }

    public static java.nio.ByteBuffer allocateDirect(int capacity) {
		return new StampByteBuffer();
    }

    @STAMP(flows = {@Flow(from="array",to="@return")})
    public static java.nio.ByteBuffer wrap(byte[] array) 
    { 
		return new StampByteBuffer();
	}

    @STAMP(flows = {@Flow(from="array",to="@return")})
    public static  java.nio.ByteBuffer wrap(byte[] array, int start, int byteCount) 
    {  
		return new StampByteBuffer();
	}
	
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public final  byte[] array() { return new byte[0]; }

    @STAMP(flows = {@Flow(from="this",to="dst")})
    public  java.nio.ByteBuffer get(byte[] dst) 
    { 
		return this;  
    }

    @STAMP(flows = {@Flow(from="this",to="dst")})
    public  java.nio.ByteBuffer get(byte[] dst, int dstOffset, int byteCount) 
    { 
		return this;  
    }

    @STAMP(flows = {@Flow(from="src",to="this")})
    public final  java.nio.ByteBuffer put(byte[] src) 
    {
		return this;
    }

    @STAMP(flows = {@Flow(from="src",to="this")})
    public  java.nio.ByteBuffer put(byte[] src, int srcOffset, int byteCount) 
    {
		return this;
    }

    @STAMP(flows = {@Flow(from="src",to="this")})
    public  java.nio.ByteBuffer put(java.nio.ByteBuffer src) 
    {  
		return this;
    }

	public final java.nio.ByteBuffer order(java.nio.ByteOrder byteOrder) 
	{
		return this;
    }
}
