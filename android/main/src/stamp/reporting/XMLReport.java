package stamp.reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;

import stamp.util.FileCopy;
import stamp.srcmap.sourceinfo.SourceInfo;

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
			File resultFile = new File(getCanonicalReportFilePath());
			if(resultFile.exists()){
				//need to merge
				File tmpFile = File.createTempFile("stamp_result", null, null);
				tmpFile.deleteOnExit();
				FileCopy.copy(resultFile, tmpFile);
				
				BufferedReader reader = new BufferedReader(new FileReader(tmpFile));
				PrintWriter writer = new PrintWriter(new FileWriter(resultFile));
				String line = null;
				while((line = reader.readLine()) != null){
					if(line.equals("</root>"))
						break;
					writer.println(line);
				}
				reader.close();
				writer.close();
			}
			PrintWriter writer = new PrintWriter(new FileWriter(resultFile));
			//System.out.println("DEBUG: Generating XMLReport " + this.getTitle());
			generate();
			write(writer);
			writer.close();
		} catch(IOException e){
			throw new Error(e);
		}
	}

	protected void write(PrintWriter writer) {
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