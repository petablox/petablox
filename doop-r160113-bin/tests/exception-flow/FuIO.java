import java.net.*;
import java.io.*;

/**
 * Example from the ISSTA'04 paper by Fu et al.
 */
public class FuIO
{
  public static Exception fromFile;
  public static Exception fromNet;
  public static Exception fromMain;

  public static void main(String[] ps)
  {
    try
    {
      Socket s = new Socket("tweakers.net", 80);
      FuIO io = new FuIO();
      io.readFile("foo.txt");
      io.readNet(s);
      s.close();
    }
    catch(Exception exc)
    {
      fromMain = exc;
      exc.printStackTrace();
    }
  }

  public void readFile(String filename)
  {
    byte[] buffer = new byte[256];
    try 
    {
      InputStream f = new FileInputStream(filename);
      InputStream fin = new BufferedInputStream(f);
      int c = fin.read(buffer);
      System.out.println("file c: " + c);
    }
    catch(IOException exc)
    {
      fromFile = exc;
      exc.printStackTrace();
    }
  }

  public void readNet(Socket socket) 
  {
    byte[] buffer = new byte[256];
    try
    {
      InputStream s = socket.getInputStream();
      InputStream sin = new BufferedInputStream(s);
      int c = sin.read(buffer);
      System.out.println("socket c: " + c);
    }
    catch(IOException exc)
    {
      fromNet = exc;
      exc.printStackTrace();
    }
  }
}