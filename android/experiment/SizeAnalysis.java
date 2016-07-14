import java.io.*;
import java.util.*;

public class SizeAnalysis
{
	public static void main(String[] args) throws IOException
	{
		String listFile = args[0];

		Map<Integer,Integer> sizeToAppCount = new TreeMap();
		BufferedReader reader = new BufferedReader(new FileReader(listFile));
		String line;
		int totalAppsCount = 0;
		while((line = reader.readLine()) != null){
			int index = line.lastIndexOf(' ');
			String apkFileName = line.substring(0, index);
			int size = Integer.parseInt(line.substring(index+1)) / 1000;
			Integer appCount = sizeToAppCount.get(size);
			if(appCount == null){
				sizeToAppCount.put(size, 1);
			} else {
				sizeToAppCount.put(size, appCount+1);
			}
			totalAppsCount++;
		}
		reader.close();

		int cummulative = 0;
		for(Map.Entry<Integer,Integer> e : sizeToAppCount.entrySet()){
			int size = e.getKey();
			int count = e.getValue();
			cummulative += count;
			System.out.println(String.format("%d\t%f", size, (cummulative*100.0)/totalAppsCount));
		}
	}
}