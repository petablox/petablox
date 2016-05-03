package petablox.android;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Scanner;

public class Launcher
{
	static String stampScript;
	static ConcurrentLinkedQueue<String> apkNames = new ConcurrentLinkedQueue();


	public static void main(String[] args) throws Exception
	{
		if(args.length <= 0){
			System.out.println("specify:\n"+ 
							   "(1) path to stamp script\n"+
							   "(2) max. number of stamp's to launch\n"+
							   "(3) path to the directory containing apk's\n");
			System.exit(1);
		}

		stampScript = args[0];
		int maxStamp = Integer.parseInt(args[1]);
		String apkDir = args[2];

		displayDirectoryContents(new File(apkDir));
		
		for(int i = 0; i < maxStamp; i++){
			new Worker().start();
		}
	}

	public static void displayDirectoryContents(File dir) {
		try {
			File[] files = dir.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					displayDirectoryContents(file);
				} else {
					apkNames.add(file.getCanonicalPath());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	static class Worker extends Thread 
	{
		public void run()
		{
			String apkName = null;
			while((apkName = apkNames.poll()) != null){
				String[] cmdArray = new String[]{stampScript, "analyze", apkName};
				try{
					Process proc = Runtime.getRuntime().exec(cmdArray, null, null); 
                    Scanner sc = new Scanner(proc.getInputStream());           
                    while (sc.hasNext()) System.out.println(sc.nextLine());
                    proc.waitFor();

				}catch(IOException e){
					System.out.println(e.getMessage());
					e.printStackTrace();
				}catch(InterruptedException e){
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}

    }
}
