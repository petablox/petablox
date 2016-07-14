public class Camera
{
    @STAMP(flows = {@Flow(from="$CAMERA.picture",to="@return")})
    public byte[] getPicture() {
		return new byte[1];
    }

    private Camera() {
    }

    public static android.hardware.Camera open(int cameraId) { return new Camera(); }

    public static android.hardware.Camera open() {  return new Camera(); }

    public final void takePicture(android.hardware.Camera.ShutterCallback shutter, 
								  final android.hardware.Camera.PictureCallback raw, 
								  final android.hardware.Camera.PictureCallback jpeg) { 
		raw.onPictureTaken(getPicture(), Camera.this);
		jpeg.onPictureTaken(getPicture(), Camera.this);
    }

    public final  void takePicture(android.hardware.Camera.ShutterCallback shutter, 
								   final android.hardware.Camera.PictureCallback raw, 
								   final android.hardware.Camera.PictureCallback postview, 
								   final android.hardware.Camera.PictureCallback jpeg) {
		raw.onPictureTaken(getPicture(), Camera.this);
		postview.onPictureTaken(getPicture(), Camera.this);
		jpeg.onPictureTaken(getPicture(), Camera.this);
    }

	public final  void setPreviewCallback(final android.hardware.Camera.PreviewCallback cb) 
	{ 
		cb.onPreviewFrame(getPicture(), Camera.this);
	}

	public final  void setOneShotPreviewCallback(final android.hardware.Camera.PreviewCallback cb) 
	{
		cb.onPreviewFrame(getPicture(), Camera.this);
	}

	public final  void setPreviewCallbackWithBuffer(final android.hardware.Camera.PreviewCallback cb) 
	{ 
		cb.onPreviewFrame(getPicture(), Camera.this);
	}
}