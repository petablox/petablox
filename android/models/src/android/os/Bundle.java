//import java.lang.CharSequence;

/**
  * The key idea of the <T> type "unknown" fields are used to perserve the soundness 
  * of the flow even when we can not infer the exactly value of the key.
  * e.g int v=src(); b.putInt(k1,v); b.putString(k2,v); str=b.getString(k3); sink(str).
  **/
class Bundle
{
	
	/* primitive type */
	public short unknown_short;
	
	public int unknown_int;
	
	public boolean unknown_boolean;
	
	public byte unknown_byte;
	
	public char unknown_char;
	
	public long unknown_long;
	
	public float unknown_float;
	
	public double unknown_double;
	
	/* general unknown object. For all reference type. */
	public java.lang.Object unknown;

	/* specific to system events */

	/* SMS */
	public byte[][] pdus;

	//called only from the model
	public Bundle(boolean flag){
		byte[][] smsData = new byte[1][];
		smsData[0] = smsByteArray();
		this.pdus = smsData;
		this.unknown = smsData;
	}

    public Bundle() {
	}
	
	//getter..
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.Object get_pdus() 
	{ 
		return this.pdus;
	}
	
	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void put_pdus(java.lang.Object value) 
	{ 
		this.pdus = (byte[][]) value;
		this.unknown = value;
	}
		
	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.String getString(java.lang.String key) 
	{ 
		return (java.lang.String)this.unknown;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.String getString(java.lang.String key, java.lang.String defaultValue) 
	{ 
		return (java.lang.String)this.unknown;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public int getInt(java.lang.String key) 
	{ 
		return this.unknown_int;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public int getInt(java.lang.String key, int defaultValue) 
	{ 
		return this.unknown_int;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public float getFloat(java.lang.String key) 
	{ 
		return this.unknown_float;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public float getFloat(java.lang.String key, float defaultValue) 
	{ 
		return this.unknown_float;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public char getChar(java.lang.String key) 
	{ 
		return this.unknown_char;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public char getChar(java.lang.String key, char defaultValue) 
	{ 
		return this.unknown_char;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public byte getByte(java.lang.String key) 
	{ 
		return this.unknown_byte;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public java.lang.Byte getByte(java.lang.String key, byte defaultValue) 
	{ 
		return this.unknown_byte;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public boolean getBoolean(java.lang.String key) 
	{ 
		return this.unknown_boolean;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public boolean getBoolean(java.lang.String key, boolean defaultValue) 
	{ 
		return this.unknown_boolean;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public short getShort(java.lang.String key) 
	{ 
		return this.unknown_short;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public short getShort(java.lang.String key, short defaultValue) 
	{ 
		return this.unknown_short;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public double getDouble(java.lang.String key) 
	{ 
		return this.unknown_double;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public double getDouble(java.lang.String key, double defaultValue) 
	{ 
		return this.unknown_double;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
    public long getLong(java.lang.String key) {
        return this.unknown_long;
    }
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
    public long getLong(java.lang.String key, long defaultValue) {
        return this.unknown_long;
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.Object get(java.lang.String key) {
        return this.unknown;
    }
	
	// @STAMP(flows={@Flow(from="this",to="@return")})
	// public <T extends android.os.Parcelable> T getParcelable(java.lang.String key) {
	//         return (T)this.unknown;
	// }
	

	// Patrick
	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.util.ArrayList<java.lang.String> getStringArrayList(java.lang.String key)
	{
		return (java.util.ArrayList<java.lang.String>)this.unknown; 
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public android.os.Bundle getBundle(java.lang.String key) {
		return (android.os.Bundle)this.unknown;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence getCharSequence(java.lang.String key) {
		return (java.lang.CharSequence)this.unknown;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence getCharSequence(java.lang.String key, java.lang.CharSequence defaultValue) {
		return (java.lang.CharSequence)this.unknown;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public android.os.Parcelable[] getParcelableArray(java.lang.String key) {
		return (android.os.Parcelable[])this.unknown;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> T getParcelable(java.lang.String key) {
	    return (T)this.unknown;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> java.util.ArrayList<T> getParcelableArrayList(java.lang.String key) {
		return (java.util.ArrayList<T>)this.unknown;
	}
		
	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> android.util.SparseArray<T> getSparseParcelableArray(java.lang.String key) {
		return (android.util.SparseArray<T>)this.unknown;
	}
		
    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.io.Serializable getSerializable(java.lang.String key) {
		return (java.io.Serializable)this.unknown;
    }
				
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.util.ArrayList<java.lang.Integer> getIntegerArrayList(java.lang.String key) {
		return (java.util.ArrayList<java.lang.Integer>)this.unknown;
    }
					
    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.util.ArrayList<java.lang.CharSequence> getCharSequenceArrayList(java.lang.String key) {
		return (java.util.ArrayList<java.lang.CharSequence>)this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public boolean[] getBooleanArray(java.lang.String key) {
		return (boolean[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public byte[] getByteArray(java.lang.String key) {
		return (byte[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public short[] getShortArray(java.lang.String key) {
		return (short[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public char[] getCharArray(java.lang.String key) {
		return (char[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public int[] getIntArray(java.lang.String key) {
		return (int[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public long[] getLongArray(java.lang.String key) {
		return (long[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public float[] getFloatArray(java.lang.String key) {
		return (float[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public double[] getDoubleArray(java.lang.String key) {
		return (double[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String[] getStringArray(java.lang.String key) {
		return (java.lang.String[])this.unknown;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence[] getCharSequenceArray(java.lang.String key) {
		return (java.lang.CharSequence[])this.unknown;
    }
	
	//Setter.....

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putBoolean(java.lang.String key, boolean value) 
	{ 
		this.unknown_boolean = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putByte(java.lang.String key, byte value) 
	{ 
		this.unknown_byte = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putChar(java.lang.String key, char value) 
	{ 
		this.unknown_char = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putShort(java.lang.String key, short value) 
	{
		this.unknown_short = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putInt(java.lang.String key, int value) 
	{
		this.unknown_int = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putLong(java.lang.String key, long value) 
	{
		this.unknown_long = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putFloat(java.lang.String key, float value) 
	{
		this.unknown_float = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putDouble(java.lang.String key, double value) 
	{
		this.unknown_double = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
	public  void putString(java.lang.String key, java.lang.String value) 
	{ 
		this.unknown = value;
	}
	
	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putCharSequence(java.lang.String key, java.lang.CharSequence value) {
			this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putParcelable(java.lang.String key, android.os.Parcelable value) {
		this.unknown = value;
	}
	
	@STAMP(flows={@Flow(from="value",to="!this")})
	public void putParcelableArray(java.lang.String key, android.os.Parcelable[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putParcelableArrayList(java.lang.String key, java.util.ArrayList<? extends android.os.Parcelable> value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putSparseParcelableArray(java.lang.String key, android.util.SparseArray<? extends android.os.Parcelable> value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putIntegerArrayList(java.lang.String key, java.util.ArrayList<java.lang.Integer> value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putStringArrayList(java.lang.String key, java.util.ArrayList<java.lang.String> value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putCharSequenceArrayList(java.lang.String key, java.util.ArrayList<java.lang.CharSequence> value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putSerializable(java.lang.String key, java.io.Serializable value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putBooleanArray(java.lang.String key, boolean[] value) {
    		this.unknown = value;
	}
    
	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putByteArray(java.lang.String key, byte[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putShortArray(java.lang.String key, short[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putCharArray(java.lang.String key, char[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putIntArray(java.lang.String key, int[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putLongArray(java.lang.String key, long[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putFloatArray(java.lang.String key, float[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putDoubleArray(java.lang.String key, double[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putStringArray(java.lang.String key, java.lang.String[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putCharSequenceArray(java.lang.String key, java.lang.CharSequence[] value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public void putBundle(java.lang.String key, android.os.Bundle value) {
		this.unknown = value;
	}

	@STAMP(flows={@Flow(from="$SMS",to="@return")})
	private byte[] smsByteArray() {
		byte[] bytes = new byte[1];
		return bytes;
	}
}
