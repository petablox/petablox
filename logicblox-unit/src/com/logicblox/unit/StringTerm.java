package com.logicblox.unit;

public class StringTerm implements Term
{
  private String _value;
  
  public StringTerm(String value)
  {
    _value = value;
  }

  public String getValue()
  {
    return _value;
  }

  public String toString()
  {
    return getValue();
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    builder.append('\"');
    builder.append(String.valueOf(getValue()));
    builder.append('\"');
  }
}