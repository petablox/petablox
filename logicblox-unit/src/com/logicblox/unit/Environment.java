package com.logicblox.unit;

import aterm.ATermFactory;
import aterm.pure.PureFactory;
import java.io.InputStream;

import org.spoofax.jsglr.ParseTable;
import org.spoofax.jsglr.SGLR;

public class Environment
{
  /**
   * Returns a parser for Datalog Testsuites.
   */
  public static SGLR getParser()
  {
    SGLR result = null;

    try
    {
      ATermFactory f = Environment.getATermFactory();
      ParseTable pt = new ParseTable(
	f.readFromFile(getResource("com/logicblox/unit/parsetable.tbl"))
      );
      result = new SGLR(f, pt);
    }
    catch(Exception exc)
    {
      exc.printStackTrace();
      System.exit(1);
    }

    return result;
  }

  public static InputStream getResource(String resource)
  {
    return Environment.class.getClassLoader().getResourceAsStream(resource);
  }

  public static ATermFactory getATermFactory()
  {
    return atermFactory.get();
  }

  public static void setATermFactory(ATermFactory factory)
  {
    atermFactory.set(factory);
  }

  private static ThreadLocal<ATermFactory> atermFactory =
    new ThreadLocal<ATermFactory>()
    {
      public ATermFactory initialValue()
      {
        return new PureFactory();
      }
    };
}