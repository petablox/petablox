package android.content;

class Intent
{
	public java.lang.String name; 
	public java.lang.String action; 
	public java.lang.String type; 

	private android.os.Bundle extras = new android.os.Bundle(true);

	@STAMP(flows = {@Flow(from="uri",to="!this")})
	public  Intent(java.lang.String action, android.net.Uri uri) 
	{ 
		this.action = action;
	}

	public Intent(java.lang.String action) {
		this.action = action;
	}

	public Intent(android.content.Context packageContext, java.lang.Class<?> cls) {
		this.name = cls.name;
	}

	public Intent(java.lang.String action, android.net.Uri uri, android.content.Context packageContext, java.lang.Class<?> cls) {
		this.name = cls.name;
		this.action = action;
	}

	public android.content.Intent setComponent(android.content.ComponentName component) {
		this.name = component.name;
		return this;
	}

	public android.content.Intent setClassName(android.content.Context packageContext, java.lang.String className) {
		this.name = className;
		return this;
	}

	public android.content.Intent setClassName(java.lang.String packageName, java.lang.String className) {
		this.name = className;
		return this;
	}

	public android.content.Intent setClass(android.content.Context packageContext, java.lang.Class<?> cls) {
		this.name = cls.name;
		return this;
	}

	public android.content.Intent setAction(java.lang.String action) {
		this.action = action;
		return this;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  android.os.Bundle getExtras() 
	{ 
		return extras;
	}
	
	@STAMP(flows = {@Flow(from="data",to="this")})
	public  android.content.Intent setData(android.net.Uri data) 
	{ 
		return this;
	}

	@STAMP(flows = {@Flow(from="data",to="this")})
	public  android.content.Intent setDataAndType(android.net.Uri data, java.lang.String type) 
	{ 
        this.type = type;
		return this;
	}

    public android.content.Intent setType(java.lang.String type) {
        this.type = type;
		return this;
    }

    public android.content.Intent setTypeAndNormalize(java.lang.String type) {
        this.type = type;
		return this;
    }

    public android.content.Intent setDataAndTypeAndNormalize(android.net.Uri data, java.lang.String type) {
        this.type = type;
		return this;
    }

    //Getter.
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
    public boolean getBooleanExtra(java.lang.String name, boolean defaultValue) {
        return extras.boolean_f;
    }
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public byte getByteExtra(java.lang.String name, byte defaultValue) {
		return extras.byte_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public short getShortExtra(java.lang.String name, short defaultValue) {
		return extras.short_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public char getCharExtra(java.lang.String name, char defaultValue) {
		return extras.char_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public int getIntExtra(java.lang.String name, int defaultValue) {
		return extras.int_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public long getLongExtra(java.lang.String name, long defaultValue) {
		return extras.long_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public float getFloatExtra(java.lang.String name, float defaultValue) {
		return extras.float_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public double getDoubleExtra(java.lang.String name, double defaultValue) {
		return extras.double_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return"),@Flow(from="defaultValue",to="@return")})
	public java.lang.String getStringExtra(java.lang.String name, java.lang.String defaultValue) {
		return (java.lang.String) extras.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence getCharSequenceExtra(java.lang.String name) {
		return (java.lang.CharSequence) extras.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> T getParcelableExtra(java.lang.String name) {
		return (T)extras.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public android.os.Parcelable[] getParcelableArrayExtra(java.lang.String name) {
		return (android.os.Parcelable[])extras.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public <T extends android.os.Parcelable> java.util.ArrayList<T> getParcelableArrayListExtra(java.lang.String name) {
		return (java.util.ArrayList<T>) extras.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.io.Serializable getSerializableExtra(java.lang.String name) {
		return (java.io.Serializable)extras.object_f;
	}
				
	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.util.ArrayList<java.lang.Integer> getIntegerArrayListExtra(java.lang.String name) 
	{ 
		return (java.util.ArrayList<java.lang.Integer>)extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.util.ArrayList<java.lang.String> getStringArrayListExtra(java.lang.String name) 
	{ 
		return (java.util.ArrayList<java.lang.String>)extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  java.util.ArrayList<java.lang.CharSequence> getCharSequenceArrayListExtra(java.lang.String name) 
	{ 
		return (java.util.ArrayList<java.lang.CharSequence>)extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  boolean[] getBooleanArrayExtra(java.lang.String name) 
	{ 
		return (boolean[]) extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  byte[] getByteArrayExtra(java.lang.String name) 
	{ 
		return (byte[]) extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  short[] getShortArrayExtra(java.lang.String name) 
	{ 
		return (short[]) extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  char[] getCharArrayExtra(java.lang.String name) 
	{ 
		return (char[]) extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  int[] getIntArrayExtra(java.lang.String name) 
	{ 
		return (int[])extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  long[] getLongArrayExtra(java.lang.String name) 
	{ 
		return (long[])extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
 	public  float[] getFloatArrayExtra(java.lang.String name) 
	{ 
		return (float[])extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
 	public  double[] getDoubleArrayExtra(java.lang.String name) 
	{ 
		return (double[])extras.object_f;
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
 	public  java.lang.String[] getStringArrayExtra(java.lang.String name) 
	{ 
		return (java.lang.String[])extras.object_f;
	}
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public java.lang.CharSequence[] getCharSequenceArrayExtra(java.lang.String name) {
		return (java.lang.CharSequence[])extras.object_f;
    }
	
	@STAMP(flows={@Flow(from="this",to="@return")})
    public android.os.Bundle getBundleExtra(java.lang.String name) {
		return (android.os.Bundle)extras.object_f;
    }

    ///Setter
	@STAMP(flows = {@Flow(from="extras",to="this")})
	public  android.content.Intent putExtras(android.os.Bundle extras) 
	{ 
		this.extras = extras;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, boolean value) 
	{ 
		extras.boolean_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, byte value) 
	{ 
		extras.byte_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, char value) 
	{ 
		extras.char_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, short value) 
	{ 
		extras.short_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, int value) 
	{ 
		extras.int_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, long value) 
	{ 
		extras.long_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, float value) 
	{ 
		extras.float_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, double value) 
	{ 
		extras.double_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.String value) 
	{ 
		extras.object_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.CharSequence value) 
	{ 
		extras.object_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, android.os.Parcelable value) 
	{ 
		extras.object_f = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, android.os.Parcelable[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, java.io.Serializable value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, boolean[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, byte[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, short[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, char[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, int[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, long[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, float[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, double[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.String[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.CharSequence[] value) 
	{ 
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows={@Flow(from="value",to="this")})
    public android.content.Intent putParcelableArrayListExtra(java.lang.String key, java.util.ArrayList<? extends android.os.Parcelable> value) {
		extras.object_f = value;
		return this;
	}
	
	@STAMP(flows={@Flow(from="value",to="this")})
    public android.content.Intent putIntegerArrayListExtra(java.lang.String key, java.util.ArrayList<java.lang.Integer> value) {
		extras.object_f = value;
		return this;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public android.content.Intent putStringArrayListExtra(java.lang.String key, java.util.ArrayList<java.lang.String> value) {
		extras.object_f = value;
		return this;
	}

	@STAMP(flows={@Flow(from="value",to="this")})
    public android.content.Intent putCharSequenceArrayListExtra(java.lang.String key, java.util.ArrayList<java.lang.CharSequence> value) {
		extras.object_f = value;
		return this;
	}

	public android.content.Intent replaceExtras(android.os.Bundle extras)
	{
		this.extras = extras;
		return this;
	}

	public android.content.Intent replaceExtras(android.content.Intent src)
	{
		this.extras = src.extras;
		return this;
	}

}

