class Bundle
{	
	/* primitive type */
	public short short_f;
	public int int_f;
	public boolean boolean_f;
	public byte byte_f;
	public char char_f;
	public long long_f;
	public float float_f;
	public double double_f;
	
	/* for all reference type. */
	public java.lang.Object object_f;

	//called only from the Intent's model
	public Bundle(boolean flag)
	{
		this.object_f = new short[0];
		this.object_f = new int[0];
		this.object_f = new boolean[0];
		this.object_f = new byte[0];
		this.object_f = new char[0];
		this.object_f = new long[0];
		this.object_f = new float[0];
		this.object_f = new double[0];

		this.object_f = new String();
		this.object_f = new String[0];
		this.object_f = new Bundle(false);

		java.util.ArrayList<Integer> il = new java.util.ArrayList<Integer>();
		il.add(new Integer(0));
		this.object_f = il;

		java.util.ArrayList<String> sl = new java.util.ArrayList<String>();
		sl.add(new String());
		this.object_f = sl;

		byte[][] b = new byte[1][];  //useful for sms receivers
		b[0] = new byte[1];
		this.object_f = b;
	}

    public Bundle() {
		
	}
	
	//getter..
	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.String getString(java.lang.String key) 
	{ 
		return (java.lang.String) this.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public java.lang.String getString(java.lang.String key, java.lang.String defaultValue) 
	{ 
		return (java.lang.String) this.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public int getInt(java.lang.String key) 
	{ 
		return this.int_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public int getInt(java.lang.String key, int defaultValue) 
	{ 
		return this.int_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public float getFloat(java.lang.String key) 
	{ 
		return this.float_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public float getFloat(java.lang.String key, float defaultValue) 
	{ 
		return this.float_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public char getChar(java.lang.String key) 
	{ 
		return this.char_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public char getChar(java.lang.String key, char defaultValue) 
	{ 
		return this.char_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public byte getByte(java.lang.String key) 
	{ 
		return this.byte_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public java.lang.Byte getByte(java.lang.String key, byte defaultValue) 
	{ 
		return this.byte_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public boolean getBoolean(java.lang.String key) 
	{ 
		return this.boolean_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public boolean getBoolean(java.lang.String key, boolean defaultValue) 
	{ 
		return this.boolean_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public short getShort(java.lang.String key) 
	{ 
		return this.short_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public short getShort(java.lang.String key, short defaultValue) 
	{ 
		return this.short_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
	public double getDouble(java.lang.String key) 
	{ 
		return this.double_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public double getDouble(java.lang.String key, double defaultValue) 
	{ 
		return this.double_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
    public long getLong(java.lang.String key) {
        return this.long_f;
    }
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
    public long getLong(java.lang.String key, long defaultValue) {
        return this.long_f;
    }

	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.lang.Object get(java.lang.String key) {
        return this.object_f;
    }
	
	// @STAMP(flows={@Flow(from="this",to="@return")})
	// public <T extends android.os.Parcelable> T getParcelable(java.lang.String key) {
	//         return (T)this.unknown;
	// }
	

	@STAMP(flows={@Flow(from="this",to="@return")})
	public java.util.ArrayList<java.lang.String> getStringArrayList(java.lang.String key)
	{
		return (java.util.ArrayList<java.lang.String> )this.object_f; 
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public android.os.Bundle getBundle(java.lang.String key) {
		return (android.os.Bundle)this.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence getCharSequence(java.lang.String key) {
		return (java.lang.CharSequence)this.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
    public java.lang.CharSequence getCharSequence(java.lang.String key, java.lang.CharSequence defaultValue) {
		return (java.lang.CharSequence)this.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public android.os.Parcelable[] getParcelableArray(java.lang.String key) {
		return (android.os.Parcelable[])this.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> T getParcelable(java.lang.String key) {
	    return (T)this.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> java.util.ArrayList<T> getParcelableArrayList(java.lang.String key) {
		return (java.util.ArrayList<T>)this.object_f;
	}
		
	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> android.util.SparseArray<T> getSparseParcelableArray(java.lang.String key) {
		return (android.util.SparseArray<T>)this.object_f;
	}
		
    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.io.Serializable getSerializable(java.lang.String key) {
		return (java.io.Serializable)this.object_f;
    }
				
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.util.ArrayList<java.lang.Integer> getIntegerArrayList(java.lang.String key) {
		return (java.util.ArrayList<java.lang.Integer>)this.object_f;
    }
					
    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.util.ArrayList<java.lang.CharSequence> getCharSequenceArrayList(java.lang.String key) {
		return (java.util.ArrayList<java.lang.CharSequence>)this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public boolean[] getBooleanArray(java.lang.String key) {
		return (boolean[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public byte[] getByteArray(java.lang.String key) {
		return (byte[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public short[] getShortArray(java.lang.String key) {
		return (short[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public char[] getCharArray(java.lang.String key) {
		return (char[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public int[] getIntArray(java.lang.String key) {
		return (int[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public long[] getLongArray(java.lang.String key) {
		return (long[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public float[] getFloatArray(java.lang.String key) {
		return (float[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public double[] getDoubleArray(java.lang.String key) {
		return (double[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.String[] getStringArray(java.lang.String key) {
		return (java.lang.String[])this.object_f;
    }

    @STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence[] getCharSequenceArray(java.lang.String key) {
		return (java.lang.CharSequence[])this.object_f;
    }
	
	//Setter.....

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putBoolean(java.lang.String key, boolean value) 
	{ 
		this.boolean_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putByte(java.lang.String key, byte value) 
	{ 
		this.byte_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putChar(java.lang.String key, char value) 
	{ 
		this.char_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putShort(java.lang.String key, short value) 
	{
		this.short_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putInt(java.lang.String key, int value) 
	{
		this.int_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putLong(java.lang.String key, long value) 
	{
		this.long_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putFloat(java.lang.String key, float value) 
	{
		this.float_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putDouble(java.lang.String key, double value) 
	{
		this.double_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
	public  void putString(java.lang.String key, java.lang.String value) 
	{ 
		this.object_f = value;
	}
	
	@STAMP(flows={@Flow(from="value",to="this")})
    public void putCharSequence(java.lang.String key, java.lang.CharSequence value) {
			this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putParcelable(java.lang.String key, android.os.Parcelable value) {
		this.object_f = value;
	}
	
	@STAMP(flows={@Flow(from="value",to="this")})
	public void putParcelableArray(java.lang.String key, android.os.Parcelable[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putParcelableArrayList(java.lang.String key, java.util.ArrayList<? extends android.os.Parcelable> value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putSparseParcelableArray(java.lang.String key, android.util.SparseArray<? extends android.os.Parcelable> value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putIntegerArrayList(java.lang.String key, java.util.ArrayList<java.lang.Integer> value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putStringArrayList(java.lang.String key, java.util.ArrayList<java.lang.String> value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putCharSequenceArrayList(java.lang.String key, java.util.ArrayList<java.lang.CharSequence> value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putSerializable(java.lang.String key, java.io.Serializable value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putBooleanArray(java.lang.String key, boolean[] value) {
    		this.object_f = value;
	}
    
	@STAMP(flows={@Flow(from="value",to="this")})
    public void putByteArray(java.lang.String key, byte[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putShortArray(java.lang.String key, short[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putCharArray(java.lang.String key, char[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putIntArray(java.lang.String key, int[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putLongArray(java.lang.String key, long[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putFloatArray(java.lang.String key, float[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putDoubleArray(java.lang.String key, double[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putStringArray(java.lang.String key, java.lang.String[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putCharSequenceArray(java.lang.String key, java.lang.CharSequence[] value) {
		this.object_f = value;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public void putBundle(java.lang.String key, android.os.Bundle value) {
		this.object_f = value;
	}
}
