package com.logicblox.unit;

import aterm.ATerm;
import aterm.ATermFactory;
import org.spoofax.jsglr.SGLR;
import org.spoofax.jsglr.SGLRException;

import com.blox.app.BloxLib;
import com.blox.app.Committable;
import com.blox.app.LbLogger;
import com.blox.app.TransactionParameters;
import com.blox.data.ArrayKey;
import com.blox.data.Predicate;
import com.blox.data.WorkSpace;
import com.blox.data.Fact;
import com.blox.ldbc.Session;
import com.blox.ldbc.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Main
{
  public static void main(String[] ps)
  {
    if(ps.length == 0)
    {
      System.err.println("error: please specify a path to a LogicBlox testsuite");
      System.err.println("usage: logicblox-unit <testsuite>");
      System.exit(1);
    }

    String testsuite = ps[0];

    try
    {
      TestSuite suite = load(testsuite);

      BloxLib.startup(BloxLib.LibType.Optimized);
      BloxLib.setLogLevel(LbLogger.BloxLibLevel.WARNING.getLevel());

      if(runTestSuite(suite, testsuite))
      {
	System.exit(0);
      }
      else
      {
	System.exit(1);
      }
    }
    catch(Exception exc)
    {
      exc.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Loads a TestSuite from the file specified by this file name.
   * 
   * @param suiteFile
   * @return
   * @throws FileNotFoundException
   * @throws IOException
   * @throws SGLRException
   */
  public static TestSuite load(String suiteFile) throws FileNotFoundException, IOException, SGLRException {
	  SGLR parser = Environment.getParser();
      ATerm t = parser.parse(new FileInputStream(suiteFile));
      TestSuiteWalker walker = new TestSuiteWalker(Environment.getATermFactory());
      return (TestSuite) walker.start(t);
  }

  public static boolean runTestSuite(TestSuite suite, String testsuite) throws Exception
  {
    int successes = 0;
    int failures = 0;

    System.out.println("-----------------------------------------------------------------------");
    System.out.println("executing testsuite " + suite.getName());
    System.out.println("  defined in file " + testsuite);
    // System.out.println("  with " + suite.getTests().size() + " tests");

    System.out.println("-----------------------------------------------------------------------");
    List<AbstractTest> tests = suite.getTests();
    for(int i = 0; i < tests.size(); i++)
    {
      if(runTest(i, tests.get(i)))
      {
	successes++;
      }
      else
      {
	failures++;
      }
    }

    System.out.println("-----------------------------------------------------------------------");
    System.out.println("results testsuite " + suite.getName());
    System.out.println("successes : " + successes);
    System.out.println("failures  : " + failures);
    System.out.println("-----------------------------------------------------------------------");

    return failures == 0;
  }
  
  public static boolean runTest(int index, final AbstractTest test) throws Exception
  {
    String dbPath = test.getDatabase();
    Session session = null;
    WorkSpace dbTemp = null;

    // hack for final messages
    final List<String> messages = new ArrayList<String>();

    try
    {
      SessionManager manager = SessionManager.instance();
      // manager.useGlobalTxnLock(false); // TODO hack, no idea why I did this.
      session = manager.createSession();

      dbTemp = WorkSpace.open(new File(dbPath).getCanonicalPath(), session);
      final WorkSpace db = dbTemp;

      db.executeInTransaction(new Committable()
      {
	public TransactionParameters transactionParameters()
	{
	  return TransactionParameters.WriteTxn;
	}
	
	public boolean run()
	{
	  try
	  {
	    DatabaseInfo info = new DatabaseInfo(db);
	    test.normalize(info);

	    if(test instanceof Test)
	    {
	      Test queryTest = (Test) test;
	      String rule = queryTest.getQuery().getLogicBloxRule();
	      // System.err.println(rule);
	      Predicate result = db.query(rule).get(0);
	      // hack for final messages
	      messages.addAll(compareRelation(info, result, queryTest));
	    }
	    else
	    {
	      AssertTest assertTest = (AssertTest) test;
	      for(Assertion assertion : assertTest.getAssertions())
	      {
		assertion.run(db, messages);
	      }
	    }
	  }
	  catch(Exception exc)
	  {
	    exc.printStackTrace();
	    messages.add(exc.getMessage());
	  }
	  
	  return false; // TODO: never commit?
	}
      });
    }
    finally
    {
      if(dbTemp != null)
      {
	dbTemp.close();
      }

      if(session != null)
      {
	session.close();
      }
    }

    if(messages.size() == 0)
    {
      System.out.println("* OK   : test " + (index + 1) + " - " + test.getDescription());
      return true;
    }
    else
    {
      System.out.println("* ERROR: test " + (index + 1) + " - " + test.getDescription());
      for(String s: messages)
      {
	System.out.println("  - " + s);
      }
      return false;
    }
  }

  public static List<String> compareRelation(DatabaseInfo info, Predicate actual, Test expected)
  throws Exception
  {
    // only HashSet is cloneable.
    HashSet<Tuple> expectedSet = expected.getExpectedResult();
    HashSet<Tuple> actualSet = factsToSet(info, actual);

    Set<Tuple> missing = (Set<Tuple>) expectedSet.clone();
    missing.removeAll(actualSet);

    Set<Tuple> toomuch = (Set<Tuple>) actualSet.clone();
    toomuch.removeAll(expectedSet);

    List<String> messages = new ArrayList<String>();
    if(toomuch.size() == 0)
    {
      for(Object t : missing)
      {
	messages.add("missing result: " + t);
      }
    }
    else
    {
      for(Object t : missing)
      {
	messages.add("missing result:    " + t);
      }
    }

    for(Object t : toomuch)
    {
      messages.add("unexpected result: " + t);
    }

    return messages;
  }

  public static HashSet<Tuple> factsToSet(DatabaseInfo info, Predicate p) throws Exception
  {
    List<Fact> facts = p.getAllFacts();
    HashSet<Tuple> result = new HashSet<Tuple>();

    Predicate[] refModes = new Predicate[p.keyArity()];
    for(int i =0 ; i < refModes.length; i++)
    {
      String refMode = info.getKeyRefMode(p, i);
      refModes[i] = info.getPredicate(refMode);
    }

    for(Fact f : facts)
    {
      ArrayKey key = f.key();
      Tuple tuple = new Tuple();

      for(int i = 0; i < key.size(); i++)
      {
	long index = key.getIndex(i);
	ArrayKey refModeKey = new ArrayKey(new long[]{index});
	Fact refModeFact = refModes[i].getFact(refModeKey);

	tuple.addTerm(refModeFact.valueAt(0));
      }

      result.add(tuple);
    }

    return result;
  }
}
