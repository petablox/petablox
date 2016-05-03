package com.google.android.maps;

public interface Projection
{

  public abstract android.graphics.Point toPixels(com.google.android.maps.GeoPoint param1, android.graphics.Point param2);

  public abstract com.google.android.maps.GeoPoint fromPixels(int param1, int param2);

  public abstract float metersToEquatorPixels(float param1);


}

