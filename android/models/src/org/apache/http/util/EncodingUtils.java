class EncodingUtils
{
    @STAMP(flows = {@Flow(from="data",to="@return")})
	public static  java.lang.String getString(byte[] data, int offset, int length, java.lang.String charset) {
	return new String();
    }
    
    @STAMP(flows = {@Flow(from="data",to="@return")})
	public static  java.lang.String getString(byte[] data, java.lang.String charset) {
	return new String();
    }
    
    @STAMP(flows = {@Flow(from="data",to="@return")})
	public static  byte[] getBytes(java.lang.String data, java.lang.String charset) {
	return new byte[1];
    }
    
    @STAMP(flows = {@Flow(from="data",to="@return")})
	public static  byte[] getAsciiBytes(java.lang.String data) {
	return new byte[1];  
    }
    
    @STAMP(flows = {@Flow(from="data",to="@return")})
	public static  java.lang.String getAsciiString(byte[] data, int offset, int length) {	
	return new String();   
    }
    
    @STAMP(flows = {@Flow(from="data",to="@return")})
	public static  java.lang.String getAsciiString(byte[] data) {	
	return new String();   
    }
}
