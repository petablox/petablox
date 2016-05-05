package petablox.android.missingmodels.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpreadsheetFormatter {
	private static String rootPath = "stamp_output";
	private static String locPath = "experimentLOC_results";
	
	private static int correctModels = 0;
	private static int totalModels = 0;
	
	public static String convert(String appName, Map<String,String> resultInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append(appName).append("\t");

		// get jimple LOC for app
		String loc = resultInfo.get("Lines of code [app,framework]");
		loc = loc.substring(1,loc.length()-1);
		String[] locTokens = loc.split(",");
		sb.append(locTokens[0]).append("\t");
		
		sb.append(resultInfo.get("Number of proposed models")).append("\t");
		sb.append(resultInfo.get("Number of rounds")).append("\t");
		sb.append(resultInfo.get("Accuracy")).append("\t");
		
		// get number of before/after flows
		String flow = resultInfo.get("Number of flows [before,after]");
		flow = flow.substring(1,flow.length()-1);
		String[] flowTokens = flow.split(",");
		sb.append(flowTokens[0]).append("\t").append(flowTokens[1]).append("\t");
		
		sb.append(resultInfo.get("Maximum running time")).append("\t");

		sb.append(resultInfo.get("pt")).append("\t");
		sb.append(resultInfo.get("ptFull")).append("\t");
		sb.append(resultInfo.get("transfer")).append("\t");
		sb.append(resultInfo.get("taintedStub")).append("\t");
		sb.append(resultInfo.get("minEdges")).append("\t");
		sb.append(resultInfo.get("maxEdges")).append("\t");
		
		// set some global parameters
		correctModels += (int)(Double.parseDouble(resultInfo.get("Accuracy"))*Integer.parseInt(resultInfo.get("Number of proposed models")));
		totalModels += Integer.parseInt(resultInfo.get("Number of proposed models"));
		
		return sb.toString();
	}
	
	public static void main(String[] args) throws IOException {
		File root = new File("../" + rootPath);
		List<String> appList = new ArrayList<String>();
		final Map<String,Map<String,String>> resultInfo = new HashMap<String,Map<String,String>>();
		for(File appDir : root.listFiles()) {
			Map<String,String> curResultInfo = new HashMap<String,String>();
			String[] appTokens = appDir.getName().split("_");
			String appName = appTokens[appTokens.length-1];
			try {
				String line;
				File resultFile;
				BufferedReader br;
				
				resultFile = new File(appDir, "cfl/results.txt");
				br = new BufferedReader(new FileReader(resultFile));
				while((line = br.readLine()) != null) {
					String[] tokens = line.split(": ");
					if(tokens.length == 2) {
						curResultInfo.put(tokens[0], tokens[1]);
					}
				}
				br.close();
				resultInfo.put(appName, curResultInfo);
				appList.add(appName);
				
				resultFile = new File(appDir, "chord_output/log.txt");
				br = new BufferedReader(new FileReader(resultFile));
				int numRef2Ref = 0;
				while((line = br.readLine()) != null) {
					if(line.contains("Relation pt:")) {
						curResultInfo.put("pt", Integer.toString((int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0])));
					}
					if(line.contains("Relation PtFull:")) {
						curResultInfo.put("ptFull", Integer.toString((int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0])));
						//System.out.println(curResultInfo.get("ptFull"));
					}
					if(line.contains("SAVING rel Framework size: ")) {
						curResultInfo.put("framework", line.split(": ")[1]);
					}
					if(line.contains("Relation out_taintedStub: ")) {
						curResultInfo.put("taintedStub", Integer.toString((int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0])));
					}
					if(line.contains("Relation out_taintedStub: ")) {
						curResultInfo.put("taintedStub", Integer.toString((int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0])));
					}
					if(line.contains("Relation Transfer: ")) {
						curResultInfo.put("transfer", Integer.toString((int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0])));						
					}
					if(line.contains("Relation Ref2RefTFullT: ")) {
						numRef2Ref += (int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0]);						
					}
					if(line.contains("Relation Prim2RefTFullT: ")) {
						numRef2Ref += (int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0]);						
					}
					if(line.contains("Relation Ref2PrimTFullT: ")) {
						numRef2Ref += (int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0]);						
					}
					if(line.contains("Relation Prim2PrimTFullT: ")) {
						numRef2Ref += (int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0]);						
					}				
				}
				curResultInfo.put("maxEdges", Integer.toString(numRef2Ref));
				System.out.println(appName + "\t" + numRef2Ref);
				br.close();
				
				resultFile = new File(appDir, "cfl/AllStubInputs.txt");
				br = new BufferedReader(new FileReader(resultFile));
				int lineCount = 0;
				while((line = br.readLine()) != null) {
					if(line.contains(":")) {
						lineCount++;
					}
				}
				curResultInfo.put("minEdges", Integer.toString(lineCount));
				br.close();
			} catch(IOException e) {
				//e.printStackTrace();
			}
		}
		
		// FIX LINES OF CODE
		File locRoot = new File("../" + locPath);
		for(File appDir : locRoot.listFiles()) {
			try {
				String[] appTokens = appDir.getName().split("_");
				String appName = appTokens[appTokens.length-1];
				Map<String,String> curResultInfo = resultInfo.get(appName);
				curResultInfo.put("Lines of app code", "-1");
				File locFile = new File(appDir, "loc.txt");
				BufferedReader br = new BufferedReader(new FileReader(locFile));
				br.readLine();
				String appLoc = br.readLine();
				String loc = "[" + appLoc + "," + br.readLine() + "]";
				br.close();
				curResultInfo.put("Lines of code [app,framework]", loc);
				// add the jimple LOC
				curResultInfo.put("Lines of app code", appLoc);
			} catch(Exception e) {
				//e.printStackTrace();
			}
		}
		
		// Sort by lines of code
		Collections.sort(appList, new Comparator<String>() {
			@Override
			public int compare(String arg0, String arg1) {
				int loc0 = Integer.parseInt(resultInfo.get(arg0).get("Lines of app code"));
				int loc1 = Integer.parseInt(resultInfo.get(arg1).get("Lines of app code"));
				if(loc0 > loc1) {
					return -1;
				} else if(loc0 == loc1) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		
		for(String appName : appList) {
			System.out.println(appName);
			System.out.println(convert(appName, resultInfo.get(appName)));
		}
		//System.out.println(correctModels + "," + totalModels);
	}
}
