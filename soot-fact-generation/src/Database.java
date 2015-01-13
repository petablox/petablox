import java.io.IOException;

public interface Database {
  public void add(String predicate, Column arg);
  public void add(String predicate, Column arg1, Column arg2);
  public void add(String predicate, Column arg1, Column arg2, Column arg3);
  public void add(String predicate, Column arg1, Column arg2, Column arg3, Column arg4);
  public Column addEntity(String string, String key);
  public Column asColumn(String arg);
  public Column asIntColumn(String arg);
  public Column asEntity(String arg);
  
  public void close() throws IOException;

}