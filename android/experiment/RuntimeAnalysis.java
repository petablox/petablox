import java.io.*;
import java.util.*;
import java.text.*;

public class RuntimeAnalysis
{
    public static void main(String[] args) throws IOException
    {
		File stamp_output = new File(args[0]);
		File outputFile = new File(args[2]);
		
		List<File> apkDirs = new ArrayList();
		String[] dirs = args[1].split(File.pathSeparator);
		for(String d : dirs){
			final String prefix = new File(stamp_output, new File(d).getCanonicalPath().replace('/', '_')).getCanonicalPath();
			//System.out.println("prefix: "+prefix);
			apkDirs.addAll(Arrays.asList(stamp_output.listFiles(new FileFilter(){
					public boolean accept(File dir){
						if(dir.getAbsolutePath().startsWith(prefix) && dir.isDirectory())
							return true;
						//System.out.println(dir);
						return false;
					}
				})));
		}

		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss:ms");

		PrintWriter writer = new PrintWriter(new FileWriter(outputFile));
		int successCount = 0;
		List<File> failedApps = new ArrayList();
		for(File apkDir : apkDirs){
			System.out.println("Processing "+apkDir);
			File resultsDir = new File(apkDir, "results");
			File flowFile = new File(resultsDir, "SrcSinkFlow.xml");
			if(!flowFile.exists()){
				failedApps.add(apkDir);
				continue;
			}
			
			successCount++;
			
			String apkName = null;
			int size = -1;
			for(String d : dirs){
				final String prefix = new File(stamp_output, new File(d).getCanonicalPath().replace('/', '_')).getCanonicalPath();
				if(!flowFile.getAbsolutePath().startsWith(prefix))
					continue;
				apkName = flowFile.getAbsolutePath().replace(prefix,"").substring(1);
				apkName = apkName.substring(0, apkName.indexOf('/'));
				String category = apkName.substring(0, apkName.indexOf('_'));
				apkName = apkName.substring(apkName.indexOf('_')+1);
				System.out.println("app: "+apkName);
				
				File apkFile = new File(d, category+"/"+apkName);
				size = (int) (apkFile.length() / 1000);

				break;
			}
			
			File logFile = new File(apkDir, "log.txt");
			if(logFile.exists()){
				//System.out.println("Log file: "+logFile);
				//BufferedReader reader = new BufferedReader(new FileReader(logFile));
				//String line;
				//String lastLine = null;
				//while((line = reader.readLine()) != null){
				//	lastLine = line;
				//}
				//reader.close();
				String lastLine = tail(logFile);
				System.out.println(lastLine);
				if(lastLine != null && lastLine.startsWith("Total time: ")){
					String timeString = lastLine.substring("Total time: ".length());
					timeString = timeString.substring(0, timeString.indexOf(' '));
					String[] tokens = timeString.split(":");
					int hour = Integer.parseInt(tokens[0]);
					int minute = Integer.parseInt(tokens[1]);
					minute += hour*60;
					System.out.println("Time " + apkName + " " + minute);
					writer.println(String.format("%d\t%d", size, minute));
				}
			}
		}
		writer.close();

		System.out.println("Successfully analyzed "+successCount+" out of "+ apkDirs.size() +" apks");
		for(File fa : failedApps)
			System.out.println("failed app: "+fa.getName()); 		
    }    


	public static String tail( File file ) {
		RandomAccessFile fileHandler = null;
		try {
			fileHandler = new RandomAccessFile( file, "r" );
			long fileLength = fileHandler.length() - 1;
			StringBuilder sb = new StringBuilder();

			for(long filePointer = fileLength; filePointer != -1; filePointer--){
				fileHandler.seek( filePointer );
				int readByte = fileHandler.readByte();

				if( readByte == 0xA ) {
					if( filePointer == fileLength ) {
						continue;
					} else {
						break;
					}
				} else if( readByte == 0xD ) {
					if( filePointer == fileLength - 1 ) {
						continue;
					} else {
						break;
					}
				}

				sb.append( ( char ) readByte );
			}

			String lastLine = sb.reverse().toString();
			return lastLine;
		} catch( java.io.FileNotFoundException e ) {
			e.printStackTrace();
			return null;
		} catch( java.io.IOException e ) {
			e.printStackTrace();
			return null;
		} finally {
			if (fileHandler != null )
				try {
					fileHandler.close();
				} catch (IOException e) {
					/* ignore */
				}
		}
	}
}