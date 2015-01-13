package com.logicblox.unit;

public class BindTerm implements Term
{
  private Term _term;
  private Variable _bind;

  public BindTerm(Term term, Variable bind)
  {
    _term = term;
    _bind = bind;
  }

  public Object getValue()
  {
    return _term.getValue();
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    _bind.appendLogicBloxString(builder);
    builder.append(':');
    _term.appendLogicBloxString(builder);
  }
}