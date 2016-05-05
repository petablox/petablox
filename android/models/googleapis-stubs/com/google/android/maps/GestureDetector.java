package com.google.android.maps;

 class GestureDetector
{

  public  GestureDetector(com.google.android.maps.GestureDetector.OnGestureListener param1, android.os.Handler param2)
  {
    throw new RuntimeException("Stub!");
  }

  public  GestureDetector(com.google.android.maps.GestureDetector.OnGestureListener param1)
  {
    throw new RuntimeException("Stub!");
  }

  public  GestureDetector(android.content.Context param1, com.google.android.maps.GestureDetector.OnGestureListener param2)
  {
    throw new RuntimeException("Stub!");
  }

  public  GestureDetector(android.content.Context param1, com.google.android.maps.GestureDetector.OnGestureListener param2, android.os.Handler param3)
  {
    throw new RuntimeException("Stub!");
  }

  public void setOnDoubleTapListener(com.google.android.maps.GestureDetector.OnDoubleTapListener param1)
  {
    throw new RuntimeException("Stub!");
  }

  public void setIsLongpressEnabled(boolean param1)
  {
    throw new RuntimeException("Stub!");
  }

  public boolean isLongpressEnabled()
  {
    throw new RuntimeException("Stub!");
  }

  public boolean onTouchEvent(android.view.MotionEvent param1)
  {
    throw new RuntimeException("Stub!");
  }

   class GestureHandler extends android.os.Handler
  {

    public void handleMessage(android.os.Message param1)
    {
      throw new RuntimeException("Stub!");
    }


  }

  public interface OnGestureListener
  {

    public abstract boolean onDown(android.view.MotionEvent param1);

    public abstract void onShowPress(android.view.MotionEvent param1);

    public abstract boolean onSingleTapUp(android.view.MotionEvent param1);

    public abstract boolean onScroll(android.view.MotionEvent param1, android.view.MotionEvent param2, float param3, float param4);

    public abstract void onLongPress(android.view.MotionEvent param1);

    public abstract boolean onFling(android.view.MotionEvent param1, android.view.MotionEvent param2, float param3, float param4);


  }

  public interface OnDoubleTapListener
  {

    public abstract boolean onSingleTapConfirmed(android.view.MotionEvent param1);

    public abstract boolean onDoubleTap(android.view.MotionEvent param1);

    public abstract boolean onDoubleTapEvent(android.view.MotionEvent param1);


  }

  public class SimpleOnGestureListener implements com.google.android.maps.GestureDetector.OnGestureListener, com.google.android.maps.GestureDetector.OnDoubleTapListener
  {

    public  SimpleOnGestureListener()
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onSingleTapUp(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }

    public void onLongPress(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onScroll(android.view.MotionEvent param1, android.view.MotionEvent param2, float param3, float param4)
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onFling(android.view.MotionEvent param1, android.view.MotionEvent param2, float param3, float param4)
    {
      throw new RuntimeException("Stub!");
    }

    public void onShowPress(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onDown(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onDoubleTap(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onDoubleTapEvent(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }

    public boolean onSingleTapConfirmed(android.view.MotionEvent param1)
    {
      throw new RuntimeException("Stub!");
    }


  }


}

