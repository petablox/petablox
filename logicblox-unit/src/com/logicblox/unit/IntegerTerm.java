package com.logicblox.unit;

public class IntegerTerm implements Term
{
  private int _value;

  public IntegerTerm(int value)
  {
    _value = value;
  }

  public Integer getValue()
  {
    return new Integer(_value);
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    builder.append(String.valueOf(getValue()));
  }
}