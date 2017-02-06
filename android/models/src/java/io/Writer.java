class Writer 
{
	@STAMP(flows = {@Flow(from="oneChar",to="!this")})
    public void write(int oneChar) throws java.io.IOException {
    }

	@STAMP(flows = {@Flow(from="buf",to="!this")})
	public abstract  void write(char[] buf, int offset, int count) throws java.io.IOException;

	@STAMP(flows = {@Flow(from="buf",to="!this")})
    public void write(char[] buf) throws java.io.IOException {
    }
	
	@STAMP(flows = {@Flow(from="str",to="!this")})
    public void write(java.lang.String str) throws java.io.IOException {
    }
	
	@STAMP(flows = {@Flow(from="str",to="!this")})
    public void write(java.lang.String str, int offset, int count) throws java.io.IOException {

    }
	
	@STAMP(flows = {@Flow(from="c",to="!this")})
    public java.io.Writer append(char c) throws java.io.IOException {
        return this;
    }
	
	@STAMP(flows = {@Flow(from="csq",to="!this")})
    public java.io.Writer append(java.lang.CharSequence csq) throws java.io.IOException {
        return this;
    }

	@STAMP(flows = {@Flow(from="csq",to="!this")})
    public java.io.Writer append(java.lang.CharSequence csq, int start, int end) throws java.io.IOException {
        return this;
    }

}
