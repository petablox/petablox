class FileReader
{
    @STAMP(flows = {@Flow(from="file",to="this")})
	public  FileReader(java.io.File file) throws java.io.FileNotFoundException { super((java.io.InputStream)null,(java.nio.charset.Charset)null); }

    @STAMP(flows = {@Flow(from="fd",to="this")})
	public  FileReader(java.io.FileDescriptor fd) { super((java.io.InputStream)null,(java.nio.charset.Charset)null); }
	
    @STAMP(flows = {@Flow(from="$FILE",to="this"), @Flow(from="filename",to="this")})
	public  FileReader(java.lang.String filename) throws java.io.FileNotFoundException { super((java.io.InputStream)null,(java.nio.charset.Charset)null); throw new RuntimeException("Stub!"); }
}