package com.logicblox.unit;

public class Variable implements Term
{
  private String _name;
  // private String _refMode;

  public Variable(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  /*
  public String getRefMode()
  {
    return _refMode;
  }

  public void setRefMode(String refMode)
  {
    _refMode = refMode;
  }
   */

  public String toString()
  {
    return _name;
  }

  public Object getValue()
  {
    throw new RuntimeException("Variable has no value");
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    builder.append(getName());
  }

  public boolean equals(final Object o)
  {
    if(!(o instanceof Variable))
    {
      return false;
    }

    return getName().equals(((Variable) o).getName());
  }

  public int hashCode()
  {
    return getName().hashCode();
  }
}