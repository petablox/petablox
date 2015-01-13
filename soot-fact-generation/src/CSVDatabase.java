import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class CSVDatabase implements Database
{
	private final char SEP = '\t';
	private final char EOL = '\n';

	private File _directory;
	private Map<String, Writer> _writers;

	public CSVDatabase(File directory)
	{
		super();
		_directory = directory;
		_writers = new HashMap<String, Writer>();
	}

	public void close() throws IOException
	{
		for(Writer w: _writers.values())
		{
			w.close();
		}
	}

	public void add(String predicate, Column arg)
	{
		try
		{
			Writer w = getWriter(predicate);
			w.write(arg.toString());
			w.write(EOL);
		}
		catch(IOException exc)
		{
			throw new RuntimeException(exc);
		}
	}

	public void add(String predicate, Column arg1, Column arg2)
	{
		try
		{
			Writer w = getWriter(predicate);
			w.write(arg1.toString());
			w.write(SEP);
			w.write(arg2.toString());
			w.write(EOL);
		}
		catch(IOException exc)
		{
			throw new RuntimeException(exc);
		}
	}

	public void add(String predicate, Column arg1, Column arg2, Column arg3)
	{
		try
		{
			Writer w = getWriter(predicate);
			w.write(arg1.toString());
			w.write(SEP);
			w.write(arg2.toString());
			w.write(SEP);
			w.write(arg3.toString());
			w.write(EOL);
		}
		catch(IOException exc)
		{
			throw new RuntimeException(exc);
		}
	}

	public void add(String predicate, Column arg1, Column arg2, Column arg3, Column arg4)
	{
		try
		{
			Writer w = getWriter(predicate);
			w.write(arg1.toString());
			w.write(SEP);
			w.write(arg2.toString());
			w.write(SEP);
			w.write(arg3.toString());
			w.write(SEP);
			w.write(arg4.toString());
			w.write(EOL);
		}
		catch(IOException exc)
		{
			throw new RuntimeException(exc);
		}
	}

	private Writer getWriter(String predicate) throws IOException
	{
		Writer result = _writers.get(predicate);
		if(result == null)
		{
			result = new OutputStreamWriter(new FileOutputStream(new File(_directory, predicate + ".facts")));
			_writers.put(predicate, result);
		}

		return result;
	}

	public Column addEntity(String string, String key) {
	  add(string, new Column(key));
	  return new Column(key);
	}

	public Column asColumn(String arg) {
		return new Column(arg);
	}

	public Column asEntity(String arg) {
		return new Column(arg);
	}

	public Column asIntColumn(String arg) {
		return new Column(arg);
	}
}
