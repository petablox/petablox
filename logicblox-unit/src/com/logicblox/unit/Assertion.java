package com.logicblox.unit;

import java.util.List;
import com.blox.data.WorkSpace;

public abstract class Assertion
{
  public abstract void normalize(DatabaseInfo info);
  public abstract void run(WorkSpace db, List<String> messages) throws Exception;
}