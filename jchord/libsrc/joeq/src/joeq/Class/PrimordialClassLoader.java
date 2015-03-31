// PrimordialClassLoader.java, created Mon Feb  5 23:23:20 2001 by joewhaley
// Copyright (C) 2001-3 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package joeq.Class;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import joeq.ClassLib.ClassLibInterface;
import joeq.Main.jq;
import joeq.UTF.Utf8;
import jwutil.collections.AppendIterator;
import jwutil.collections.Filter;
import jwutil.collections.FilterIterator;
import jwutil.collections.UnmodifiableIterator;
import jwutil.util.Assert;
import java.io.Serializable;

/**
 * PrimordialClassLoader
 *
 * @author  John Whaley <jwhaley@alum.mit.edu>
 * @version $Id: PrimordialClassLoader.java,v 1.46 2005/02/18 06:07:06 livshits Exp $
 */
public class PrimordialClassLoader extends ClassLoader implements jq_ClassFileConstants {
    
    public static boolean TRACE = false;

    private PrimordialClassLoader() {
		classpath = new Classpath();
        bs_desc2type = new HashMap();
        allTypes = new jq_Type[1024];
		numTypes = 0;
    }
    
    private void put_desc2type(Utf8 desc, jq_Type type) {
        Object result = bs_desc2type.put(desc, type);
        Assert._assert(result == null);
        if (numTypes == allTypes.length) {
            jq_Type[] a = new jq_Type[allTypes.length * 2];
            System.arraycopy(allTypes, 0, a, 0, numTypes);
            allTypes = a;
        }
        allTypes[numTypes++] = type;
    }
    
    private static void initPrimitiveTypes() {
        // trigger jq_Primitive clinit
        loader.getOrCreateBSType(jq_Primitive.BYTE.getDesc());
        loader.put_desc2type(jq_Array.BYTE_ARRAY.getDesc(), jq_Array.BYTE_ARRAY);
        loader.put_desc2type(jq_Array.CHAR_ARRAY.getDesc(), jq_Array.CHAR_ARRAY);
        loader.put_desc2type(jq_Array.DOUBLE_ARRAY.getDesc(), jq_Array.DOUBLE_ARRAY);
        loader.put_desc2type(jq_Array.FLOAT_ARRAY.getDesc(), jq_Array.FLOAT_ARRAY);
        loader.put_desc2type(jq_Array.INT_ARRAY.getDesc(), jq_Array.INT_ARRAY);
        loader.put_desc2type(jq_Array.LONG_ARRAY.getDesc(), jq_Array.LONG_ARRAY);
        loader.put_desc2type(jq_Array.SHORT_ARRAY.getDesc(), jq_Array.SHORT_ARRAY);
        loader.put_desc2type(jq_Array.BOOLEAN_ARRAY.getDesc(), jq_Array.BOOLEAN_ARRAY);
    }
    
    public static final PrimordialClassLoader loader;
    public static final jq_Class JavaLangObject;
    public static final jq_Class JavaLangClass;
    public static final jq_Class JavaLangString;
    public static final jq_Class JavaLangSystem;
    public static final jq_Class JavaLangThrowable;
    public static final jq_Array AddressArray;
	public static final jq_Class JavaLangException;
	public static final jq_Class JavaLangArrayStoreException;
	public static final jq_Class JavaLangError;
	public static final jq_Class JavaLangRuntimeException;
	public static final jq_Class JavaLangNullPointerException;
	public static final jq_Class JavaLangIndexOutOfBoundsException;
	public static final jq_Class JavaLangArrayIndexOutOfBoundsException;
	public static final jq_Class JavaLangNegativeArraySizeException;
	public static final jq_Class JavaLangArithmeticException;
	public static final jq_Class JavaLangIllegalMonitorStateException;
	public static final jq_Class JavaLangClassCastException;
	public static final jq_Class JavaLangClassLoader;
	public static final jq_Class JavaLangReflectField;
	public static final jq_Class JavaLangReflectMethod;
	public static final jq_Class JavaLangReflectConstructor;
	public static final jq_Class JavaLangThread;
	public static final jq_Class JavaLangRefFinalizer;
    static {
        loader = new PrimordialClassLoader();
        initPrimitiveTypes();
        JavaLangObject = (jq_Class)loader.getOrCreateBSType("Ljava/lang/Object;");
        JavaLangClass = (jq_Class)loader.getOrCreateBSType("Ljava/lang/Class;");
        JavaLangString = (jq_Class)loader.getOrCreateBSType("Ljava/lang/String;");
        JavaLangSystem = (jq_Class)loader.getOrCreateBSType("Ljava/lang/System;");
        JavaLangThrowable = (jq_Class)loader.getOrCreateBSType("Ljava/lang/Throwable;");
        AddressArray = (jq_Array)loader.getOrCreateBSType("[Ljoeq/Memory/Address;");
		JavaLangException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/Exception;");
		JavaLangArrayStoreException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/ArrayStoreException;");
		JavaLangError = (jq_Class)loader.getOrCreateBSType("Ljava/lang/Error;");
		JavaLangRuntimeException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/RuntimeException;");
		JavaLangNullPointerException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/NullPointerException;"); 
		JavaLangIndexOutOfBoundsException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/IndexOutOfBoundsException;"); 
		JavaLangArrayIndexOutOfBoundsException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/ArrayIndexOutOfBoundsException;"); 
		JavaLangNegativeArraySizeException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/NegativeArraySizeException;"); 
		JavaLangArithmeticException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/ArithmeticException;"); 
		JavaLangIllegalMonitorStateException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/IllegalMonitorStateException;"); 
		JavaLangClassCastException = (jq_Class)loader.getOrCreateBSType("Ljava/lang/ClassCastException;");
		JavaLangClassLoader = (jq_Class)loader.getOrCreateBSType("Ljava/lang/ClassLoader;");
		JavaLangReflectField = (jq_Class)loader.getOrCreateBSType("Ljava/lang/reflect/Field;");
		JavaLangReflectMethod = (jq_Class)loader.getOrCreateBSType("Ljava/lang/reflect/Method;");
		JavaLangReflectConstructor = (jq_Class)loader.getOrCreateBSType("Ljava/lang/reflect/Constructor;");
		JavaLangThread = (jq_Class)loader.getOrCreateBSType("Ljava/lang/Thread;");
		JavaLangRefFinalizer = (jq_Class)loader.getOrCreateBSType("Ljava/lang/ref/Finalizer;");
    }
    
    public static jq_Class getJavaLangObject() { return JavaLangObject; }
    public static jq_Class getJavaLangClass() { return JavaLangClass; }
    public static jq_Class getJavaLangString() { return JavaLangString; }
    public static jq_Class getJavaLangSystem() { return JavaLangSystem; }
    public static jq_Class getJavaLangThrowable() { return JavaLangThrowable; }
    public static jq_Array getAddressArray() { return AddressArray; }
    public static jq_Class getJavaLangException() { return JavaLangException; }
    public static jq_Class getJavaLangError() { return JavaLangError; }
    public static jq_Class getJavaLangRuntimeException() { return JavaLangRuntimeException; }
    public static jq_Class getJavaLangNullPointerException() { return JavaLangNullPointerException; }
    public static jq_Class getJavaLangIndexOutOfBoundsException() { return JavaLangIndexOutOfBoundsException; }
    public static jq_Class getJavaLangArrayIndexOutOfBoundsException() { return JavaLangArrayIndexOutOfBoundsException; }
    public static jq_Class getJavaLangArrayStoreException() { return JavaLangArrayStoreException; }
    public static jq_Class getJavaLangNegativeArraySizeException() { return JavaLangNegativeArraySizeException; }
    public static jq_Class getJavaLangArithmeticException() { return JavaLangArithmeticException; }
    public static jq_Class getJavaLangIllegalMonitorStateException() { return JavaLangIllegalMonitorStateException; }
    public static jq_Class getJavaLangClassCastException() { return JavaLangClassCastException; }
    public static jq_Class getJavaLangClassLoader() { return JavaLangClassLoader; }
    public static jq_Class getJavaLangReflectField() { return JavaLangReflectField; }
    public static jq_Class getJavaLangReflectMethod() { return JavaLangReflectMethod; }
    public static jq_Class getJavaLangReflectConstructor() { return JavaLangReflectConstructor; }
    public static jq_Class getJavaLangThread() { return JavaLangThread; }
    public static jq_Class getJavaLangRefFinalizer() { return JavaLangRefFinalizer; }
    private final Map/*<Utf8, jq_Type>*/ bs_desc2type;
    private jq_Type[] allTypes;
	private int numTypes;
    private final Classpath classpath;

	public Classpath getClasspath() { return classpath; }

    public jq_Type[] getAllTypes() { return allTypes; }
    
    public int getNumTypes() { return numTypes; }
    
    public final Set/*<jq_Class>*/ getClassesThatReference(jq_Member m) {
        HashSet s = new HashSet();
        for (int i = 0; i < numTypes; ++i) {
            jq_Type t = allTypes[i];
            if (t instanceof jq_Class) {
                jq_Class k = (jq_Class) t;
                if (k.doesConstantPoolContain(m))
                    s.add(k);
            }
        }
        return s;
    }
    
    public final jq_Class getOrCreateClass(String desc, DataInput in) {
        jq_Class t = (jq_Class)getOrCreateBSType(Utf8.get(desc));
        t.load(in);
        return t;
    }

    public final jq_Type getBSType(String desc) { return getBSType(Utf8.get(desc)); }
    public final jq_Type getBSType(Utf8 desc) {
        return (jq_Type)bs_desc2type.get(desc);
    }
    public final jq_Type getOrCreateBSType(String desc) { return getOrCreateBSType(Utf8.get(desc)); }
    public final jq_Type getOrCreateBSType(Utf8 desc) {
        if (jq.RunningNative)
            return ClassLibInterface.DEFAULT.getOrCreateType(this, desc);
        jq_Type t = (jq_Type)bs_desc2type.get(desc);
        if (t == null) {
            if (desc.isDescriptor(jq_ClassFileConstants.TC_CLASS)) {
                // as a side effect, the class type is registered.
                if (TRACE) System.out.println("Adding class type "+desc);
                t = jq_Class.newClass(this, desc);
            } else if (desc.isDescriptor(jq_ClassFileConstants.TC_ARRAY)) {
                if (TRACE) System.out.println("Adding array type "+desc);
                Utf8 elementDesc = desc.getArrayElementDescriptor();
                jq_Type elementType = getOrCreateBSType(elementDesc); // recursion
                // as a side effect, the array type is registered.
                t = jq_Array.newArray(desc, this, elementType);
            } else {
                // this code only gets executed at the very beginning, when creating primitive types.
                if (desc == Utf8.BYTE_DESC)
                    t = jq_Primitive.newPrimitive(desc, "byte", 1);
                else if (desc == Utf8.CHAR_DESC)
                    t = jq_Primitive.newPrimitive(desc, "char", 2);
                else if (desc == Utf8.DOUBLE_DESC)
                    t = jq_Primitive.newPrimitive(desc, "double", 8);
                else if (desc == Utf8.FLOAT_DESC)
                    t = jq_Primitive.newPrimitive(desc, "float", 4);
                else if (desc == Utf8.INT_DESC)
                    t = jq_Primitive.newPrimitive(desc, "int", 4);
                else if (desc == Utf8.LONG_DESC)
                    t = jq_Primitive.newPrimitive(desc, "long", 8);
                else if (desc == Utf8.SHORT_DESC)
                    t = jq_Primitive.newPrimitive(desc, "short", 2);
                else if (desc == Utf8.BOOLEAN_DESC)
                    t = jq_Primitive.newPrimitive(desc, "boolean", 1);
                else if (desc == Utf8.VOID_DESC)
                    t = jq_Primitive.newPrimitive(desc, "void", 0);
                /*
                else if (desc == jq_Array.BYTE_ARRAY.getDesc()) return jq_Array.BYTE_ARRAY;
                else if (desc == jq_Array.CHAR_ARRAY.getDesc()) return jq_Array.CHAR_ARRAY;
                else if (desc == jq_Array.DOUBLE_ARRAY.getDesc()) return jq_Array.DOUBLE_ARRAY;
                else if (desc == jq_Array.FLOAT_ARRAY.getDesc()) return jq_Array.FLOAT_ARRAY;
                else if (desc == jq_Array.INT_ARRAY.getDesc()) return jq_Array.INT_ARRAY;
                else if (desc == jq_Array.LONG_ARRAY.getDesc()) return jq_Array.LONG_ARRAY;
                else if (desc == jq_Array.SHORT_ARRAY.getDesc()) return jq_Array.SHORT_ARRAY;
                else if (desc == jq_Array.BOOLEAN_ARRAY.getDesc()) return jq_Array.BOOLEAN_ARRAY;
                 */
                else Assert.UNREACHABLE("bad descriptor! "+desc);
            }
            put_desc2type(desc, t);
        }
        return t;
    }
    
    /*
     * @param cName a string, not a descriptor.
     * @author Chrislain Razafimahefa <razafima@cui.unige.ch>
     */
    public final void replaceClass(String cName)
    {
        Utf8 oldDesc = Utf8.get("L"+cName.replace('.', '/')+";") ;
        jq_Type old = PrimordialClassLoader.getOrCreateType(this, oldDesc);
        Assert._assert(old != null);
        Assert._assert(oldDesc.isDescriptor(jq_ClassFileConstants.TC_CLASS));

        // now load 'new' with a fake name
        Utf8 newDesc = Utf8.get("LREPLACE"+cName.replace('.', '/')+";") ;
        jq_Class new_c = jq_Class.newClass(this, newDesc);
        put_desc2type(newDesc, new_c);

        // take inputstream on OLD class, but load in NEW class.
        DataInputStream in = null;
        try {
            in = classpath.getClassFileStream(oldDesc);
            if (in == null) throw new NoClassDefFoundError(jq_Class.className(oldDesc));
            new_c.load(in); // will generate the replacement
        } catch (IOException x) {
            x.printStackTrace(); // for debugging
            throw new ClassFormatError(x.toString());
        } finally {
            try { if (in != null) in.close(); } catch (IOException _) { }
        }
    }
    
    public void unloadBSType(jq_Type t) {
        bs_desc2type.remove(t.getDesc());
        for (int i = 0; ; ++i) {
            if (allTypes[i] == t) {
                numTypes--;
                System.arraycopy(allTypes, i+1, allTypes, i, numTypes - i);
                allTypes[numTypes] = null;
                break;
            }
        }
    }
    
    public static final jq_Type getOrCreateType(ClassLoader cl, Utf8 desc) {
        if (jq.RunningNative)
            return ClassLibInterface.DEFAULT.getOrCreateType(cl, desc);
        Assert._assert(cl == PrimordialClassLoader.loader);
        return PrimordialClassLoader.loader.getOrCreateBSType(desc);
    }
    
    public static final void unloadType(ClassLoader cl, jq_Type t) {
        if (jq.RunningNative) {
            ClassLibInterface.DEFAULT.unloadType(cl, t);
            return;
        }
        Assert._assert(cl == PrimordialClassLoader.loader);
        PrimordialClassLoader.loader.unloadBSType(t);
    }
}
