package petablox.souffle;

import petablox.project.Config;
import petablox.project.OutDirUtils;

public class Solver {
	public static void run(String fileName) {
		String[] cmdArray = 
			{
				"souffle",
				"-j8",
				"-F",
				Config.souffleWorkDirName,
				"-c",
				fileName
			};
		OutDirUtils.executeWithFailOnError(cmdArray);
	}
}
