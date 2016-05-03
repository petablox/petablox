package petablox.android.missingmodels.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KCFAAggregation {
	private static final int ksort = 2;
	
	public static void main(String[] args) throws IOException {
		final Map<String,Map<Integer,Integer>> resultInfo = new HashMap<String,Map<Integer,Integer>>();
		process(new File("../../shord3/stamp_output/"), 0, resultInfo);
		process(new File("../../shord2/stamp_output/"), 1, resultInfo);
		process(new File("../../shord/stamp_output/"), 2, resultInfo);
		process(new File("../../shord4/stamp_output/"), 3, resultInfo);
		
		List<String> appList = getAppList(resultInfo);
		for(String appName : appList) {
			System.out.println(convert(appName, resultInfo.get(appName)));
		}
	}
	
	public static String convert(String appName, Map<Integer,Integer> curResultInfo) {
		double best = (double)curResultInfo.get(3);
		//return appName + "\t" + curResultInfo.get(0) + "\t" + curResultInfo.get(1) + "\t" + curResultInfo.get(2) + "\t" + curResultInfo.get(3);
		return appName + "\t" + curResultInfo.get(0)/best + "\t" + curResultInfo.get(1)/best + "\t" + curResultInfo.get(2)/best + "\t" + curResultInfo.get(3)/best;
	}

	// Sort apps by 2-CFA results
	public static List<String> getAppList(final Map<String,Map<Integer,Integer>> resultInfo) {
		List<String> appList = new ArrayList<String>(resultInfo.keySet());
		Set<String> toRemove = new HashSet<String>();
		for(String app : appList) {
			if(!resultInfo.containsKey(app)) {
				//System.out.println(app + ": all");
				toRemove.add(app);
			} else {
				for(int k=0; k<4; k++) {
					if(!resultInfo.containsKey(app) || !resultInfo.get(app).containsKey(k)) {
						//System.out.println(app + ": " + k);
						toRemove.add(app);
					}
				}
			}
		}
		for(String app : toRemove) {
			appList.remove(app);
		}
		
		Collections.sort(appList, new Comparator<String>() {
			@Override
			public int compare(String arg0, String arg1) {
				int numFlows0 = resultInfo.get(arg0).get(ksort);
				int numFlows1 = resultInfo.get(arg1).get(ksort);
				if(numFlows0 > numFlows1) {
					return -1;
				} else if(numFlows0 == numFlows1) {
					return 0;
				} else {
					return 1;
				}
			}
		});
		
		return appList; 
	}
	
	public static void process(File root, int k, final Map<String,Map<Integer,Integer>> resultInfo) throws IOException {
		for(File appDir : root.listFiles()) {
			// get the app name
			String[] appTokens = appDir.getName().split("_");
			String appName = appTokens[appTokens.length-1];
			if(appName.contains(".sqlite") || appName.contains(".db")) {
				continue;
			}
			
			// get the app results
			Map<Integer,Integer> curResultInfo = resultInfo.get(appName);
			if(curResultInfo == null) {
				curResultInfo = new HashMap<Integer,Integer>();
				resultInfo.put(appName, curResultInfo);
			}
			
			// try to add new results
			try {
				File resultFile = new File(appDir, "chord_output/log.txt");
				BufferedReader br = new BufferedReader(new FileReader(resultFile));
				String line;
				while((line = br.readLine()) != null) {
					if(line.contains("Relation Src2Sink: ")) {
						int numFlows = (int)Double.parseDouble(line.split("nodes, ")[1].split(" elements")[0]);
						curResultInfo.put(k, numFlows);
					}				
				}
				br.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
