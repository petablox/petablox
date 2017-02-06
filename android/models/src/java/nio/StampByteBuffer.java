package java.nio;

import edu.stanford.stamp.annotation.STAMP;
import edu.stanford.stamp.annotation.Flow;

class StampByteBuffer extends ByteBuffer 
{
	@STAMP(flows = {@Flow(from="this",to="@return")})
    public java.nio.CharBuffer asCharBuffer(){ return new StampCharBuffer(); }

    public java.nio.DoubleBuffer asDoubleBuffer() {
        throw new RuntimeException("Stub!");
	}

    public java.nio.FloatBuffer asFloatBuffer(){
        throw new RuntimeException("Stub!");
	}

    public java.nio.IntBuffer asIntBuffer() {
        throw new RuntimeException("Stub!");
	}

    public java.nio.LongBuffer asLongBuffer() {
        throw new RuntimeException("Stub!");
	}

    public java.nio.ByteBuffer asReadOnlyBuffer() {
        throw new RuntimeException("Stub!");
	}

    public java.nio.ShortBuffer asShortBuffer() {
        throw new RuntimeException("Stub!");
	}

    public java.nio.ByteBuffer compact(){ return this; }

    public java.nio.ByteBuffer duplicate(){ return this; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public byte get(){ return (byte)0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public byte get(int index){ return (byte)0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public char getChar(){ return 'a'; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public char getChar(int index){ return 'a'; }
	@STAMP(flows = {@Flow(from="this",to="@return")})
    public double getDouble(){ return 0.0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public double getDouble(int index){ return 0.0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public float getFloat(){ return 0.0f; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public float getFloat(int index){ return 0.0f; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public int getInt(){ return 0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public int getInt(int index){ return 0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public long getLong(){ return 0L; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public long getLong(int index){ return 0L; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public short getShort(){ return (short)0; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public short getShort(int index){ return (short)0; }

    public boolean isDirect(){ return false; }

	@STAMP(flows = {@Flow(from="b",to="this")})
    public java.nio.ByteBuffer put(byte b){ return this; }

	@STAMP(flows = {@Flow(from="b",to="this")})
    public java.nio.ByteBuffer put(int index, byte b){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putChar(char value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putChar(int index, char value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putDouble(double value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putDouble(int index, double value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putFloat(float value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putFloat(int index, float value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putInt(int value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putInt(int index, int value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putLong(long value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putLong(int index, long value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putShort(short value){ return this; }

	@STAMP(flows = {@Flow(from="value",to="this")})
    public java.nio.ByteBuffer putShort(int index, short value){ return this; }

    public java.nio.ByteBuffer slice(){ return this; }

    public StampByteBuffer() {
    }

	public boolean isReadOnly() { return true; }
 
}

