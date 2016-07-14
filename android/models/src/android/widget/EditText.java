class EditText
{
	public  EditText(android.content.Context context)
	{
		super(context);
	}

	@STAMP(flows={@Flow(from="this",to="@return")})
	public  android.text.Editable getText() 
	{
		return new android.text.SpannableStringBuilder();
	}
}