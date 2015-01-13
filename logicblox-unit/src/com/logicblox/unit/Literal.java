package com.logicblox.unit;

public class Literal
{
  private Atom _atom;
  private boolean _positive = true;

  public Literal(Atom atom)
  {
    super();
    setAtom(atom);
  }

  public Atom getAtom()
  {
    return _atom;
  }

  public void setAtom(Atom atom)
  {
    _atom = atom;
  }

  public boolean isPositive()
  {
    return _positive;
  }

  public boolean isNegated()
  {
    return !isPositive();
  }

  public void setNegated()
  {
    _positive = false;
  }

  public void normalize(DatabaseInfo info, QueryContext query, FreshVarGenerator generator)
  {
    getAtom().normalize(info, query, generator);
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    if(isNegated())
    {
      builder.append("not ");
    }

    getAtom().appendLogicBloxString(builder);
  }
}