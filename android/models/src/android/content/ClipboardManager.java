class ClipboardManager
{
	public  void addPrimaryClipChangedListener(android.content.ClipboardManager.OnPrimaryClipChangedListener listener) 
	{ 
		listener.onPrimaryClipChanged();
	}

	@STAMP(flows = {@Flow(from="$Clipboard",to="@return")})
	public  java.lang.CharSequence getText() 
	{ 
		return new java.lang.String();
	}

	@STAMP(flows = {@Flow(from="text",to="!Clipboard")})
	public  void setText(java.lang.CharSequence text) { 
	}

	private static ClipboardManager clipboardManager = new ClipboardManager();
	public static ClipboardManager getInstance()
	{
		return clipboardManager;
	}

}