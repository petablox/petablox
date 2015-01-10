public class Threads
{
  public static void main(String[] ps)
  {
    Test1.test();
    Test1b.test();
    Test1c.test();
    Test1d.test();
    Test2.test();
    Test3.test();
    Test4.test();
  }
}

/**
 * Test for subclassed thread.
 */
class Test1
{
  public static void test()
  {
    Thread t = new MyThread();
    t.start();
  }

  static class MyThread extends Thread
  {
    public void run()
    {
    }
  }
}

/**
 * Test for subclassed thread.
 */
class Test1b
{
  public static void test()
  {
    Thread t = new MyThread();
    t.start();
  }

  static class MyThread extends Thread
  {
    /**
     * Thread.start is overriden and invokes super.start, so the
     * method run is reachable.
     */
    public void start()
    {
      super.start();
    }

    public void run()
    {
    }
  }
}

/**
 * Test for a naughty subclassed thread
 */
class Test1c
{
  public static void test()
  {
    Thread t = new MyThread();
    indirect(t);
  }

  public static void indirect(Thread t)
  {
    t.start();
  }

  static class MyThread extends Thread
  {
    // start is overriden and does not invoke super.start, so the
    // method run is not reachable.
    public void start()
    {
    }

    public void run()
    {
    }
  }
}

/**
 * Test for a subclassed thread
 */
class Test1d
{
  public static void test()
  {
    Thread t = new MyThread();
    t.start();
  }

  static class BaseThread extends Thread
  {

    /**
     * Test method lookup in implementation of thread start: the
     * method run in BaseThread is not reachable because it is
     * overriden in MyThread.
     */
    public void run()
    {
    }
  }

  static class MyThread extends BaseThread
  {
    public void run()
    {
    }
  }
}


/**
 * Test for Thread with Runnable
 */
class Test2
{
  public static void test()
  {
    Thread t = new Thread(new MyRunnable());
    t.start();
  }

  static class MyRunnable implements Runnable
  {
    public void run()
    {
    }
  }
}


/**
 * Test for class that mimics thread, but is not a thread.
 *
 * The run method should not be reachable.
 */
class Test3
{
  public static void test()
  {
    new MyFakeThread().start();
  }

  static class MyFakeThread
  {
    public void start()
    {
    }

    public void run()
    {
    }
  }
}

class Test4
{ 
  private static Thread currentThread;

  public static void test()
  {
    currentThread = Thread.currentThread();
  }
}
