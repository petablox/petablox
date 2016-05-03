package petablox.android.analyses;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import petablox.project.analyses.JavaAnalysis;
import petablox.android.util.PropertyHelper;
import soot.Scene;
import soot.SootClass;
import petablox.android.missingmodels.jimplesrcmapper.PetabloxJimpleAdapter;
import petablox.android.missingmodels.jimplesrcmapper.CodeStructureInfo;
import petablox.android.missingmodels.jimplesrcmapper.JimpleStructureExtractor;
import petablox.android.missingmodels.jimplesrcmapper.Printer;
import petablox.android.missingmodels.util.xml.XMLObject;
import petablox.android.srcmap.SourceInfoSingleton;
import petablox.android.srcmap.sourceinfo.jimpleinfo.JimpleSourceInfo;
import petablox.project.Petablox;

import com.google.common.io.NullOutputStream;

/*
 * @author Osbert Bastani
 */
@Petablox(name = "jimpleprinter")
public class JimplePrinterAnalysis extends JavaAnalysis {

	@Override public void run() {
		try {

			boolean printClasses =
				PropertyHelper.getBoolProp("stamp.print.allclasses");
			System.out.println("++stamp.print.allclasses = "+printClasses);
			if(!printClasses)
				return;

			// SET UP SCRATCH DIRECTORY
			String outDir = System.getProperty("stamp.out.dir");

			// PRINT JIMPLE
			JimpleStructureExtractor jse = new JimpleStructureExtractor();
			Printer printer = new Printer(jse);
			printer.printAll(outDir + "/jimple/");

			PrintWriter pw = new PrintWriter(outDir + "/loc.txt");
			pw.println(printer.getAppLOC() + "\n" + printer.getFrameworkLOC());
			pw.close();

			// GET STRUCTURE AND PRINT
			CodeStructureInfo codeInfo = jse.getCodeStructureInfo();
			JimpleSourceInfo sourceInfo = SourceInfoSingleton.getJimpleSourceInfo();

			for(SootClass cl : Scene.v().getClasses()) {
				//System.out.println("READING: " + cl.getName());

				// GET THE OUTPUT FILE PATH	
				StringBuffer b = new StringBuffer();
				//b.append(outputPath);
				b.append(outDir + "/jimple/" + cl.getPackageName().replace('.', '/') + '/'); 
				b.append(cl.getName());	
				b.append(".xml");
				String xmlOutputPath = b.toString();

				// CREATE THE OBJECT
				PetabloxJimpleAdapter cja = new PetabloxJimpleAdapter(sourceInfo);
				printer = new Printer(cja.toJimpleVisitor(codeInfo));
				printer.printTo(cl, new NullOutputStream());
				XMLObject object = cja.getResults().get(cl);

				// WRITE THE XML OBJECT
				File objectOutputFile = new File(xmlOutputPath);
				objectOutputFile.getParentFile().mkdirs();
				//System.out.println("PRINTING TO: " + objectOutputFile.getCanonicalPath());
				pw = new PrintWriter(new FileOutputStream(objectOutputFile));
				pw.println(object.toString());
				pw.close();
			}
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
