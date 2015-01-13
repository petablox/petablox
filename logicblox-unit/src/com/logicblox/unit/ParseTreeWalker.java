package com.logicblox.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import aterm.ATerm;
import aterm.ATermList;
import aterm.ATermAppl;
import aterm.ATermFactory;
import org.spoofax.jsglr.ParseTreeTools;

public class ParseTreeWalker
{
  private Map<String, Action> _actions;
  private ATermAppl _current;
  private ParseTreeTools _tools;

  public ParseTreeWalker(ATermFactory factory)
  {
    super();
    _actions = new HashMap<String, Action>();
    _tools = new ParseTreeTools(factory);
  }

  public Object start(ATerm t)
  {
    List<ATerm> tops = findConstructorApplications(((ATermAppl) t).getArgument(0));
    if(tops.size() > 1)
    {
      throw new IllegalArgumentException("More than 1 outermost constructor application found");
    }

    return apply(tops.get(0));
  }

  public Object apply(ATerm t)
  {
    String s = _tools.getConstructor(t);
    Action a = getAction(s);
    if(a == null)
    {
      System.err.println("warning: no action for constructor " + s);
      return null;
    }
    else
    {
      ATermAppl previous = getCurrent();
      _current = (ATermAppl) t;
      Object result =a.apply();
      _current = previous;
      return result;
    }
  }

  /**
   * Set the action for the given constructor name.
   */
  public void setAction(String constructor, Action action)
  {
    _actions.put(constructor, action);
  }

  public Action getAction(String constructor)
  {
    return _actions.get(constructor);
  }

  /**
   * Returns the current production application.
   */
  public ATermAppl getCurrent()
  {
    return _current;
  }

  /**
   * Returns the production of the current production application.
   */
  public ATermAppl getCurrentProduction()
  {
    return (ATermAppl) getCurrent().getArgument(0);
  }

  public ATermList getCurrentArguments()
  {
    return (ATermList) getCurrent().getArgument(1);
  }

  public ATerm getCurrentArgument(int index)
  {
    return getCurrentArguments().elementAt(index);
  }

  /**
   * Yield argument at index of the current production application.
   */
  public String yieldArgument(int index)
  {
    return _tools.yield(getCurrentArgument(index));
  }

  /**
   * Yield all the arguments of the current production application.
   */
  public String yieldArguments()
  {
    StringBuilder builder = new StringBuilder();
    for(ATerm arg: getCurrentArguments())
    {
      _tools.yield(arg, builder);
    }
    return builder.toString();
  }

  /**
   * Collect all the results of walking all the outermost constructor applications.
   */
  public <T> List<T> collectArgumentList(int index, final Class<T> clazz)
  {
    final List<T> result = new ArrayList<T>();

    collectArgument(index, new Collect()
    {
      public void apply(Object o)
      {
	result.add(clazz.cast(o));
      }
    });

    return result;
  }

  public <T> T collectSingleArgument(int index, Class<T> clazz)
  {
    List<T> result = collectArgumentList(index, clazz);
    if(result.size() > 1)
    {
      throw new RuntimeException("Uh, didn't expect that");
    }

    return result.get(0);
  }

  /**
   * Collect all the results of walking all the outermost constructor applications.
   */
  public void collectArgument(int index, final Collect collect)
  {
    findConstructorApplications(getCurrentArgument(index), new CollectCons()
    {
      public void apply(ATerm t, String constructor) {
	collect.apply(ParseTreeWalker.this.apply(t));
      }
    });
  }

  /**
   * Walk the outermost constructor applications at index and ignore the results;
   */
  public void traverse(int index)
  {
    collectArgument(index,
      new Collect() {
	public void apply(Object o) {}
      });
  }

  interface Action
  {
    public Object apply();
  }

  interface Collect
  {
    public void apply(Object t);
  }

  interface CollectATerm
  {
    public void apply(ATerm t);
  }

  interface CollectCons
  {
    public void apply(ATerm t, String constructor);
  }

  public List<ATerm> findConstructorApplications(ATerm t)
  {
    final List<ATerm> result = new ArrayList<ATerm>();
    findConstructorApplications(t, new CollectCons()
    {
      public void apply(ATerm t, String constructor)
      {
	result.add(t);
      }
    });

    return result;
  }

  public void findConstructorApplications(ATerm t, CollectCons collect)
  {
    String cons = _tools.getConstructor(t);
    if(cons == null)
    {
      for(ATerm arg : (ATermList) ((ATermAppl) t).getArgument(1))
      {
	if(arg instanceof ATermAppl)
	{
	  findConstructorApplications(arg, collect);
	}
      }
    }
    else
    {
      collect.apply(t, cons);
    }
  }

}
