package com.logicblox.unit;

public class FreshVarGenerator
{
  private int _counter = 0;

  public String next()
  {
    return "thisMustBeFresh" + _counter++;
  }
}