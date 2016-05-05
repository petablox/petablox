class Object
{
	protected  java.lang.Object clone() throws java.lang.CloneNotSupportedException { return this; }

	@STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.lang.String toString() 
	{ 
		return new String(); 
	}
	
	//public Object(){  }
}
