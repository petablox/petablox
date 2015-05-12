package chord.analyses.logicblox;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import chord.bddbddb.Dom;
import chord.bddbddb.Rel;
import chord.logicblox.LogicBloxExporter;
import chord.logicblox.LogicBloxUtils;
import chord.project.Chord;
import chord.project.ChordException;
import chord.project.ClassicProject;
import chord.project.Config;
import chord.project.Config.DatalogEngineType;
import chord.project.Messages;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.Utils;

/**
 * A task that exports all targets to a LogicBlox workspace.
 * <p>
 * This is not a real analysis, but simply takes every completed target and 
 * exports it to LogicBlox.  All config options relevant to LB are used except 
 * that <tt>chord.logicblox.export.mode</tt> indicates the LB engine type  
 * (<tt>chord.datalog.engine</tt> should be set to <tt>bddbddb</tt>).  LB 4 is 
 * used by default.
 * <p>
 * To use this exporter, simply run it as the last analysis in the series, for example 
 * using options like:<br />
 * <code>-Dchord.datalog.engine=bddbddb -Dchord.run.analyses=ctxts-java,argCopy-dlog,logicblox-export</code>
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
@Chord(name = "logicblox-export")
public class LogicBloxExportAnalysis extends JavaAnalysis {
    private DatalogEngineType logicbloxType;

    public LogicBloxExportAnalysis() {
        logicbloxType = Utils.buildEnumProperty("chord.logicblox.export.mode", DatalogEngineType.LOGICBLOX4);
        switch (logicbloxType) {
        case LOGICBLOX3:
        case LOGICBLOX4:
            break;
        default:
            throw new ChordException("Unsupported chord.logicblox.export.mode type: " + logicbloxType);
        }
    }
    
    @Override
    public void run() {
        boolean v = Config.verbose >= 1;
        
        // make sure we're not using LB already since we'd trash the workspace
        switch (Config.datalogEngine) {
        case LOGICBLOX3:
        case LOGICBLOX4:
            throw new ChordException("Datalog engine is already a LogicBlox type (" 
                + Config.datalogEngine + "), refusing to overwrite workspace.");
        default:
            break;
        }
        
        LogicBloxUtils.initializeWorkspace();
        LogicBloxExporter exporter = new LogicBloxExporter(logicbloxType);
        
        ClassicProject project = ClassicProject.g();
        Set<String> targetNames = project.getFinishedTargetNames();
        
        List<Dom<?>> domains = new ArrayList<Dom<?>>( (targetNames.size() / 2) + 1);
        List<Rel> relations = new ArrayList<Rel>( (targetNames.size() / 2) + 1);
        
        // split the targets into domains and relations
        for (String targetName: targetNames) {
            Object target = project.getTrgt(targetName);
            if (target instanceof Dom) {
                domains.add((Dom<?>)target);
            } else if (target instanceof Rel) {
                relations.add((Rel)target);
            } else {
                Messages.warn("Unhandled target: name=%s, type=%s", targetName, target.getClass().getName());
            }
        }
        
        // domains first since relation type signatures depend on them
        for (Dom<?> domain: domains) {
            if (v) Messages.log("Exporting domain: %s", domain.getName());
            exporter.saveDomain(domain);
        }
        
        for (Rel relation: relations) {
            if (relation instanceof ProgramRel) {
                ((ProgramRel)relation).load();
            } else {
                Messages.warn("Relation %s is not a ProgramRel, attempting BDDBDDB load.", relation.getName());
                relation.loadFromBDDBDDB(Config.bddbddbWorkDirName);
            }

            if (v) Messages.log("Exporting relation: %s", relation.getName());
            exporter.saveRelation(relation);
            relation.close();
        }
    }

}
