
import java.util.*;
import java.io.*;

public class RandomSelect
{
	public static void main(String[] args) throws IOException
	{
		int num = Integer.parseInt(args[0]);
		String listFile = args[1];
		String outFile = args[2];

		List<String> apps = new LinkedList();
		int appCount = 0;

		BufferedReader reader = new BufferedReader(new FileReader(listFile));
		String line;
		while((line = reader.readLine()) != null){
			apps.add(line);
			appCount++;
		}
		reader.close();

		PrintWriter writer = new PrintWriter(new FileWriter(outFile));		
		Random random = new Random();
		for(int i = 0; i < num; i++){
			int index = random.nextInt(appCount);
			writer.println(apps.get(index));
		}
		writer.close();
	}
}