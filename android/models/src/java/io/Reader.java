class Reader 
{
    @STAMP(flows = { @Flow(from = "this", to = "@return") })
	public  int read() throws java.io.IOException { return 1; }

    @STAMP(flows = { @Flow(from = "this", to = "buf") })
    public int read(char[] buf) throws java.io.IOException { return 1; }
	
    @STAMP(flows = { @Flow(from = "this", to = "buf") })
    public abstract int read(char[] buf, int offset, int count) throws java.io.IOException;
	
    @STAMP(flows = { @Flow(from = "this", to = "target") })
    public int read(java.nio.CharBuffer target) throws java.io.IOException { return 1;}
}