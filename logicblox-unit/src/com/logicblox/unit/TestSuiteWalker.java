package com.logicblox.unit;

import aterm.ATermFactory;
import java.util.HashSet;
import java.util.List;

public class TestSuiteWalker extends ParseTreeWalker
{
  private AbstractTest _currentTest;
  private Query _currentQuery;

  public TestSuiteWalker(ATermFactory factory)
  {
    super(factory);

    setAction("logicblox-testsuite",
      new Action() {
	public Object apply() {
	  final TestSuite result = new TestSuite();
	  result.setName(yieldArgument(2));
	  result.setTests(collectArgumentList(4, AbstractTest.class));
	  return result;
	}
      });
    
    setAction("logicblox-test",
      new Action() {
	public Object apply() {
	  Test result = new Test();
	  result.setDescription(yieldArgument(2));
	  _currentTest = result;
	  traverse(4);
	  traverse(6);
	  _currentTest = null;
	  return result;
	}
      });

    setAction("logicblox-test-no-description",
      new Action() {
	public Object apply() {
	  Test result = new Test();
	  _currentTest = result;
	  traverse(2);
	  traverse(4);
	  _currentTest = null;
	  return result;
	}
      });

    /* Assertions */

    setAction("logicblox-assertions",
      new Action() {
	public Object apply() {
	  AssertTest result = new AssertTest();
	  result.setDescription(yieldArgument(2));
	  _currentTest = result;
	  traverse(4);
	  traverse(6);
	  _currentTest = null;
	  return result;
	}
      });

    setAction("database",
      new Action() {
	public Object apply() {
	  _currentTest.setDatabase(yieldArgument(2));
	  _currentQuery = null;
	  return null;
	}
      });

    setAction("assertions",
      new Action() {
	public Object apply()
	{
	  ((AssertTest) _currentTest).setAssertions(collectArgumentList(2, Assertion.class));
	  return null;
	}
      });

    setAction("assert-literal",
      new Action() {
	public Object apply()
	{
	  return new AssertLiteral(collectSingleArgument(0, Literal.class));
	}
      });

    /* Queries */

    setAction("query-database",
      new Action() {
	public Object apply() {
	  _currentTest.setDatabase(yieldArgument(2));
	  traverse(6);
	  _currentQuery = null;
	  return null;
	}
      });

    setAction("query",
      new Action() {
	public Object apply()
	{
	  _currentQuery = new Query();
	  ((Test) _currentTest).setQuery(_currentQuery);
	  _currentQuery.setQueryString(yieldArgument(0));
	  _currentQuery.setLiterals(collectArgumentList(0, Literal.class));
	  return null;
	}
      });

    setAction("positive",
      new Action() {
	public Object apply()
	{
	  return new Literal(collectSingleArgument(0, Atom.class));
	}
      });

    setAction("negative",
      new Action() {
	public Object apply()
	{
	  Literal result = new Literal(collectSingleArgument(2, Atom.class));
	  result.setNegated();
	  return result;
	}
      });

    setAction("atom",
      new Action() {
	public Object apply()
	{
	  final Atom atom = new Atom(yieldArgument(0));
	  atom.setKeyTerms(collectArgumentList(4, Term.class));
	  return atom;
	}
      });

    setAction("atom-function",
      new Action() {
	public Object apply()
	{
	  final Atom atom = new Atom(yieldArgument(0));
	  atom.setKeyTerms(collectArgumentList(4, Term.class));
	  atom.setValueTerm(collectSingleArgument(10, Term.class));
	  return atom;
	}
      });

    setAction("variable",
      new Action() {
	public Object apply() {
	  String s = yieldArguments();
	  Variable v = new Variable(s);
	  if(_currentQuery != null)
	  {
	    _currentQuery.addVariable(v);
	  }
	  return v;
	}
      });

    setAction("integer",
      new Action() {
	public Object apply() {
	  return new IntegerTerm(Integer.parseInt(yieldArguments()));
	}
      });

    setAction("wildcard",
      new Action() {
	public Object apply() {
	  return new Wildcard();
	}
      });

    setAction("string",
      new Action() {
	public Object apply() {
	  String s = yieldArguments();
	  return new StringTerm(s.substring(1, s.length() - 1));
	}
      });

    setAction("result-project",
      new Action() {
	public Object apply() {
	  ((Test) _currentTest).getQuery().addProjectVariables(collectArgumentList(4, Variable.class));
	  traverse(6);
	  return null;
	}
      });

    setAction("result-single-row-relation",
      new Action() {
	public Object apply() {
	  final HashSet<Tuple> set = new HashSet<Tuple>();
	  collectArgument(2,
	    new Collect() {
	      public void apply(Object o) {
		Object value = ((Term) o).getValue();
		set.add(new Tuple(value));
	      }
	    });

	  ((Test) _currentTest).setExpectedResult(set);
	  return null;
	}
      });

    setAction("result-relation",
      new Action() {
	public Object apply() {
	  final HashSet<Tuple> set = new HashSet<Tuple>();
	  collectArgument(2,
	    new Collect() {
	      public void apply(Object o) {
		set.add((Tuple) o);
	      }
	    });

	  ((Test) _currentTest).setExpectedResult(set);
	  return null;
	}
      });

    setAction("result-empty-relation",
      new Action() {
	public Object apply() {
	  final HashSet<Tuple> set = new HashSet<Tuple>();
	  ((Test) _currentTest).setExpectedResult(set);
	  return null;
	}
      });

    setAction("tuple",
      new Action() {
	public Object apply() {
	  List<Term> terms = collectArgumentList(2, Term.class);
	  Tuple result = new Tuple();
	  for(Term t : terms)
	  {
	    Object value = t.getValue();
	    result.addTerm(value);
	  }

	  return result;
	}
      });
  }
}
