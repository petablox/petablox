package android.os;
public class Build
{
	@STAMP(flows = {@Flow(from="$SDK",to="@return")})
	private static java.lang.String taintedSDK()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$ID",to="@return")})
	private static java.lang.String taintedId()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$CPU_ABI",to="@return")})
	private static java.lang.String taintedCPU()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$MANUFACTURER",to="@return")})
	private static java.lang.String taintedManufact()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$SERIAL",to="@return")})
	private static java.lang.String taintedSerial()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$FINGERPRINT",to="@return")})
	private static java.lang.String taintedFinger()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$HOST",to="@return")})
	private static java.lang.String taintedHost()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$MODEL",to="@return")})
	private static java.lang.String taintedModel()
	{
		return new java.lang.String("");
	}

	@STAMP(flows = {@Flow(from="$BRAND",to="@return")})
	private static java.lang.String taintedBrand()
	{
		return new java.lang.String("");
	}

    public static class VERSION
    {
        public  VERSION() { throw new RuntimeException("Stub!"); }
        /*public static final java.lang.String INCREMENTAL;
        public static final java.lang.String RELEASE;
        @java.lang.Deprecated()
        public static final java.lang.String SDK;
        public static final int SDK_INT;
        public static final java.lang.String CODENAME;*/
        static { 
            INCREMENTAL = null; 
            RELEASE = null; 
            //SDK = taintedSDK(); 
            SDK = null; 
            SDK_INT = 0; 
            CODENAME = null; 
        }
    }

    public static class VERSION_CODES
    {
        public  VERSION_CODES() { throw new RuntimeException("Stub!"); }
        /*public static final int CUR_DEVELOPMENT = 10000;
        public static final int BASE = 1;
        public static final int BASE_1_1 = 2;
        public static final int CUPCAKE = 3;
        public static final int DONUT = 4;
        public static final int ECLAIR = 5;
        public static final int ECLAIR_0_1 = 6;
        public static final int ECLAIR_MR1 = 7;
        public static final int FROYO = 8;
        public static final int GINGERBREAD = 9;
        public static final int GINGERBREAD_MR1 = 10;
        public static final int HONEYCOMB = 11;
        public static final int HONEYCOMB_MR1 = 12;
        public static final int HONEYCOMB_MR2 = 13;
        public static final int ICE_CREAM_SANDWICH = 14;
        public static final int ICE_CREAM_SANDWICH_MR1 = 15;
        public static final int JELLY_BEAN = 16;*/
    }

    public  Build() 
    { 

    }

    public static  java.lang.String getRadioVersion() { throw new RuntimeException("Stub!"); }
    /*public static final java.lang.String UNKNOWN = "unknown";
    public static final java.lang.String ID;
    public static final java.lang.String DISPLAY;
    public static final java.lang.String PRODUCT;
    public static final java.lang.String DEVICE;
    public static final java.lang.String BOARD;
    public static final java.lang.String CPU_ABI;
    public static final java.lang.String CPU_ABI2;
    public static final java.lang.String MANUFACTURER;
    public static final java.lang.String BRAND;
    public static final java.lang.String MODEL;
    public static final java.lang.String BOOTLOADER;
    @java.lang.Deprecated()
    public static final java.lang.String RADIO;
    public static final java.lang.String HARDWARE;
    public static final java.lang.String SERIAL;
    public static final java.lang.String TYPE;
    public static final java.lang.String TAGS;
    public static final java.lang.String FINGERPRINT;
    public static final long TIME;
    public static final java.lang.String USER;
    public static final java.lang.String HOST;*/

    static { 
        //ID = taintedId(); 
        ID = null;
        DISPLAY = null; 
        PRODUCT = null; 
        DEVICE = null; 
        BOARD = null; 
        CPU_ABI = null;
        //CPU_ABI = taintedCPU(); 
        CPU_ABI2 = null; 
        MANUFACTURER = null;
        //BRAND = taintedBrand(); 
        BRAND = null;
        //MODEL = taintedModel(); 
        MODEL = null;
        BOOTLOADER = null; 
        RADIO = null; 
        HARDWARE = null; 
        SERIAL = null;
        //SERIAL = taintedSerial(); 
        TYPE = null; 
        TAGS = null; 
        FINGERPRINT = null;
        //FINGERPRINT = taintedFinger(); 
        TIME = 0; 
        USER = null; 
        HOST = null;
        //HOST = taintedHost(); 
    }
}
