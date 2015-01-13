import soot.Local;
import soot.RefLikeType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;

import soot.Type;
import soot.PrimType;
import soot.RefType;
import soot.NullType;
import soot.ArrayType;


import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.jimple.internal.JimpleLocal;

/**
 * FactWriter determines the format of a fact and adds it to a
 * database. No traversal code here (see FactGenerator for that).
 *
 * @author Martin Bravenboer
 * @license MIT
 */
public class FactWriter
{

  private Database _db;
  private Representation _rep;

  public FactWriter(Database db)
  {
    _db = db;
    _rep = new Representation();
  }

  public void writeStandardFacts()
  {
    _db.addEntity("PrimitiveType", "boolean");
    _db.addEntity("PrimitiveType", "byte");
    _db.addEntity("PrimitiveType", "char");
    _db.addEntity("PrimitiveType", "short");
    _db.addEntity("PrimitiveType", "int");
    _db.addEntity("PrimitiveType", "long");
    _db.addEntity("PrimitiveType", "float");
    _db.addEntity("PrimitiveType", "double");
    _db.addEntity("NullType", "null_type");
  }

  public void writeClassOrInterfaceType(SootClass c)
  {
    String rep = _rep.type(c);
    Column col;

    if(c.isInterface())
    {
      col = _db.addEntity("InterfaceType", rep);
    }
    else
    {
      col = _db.addEntity("ClassType", rep);
    }

    Column constant = _db.addEntity("ClassConstant", _rep.classconstant(c));
    _db.add("ReifiedClass", col, constant);
    _db.add("HeapAllocation-Type", constant, _db.addEntity("ClassType", "java.lang.Class"));
  }

  public void writeDirectSuperclass(SootClass sub, SootClass sup)
  {
    _db.add("DirectSuperclass",
      writeType(sub),
      writeType(sup));
  }

  public void writeDirectSuperinterface(SootClass clazz, SootClass iface)
  {
    _db.add("DirectSuperinterface",
      writeType(clazz),
      writeType(iface));
  }

  public Column writeType(SootClass c)
  {
    String result = _rep.type(c);

    // The type itself is already taken care of by writing the
    // SootClass declaration, so we don't actually write the type
    // here, and just return the string.

    return _db.asEntity(result);
  }

  public Column writeType(Type t)
  {
    String result = _rep.type(t);
    Column c;

    if(t instanceof ArrayType)
    {
      c = _db.addEntity("ArrayType", result);
      Type componentType = ((ArrayType) t).getElementType();
      _db.add("ComponentType", c, writeType(componentType));
    }
    else if(t instanceof PrimType || t instanceof NullType)
    {
      // taken care of by the standard facts
    	c = _db.asEntity(result);
    }
    else if(t instanceof RefType)
    {
      // taken care of by SootClass declaration.
    	c = _db.asEntity(result);
    }
    else
    {
      throw new RuntimeException("Don't know what to do with type " + t);
    }

    return c;
  }

  public void writeAssignLocal(SootMethod m, Local to, Local from)
  {
	  if(to.getType() instanceof RefLikeType)
	  {
		  _db.add("AssignLocal",
				  _db.addEntity("VarRef", _rep.local(m, from)),
				  _db.addEntity("VarRef", _rep.local(m, to)),
				  _db.addEntity("MethodSignatureRef", _rep.method(m)));
	  }
  }

  public void writeAssignLocal(SootMethod m, Local to, ThisRef ref)
  {
	  _db.add("AssignLocal",
			  _db.addEntity("VarRef", _rep.thisVar(m)),
			  _db.addEntity("VarRef", _rep.local(m, to)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeAssignLocal(SootMethod m, Local to, ParameterRef ref)
  {
    if(to.getType() instanceof RefLikeType)
    {
    	_db.add("AssignLocal",
    			_db.addEntity("VarRef", _rep.param(m, ref.getIndex())),
    			_db.addEntity("VarRef", _rep.local(m, to)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
  }

  public void writeAssignInvoke(SootMethod inMethod, Local to, InvokeExpr expr, Session session)
  {
    String rep = writeInvokeHelper(inMethod, expr, session);

    if(to.getType() instanceof RefLikeType)
    {
      _db.add("AssignReturnValue",
	_db.addEntity("MethodInvocationRef", rep),
	_db.addEntity("VarRef", _rep.local(inMethod, to)));
    }
  }

  public void writeAssignHeapAllocation(SootMethod m, Local l, AnyNewExpr expr, Session session)
  {
    String rep = _rep.heapAlloc(m, expr, session);

    _db.add("AssignHeapAllocation",
    		_db.addEntity("NormalHeapAllocationRef", rep),
    		_db.addEntity("VarRef", _rep.local(m, l)),
    		_db.addEntity("MethodSignatureRef", _rep.method(m)));
    
    
    _db.addEntity("NormalHeapAllocationRef", rep);

    _db.add("HeapAllocation-Type",
    		_db.asEntity(rep),
    		writeType(expr.getType()));
  }

  private Type getComponentType(ArrayType type)
  {
    // Soot calls the component type of an array type the "element
    // type", which is rather confusing, since in an array type
    // A[][][], the JVM Spec defines A to be the element type, and
    // A[][] is the component type.
    return type.getElementType();
  }

  /**
   * NewMultiArray is slightly complicated because an array needs to
   * be allocated separately for every dimension of the array.
   */
  public void writeAssignNewMultiArrayExpr(SootMethod m, Local l, NewMultiArrayExpr expr, Session session)
  {
    Type type = (ArrayType) expr.getType();

    // local variable to assign the current array allocation to.
    String assignTo = _rep.local(m, l);

    while(type instanceof ArrayType)
    {
      ArrayType arrayType = (ArrayType) type;
      String rep = _rep.heapMultiArrayAlloc(m, expr, arrayType, session);

      // assign the the current allocation to assignTo
      _db.add("AssignHeapAllocation", 
    		  _db.addEntity("NormalHeapAllocationRef", rep), 
    		  _db.addEntity("VarRef", assignTo),
    		  _db.addEntity("MethodSignatureRef", _rep.method(m)) 
    		);

      _db.addEntity("NormalHeapAllocationRef", rep);

      
      // write the type of the current allocation
      _db.add("HeapAllocation-Type",
	_db.asEntity(rep),
	writeType(arrayType));

      type = getComponentType(arrayType);
      if(type instanceof ArrayType)
      {
	// there is a subarray to allocate and assign to the array.

        String base = assignTo;
	assignTo = _rep.newLocalIntermediate(m, l, session);

	// assign the subarray to the array of dim+1
	_db.add("StoreArrayIndex",
			_db.addEntity("VarRef", assignTo),
			_db.addEntity("VarRef", base),
			_db.addEntity("MethodSignatureRef", _rep.method(m)));

	// the assignTo is a fresh variable, so we need to write a
	// fact for its type and the declaring method.
	_db.add("Var-Type",
	  _db.asEntity(assignTo),
	  writeType(type));

	_db.add("Var-DeclaringMethod",
	  _db.asEntity(assignTo),
	  _db.asEntity(_rep.method(m)));
      }
    }
  }

  public void writeAssignStringConstant(SootMethod m, Local l, StringConstant s)
  {
    String rep = _rep.stringconstant(m, s);

    _db.add("AssignHeapAllocation",
    		_db.addEntity("StringConstant", rep),
    		_db.addEntity("VarRef", _rep.local(m, l)),
    		_db.addEntity("MethodSignatureRef", _rep.method(m)));

    _db.add("HeapAllocation-Type",
      _db.asEntity(rep),
      writeType(s.getType()));
  }

  public void writeAssignClassConstant(SootMethod m, Local l, ClassConstant constant)
  {
    // System.err.println("class constant " + constant + " in " + m);
    String s = constant.getValue().replace('/', '.');

    String rep;

    /* There is some weirdness in class constants: normal Java class
       types seem to have been translated to a syntax with the initial
       L, but arrays are still represented as [, for example [C for
       char[] */
    if(s.charAt(0) == '[')
    {
      // array type
      Type t = soot.coffi.Util.v().jimpleTypeOfFieldDescriptor(s);

      rep = _rep.classconstant(t);
      _db.add("ReifiedClass", _db.asEntity(_rep.type(t)), _db.asEntity(rep));

      // TODO only classes have their heap allocation type written already
    }
    else
    {
      SootClass c = soot.Scene.v().getSootClass(s);
      if(c == null)
      {
	throw new RuntimeException("Unexpected class constant: " + constant);
      }

      rep =  _rep.classconstant(c);
    }

    _db.add("AssignHeapAllocation",
    		_db.asEntity(rep),
    		_db.addEntity("VarRef", _rep.local(m, l)),
    		_db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeAssignCast(SootMethod m, Local to, Local from, Type t)
  {
    if(to.getType() instanceof RefLikeType && from.getType() instanceof RefLikeType)
    {
    	_db.add("AssignCast",
    			writeType(t),
    			_db.addEntity("VarRef", _rep.local(m, from)),
    			_db.addEntity("VarRef", _rep.local(m, to)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
    else
    {
      if(to.getType() instanceof RefLikeType || from.getType() instanceof RefLikeType)
      {
	throw new RuntimeException("error: didn't expect cast involving one reference type");
      }
      else
      {
	// cast from prim to prim, not relevant for pointer analysis.
      }
    }
  }

  public void writeStoreInstanceField(SootMethod m, SootField f, Local base, Local from)
  {
    if(f.getType() instanceof RefLikeType)
    {
    	_db.add("StoreInstanceField",
    			_db.addEntity("VarRef", _rep.local(m, from)),
    			_db.addEntity("VarRef", _rep.local(m, base)),
    			_db.addEntity("FieldSignatureRef", _rep.signature(f)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
  }

  public void writeLoadInstanceField(SootMethod m, SootField f, Local base, Local to)
  {
    if(to.getType() instanceof RefLikeType)
    {
    	_db.add("LoadInstanceField",
    			_db.addEntity("VarRef", _rep.local(m, base)),
    			_db.addEntity("FieldSignatureRef", _rep.signature(f)),
    			_db.addEntity("VarRef", _rep.local(m, to)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
  }

  public void writeStoreStaticField(SootMethod m, SootField f, Local from)
  {
	  _db.add("StoreStaticField",
			  _db.addEntity("VarRef", _rep.local(m, from)),
			  _db.addEntity("FieldSignatureRef", _rep.signature(f)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeStorePrimStaticField(SootMethod m, SootField f)
  {
	  _db.add("StorePrimStaticField",
			  _db.addEntity("FieldSignatureRef", _rep.signature(f)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeLoadStaticField(SootMethod m, SootField f, Local to)
  {
	  _db.add("LoadStaticField",
			  _db.addEntity("FieldSignatureRef", _rep.signature(f)),
			  _db.addEntity("VarRef", _rep.local(m, to)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeLoadPrimStaticField(SootMethod m, SootField f)
  {
	  _db.add("LoadPrimStaticField",
			  _db.addEntity("FieldSignatureRef", _rep.signature(f)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeLoadArrayIndex(SootMethod m, Local base, Local to)
  {
    if(to.getType() instanceof RefLikeType)
    {
    	_db.add("LoadArrayIndex",
    			_db.addEntity("VarRef", _rep.local(m, base)),
    			_db.addEntity("VarRef", _rep.local(m, to)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
  }

  public void writeStoreArrayIndex(SootMethod m, Local base, Local from)
  {
    if(from.getType() instanceof RefLikeType)
    {
    	_db.add("StoreArrayIndex",
    			_db.addEntity("VarRef", _rep.local(m, from)),
    			_db.addEntity("VarRef", _rep.local(m, base)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
  }

  public void writeApplicationClass(SootClass application)
  {
    _db.add("ApplicationClass",
      writeType(application));
  }

  public void writeFieldSignature(SootField f)
  {
	  _db.add("FieldSignature",
			  _db.addEntity("FieldSignatureRef", _rep.signature(f)),
			  writeType(f.getDeclaringClass()),
			  _db.addEntity("SimpleNameRef", _rep.simpleName(f)),
			  writeType(f.getType()));
  }

  public void writeFieldModifier(SootField f, String modifier)
  {
    _db.add("FieldModifier",
      _db.addEntity("ModifierRef", _rep.modifier(modifier)),
      _db.addEntity("FieldSignatureRef", _rep.signature(f)));
  }

  public void writeMethodDeclaration(SootMethod m)
  {
	  _db.add("MethodDeclaration",
			  _db.addEntity("MethodSignatureRef", _rep.signature(m)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeMethodSignature(SootMethod m)
  {

	  _db.add("MethodSignature-SimpleName",
			  _db.addEntity("MethodSignatureRef", _rep.signature(m)),
			  _db.addEntity("SimpleNameRef", _rep.simpleName(m)));
	  _db.add("MethodSignature-Descriptor",
			  _db.addEntity("MethodSignatureRef", _rep.signature(m)),
			  _db.addEntity("MethodDescriptorRef", _rep.descriptor(m)));
	  _db.add("MethodSignature-Type",
			  _db.addEntity("MethodSignatureRef", _rep.signature(m)),
			  writeType(m.getDeclaringClass()));
  }

  public void writeMethodModifier(SootMethod m, String modifier)
  {
	  _db.add("MethodModifier",
			  _db.addEntity("ModifierRef", _rep.modifier(modifier)),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeReturnVar(SootMethod m, Local l)
  {
    if(l.getType() instanceof RefLikeType)
    {
    	_db.add("ReturnVar",
    			_db.addEntity("VarRef", _rep.local(m, l)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
      
    	_db.add("Var-Type",
    			_db.addEntity("VarRef", _rep.local(m, l)),
    			writeType(l.getType()));
    }
  }

  public void writeNativeReturnVar(SootMethod m)
  {
    if(m.getReturnType() instanceof RefLikeType)
    {
    	_db.add("ReturnVar",
    			_db.addEntity("VarRef", _rep.nativeReturnVar(m)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)));
      
    	_db.add("Var-Type",
    			_db.asEntity(_rep.nativeReturnVar(m)),
    			writeType(m.getReturnType()));

    	_db.add("Var-DeclaringMethod",
    			_db.asEntity(_rep.nativeReturnVar(m)),
    			_db.asEntity(_rep.method(m)));
    }
  }

  /**
   * Throw statement
   */
  public void writeThrow(SootMethod m, Local l, Session session)
  {
    String rep = _rep.throwLocal(m, l, session);

    _db.add("Throw",
    		_db.addEntity("ThrowRef", rep),
    		_db.addEntity("VarRef", _rep.local(m, l)));
    _db.add("Throw-Method",
    		_db.addEntity("ThrowRef", rep),
    		_db.addEntity("MethodSignatureRef", _rep.method(m)));

    int index = session.getUnitNumber(session.getCurrentUnit());

    _db.add("Instruction-Index",
      _db.asEntity(rep),
      _db.asIntColumn(String.valueOf(index)));
  }

  public void writeExceptionHandlerPrevious(SootMethod m, Trap current, Trap previous, Session session)
  {
    _db.add("ExceptionHandler-Previous",
      _db.addEntity("ExceptionHandlerRef", _rep.handler(m, current, session)),
      _db.addEntity("ExceptionHandlerRef", _rep.handler(m, previous, session)));
  }

  public void writeExceptionHandler(SootMethod m, Trap handler, Session session)
  {
    SootClass exc = handler.getException();

    Local caught;
    {
      Unit handlerUnit = handler.getHandlerUnit();
      IdentityStmt stmt = (IdentityStmt) handlerUnit;
      Value left = stmt.getLeftOp();
      Value right = stmt.getRightOp();
      

      if(right instanceof CaughtExceptionRef && left instanceof Local)
      {
	caught = (Local) left;
      }
      else
      {
	throw new RuntimeException("Unexpected start of exception handler: " + handlerUnit);
      }
    }

    /* simple fact for Paddle compatibility mode */
    _db.add("SimpleExceptionHandler",
    		_db.asEntity(_rep.type(exc)),
    		_db.addEntity("VarRef",_rep.local(m, caught)),
    		_db.addEntity("MethodSignatureRef", _rep.method(m)));

    String rep = _rep.handler(m, handler, session);
    _db.addEntity("ExceptionHandlerRef", rep);

    _db.add("ExceptionHandler-Method",
    		_db.asEntity(rep),
    		_db.asEntity(_rep.method(m)));

    _db.add("ExceptionHandler-Type",
    		_db.asEntity(rep),
    		_db.asEntity(_rep.type(exc)));

    _db.add("ExceptionHandler-FormalParam",
    		_db.asEntity(rep),
    		_db.asEntity(_rep.local(m, caught)));

    _db.add("ExceptionHandler-Begin",
    		_db.asEntity(rep),
    		_db.asIntColumn(String.valueOf(session.getUnitNumber(handler.getBeginUnit()))));

    _db.add("ExceptionHandler-End",
    		_db.asEntity(rep),
    		_db.asIntColumn(String.valueOf(session.getUnitNumber(handler.getEndUnit()))));
  }

  public void writeThisVar(SootMethod m)
  {
	  _db.add("ThisVar",
			  _db.addEntity("MethodSignatureRef", _rep.method(m)),
			  _db.addEntity("VarRef", _rep.thisVar(m)));

	  _db.add("Var-Type",
			  _db.asEntity(_rep.thisVar(m)),
			  writeType(m.getDeclaringClass()));    

	  _db.add("Var-DeclaringMethod",
			  _db.asEntity(_rep.thisVar(m)),
			  _db.asEntity(_rep.method(m)));
  }

  public void writeMethodDeclarationException(SootMethod m, SootClass exception)
  {
	  _db.add("MethodDeclaration-Exception",
			  writeType(exception),
			  _db.addEntity("MethodSignatureRef", _rep.method(m)));
  }

  public void writeFormalParam(SootMethod m, int i)
  {
    if(m.getParameterType(i) instanceof RefLikeType)
    {
    	_db.add("FormalParam",
    			_db.asIntColumn(_rep.index(i)),
    			_db.addEntity("MethodSignatureRef", _rep.method(m)),
    			_db.addEntity("VarRef", _rep.param(m, i)));

    	_db.add("Var-Type",
    			_db.asEntity(_rep.param(m, i)),
    			writeType(m.getParameterType(i)));

    	_db.add("Var-DeclaringMethod",
    			_db.asEntity(_rep.param(m, i)),
    			_db.asEntity(_rep.method(m)));
    }
  }

  public void writeLocal(SootMethod m, Local l)
  {
    if(l.getType() instanceof RefLikeType)
    {
    	_db.add("Var-Type",
    			_db.addEntity("VarRef", _rep.local(m, l)),
    			writeType(l.getType()));

      _db.add("Var-DeclaringMethod",
    		  _db.asEntity(_rep.local(m, l)),
    		  _db.addEntity("MethodSignatureRef", _rep.method(m)));
    }
  }

  public Local writeStringConstantExpression(SootMethod inMethod, StringConstant constant, Session session)
  {
    // introduce a new temporary variable
    String basename = "$stringconstant";
    String varname = basename + session.nextNumber(basename);
    Local l = new JimpleLocal(varname, RefType.v("java.lang.String"));
    writeLocal(inMethod, l);
    writeAssignStringConstant(inMethod, l, constant);
    return l;
  }

  public Local writeClassConstantExpression(SootMethod inMethod, ClassConstant constant, Session session)
  {
    // introduce a new temporary variable
    String basename = "$classconstant";
    String varname = basename + session.nextNumber(basename);
    Local l = new JimpleLocal(varname, RefType.v("java.lang.Class"));
    writeLocal(inMethod, l);
    writeAssignClassConstant(inMethod, l, constant);
    return l;
  }

  private void writeActualParams(SootMethod inMethod, InvokeExpr expr, String invokeExprRepr, Session session)
  {
    for(int i = 0; i < expr.getArgCount(); i++)
    {
      Value v = expr.getArg(i);

      if(v instanceof StringConstant)
      {
	v = writeStringConstantExpression(inMethod, (StringConstant) v, session);
      }
      else if(v instanceof ClassConstant)
      {
	v = writeClassConstantExpression(inMethod, (ClassConstant) v, session);
      }

      if(v instanceof Local)
      {
	Local l = (Local) v;
	if(l.getType() instanceof RefLikeType)
	{
		_db.add("ActualParam",
				_db.asIntColumn(_rep.index(i)),
				_db.addEntity("MethodInvocationRef", invokeExprRepr),
				_db.addEntity("VarRef", _rep.local(inMethod, l)));
	}
      }
      else if(  v instanceof IntConstant
	  || v instanceof LongConstant
	  || v instanceof FloatConstant
	  || v instanceof DoubleConstant
          || v instanceof NullConstant)
      {
	continue;
      }
      else 
      {
	throw new RuntimeException("Unknown actual parameter: " + v + " " + v.getClass());
      }
    }
  }

  public void writeInvoke(SootMethod inMethod, InvokeExpr expr, Session session)
  {
    writeInvokeHelper(inMethod, expr, session);
  }

  private String writeInvokeHelper(SootMethod inMethod, InvokeExpr expr, Session session)
  {
    String rep = _rep.invoke(inMethod, expr, session);
    writeActualParams(inMethod, expr, rep, session);
    
    _db.addEntity("MethodSignatureRef", _rep.method(inMethod));
    _db.addEntity("MethodInvocationRef", rep);
    _db.addEntity("MethodSignatureRef", _rep.signature(expr.getMethod()));
    
    if(expr instanceof StaticInvokeExpr)
    {
    	_db.add("StaticMethodInvocation",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.signature(expr.getMethod())),
    			_db.asEntity(_rep.method(inMethod)));
    	_db.add("StaticMethodInvocation-In",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.method(inMethod)));
    	_db.add("StaticMethodInvocation-Signature",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.signature(expr.getMethod())));
    }
    else if(expr instanceof VirtualInvokeExpr || expr instanceof InterfaceInvokeExpr)
    {
    	_db.add("VirtualMethodInvocation-Base",
    			_db.asEntity(rep),
    			_db.addEntity("VarRef", _rep.local(inMethod, (Local) ((InstanceInvokeExpr) expr).getBase())));
    	_db.add("VirtualMethodInvocation-Signature",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.signature(expr.getMethod())));
    	_db.add("VirtualMethodInvocation-In",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.method(inMethod)));
    	_db.add("VirtualMethodInvocation",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.signature(expr.getMethod())),
    			_db.asEntity(_rep.method(inMethod)));
    }
    else if(expr instanceof SpecialInvokeExpr)
    {
    	_db.add("SpecialMethodInvocation-Base",
    			_db.asEntity(rep),
    			_db.addEntity("VarRef", _rep.local(inMethod, (Local) ((InstanceInvokeExpr) expr).getBase())));
    	_db.add("SpecialMethodInvocation-Signature",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.signature(expr.getMethod())));
    	_db.add("SpecialMethodInvocation-In",
    			_db.asEntity(rep),
    			_db.asEntity(_rep.method(inMethod)));
    }
    else
    {
      throw new RuntimeException("Cannot handle invoke expr: " + expr);
    }

    _db.add("Instruction-Index",
    		_db.asEntity(rep),
    		_db.asIntColumn(String.valueOf(session.getUnitNumber(session.getCurrentUnit()))));

    return rep;
  }
}
