package com.logicblox.unit;

import java.util.List;

public class AssertTest extends AbstractTest
{
  private List<Assertion> _assertions;

  public void setAssertions(List<Assertion> assertions)
  {
    _assertions = assertions;
  }

  public List<Assertion> getAssertions()
  {
    return _assertions;
  }

  public void normalize(DatabaseInfo info)
  {
    for(Assertion a : getAssertions())
    {
      a.normalize(info);
    }
  }
}