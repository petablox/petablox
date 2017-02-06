class SurfaceView
{
	public  SurfaceView(android.content.Context context) { 
		super(context);
	}

	public  android.view.SurfaceHolder getHolder() { 
		return new StampSurfaceHolder();
	}
}