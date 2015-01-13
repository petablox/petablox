package com.logicblox.unit;

public abstract class AbstractTest
{
  private String _database;
  private String _description;

  public AbstractTest()
  {
  }

  public void setDescription(String description)
  {
    _description = description;
  }

  public String getDescription()
  {
    return _description;
  }

  public String getDatabase()
  {
    return _database;
  }

  public void setDatabase(String file)
  {
    _database = file;
  }

  public abstract void normalize(DatabaseInfo info);
}