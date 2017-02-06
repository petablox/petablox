class GLSurfaceView {
    public  void setRenderer(final android.opengl.GLSurfaceView.Renderer renderer) {
        edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						renderer.onSurfaceCreated(null, null);
					}
				});
        edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						renderer.onSurfaceChanged(null, 0, 0);
					}
				});
        edu.stanford.stamp.harness.ApplicationDriver.getInstance().
			registerCallback(new edu.stanford.stamp.harness.Callback(){
					public void run() {
						renderer.onDrawFrame(null);
					}
				});
    }
}
