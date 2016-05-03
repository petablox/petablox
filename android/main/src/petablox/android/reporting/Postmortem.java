package petablox.reporting;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import petablox.program.Program;
import petablox.project.analyses.JavaAnalysis;
import petablox.android.srcmap.SourceInfoSingleton;
import petablox.android.srcmap.SourceInfoSingleton.SourceInfoType;
import petablox.project.Petablox;

/**
 * @author Saswat Anand
 * @author Osbert Bastani
 */
@Petablox(name = "post-java")
public class Postmortem extends JavaAnalysis {
	public static final String reportsTxtFilePath = System.getProperty("stamp.out.dir")+"/reports.txt";
	public static final String resultsDir = System.getProperty("stamp.out.dir")+"/results";
	public static final boolean processingSrc = System.getProperty("stamp.input.type", "src").equals("src");
	public static final boolean genAllReports = System.getProperty("stamp.all.reports", processingSrc ? "true" : "false").equals("true");
	
	public static void processReports(Class[] allReports, Class[] dontShowReports, PrintWriter reportsTxtWriter, boolean jimple) throws InstantiationException, IllegalAccessException {
		for(Class reportClass : allReports) {
			if(jimple) {
				//report.setSourceInfo(SourceInfoSingleton.getJimpleSourceInfo());
				SourceInfoSingleton.setSourceInfoType(SourceInfoType.JIMPLE);
			} else {
				SourceInfoSingleton.setSourceInfoType(SourceInfoType.JAVA);
			}
			XMLReport report = (XMLReport) reportClass.newInstance();
			
			boolean show = true;
			for(int i = 0; show && i < dontShowReports.length; i++){
				if(reportClass.equals(dontShowReports[i]))
					show = false;
			}
			if(show) {
				reportsTxtWriter.println(report.getTitle() + " " + report.getCanonicalReportFilePath());
			}
		
			report.write();
		}

	}

    public void run() {
		new File(resultsDir).mkdirs();

		Class[] allReports = new Class[]{
			SrcFlow.class
			,ArgSinkFlow.class
			,SrcSinkFlow.class
			,SrcSinkFlowViz.class
			//,ReachableStub.class,
			,TaintedStub.class
			//,InvkNone.class,
			,TaintedVar.class
			,IM.class
			,PotentialCallbacks.class
			,AllReachable.class
			,FileNames.class
			,MissingModels.class
		};

		Class[] finalReport = new Class[]{
			PotentialCallbacks.class
			,SrcSinkFlow.class
		};

		Class[] dontShowReports = new Class[]{
			IM.class
			,AllReachable.class
			,SrcSinkFlowViz.class
			,FileNames.class
			//,MissingModels.class
		};

		try{
			PrintWriter reportsTxtWriter = new PrintWriter(new FileWriter(new File(reportsTxtFilePath)));

			Class[] reportsToGenerate = genAllReports ? allReports : finalReport;
			
			if(processingSrc) {
				processReports(reportsToGenerate, dontShowReports, reportsTxtWriter, false);
			} else {
				processReports(reportsToGenerate, dontShowReports, reportsTxtWriter, true);
			}
			
			reportsTxtWriter.close();
		} catch(Exception e){
			throw new Error(e);
		}
	}
}
