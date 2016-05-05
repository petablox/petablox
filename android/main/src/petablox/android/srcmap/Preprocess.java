package petablox.android.srcmap;

import java.util.*;
import java.io.*;

public class Preprocess {
    public static void preprocess(File file) throws Exception 
	{
		if(file.isDirectory()) {
			for(File f : file.listFiles()) {
				preprocess(f);
			}
		} else {
			String fileName = file.getName();
			if(!fileName.endsWith(".java"))
				return;
			if(fileName.indexOf('#') >= 0)
				return;
			BufferedReader br = new BufferedReader(new FileReader(file));
			StringBuilder sourceBuilder = new StringBuilder();
			String line;
			while((line = br.readLine()) != null) {
				sourceBuilder.append(line+"\n");
			}
			br.close();
			String source = sourceBuilder.toString();
			PrintWriter pw = new PrintWriter(file);
			pw.print(source);
			pw.close();
		}
    }	

    public static void main(String[] args) throws Exception {
		for(String appPath : args[0].split(":")){
			File directory = new File(appPath);
			preprocess(directory);
		}
    }
}
