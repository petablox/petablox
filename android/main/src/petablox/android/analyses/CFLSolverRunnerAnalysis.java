package petablox.android.analyses;

import petablox.android.util.PropertyHelper;
import petablox.android.util.ShellProcessRunner;

import java.util.Calendar;

import petablox.project.Petablox;
import petablox.project.analyses.JavaAnalysis;

@Petablox(
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
