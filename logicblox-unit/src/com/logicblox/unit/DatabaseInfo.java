package com.logicblox.unit;

import com.blox.data.Predicate;
import com.blox.data.WorkSpace;

public class DatabaseInfo
{
  private WorkSpace _db;

  public DatabaseInfo(WorkSpace db)
  {
    _db = db;
  }

  public Predicate getPredicate(String predicate)
  {
    try
    {
      Predicate p = _db.getPredicate(predicate);
      if(p == null)
      {
	throw new RuntimeException("No predicate " + predicate + " in workspace.");
      }
      return p;
    }
    catch(Exception exc)
    {
      throw new RuntimeException(exc);
    }
  }

  /**
   * TODO: temporary hack to work around difficulties with ref-modes on subtypes.
   */
  private String getKnownRefMode(String entity)
  {
    if(   "HeapAllocationRef".equals(entity)
       || "MethodInvocationRef".equals(entity)
       || "CallGraphEdgeSourceRef".equals(entity))
    {
      return "InstructionRef:Value";
    }
    else
    {
      return entity + ":Value";
    }
  }

  public String getRefMode(String entity)
  {
    // TODO: don't know how to find RefMode. Assuming Value.
    return getKnownRefMode(entity);
  }

  public String getKeyRefMode(String predicate, int i)
  {
    try
    {
      Predicate p = getPredicate(predicate);
      return getKeyRefMode(p, i);
    }
    catch(Exception exc)
    {
      throw new RuntimeException(exc);
    }
  }

  public String getKeyRefMode(Predicate p, int i)
  {
    try
    {
      return getRefMode(p.id().getKeyIdAt(i).name());
    }
    catch(Exception exc)
    {
      throw new RuntimeException(exc);
    }
  }

  public String getValueRefMode(String predicate)
  {
    try
    {
      Predicate p = _db.getPredicate(predicate);

      if(p == null)
      {
	throw new RuntimeException("No predicate " + predicate + " in workspace.");
      }

      return getRefMode(p.id().getValueIdAt(0).name());
    }
    catch(Exception exc)
    {
      throw new RuntimeException(exc);
    }
  }
}
