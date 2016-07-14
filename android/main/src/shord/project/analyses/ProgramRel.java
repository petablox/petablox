package shord.project.analyses;

import java.util.List;
import java.io.File;

import shord.project.ClassicProject;

import shord.project.Config;
import shord.project.Messages;
import shord.project.ITask;

import chord.bddbddb.Rel;
import chord.bddbddb.RelSign;
import chord.util.Utils;


/**
 * Generic implementation of a program relation (a specialized kind of Java task).
 * <p>
 * A program relation is a relation over one or more program domains.
 *
 * @author Mayur Naik (mhn@cs.stanford.edu)
 * @author Saswat Anand
 */
public class ProgramRel extends Rel implements ITask {
    private static final String SKIP_TUPLE =
        "WARN: Skipping a tuple from relation '%s' as element '%s' was not found in domain '%s'.";
    protected Object[] consumes;
    @Override
    public void run() {
        zero();
        init();
        fill();
        save();
    }
    public void init() { }
    public void save() {
        if (Config.v().verbose >= 1)
            System.out.println("SAVING rel " + name + " size: " + size());
        super.save(Config.v().bddbddbWorkDirName);
		ClassicProject.g().setTrgtDone(this);
    }
    public void load() {
        super.load(Config.v().bddbddbWorkDirName);
    }

    public void fill()
	{
		throw new RuntimeException("implement");
	}

    public void print() {
        super.print(Config.v().outDirName);
    }
    public String toString() {
        return name;
    }
    public void skip(Object elem, ProgramDom dom) {
        Messages.log(SKIP_TUPLE, getClass().getName(), elem, dom.getClass().getName());
    }
}
