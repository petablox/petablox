public class SpeechRecognizer
{
	private android.speech.RecognitionListener myListener;
	
	public void setRecognitionListener(android.speech.RecognitionListener listener) 
	{
		myListener = listener;
	}

	public void startListening(android.content.Intent recognizerIntent) 
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		list.add(taintedString());
		android.os.Bundle b = new android.os.Bundle();
		b.putStringArrayList(RESULTS_RECOGNITION, list);
		myListener.onResults(b);
	}

	@STAMP(flows={@Flow(from="$AUDIO",to="@return")})
	private String taintedString()
	{
		return new String();
	}
}
