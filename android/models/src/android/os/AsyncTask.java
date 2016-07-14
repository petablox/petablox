
class AsyncTask
{	
	public final  android.os.AsyncTask<Params, Progress, Result> execute(Params... params) 
	{ 
		onPreExecute();
		Result result = doInBackground(params);
		onPostExecute(result);
		return this;
	}
}
