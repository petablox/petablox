package stamp.reporting;

import java.io.*;

/*
  @author Saswat Anand
**/
public class RootCategory extends Category
{
	private String title;

	public RootCategory(String title)
	{
		super();
		this.title = title;
	}
	
	public void write(PrintWriter writer)
	{
		writer.println("<root>");
		writer.println("<title>"+title+"</title>>");
		for(Tuple t : tuples)
			t.write(writer);
		for(Category sc : sortSubCats())
			sc.write(writer);
		writer.println("</root>");
	}
}