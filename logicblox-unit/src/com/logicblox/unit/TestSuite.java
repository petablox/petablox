package com.logicblox.unit;

import java.util.ArrayList;
import java.util.List;

public class TestSuite
{

  private String _name;
  private List<AbstractTest> _tests;

  public TestSuite()
  {
    super();
    _tests = new ArrayList<AbstractTest>();
  }

  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }

  public void setTests(List<AbstractTest> tests)
  {
    _tests = tests;
  }

  public List<AbstractTest> getTests()
  {
    return _tests;
  }
}
