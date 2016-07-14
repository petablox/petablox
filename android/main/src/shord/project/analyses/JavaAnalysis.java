package shord.project.analyses;

import java.util.List;

import shord.project.Messages;
import shord.project.ITask;

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
    public String toString() {
        return name;
    }
}
