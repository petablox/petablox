interface CharSequence
{
	@STAMP(flows = {@Flow(from="this",to="@return")})
	public abstract  char charAt(int index);
}