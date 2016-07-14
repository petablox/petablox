class CharArrayWriter 
{
	@STAMP(flows = {@Flow(from="this",to="@return")})
    public char[] toCharArray() { return new char[1];}
}