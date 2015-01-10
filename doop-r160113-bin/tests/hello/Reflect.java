import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Reflect
{
  public static void main(String[] ps) {
    Test1.run();
    Test2.run();
    Test3.run();
    Test4.run();
    Test5.run();
    Test6.run();
    Test7.run();
    Test8.run();
    Test9.run();
    Test10.run();
    Test11.run();
    Test12.run();
    Test13.run();
    Test14.run();
    Test15.run();
    Test16.run();
    Test17.run();
    Test18.run();
    Test19.run();
    Test20.run();
    Test21.run();
    Test22.run();
    Test23.run();
    Test24.run();
    Test25.run();
    Test26.run();
    Test27.run();
    Test28.run();
    Test29.run();
    Test30.run();
    Test31.run();
    Test32.run();
    Test33.run();
    Test34.run();
    Test35.run();
    Test36.run();
    Test37.run();
  }
}

/**
 * Object.getClass
 */
class Test1 {
  static Object o;

  public static void run() {
    Integer i = new Integer(1);
    o = i.getClass();
  }
}

/**
 * Class.newInstance on Class object retrieved via Object.getClass
 */
class Test2 {
  static Object o;

  public static void run() {
    try {
      String s = new String();
      o = s.getClass().newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Class.newInstance on Class object of class constant.
 */
class Test3 {
  static Object o;

  public static void run() {
    try {
      Class c = String.class;
      o = c.newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Class.newInstance on Class object of class constant returned from method.
 */
class Test4 {
  static Object o;

  public static void run() {
    try {
      o = get().newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public static Class get() {
    return String.class;
  }
}

/**
 * Class.newInstance on Class object of class constant assigned to static field
 */
class Test5 {
  static Class c;
  static Object o;

  public static void run() {
    try {
      set();
      o = c.newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public static void set() {
    c = String.class;
  }
}

/**
 * Class.newInstance on Class object of class constant passed as actual parameter
 */
class Test6 {
  static Object o;

  public static void run() {
    set(String.class);
  }

  public static void set(Class c) {
    try {
      o = c.newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Class.newInstance on Class object via Class.forName and string constant.
 */
class Test7
{
  static Object o;

  public static void run() {
    try {
      Class c = Class.forName("java.lang.String");
      o = c.newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Class.newInstance on Class object via Class.forName and string constant.
 */
class Test8 {
  static Object o;

  public static void run() {
    try {
      Class c = get("java.lang.String");
      o = c.newInstance();
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public static Class get(String s) throws Exception {
    return Class.forName(s);
  }
}

/**
 * Class initialization and Class.forName
 */
class Test9 {
  public static void run()
  {
    try {
      Class c = Class.forName("Test9Helper");
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test9Helper {
  static Object o;

  static {
    o = new Object();  
  }
}

/**
 * Class initialization and class constants: Test10Helper is not
 * initialized (only jre1.5 and jre1.6)
 */
class Test10 {
  public static void run() {
    try {
      Class c = Test10Helper.class;
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test10Helper {
  static Object o;

  static {
    o = new Object();  
  }
}

/**
 * Class initialization and class constants: Test11Helper is initialized.
 */
class Test11 {
  static Object o;

  public static void run() {
    try {
      Class c = Test11Helper.class;
      o = c.newInstance();      
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test11Helper {
  static Object o;

  static {
    o = new Object();  
  }
}

/**
 * Class initialization and class constants: Test12Helper is initialized.
 */
class Test12 {
  public static void run() {
    try {
      Class c = Test12Helper.class;
      c.newInstance();      
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test12Helper {
  // points to an Object
  static Object o;

  // reachable
  static {
    o = new Object();  
  }
}

/**
 * get a declared method
 */
class Test13 {
  // points to a Method object
  static Object o;

  public static void run() {
    try {
      Class c = Test13Helper.class;
      o = c.getDeclaredMethod("foo", new Class[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test13Helper {
  public static void foo() {}
}

/**
 * Reflectively invoke a static method
 */
class Test14 {
  static Method foo;

  public static void run() {
    try {
      Class c = Test14Helper.class;
      foo = c.getDeclaredMethod("foo", new Class[]{});
      foo.invoke(null, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test14Helper {
  // reachable
  static {
    System.out.println("Hey");
  }

  // reachable
  public static void foo() {}
}

/**
 * Reflectively invoke a virtual method
 */
class Test15 {
  static Method foo;

  public static void run() {
    try {
      Test15Helper o = new Test15Helper();
      foo  = Test15Helper.class.getDeclaredMethod("foo", new Class[]{});
      foo.invoke(o, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test15Helper  {
  // reachable
  public void foo() {}
}

/**
 * Reflective method invocation does virtual method lookup
 */
class Test16 {
  static Method foo;

  public static void run() {
    try {
      Test16Helper o = new Test16Helper();
      foo = Test16HelperBase.class.getDeclaredMethod("foo", new Class[]{});
      foo.invoke(o, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test16HelperBase {
  // reachable
  public void foo() {}  
}

class Test16Helper extends Test16HelperBase {
}

/**
 * Reflective method invocation does virtual method lookup (and considers overriding)
 */
class Test17 {
  static Method foo;

  public static void run() {
    try {
      Test17Helper o = new Test17Helper();
      foo = Test17HelperBase.class.getDeclaredMethod("foo", new Class[]{});
      foo.invoke(o, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test17HelperBase {
  // not reachable
  public void foo() {}  
}

class Test17Helper extends Test17HelperBase {
  // reachable
  public void foo() {}
}

/**
 * Reflective private method invocations do not do virtual method lookup.
 */
class Test18 {
  static Method foo;

  public static void run() {
    try {
      Test18Sub o = new Test18Sub();
      foo = Test18.class.getDeclaredMethod("foo", new Class[]{});
      foo.invoke(o, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  // reachable
  private void foo() {}
}

class Test18Sub extends Test18 {
  // not reachable
  private void foo() {}
}

/**
 * Reflective invocation of two different methods at same call-site
 */
class Test19 {
  public static void run() {
    invoke(new Test19Helper1());
    invoke(new Test19Helper2());
  }

  public static void invoke(Object o) {
    try {
      Method foo = o.getClass().getDeclaredMethod("foo", new Class[]{});
      foo.invoke(o, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

class Test19Helper1 {
  // reachable
  public void foo() {}  
}

class Test19Helper2 {
  // reachable
  public void foo() {}
}

/**
 * The heap type is checked for virtual method invocations
 */
class Test20 {
  public static void run() {
    invoke(new Test20Helper1());
  }

  public static void invoke(Object o) {
    try {
      Method foo = Test20Helper2.class.getDeclaredMethod("foo", new Class[]{});
      foo.invoke(o, new Object[]{});
    }
    // will throw exception
    catch(Exception exc) {
      System.err.println("Excepted exception.");
    }
  }
}

class Test20Helper1 {
  // not reachable
  public void foo() {}  
}

class Test20Helper2 {
  // not reachable
  public void foo() {}
}

/**
 * Return value of a reflective static method invocation
 */
class Test21 {
  static String o;

  public static void run() {
    try {
      Class c = Test21.class;
      Method foo = c.getDeclaredMethod("foo", new Class[]{});
      o = (String) foo.invoke(null, new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public static String foo() {
    return new String();
  }
}

/**
 * Parameter of a reflective static method invocation
 */
class Test22 {
  public static void run() {
    try {
      Class c = Test22.class;
      Method foo = c.getDeclaredMethod("foo", new Class[]{Object.class});
      foo.invoke(null, new Object[]{new Object()});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  static Object o;
  public static void foo(Object arg1) {
    o = arg1;
  }
}

/**
 * Parameter of a reflective static method invocation
 */
class Test23 {
  public static void run() {
    try {
      Class c = Test23.class;
      Method foo = c.getDeclaredMethod("foo", new Class[]{Object.class, Object.class});
      foo.invoke(null, new Object[]{new Object(), new Object()});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  static Object o1;
  static Object o2;
  public static void foo(Object arg1, Object arg2) {
    o1 = arg1;
    o2 = arg2;
  }
}

/**
 * Parameter of a reflective static method invocation
 */
class Test24 {
  public static void run() {
    try {
      Class c = Test24.class;
      Method foo = c.getDeclaredMethod("foo", new Class[]{Integer.class, String.class});
      foo.invoke(null, new Object[]{new Integer(1), new String()});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  static Object o1;
  static Object o2;
  public static void foo(Integer arg1, String arg2) {
    o1 = arg1;
    o2 = arg2;
  }
}

/**
 * Return value of a reflective static method invocation
 */
class Test25 {
  static String o;

  public static void run() {
    try {
      Class c = Test25.class;
      Method foo = c.getDeclaredMethod("id", new Class[]{Object.class});
      o = (String) foo.invoke(null, new Object[]{new String()});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public static Object id(Object o) {
    return o;
  }
}

/**
 * Get a public constructor
 */
class Test26 {
  // points to a Constructor object
  static Object o;

  public static void run() {
    try {
      Class c = Test26.class;
      o = c.getConstructor(new Class[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public Test26() {
  }
}

/**
 * Get a public constructor
 */
class Test27 {
  // points to a Constructor object
  static Object o;

  public static void run() {
    try {
      Class c = Test27.class;
      o = c.getConstructor(new Class[]{String.class});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public Test27(String s) {
  }
}

/**
 * Get a public constructor
 */
class Test28 {
  // points to a Constructor object
  static Object o;

  public static void run() {
    try {
      Class c = Test28.class;
      o = c.getDeclaredConstructor(new Class[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  public Test28() {
  }
}

/**
 * Get a public constructor
 */
class Test29 {
  // points to a Constructor object
  static Object o;

  public static void run() {
    try {
      Class c = Test29.class;
      o = c.getDeclaredConstructor(new Class[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  // private constructor returned by getDeclaredConstructor
  private Test29() {
  }
}

class Test30 {
  // does not point to a Constructor object
  static Object o;

  public static void run() {
    try {
      Class c = Test30.class;
      o = c.getConstructor(new Class[]{});
    }
    catch(Exception exc) {
      System.out.println("Test30: Expected exception.");
    }
  }

  // private constructor not returned by getConstructor
  private Test30() {
  }
}

/**
 * Constructor.newInstance
 */
class Test31 {
  // points to a Test31 object
  static Object o1;
  static Object o2;
  public static void run() {
    try {
      Class c = Test31.class;
      Constructor m = c.getConstructor(new Class[]{});
      o1 = m.newInstance(new Object[]{});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  // reachable
  public Test31() {
    super();
    System.out.println("Reachable Test31 okay");
    o2 = this;
  }
}

/**
 * Constructor.newInstance with parameters
 */
class Test32 {
  public static void run() {
    try {
      Class c = Test32.class;
      Constructor m = c.getConstructor(new Class[]{Integer.class, String.class});
      Test32 o = (Test32) m.newInstance(new Object[]{new Integer(1), new String()});
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }

  private static Object o1;
  private static Object o2;  

  // reachable
  public Test32(Integer i, String s) {
    super();
    o1 = i;
    o2 = s;
  }
}


/**
 * Get a declared field
 */
class Test33 {
  // points to a Field object
  static Object o;

  public static void run() {
    try {
      Class c = Test33.class;
      o = c.getDeclaredField("o");
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Reflectively load a static field.
 */
class Test34 {
  // points to an Object
  static Object o1;
  // points to the same Object
  static Object o2;

  public static void run() {
    o1 = new Object();

    try {
      Class c = Test34.class;
      Field field = c.getDeclaredField("o1");
      o2 = field.get(null);
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Reflectively set a static field.
 */
class Test35 {
  // points to an Object
  static Object o;

  public static void run() {
    Object local = new Object();

    try {
      Field field = Test35.class.getDeclaredField("o");
      field.set(null, local);
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Reflectively load an instance field.
 */
class Test36 {
  // points to an Object
  Object o1;
  // points to the same Object
  static Object o2;

  public static void run() {
    Test36 t = new Test36();
    t.o1 = new Object();

    try {
      Field field = Test36.class.getDeclaredField("o1");
      o2 = field.get(t);
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}

/**
 * Reflectively set an instance field.
 */
class Test37 {
  // points to an Object
  Object o1;
  static Object o2;

  public static void run() {
    Test37 t = new Test37();
    Object local = new Object();

    try {
      Field field = Test37.class.getDeclaredField("o1");
      field.set(t, local);
      o2 = t.o1;
    }
    catch(Exception exc) {
      throw new RuntimeException("oops");
    }
  }
}
