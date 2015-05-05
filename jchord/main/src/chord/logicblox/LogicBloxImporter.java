package chord.logicblox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import chord.bddbddb.Dom;
import chord.bddbddb.Rel;
import chord.project.ChordException;
import chord.project.Config;
import chord.project.Config.DatalogEngineType;
import chord.project.Messages;
import chord.util.ProcessExecutor.StreamGobbler;
import chord.util.Utils;

/**
 * An importer for loading data from a LogicBlox workspace.
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxImporter extends LogicBloxIOBase {
    private static final Pattern rowOfIntsPattern = Pattern.compile("^\\d+(\\s+\\d+)*$");
    
    // lb query prints these around the results
    private static final Pattern headerOrFooter = 
        Pattern.compile("[/\\\\]--------------- _ ---------------[/\\\\]");

    public LogicBloxImporter() {
        super();
    }
    
    public LogicBloxImporter(DatalogEngineType engineType) {
        super(engineType);
    }
    
    /**
     * Imports a relation from the workspace.
     * <p>
     * The passed relation is emptied out and then populated by querying it's data 
     * by name from the workspace.
     * 
     * @param relation the relation to load
     * @throws ChordException if an error occurs loading the data
     */
    public void importRelation(Rel relation) {
        if( relation == null ) throw new NullPointerException("relation is null");
        relation.zero();
        
        String[] cmds = {Config.logicbloxCommand, "query", "--readonly", workspace, buildQuery(relation)};
        try {
            Process proc = Runtime.getRuntime().exec(cmds);
            new StreamGobbler(proc.getErrorStream(), System.err).start();
            Utils.close(proc.getOutputStream());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF-8"));
            String line;
            while (null != (line = reader.readLine())) {
                line = line.trim();
                if (!rowOfIntsPattern.matcher(line).matches()) {
                    if (!headerOrFooter.matcher(line).matches())
                        Messages.warn("Ignoring unexpected lb query line: %s", line);
                    continue;
                }
                int[] indexes = parseIntRow(line);
                relation.add(indexes);
            }
            Utils.close(reader);
        } catch (IOException e) {
            throw new ChordException(e);
        }
    }
    
    /**
     * Builds the query for a relation.
     * <p>
     * By example, a relation VH(v,h) over domains V and H will generate the following query:<br />
     * <code>_(id0, id1) &lt;- VH(d0, d1), V_values[id0, _] = d0, H_values[id1, _] = d1.</code>
     * 
     * @param relation the relation to query for
     * @return the LB query string
     */
    private String buildQuery(Rel relation) {
        Dom<?>[] doms = relation.getDoms();
        StringBuilder sb = new StringBuilder();
        
        String idList  = makeVarList("id", doms.length);
        String varList = getRelationVariablesList(relation);
        sb.append("_(").append(idList).append(") <- ");
        sb.append(relation.getName()).append('(').append(varList).append("), ");
        for (int i = 0, size = doms.length; i < size; ++i) {
            Dom<?> dom = doms[i];
            sb.append(dom.getName()).append("_index[d").append(i).append("] = id").append(i).append(',');
        }
        sb.setCharAt(sb.length() - 1, '.');
        
        return sb.toString();
    }
    
    private int[] parseIntRow(String line) {
        String[] parts = line.split("\\s+");
        int size = parts.length;
        int[] result = new int[size];
        for (int i = 0; i < size; ++i)
            result[i] = Integer.parseInt(parts[i], 10);
        return result;
    }
}
