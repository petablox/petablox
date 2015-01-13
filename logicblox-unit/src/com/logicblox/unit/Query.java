package com.logicblox.unit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Query implements QueryContext
{
  private String _query;

  private List<Literal> _literals;
  private List<Variable> _variables;
  private Set<Variable> _project = null;

  public Query()
  {
    super();
    _variables = new ArrayList<Variable>();
    _literals = new ArrayList<Literal>();
  }

  public List<Literal> getLiterals()
  {
    return _literals;
  }

  public void addAuxLiteral(Literal lit)
  {
    addLiteral(lit);
  }

  public void addLiteral(Literal lit)
  {
    _literals.add(lit);
  }

  public void setLiterals(List<Literal> literals)
  {
    _literals = literals;
  }

  public List<Variable> getVariables()
  {
    return _variables;
  }

  /**
   * TODO: this should be implemented better.
   */
  public void addProjectVariables(Collection<Variable> v)
  {
    _project = new HashSet<Variable>();
    _project.addAll(v);
  }

  public List<Variable> getVariablesOfResult()
  {
    List<Variable> vars = getVariables();

    List<Variable> projected;
    if(_project == null) 
    {
      projected = vars;
    }
    else
    {
      projected = new ArrayList<Variable>();

      for(int i = 0; i < vars.size(); i++)
      {
	Variable v = vars.get(i);
	if(_project.contains(v))
	{
	  projected.add(v);
	}
      }
    }

    return projected;
  }

  public void addVariable(Variable var)
  {
    if(!_variables.contains(var))
    {
      _variables.add(var);
    }
  }

  public String getQueryString()
  {
    return _query;
  }

  public void setQueryString(String s)
  {
    _query = s;
  }

  public void normalize(DatabaseInfo info)
  {
    FreshVarGenerator generator = new FreshVarGenerator();
    // convert to array to deal with concurrent modfication
    for(Literal lit : getLiterals().toArray(new Literal[getLiterals().size()]))
    {
      lit.normalize(info, this, generator);
    }
  }

  public String getLogicBloxRule()
  {
    StringBuilder result = new StringBuilder();

    List<Variable> vars = getVariablesOfResult();

    result.append("_logicbloxUnit:query(");
    for(int i = 0; i < vars.size(); i++)
    {
      vars.get(i).appendLogicBloxString(result);

      if(i != vars.size() - 1)
      {
	result.append(", ");
      }
    }
    result.append(") <- ");

    List<Literal> literals = getLiterals();
    for(int i = 0; i < literals.size(); i++)
    {
      literals.get(i).appendLogicBloxString(result);

      if(i != literals.size() - 1)
      {
	result.append(", ");
      }
    }
    result.append('.');

    // System.err.println(result);
    return result.toString();
  }
}