package com.logicblox.unit;

public class Wildcard implements Term
{
  public Object getValue()
  {
    return new RuntimeException("Wildcards don't have a value");
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    builder.append('_');
  }
}