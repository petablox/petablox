package petablox.reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import petablox.android.srcmap.sourceinfo.SourceInfo;

/*
 * @author Saswat Anand
**/
public abstract class XMLReport extends Category {
	private String relName;
	private String title;

	protected XMLReport(String title) {
		this.title = title;
		String className = this.getClass().getName();
		this.relName = className.substring(className.lastIndexOf('.')+1);
	}

	public String getTitle() {
		return title;
	}

	public String getCanonicalReportFilePath() { 
		try{
			File f = new File(Postmortem.resultsDir, relName+".xml");
			return f.getCanonicalPath();
		}catch(IOException e){
			throw new Error(e);
		}
	}
	/*
	public String getCanonicalReportFilePath(boolean jimple) { 
		try{
			File f;
			if(jimple) {
				f = new File(Postmortem.resultsDir, relName+"_jimple"+".xml");
			} else {
				f = new File(Postmortem.resultsDir, relName+".xml");
			}
			return f.getCanonicalPath();
		}catch(IOException e){
			throw new Error(e);
		}
	}
	*/

	public void write() {
		try{
			PrintWriter writer = new PrintWriter(new FileWriter(new File(getCanonicalReportFilePath())));
			//System.out.println("DEBUG: Generating XMLReport " + this.getTitle());
			generate();
			write(writer);
			writer.close();
		} catch(IOException e){
			throw new Error(e);
		}
	}

	public void write(PrintWriter writer) {
		writer.println("<root>");
		writer.println("<title>"+title+"</title>");
		for(Tuple t : tuples)
			t.write(writer);
		for(Category sc : sortSubCats())
			sc.write(writer);
		writer.println("</root>");
	}

	public abstract void generate();
	
    //String mstr(jq_Method m) { return m.getDeclaringClass().shortName()+"."+m.getName(); }
}
