public class ClassInit3
{
  public static void main(String[] ps)
  {
    // triggers clinit
    new TestNewInstance();

    // triggers clinit
    TestStaticMethod.bar();

    // triggers clinit
    TestStoreStaticField.x = new Object();

    // triggers clinit
    TestStorePrimStaticField.x = 5;

    // triggers clinit
    TestStoreNullStaticField.x = null;

    // triggers clinit
    Object o = TestLoadStaticField.x;

    // triggers clinit
    int y = TestLoadPrimStaticField.x;

    // does not trigger clinit
    TestNewArray[] xs = new TestNewArray[5];

    Class foo = TestClassLiteral.class;
  }
}

class TestNewInstance
{
  static {}
}

class TestStaticMethod
{
  static {}
  public static void bar() {}
}

class TestStoreStaticField
{
  static {}
  static Object x;
}

class TestStoreNullStaticField
{
  static {}
  static Object x;
}

class TestStorePrimStaticField
{
  static {}
  static int x;
}

class TestLoadStaticField
{
  static {}
  static Object x = new Object();
}

class TestLoadPrimStaticField
{
  static {}
  static int x = 3;
}

class TestNewArray
{
  static {}
}

class TestClassLiteral
{
  static {}
}