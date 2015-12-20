package petablox.util;

public final class ExceptionUtil {
  private ExceptionUtil() { /* prevent instantiation */ }
  public static void fail(String m) { throw new RuntimeException(m); }
  public static void fail(Throwable t) { throw new RuntimeException(t); }
  public static void fail(String m, Throwable t) { throw new RuntimeException(m, t); }
  public static void notNull(Object o) { if (o == null) fail("unexpected null"); }
}
