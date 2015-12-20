package petablox.project.analyses;

import java.util.List;

import CnCHJ.api.ItemCollection;
import petablox.project.ICtrlCollection;
import petablox.project.IDataCollection;
import petablox.project.IStepCollection;
import petablox.project.ITask;
import petablox.project.Messages;
import petablox.project.ModernProject;

/**
 * Generic implementation of a Java task (a program analysis
 * expressed in Java).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class JavaAnalysis implements ITask {
    private static final String UNDEFINED_RUN = "ERRROR: Analysis '%s' must override method 'run()'";
    protected String name;
    protected Object[] consumes;
    protected Object[] produces;
    protected Object[] controls;
    @Override
    public void setName(String name) {
        assert (name != null);
        assert (this.name == null);
        this.name = name;
    }
    @Override
    public String getName() {
        return name;
    }
    @Override
    public void run() {
        Messages.fatal(UNDEFINED_RUN, name);
    }
    @Override
    public void run(Object ctrl, IStepCollection sc) {
        ModernProject p = ModernProject.g();
        consumes = p.runPrologue(ctrl, sc);
        run();
        p.runEpilogue(ctrl, sc, produces, controls);
    }
    @Override
    public String toString() {
        return name;
    }
}
