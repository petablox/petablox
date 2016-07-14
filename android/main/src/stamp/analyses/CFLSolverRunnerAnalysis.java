package stamp.analyses;

import stamp.util.PropertyHelper;
import stamp.util.ShellProcessRunner;

import java.util.Calendar;

import chord.project.Chord;
import shord.project.analyses.JavaAnalysis;

@Chord(
	name = "cfl-solver-runner-java"
)
public class CFLSolverRunnerAnalysis extends JavaAnalysis {
	@Override
	public void run() {
		String workDir = PropertyHelper.getProperty("stamp.solvergen.workdir");
		String executable =
			PropertyHelper.getProperty("stamp.solvergen.executable");
		long startTime = Calendar.getInstance().getTimeInMillis();
		ShellProcessRunner.run(new String[]{executable}, workDir, true);
	}
}
