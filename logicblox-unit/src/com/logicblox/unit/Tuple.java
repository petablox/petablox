package com.logicblox.unit;

import java.util.List;
import java.util.ArrayList;

public class Tuple
{
  private List<Object>_terms;

  public Tuple()
  {
    super();
    _terms = new ArrayList<Object>();
  }

  public Tuple(Object o)
  {
    this();
    addTerm(o);
  }

  public void addTerm(Object o)
  {
    _terms.add(o);
  }

  public List<Object> getTerms()
  {
    return _terms;
  }

  public void setTerms(List<Object> terms)
  {
    _terms = terms;
  }

  public String toString()
  {
    return _terms.toString();
  }

  public boolean equals(final Object o)
  {
    if(!(o instanceof Tuple))
    {
      return false;
    }

    return _terms.equals(((Tuple) o).getTerms());
  }

  public int hashCode()
  {
    return _terms.hashCode();
  }
}
