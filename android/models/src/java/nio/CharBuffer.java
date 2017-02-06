class CharBuffer
{
    public CharBuffer() {}

    @STAMP(flows = {@Flow(from="array",to="@return")})
    public static  java.nio.CharBuffer wrap(char[] array) 
    {
		return new StampCharBuffer();
	}

	@STAMP(flows = {@Flow(from="array",to="@return"), @Flow(from="start",to="@return"), @Flow(from="charCount",to="@return")})
	public static  java.nio.CharBuffer wrap(char[] array, int start, int charCount) 
	{ 
		return new StampCharBuffer();
	}

	@STAMP(flows = {@Flow(from="chseq",to="@return")})
    public static  java.nio.CharBuffer wrap(java.lang.CharSequence chseq) 
	{
		return new StampCharBuffer();
	}

    @STAMP(flows = {@Flow(from="cs",to="@return"), @Flow(from="start",to="@return"), @Flow(from="end",to="@return")})
	public static  java.nio.CharBuffer wrap(java.lang.CharSequence cs, int start, int end) 
	{ 
		return new StampCharBuffer();
	}

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public final  char[] array() 
    {
		return new char[0];
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public final  char charAt(int index) 
    {
		return 'a';
    }
	
    @STAMP(flows = {@Flow(from="this",to="dst")})
    public  java.nio.CharBuffer get(char[] dst) 
    { 
		return this;
    }

    @STAMP(flows = {@Flow(from="this",to="dst")})
    public  java.nio.CharBuffer get(char[] dst, int dstOffset, int charCount) 
    {  
		return this;
    }

    @STAMP(flows = {@Flow(from="src",to="this")})
    public final  java.nio.CharBuffer put(char[] src) 
    {  
		return this;
    }
	
    @STAMP(flows = {@Flow(from="src",to="this")})
    public  java.nio.CharBuffer put(char[] src, int srcOffset, int charCount) 
    {
		return this;
    }

    @STAMP(flows = {@Flow(from="src",to="this")})
    public  java.nio.CharBuffer put(java.nio.CharBuffer src) 
    {  
		return this;
    }

    @STAMP(flows = {@Flow(from="str",to="this")})
	public final  java.nio.CharBuffer put(java.lang.String str) 
    {  
		return this;
    }

    @STAMP(flows = {@Flow(from="str",to="this")})
    public  java.nio.CharBuffer put(java.lang.String str, int start, int end) 
    {
		return this;
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public  java.lang.String toString() 
    {
		return new String();
    }

    @STAMP(flows = {@Flow(from="c",to="this")})
    public  java.nio.CharBuffer append(char c) 
    {
		return this;
    }

    @STAMP(flows = {@Flow(from="csq",to="this")})
    public  java.nio.CharBuffer append(java.lang.CharSequence csq) 
    {
		return this;
    }

    @STAMP(flows = {@Flow(from="csq",to="this")})
    public  java.nio.CharBuffer append(java.lang.CharSequence csq, int start, int end) 
    {  
		return this;
    }

    @STAMP(flows = {@Flow(from="this",to="target"), @Flow(from="this",to="@return"), @Flow(from="target",to="@return")})
    public  int read(java.nio.CharBuffer target) throws java.io.IOException 
    {  
    	return 0;
    }
}
