package android.view;
public class StampSurfaceHolder implements SurfaceHolder
{
	public   void addCallback(final android.view.SurfaceHolder.Callback callback)
	{
		callback.surfaceCreated(StampSurfaceHolder.this);
		callback.surfaceChanged(StampSurfaceHolder.this, 0, 0, 0);
		callback.surfaceDestroyed(StampSurfaceHolder.this);
	}

	public   void removeCallback(android.view.SurfaceHolder.Callback callback){ throw new RuntimeException("Stub!"); }

	public   boolean isCreating(){ throw new RuntimeException("Stub!"); }

	@java.lang.Deprecated()
		public   void setType(int type){ throw new RuntimeException("Stub!"); }

	public   void setFixedSize(int width, int height){ throw new RuntimeException("Stub!"); }

	public   void setSizeFromLayout(){ throw new RuntimeException("Stub!"); }

	public   void setFormat(int format){ throw new RuntimeException("Stub!"); }

	public   void setKeepScreenOn(boolean screenOn){ throw new RuntimeException("Stub!"); }

	public   android.graphics.Canvas lockCanvas(){ throw new RuntimeException("Stub!"); }

	public   android.graphics.Canvas lockCanvas(android.graphics.Rect dirty){ throw new RuntimeException("Stub!"); }

	public   void unlockCanvasAndPost(android.graphics.Canvas canvas){ throw new RuntimeException("Stub!"); }

	public   android.graphics.Rect getSurfaceFrame(){ throw new RuntimeException("Stub!"); }

	public   android.view.Surface getSurface(){ throw new RuntimeException("Stub!"); }

}
