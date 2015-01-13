package com.logicblox.unit;

import java.util.ArrayList;
import java.util.List;

import com.blox.data.Predicate;
import com.blox.data.WorkSpace;

public class AssertLiteral extends Assertion implements QueryContext
{
  Literal _literal;
  List<Literal> _auxLiterals;
  String _text;
 
  public AssertLiteral(Literal lit)
  {
    super();
    _literal = lit;
    _auxLiterals = new ArrayList<Literal>();
  }

  public void normalize(DatabaseInfo info)
  {
    StringBuilder result = new StringBuilder();
    getLiteral().appendLogicBloxString(result);
    _text = result.toString();

    FreshVarGenerator generator = new FreshVarGenerator();
    _literal.normalize(info, this, generator);
  }

  public void addAuxLiteral(Literal lit)
  {
    _auxLiterals.add(lit);
  }

  public Literal getLiteral()
  {
    return _literal;
  }

  public int getExpectedResult()
  {
    if(getLiteral().isPositive())
    {
      return 1;
    }
    else
    {
      return 0;
    }
  }

  public void run(WorkSpace db, List<String> messages) throws Exception
  {
    Predicate result = db.query(getLogicBloxRule()).get(0);
    if(result.getAllFacts().size() != getExpectedResult())
    {
      messages.add("failed: " + _text);
    }
  }

  public String getLogicBloxRule()
  {
    StringBuilder result = new StringBuilder();
    result.append("_logicbloxUnit:query(thisMustBeFresh0) <- ");

    // we handle negation ourselves, so we get the atom
    getLiteral().getAtom().appendLogicBloxString(result);

    for(Literal l : _auxLiterals)
    {
      result.append(", ");
      l.appendLogicBloxString(result);
    }
    result.append('.');

    // System.err.println(result);
    return result.toString();
  }
}