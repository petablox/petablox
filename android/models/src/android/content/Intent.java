package android.content;

class Intent
{
	//add by yu.
	private static android.os.Bundle extras = new android.os.Bundle(true);

	@STAMP(flows = {@Flow(from="uri",to="!this")})
	public  Intent(java.lang.String action, android.net.Uri uri) 
	{ 
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return"),@Flow(from="this",to="@return")})
	public  android.os.Bundle getExtras() 
	{ 
		return extras;
	}
	
	@STAMP(flows = {@Flow(from="data",to="!this")})
	public  android.content.Intent setData(android.net.Uri data) 
	{ 
		return this;
	}

	@STAMP(flows = {@Flow(from="data",to="!this")})
	public  android.content.Intent setDataAndType(android.net.Uri data, java.lang.String type) 
	{ 
		return this;
	}

    //Getter.
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public boolean getBooleanExtra(java.lang.String name, boolean defaultValue) {
        return extras.unknown_boolean;
    }
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public byte getByteExtra(java.lang.String name, byte defaultValue) {
		return extras.unknown_byte;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public short getShortExtra(java.lang.String name, short defaultValue) {
		return extras.unknown_short;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public char getCharExtra(java.lang.String name, char defaultValue) {
		return extras.unknown_char;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public int getIntExtra(java.lang.String name, int defaultValue) {
		return extras.unknown_int;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public long getLongExtra(java.lang.String name, long defaultValue) {
		return extras.unknown_long;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public float getFloatExtra(java.lang.String name, float defaultValue) {
		return extras.unknown_float;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public double getDoubleExtra(java.lang.String name, double defaultValue) {
		return extras.unknown_double;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public java.lang.String getStringExtra(java.lang.String name, java.lang.String defaultValue) {
		return (java.lang.String)extras.unknown;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public java.lang.CharSequence getCharSequenceExtra(java.lang.String name) {
		return (java.lang.CharSequence)extras.unknown;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public <T extends android.os.Parcelable> T getParcelableExtra(java.lang.String name) {
		return (T)extras.unknown;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public android.os.Parcelable[] getParcelableArrayExtra(java.lang.String name) {
		return (android.os.Parcelable[])extras.unknown;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public <T extends android.os.Parcelable> java.util.ArrayList<T> getParcelableArrayListExtra(java.lang.String name) {
		return (java.util.ArrayList<T>)extras.unknown;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public java.io.Serializable getSerializableExtra(java.lang.String name) {
		return (java.io.Serializable)extras.unknown;
	}
				
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  java.util.ArrayList<java.lang.Integer> getIntegerArrayListExtra(java.lang.String name) 
	{ 
		return (java.util.ArrayList<java.lang.Integer>)extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  java.util.ArrayList<java.lang.String> getStringArrayListExtra(java.lang.String name) 
	{ 
		return (java.util.ArrayList<java.lang.String>)extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  java.util.ArrayList<java.lang.CharSequence> getCharSequenceArrayListExtra(java.lang.String name) 
	{ 
		return (java.util.ArrayList<java.lang.CharSequence>)extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  boolean[] getBooleanArrayExtra(java.lang.String name) 
	{ 
		return (boolean[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  byte[] getByteArrayExtra(java.lang.String name) 
	{ 
		return (byte[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  short[] getShortArrayExtra(java.lang.String name) 
	{ 
		return (short[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  char[] getCharArrayExtra(java.lang.String name) 
	{ 
		return (char[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  int[] getIntArrayExtra(java.lang.String name) 
	{ 
		return (int[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
	public  long[] getLongArrayExtra(java.lang.String name) 
	{ 
		return (long[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
 	public  float[] getFloatArrayExtra(java.lang.String name) 
	{ 
		return (float[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
 	public  double[] getDoubleArrayExtra(java.lang.String name) 
	{ 
		return (double[])extras.unknown;
	}

	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
 	public  java.lang.String[] getStringArrayExtra(java.lang.String name) 
	{ 
		return (java.lang.String[])extras.unknown;
	}
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public java.lang.CharSequence[] getCharSequenceArrayExtra(java.lang.String name) {
		return (java.lang.CharSequence[])extras.unknown;
    }
	
	@STAMP(flows={@Flow(from="$getExtras",to="@return")})
    public android.os.Bundle getBundleExtra(java.lang.String name) {
		return (android.os.Bundle)extras.unknown;
    }

    ///Setter
	@STAMP(flows = {@Flow(from="extras",to="!this")})
	public  android.content.Intent putExtras(android.os.Bundle extras) 
	{ 
		this.extras = extras;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, boolean value) 
	{ 
		extras.unknown_boolean = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, byte value) 
	{ 
		extras.unknown_byte = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, char value) 
	{ 
		extras.unknown_char = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, short value) 
	{ 
		extras.unknown_short = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, int value) 
	{ 
		extras.unknown_int = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, long value) 
	{ 
		extras.unknown_long = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, float value) 
	{ 
		extras.unknown_float = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, double value) 
	{ 
		extras.unknown_double = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.String value) 
	{ 
		extras.unknown = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.CharSequence value) 
	{ 
		extras.unknown = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, android.os.Parcelable value) 
	{ 
		extras.unknown = value;
		return this;
	}

	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, android.os.Parcelable[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, java.io.Serializable value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, boolean[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, byte[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, short[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, char[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, int[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, long[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, float[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, double[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.String[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows = {@Flow(from="value",to="!this")})
	public  android.content.Intent putExtra(java.lang.String name, java.lang.CharSequence[] value) 
	{ 
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows={@Flow(from="value",to="!this")})
    public android.content.Intent putParcelableArrayListExtra(java.lang.String key, java.util.ArrayList<? extends android.os.Parcelable> value) {
		extras.unknown = value;
		return this;
	}
	
	@STAMP(flows={@Flow(from="value",to="!this")})
    public android.content.Intent putIntegerArrayListExtra(java.lang.String key, java.util.ArrayList<java.lang.Integer> value) {
		extras.unknown = value;
		return this;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public android.content.Intent putStringArrayListExtra(java.lang.String key, java.util.ArrayList<java.lang.String> value) {
		extras.unknown = value;
		return this;
	}

	@STAMP(flows={@Flow(from="value",to="!this")})
    public android.content.Intent putCharSequenceArrayListExtra(java.lang.String key, java.util.ArrayList<java.lang.CharSequence> value) {
		extras.unknown = value;
		return this;
	}
}
