package com.logicblox.unit;

import java.util.List;
import java.util.HashSet;

public class Test extends AbstractTest
{
  private Query _query;
  private HashSet<Tuple> _result;

  public String getDescription()
  {
    String description = super.getDescription();
    if(description != null)
    {
      return description;
    }
    else
    {
      return getQuery().getQueryString();
    }
  }

  public void setQuery(Query query)
  {
    _query = query;
  }

  public Query getQuery()
  {
    return _query;
  }

  public void setExpectedResult(HashSet<Tuple> relation)
  {
    _result = relation;
  }

  public HashSet<Tuple> getExpectedResult()
  {
    return _result;
  }

  public void normalize(DatabaseInfo info)
  {
    getQuery().normalize(info);
  }
}