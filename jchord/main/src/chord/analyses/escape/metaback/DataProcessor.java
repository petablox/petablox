package chord.analyses.escape.metaback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import chord.project.Chord;
import chord.project.analyses.JavaAnalysis;
import chord.util.Execution;
import chord.util.Utils;

/**
 * The post data processor of metaback thresc analysis.
 * data.config file format:
 *		path of the file containing the proven queries
 *		path of the file containing the impossible queries
 *		number of workers
 * 		path of worker 1 log file
 * 		..........
 * 		path of worker n log file
 * 
 * 
 * @author xin
 *
 */

@Chord(name = "iter-thresc-data")
public class DataProcessor extends JavaAnalysis {
	private Execution X;
	private String input;
	private ArrayList<Integer> trackedQueries;
	private ArrayList<Integer> fq;
	private ArrayList<Integer> tq;
	private boolean trackForward;
	private Map<Integer,Long> timeMap;

	public DataProcessor() {
		X = Execution.v();
		input = X.getStringArg("config", "data.config");
		trackedQueries = new ArrayList<Integer>();
		fq = new ArrayList<Integer>();
		tq = new ArrayList<Integer>();
		trackForward = false;
		timeMap = new HashMap<Integer,Long>();
	}

	@Override
	public void run() {
		Scanner sc = getScanner(input);
		String tqFile = sc.nextLine();
		fillInQueryList(tqFile,tq);
		String fqFile = sc.nextLine();
		fillInQueryList(fqFile,fq);
		int numFiles = Integer.parseInt(sc.nextLine());
		for (int i = 0; i < numFiles; i++) {
			String fileName = sc.nextLine();
			Scanner fsc = getScanner(fileName);
			while (fsc.hasNext()) {
				String line = fsc.nextLine().trim();
				if(line.equals(""))
					continue;
				processForward(line);
				processBackward(line);
			}
		}
		PrintWriter tp = Utils.openOut(X.path("timeT.txt"));
		PrintWriter fp = Utils.openOut(X.path("timeF.txt"));
		for(Map.Entry<Integer, Long> entry: timeMap.entrySet()){
			if(fq.contains(entry.getKey()))
				fp.println(getFormattedTime(entry.getValue()));
			if(tq.contains(entry.getKey()))
				tp.println(getFormattedTime(entry.getValue()));
		}
		tp.flush();
		tp.close();
		fp.flush();
		fp.close();
	}
	
	private Scanner getScanner(String fileName){
		Scanner sc;
		try {
			sc = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return sc;
	}
	
	private float getFormattedTime(long milliseconds){
		return ((float)milliseconds)/1000;
	}
	
	private void processForward(String line){
		if(line.startsWith("currEs")){
			trackedQueries.clear();
			trackForward = true;
			return;
		}
		if(line.startsWith("currHs")){
			trackForward = false;
			return;
		}
		if(trackForward){
			String queryLine[] = line.split(" ");
			trackedQueries.add(Integer.parseInt(queryLine[queryLine.length-1]));
			return;
		}
		if(line.startsWith("ForwardTime")){
			String timeLine[] = line.split(" ");
			long time = Long.parseLong(timeLine[1]);
			for(int i:trackedQueries){
				updateTimeMap(i,time);
			}
			return;
		}
	}

	private void updateTimeMap(int query, long time){
		Long currTime = timeMap.get(query);
		if(currTime == null)
			timeMap.put(query, 0-time);
		else
			timeMap.put(query,Math.abs(currTime.longValue())+time);
	}
	
	private void processBackward(String line){
		if(line.startsWith("BackwardTime")){
			String timeLine[] = line.split(" ");
			int qindex = Integer.parseInt(timeLine[1]);
			long time = Long.parseLong(timeLine[2]);
			updateTimeMap(qindex,time);
		}
	}
	
	private void fillInQueryList(String fileName, ArrayList<Integer> list){
		Scanner sc = getScanner(fileName);
		while(sc.hasNext()){
			String line = sc.nextLine().trim();
			if(line.equals(""))
				continue;
			list.add(Integer.parseInt(line));
		}
	}
}
