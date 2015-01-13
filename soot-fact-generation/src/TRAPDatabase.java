import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class TRAPDatabase implements Database {
	private final char SEP = ',';
	private final char EOL = '\n';

	private File _directory;
	private Map<String, Writer> _writers;

	public TRAPDatabase(File directory)
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
			writePredicate(predicate, w);
			w.write("(");
			w.write(arg.toString());
			w.write(")");
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
			writePredicate(predicate, w);
			w.write("(");
			w.write(arg1.toString());
			w.write(SEP);
			w.write(arg2.toString());
			w.write(")");
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
			writePredicate(predicate, w);
			w.write("(");
			w.write(arg1.toString());
			w.write(SEP);
			w.write(arg2.toString());
			w.write(SEP);
			w.write(arg3.toString());
			w.write(")");
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
			writePredicate(predicate, w);
			w.write("(");
			w.write(arg1.toString());
			w.write(SEP);
			w.write(arg2.toString());
			w.write(SEP);
			w.write(arg3.toString());
			w.write(SEP);
			w.write(arg4.toString());
			w.write(")");
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
			result = new OutputStreamWriter(new FileOutputStream(new File(_directory, predicate + ".trap")));
			_writers.put(predicate, result);
		}

		return result;
	}

	
	private String[] entities = new String[20];
	int index = 0;
	
	public Column addEntity(String predicate, String id) {
		try
		{
			String escapedID = escape(id);
			String key = createKey(escapedID);
			
			String check = key + predicate;
			for(int i = 0; i < 20; i++) {
				if(check.equals(entities[(index - 1 - i + 20) % 20]))
					return new Column(key);
			}
			entities[index] = check;
			index = (index + 1) % 20;

			Writer w = getWriter(predicate);
			writePredicate(predicate, w);
			w.write("(");
			w.write(key);
			w.write(SEP);
			w.write(escapedID);
			w.write(")");
			w.write(EOL);
			return new Column(key);
		}
		catch(IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	public Column asColumn(String arg) {
		return new Column(escape(arg));
	}

	public Column asIntColumn(String arg) {
		return new Column(arg);
	}

	private String escape(String id) {
		id = id.replaceAll("\"", "\"\"");
		return "\"" + id + "\"";
	}
	
	private void writePredicate(String predicate, Writer w) throws IOException {
		predicate = predicate.replaceAll("-", "");
		w.write(Character.toLowerCase(predicate.charAt(0)));
		w.write(predicate.substring(1));
	}

	public Column asEntity(String id) {
		return new Column(createKey(escape(id)));
	}
	
	private String createKey(String escapedID) {
		escapedID = escapedID.replace('{', '_');
		escapedID = escapedID.replace('}', '_');
		return "@" + escapedID;
	}
}
