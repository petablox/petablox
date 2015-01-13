package com.logicblox.unit;

import java.util.ArrayList;
import java.util.List;

public class Atom
{
  private String _predicate;
  private List<Term> _keyTerms;
  private Term _valueTerm;

  public Atom(String predicate)
  {
    super();
    _predicate = predicate;
    _keyTerms = new ArrayList<Term>();
  }

  public String getPredicate()
  {
    return _predicate;
  }

  public List<Term> getTerms()
  {
    return _keyTerms;
  }

  public void setKeyTerms(List<Term> terms)
  {
    _keyTerms = terms;
  }

  public void addKeyTerm(Term t)
  {
    _keyTerms.add(t);
  }

  public void setValueTerm(Term t)
  {
    _valueTerm = t;
  }

  public Term getValueTerm()
  {
    return _valueTerm;
  }

  public boolean isFunction()
  {
    return getValueTerm() != null;
  }

  public void normalize(DatabaseInfo info, QueryContext query, FreshVarGenerator generator)
  {
    List<Term> terms = getTerms();
    for(int i = 0; i < terms.size(); i++)
    {
      Term t = terms.get(i);
      String refMode = info.getKeyRefMode(getPredicate(), i);
      //String refMode = "Type:Value";
      
      if(t instanceof Variable)
      {
	Variable v = (Variable) t;
      }
      else
      {
	Variable v = new Variable(generator.next());
	Atom newAtom = new Atom(refMode);
	newAtom.addKeyTerm(new BindTerm(t, v));
	query.addAuxLiteral(new Literal(newAtom));
	terms.set(i, v);
      }
    }

    if(isFunction())
    {
      String refMode = info.getValueRefMode(getPredicate());

      if(getValueTerm() instanceof Variable)
      {
	Variable v = (Variable) getValueTerm();
      }
      else
      {
	Variable v = new Variable(generator.next());
	Atom newAtom = new Atom(refMode);
	newAtom.addKeyTerm(new BindTerm(getValueTerm(), v));
	query.addAuxLiteral(new Literal(newAtom));
	_valueTerm = v;
      }
    }
  }

  public void appendLogicBloxString(StringBuilder builder)
  {
    builder.append(getPredicate());

    if(isFunction())
    {
      builder.append('[');
    }
    else
    {
      builder.append('(');
    }

    List<Term> terms = getTerms();
    for(int i =0; i < terms.size(); i++)
    {
      Term t = terms.get(i);
      t.appendLogicBloxString(builder);

      if(i != terms.size() - 1)
      {
	builder.append(", ");
      }
    }

    if(isFunction())
    {
      builder.append("] = ");
      _valueTerm.appendLogicBloxString(builder);
    }
    else
    {
      builder.append(')');
    }
  }
}