import soot.jimple.*;
import soot.Body;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.shimple.PhiExpr;
import soot.shimple.Shimple;

/**
 * Traverses Soot classes and invokes methods in FactWriter to
 * generate facts. The class FactGenerator is the main class
 * controlling what facts are generated.
 *
 * @author Martin Bravenboer
 * @license MIT
 */
public class FactGenerator
{
  private FactWriter _writer;
  private boolean _ssa;

  public FactGenerator(FactWriter writer, boolean ssa)
  {
    _writer = writer;
    _writer.writeStandardFacts();
    _ssa = ssa;
  }
  
  public void generate(SootClass c)
  {
    _writer.writeClassOrInterfaceType(c);

    // the isInterface condition prevents Object as superclass of interface
    if(c.hasSuperclass() && !c.isInterface()) 
    {
      _writer.writeDirectSuperclass(c, c.getSuperclass());
    }

    for(SootClass i : c.getInterfaces())
    {
      _writer.writeDirectSuperinterface(c, i);
    }

    for(SootField f : c.getFields())
    {
      generate(f);
    }

    for(SootMethod m : c.getMethods())
    {
      Session session = new Session();
      generate(m, session);
    }
  }

  public void generate(SootField f)
  {
    _writer.writeFieldSignature(f);

    int modifiers = f.getModifiers();
    if(Modifier.isAbstract(modifiers))
      _writer.writeFieldModifier(f, "abstract");
    if(Modifier.isFinal(modifiers))
      _writer.writeFieldModifier(f, "final");
    if(Modifier.isNative(modifiers))
      _writer.writeFieldModifier(f, "native");
    if(Modifier.isPrivate(modifiers))
      _writer.writeFieldModifier(f, "private");
    if(Modifier.isProtected(modifiers))
      _writer.writeFieldModifier(f, "protected");
    if(Modifier.isPublic(modifiers))
      _writer.writeFieldModifier(f, "public");
    if(Modifier.isStatic(modifiers))
      _writer.writeFieldModifier(f, "static");
    if(Modifier.isSynchronized(modifiers))
      _writer.writeFieldModifier(f, "synchronized");
    if(Modifier.isTransient(modifiers)) 
      _writer.writeFieldModifier(f, "transient");
    if(Modifier.isVolatile(modifiers)) 
      _writer.writeFieldModifier(f, "volatile");
    // TODO interface?
    // TODO strictfp?
    // TODO annotation?
    // TODO enum?
  }

  public void generate(SootMethod m, Session session)
  {
    if (m.isPhantom())
      return;

    _writer.writeMethodDeclaration(m);
    _writer.writeMethodSignature(m);

    int modifiers = m.getModifiers();
    if(Modifier.isAbstract(modifiers))
      _writer.writeMethodModifier(m, "abstract");
    if(Modifier.isFinal(modifiers))
      _writer.writeMethodModifier(m, "final");
    if(Modifier.isNative(modifiers))
      _writer.writeMethodModifier(m, "native");
    if(Modifier.isPrivate(modifiers))
      _writer.writeMethodModifier(m, "private");
    if(Modifier.isProtected(modifiers))
      _writer.writeMethodModifier(m, "protected");
    if(Modifier.isPublic(modifiers))
      _writer.writeMethodModifier(m, "public");
    if(Modifier.isStatic(modifiers))
      _writer.writeMethodModifier(m, "static");
    if(Modifier.isSynchronized(modifiers))
      _writer.writeMethodModifier(m, "synchronized");
    // TODO would be nice to have isVarArgs in Soot
    if(Modifier.isTransient(modifiers)) 
      _writer.writeMethodModifier(m, "varargs");
    // TODO would be nice to have isBridge in Soot
    if(Modifier.isVolatile(modifiers)) 
      _writer.writeMethodModifier(m, "bridge");
    // TODO interface?
    // TODO strictfp?
    // TODO annotation?
    // TODO enum?

    if(!m.isStatic())
    {
      _writer.writeThisVar(m);
    }

    if(m.isNative())
    {
      _writer.writeNativeReturnVar(m);
    }

    for(int i = 0 ; i < m.getParameterCount(); i++)
    {
      _writer.writeFormalParam(m, i);
    }

    for(SootClass clazz: m.getExceptions())
    {
      _writer.writeMethodDeclarationException(m, clazz);
    }

    if(!(m.isAbstract() || m.isNative()))
    {
      if(!m.hasActiveBody())
      {
	m.retrieveActiveBody();
      }

      Body b = m.getActiveBody();
      if(_ssa)
      {
	b = Shimple.v().newBody(b);
	m.setActiveBody(b);
      }
      
      generate(m, b, session);
      
      m.releaseActiveBody();
    }
  }

  public void generate(SootMethod m, Body b, Session session)
  {
    b.validate();
    session.numberUnits(b.getUnits().iterator());

    for(Local l : b.getLocals())
    {
      _writer.writeLocal(m, l);
    }

    IrrelevantStmtSwitch sw =  new IrrelevantStmtSwitch();
    for(Unit u : b.getUnits())
    {
      session.setCurrentUnit(u);

      Stmt stmt = (Stmt) u;

      stmt.apply(sw);

      if(sw.relevant)
      {
	if(stmt instanceof AssignStmt)
	{
	  generate(m, (AssignStmt) stmt, session);
	}
	else if(stmt instanceof IdentityStmt)
	{
	  generate(m, (IdentityStmt) stmt);
	}
	else if(stmt instanceof InvokeStmt)
	{
	  _writer.writeInvoke(m, ((InvokeStmt) stmt).getInvokeExpr(), session);
	}
	else if(stmt instanceof ReturnStmt)
	{
	  generate(m, (ReturnStmt) stmt, session);
	}
	else if(stmt instanceof ThrowStmt)
	{
	  generate(m, (ThrowStmt) stmt, session);
	}
	else
	{
	  throw new RuntimeException("Cannot handle statement: " + stmt);
	}
      }
    }

    Trap previous = null;
    for(Trap t : b.getTraps())
    {
      _writer.writeExceptionHandler(m, t, session);
      if(previous != null)
      {
	_writer.writeExceptionHandlerPrevious(m, t, previous, session);
      }

      previous = t;
    }
  }

  /**
   * Assignment statement
   */
  public void generate(SootMethod inMethod, AssignStmt stmt, Session session)
  {
    Value left = stmt.getLeftOp();

    if(left instanceof Local)
    {
      generateLeftLocal(inMethod, stmt, session);
    }
    else
    {
      generateLeftNonLocal(inMethod, stmt, session);
    }
  }

  public void generateLeftLocal(SootMethod inMethod, AssignStmt stmt, Session session)
  {
    Local left = (Local) stmt.getLeftOp();
    Value right = stmt.getRightOp();

    if(right instanceof Local)
    {
      _writer.writeAssignLocal(inMethod, left, (Local) right);
    }
    else if(right instanceof InvokeExpr)
    {
      _writer.writeAssignInvoke(inMethod, left, (InvokeExpr) right, session);
    }
    else if(right instanceof NewExpr)
    {
      _writer.writeAssignHeapAllocation(inMethod, left, (NewExpr) right, session);
    }
    else if(right instanceof NewArrayExpr)
    {
      _writer.writeAssignHeapAllocation(inMethod, left, (NewArrayExpr) right, session);
    }
    else if(right instanceof NewMultiArrayExpr)
    {
      _writer.writeAssignNewMultiArrayExpr(inMethod, left, (NewMultiArrayExpr) right, session);
    }
    else if(right instanceof StringConstant)
    {
      _writer.writeAssignStringConstant(inMethod, left, (StringConstant) right);
    }
    else if(right instanceof ClassConstant)
    {
      _writer.writeAssignClassConstant(inMethod, left, (ClassConstant) right);
    }
    else if(right instanceof InstanceFieldRef)
    {
      InstanceFieldRef ref = (InstanceFieldRef) right;
      _writer.writeLoadInstanceField(inMethod, ref.getField(), (Local) ref.getBase(), left);
    }
    else if(right instanceof StaticFieldRef)
    {
      StaticFieldRef ref = (StaticFieldRef) right;
      
      if(left.getType() instanceof PrimType)
      {
	// These load operations are not relevant for points-to
	// analysis, but they are for class initialization, so they
	// affect the call graph.
	_writer.writeLoadPrimStaticField(inMethod, ref.getField());
      }
      else
      {
	_writer.writeLoadStaticField(inMethod, ref.getField(), left);
      }
    }
    else if(right instanceof ArrayRef)
    {
      ArrayRef ref = (ArrayRef) right;
      Local base = (Local) ref.getBase();
      Value index = ref.getIndex();
      
      if(index instanceof Local || index instanceof IntConstant)
      {
	_writer.writeLoadArrayIndex(inMethod, base, left);
      }
      else
      {
	throw new RuntimeException("Cannot handle assignment: " + stmt + " (index: " + index.getClass() + ")");
      }
    }
    else if(right instanceof CastExpr)
    {
      CastExpr cast = (CastExpr) right;
      Value op = cast.getOp();

      if(op instanceof Local)
      {
	_writer.writeAssignCast(inMethod, left, (Local) op, cast.getCastType());
      }
      else if(
           op instanceof IntConstant
        || op instanceof LongConstant
        || op instanceof FloatConstant
        || op instanceof DoubleConstant
        || op instanceof NullConstant
      )
      {
	// ignore, not relevant for pointer analysis
      }
      else
      {
	throw new RuntimeException("Cannot handle assignment: " + stmt + " (op: " + op.getClass() + ")");
      }
    }
    else if(right instanceof PhiExpr)
    {
      for(Value alternative : ((PhiExpr) right).getValues())
      {
	_writer.writeAssignLocal(inMethod, left, (Local) alternative);
      }
    }
    else if(
      right instanceof IntConstant
      || right instanceof LongConstant
      || right instanceof FloatConstant
      || right instanceof DoubleConstant
      || right instanceof NullConstant
      || right instanceof BinopExpr
      || right instanceof NegExpr
      || right instanceof LengthExpr
      || right instanceof InstanceOfExpr)
    {
      // ignore, not relevant for pointer analysis
    }
    else
    {
      throw new RuntimeException("Cannot handle assignment: " + stmt + " (right: " + right.getClass() + ")");
    }
  }

  public void generateLeftNonLocal(SootMethod inMethod, AssignStmt stmt, Session session)
  {
    Value left = stmt.getLeftOp();
    Value right = stmt.getRightOp();

    // first make sure we have local variable for the right-hand-side.
    Local rightLocal = null;
    
    if(right instanceof Local)
    {
      rightLocal = (Local) right;
    }
    else if(right instanceof StringConstant)
    {
      rightLocal = _writer.writeStringConstantExpression(inMethod, (StringConstant) right, session);
    }
    else if(right instanceof ClassConstant)
    {
      rightLocal = _writer.writeClassConstantExpression(inMethod, (ClassConstant) right, session);
    }
    
    // arrays
    if(left instanceof ArrayRef && rightLocal != null)
    {
      ArrayRef ref = (ArrayRef) left;
      Local base = (Local) ref.getBase();
      _writer.writeStoreArrayIndex(inMethod, base, rightLocal);
    }
    else if(left instanceof ArrayRef &&
      (  right instanceof IntConstant
        || right instanceof LongConstant
        || right instanceof FloatConstant
        || right instanceof DoubleConstant
        || right instanceof NullConstant))
    {
      // skip, not relevant for pointer analysis
    }
    // instance fields
    else if(left instanceof InstanceFieldRef && rightLocal != null)
    {
      InstanceFieldRef ref = (InstanceFieldRef) left;
	_writer.writeStoreInstanceField(inMethod, ref.getField(), (Local) ref.getBase(), rightLocal);
    }
    else if(left instanceof InstanceFieldRef &&
      (  right instanceof IntConstant
        || right instanceof LongConstant
        || right instanceof FloatConstant
        || right instanceof DoubleConstant
        || right instanceof NullConstant))
    {
      // skip, not relevant for pointer analysis
    }
    // static fields
    else if(left instanceof StaticFieldRef  && (right.getType() instanceof PrimType || right instanceof NullConstant))
    {
      // These store operations are not relevant for points-to
      // analysis, but they are for class initialization, so they
      // affect the call graph.
      StaticFieldRef ref = (StaticFieldRef) left;
      _writer.writeStorePrimStaticField(inMethod, ref.getField());
      
      // TODO: the NullConstant is a bit hacky. It's the right behaviour, but it's a bit ugly to call this a Prim.
    }
    else if(left instanceof StaticFieldRef && rightLocal != null)
    {
      StaticFieldRef ref = (StaticFieldRef) left;
      _writer.writeStoreStaticField(inMethod, ref.getField(), rightLocal);
    }
    else
    {
      throw new RuntimeException("Cannot handle assignment: " + stmt
	+ " (right: " + right.getClass() + ")");
      }
  }

  public void generate(SootMethod inMethod, IdentityStmt stmt)
  {
    Value left = stmt.getLeftOp();
    Value right = stmt.getRightOp();

    if(right instanceof CaughtExceptionRef) {
      /* Handled by ExceptionHandler generation (ExceptionHandler:FormalParam).

         TODO Would be good to check more carefully that a caught
         exception does not occur anywhere else.
       */
      return;
    }
    else if(left instanceof Local && right instanceof ThisRef)
    {
      _writer.writeAssignLocal(inMethod, (Local) left, (ThisRef) right);
    }
    else if(left instanceof Local && right instanceof ParameterRef)
    {
      _writer.writeAssignLocal(inMethod, (Local) left, (ParameterRef) right);
    }
    else
    {
      throw new RuntimeException("Cannot handle identity statement: " + stmt);
    }
  }

  /**
   * Return statement
   */
  public void generate(SootMethod inMethod, ReturnStmt stmt, Session session)
  {
    Value v = stmt.getOp();
    
    if(v instanceof Local)
    {
      _writer.writeReturnVar(inMethod, (Local) v);
    }
    else if(v instanceof StringConstant)
    {
      Local tmp = _writer.writeStringConstantExpression(inMethod, (StringConstant) v, session);
      _writer.writeReturnVar(inMethod, tmp);      
    }
    else if(v instanceof ClassConstant)
    {
      Local tmp = _writer.writeClassConstantExpression(inMethod, (ClassConstant) v, session);
      _writer.writeReturnVar(inMethod, tmp);
    }
    else if(
         v instanceof IntConstant
      || v instanceof LongConstant
      || v instanceof FloatConstant
      || v instanceof DoubleConstant
      || v instanceof NullConstant)
    {
      // skip, not relevant for pointer analysis
    }
    else
    {
      throw new RuntimeException("Unhandled return statement: " + stmt);
    }
  }

  public void generate(SootMethod inMethod, ThrowStmt stmt, Session session)
  {
    Value v = stmt.getOp();
    
    if(v instanceof Local)
    {
      _writer.writeThrow(inMethod, (Local) v, session);
    }
    else if(v instanceof NullConstant)
    {
      // skip, not relevant for pointer analysis.
    }
    else
    {
      throw new RuntimeException("Unhandled throw statement: " + stmt);
    }
  }
}
