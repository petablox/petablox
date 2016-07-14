class FileWriter 
{  
	@STAMP(flows = {@Flow(from="file", to="this")})
	public  FileWriter(java.io.File file) throws java.io.IOException { super((java.io.OutputStream)null,(java.nio.charset.CharsetEncoder)null); }

	@STAMP(flows = {@Flow(from="file", to="this")})
	public  FileWriter(java.io.File file, boolean append) throws java.io.IOException { super((java.io.OutputStream)null,(java.nio.charset.CharsetEncoder)null);  }

	@STAMP(flows = {@Flow(from="fd", to="this")})
	public  FileWriter(java.io.FileDescriptor fd) { super((java.io.OutputStream)null,(java.nio.charset.CharsetEncoder)null); }

	@STAMP(flows = {@Flow(from="filename", to="this")})
	public  FileWriter(java.lang.String filename) throws java.io.IOException { super((java.io.OutputStream)null,(java.nio.charset.CharsetEncoder)null);  }

	@STAMP(flows = {@Flow(from="filename", to="this")})
	public  FileWriter(java.lang.String filename, boolean append) throws java.io.IOException { super((java.io.OutputStream)null,(java.nio.charset.CharsetEncoder)null); }
}
