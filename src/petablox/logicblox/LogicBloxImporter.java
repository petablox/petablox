package petablox.logicblox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.OutDirUtils;
import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.Config.DatalogEngineType;
import petablox.util.ArraySet;
import petablox.util.ProcessExecutor;
import petablox.util.Utils;
import petablox.util.ProcessExecutor.StreamGobbler;
import petablox.util.tuple.object.Pair;

/**
 * An importer for loading data from a LogicBlox workspace.
 * 
 * @author Jake Cobb <tt>&lt;jake.cobb@gatech.edu&gt;</tt>
 */
public class LogicBloxImporter extends LogicBloxIOBase {
	private static final String INVALID_MULTIPGM_MISSING1 = "ERROR: Multiple program support: Missing information (Doms_string). Likely not a valid multipgm workspace.";
	private static final String INVALID_MULTIPGM_MISSING2 = "ERROR: Multiple program support: Missing information (domRanges). Likely not a valid multipgm workspace.";
	private static final Pattern rowOfIntsPattern = Pattern.compile("^\\d+(\\s+\\d+)*$");
    
    // lb query prints these around the results
    private static final Pattern headerOrFooter = 
        Pattern.compile("[/\\\\]--------------- _ ---------------[/\\\\]");
    private static HashMap<String,Integer> domNdxMap = null;
    
   
    public LogicBloxImporter() {
        super();
        domNdxMap = LogicBloxUtils.getDomNdxMap();
    }
    
    public LogicBloxImporter(DatalogEngineType engineType) {
        super(engineType);
        domNdxMap = LogicBloxUtils.getDomNdxMap();
    }
    
    /**
     * Imports a relation from the workspace.
     * <p>
     * The passed relation is emptied out and then populated by querying it's data 
     * by name from the workspace.
     * 
     * @param relation the relation to load
     * @throws PetabloxException if an error occurs loading the data
     */
    public void importRelation(Rel relation) {
        if( relation == null ) throw new NullPointerException("relation is null");
        relation.zero();
        
        String[] cmds = {Config.logicbloxCommand, "query", workspace, buildQuery(relation)};
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
                Dom<?>[] relDoms = relation.getDoms();
                int[] domSz = new int[relDoms.length];
                int i = 0;
                for (Dom<?> d : relDoms) {
                	if (Config.populate) {
                		String nm = d.getName().substring(Config.multiTag.length());
        	        	if (domNdxMap.containsKey(nm))
        	        		domSz[i] = domNdxMap.get(nm);
        	        	else
        	        		domSz[i] = 0;
                	} else
                		domSz[i] = 0;
                	indexes[i] = indexes[i] - domSz[i];
                	i++;
                }
                relation.add(indexes);
            }
            Utils.close(reader);
        } catch (IOException e) {
            throw new PetabloxException(e);
        }
    }
    
    /**
     * Builds the query for a relation.
     * <p>
     * By example, a relation VH(v,h) over domains V and H will generate the following query:<br />
     * <code>_(id0, id1) &lt;- VH(d0, d1), V_index[d0] = id0, H_index[d1] = id1.</code>
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
    
    public static void loadDomain(String domName, ArraySet<String> domSet) {
    	ProcessExecutor.Result result = OutDirUtils.executeCaptureWithWarnOnError(
	            Config.logicbloxCommand,
	            "print",
	            Config.logicbloxWorkspace,
	            domName + "_string"
	        );
    	if (result.getExitCode() != 0) Messages.fatal(INVALID_MULTIPGM_MISSING1);
		String op = result.getOutput();
		String[] lines = op.split("\n");
		String[] elems = new String[lines.length];
		for(String line : lines) {
			List<String> parts = Utils.tokenize(line);
			int len = parts.get(2).length();
			elems[Integer.parseInt(parts.get(1))] = parts.get(2).substring(1,len-1);
		}
		for(String s : elems) domSet.add(s);
    }
    
    public static void loadDomRangeRelation() {
    	ArraySet<String> tagASet = LogicBloxUtils.getTagASet();
    	ArraySet<String> domASet = LogicBloxUtils.getDomASet();
    	ProcessExecutor.Result result = OutDirUtils.executeCaptureWithWarnOnError(
	            Config.logicbloxCommand,
	            "print",
	            Config.logicbloxWorkspace,
	            LogicBloxUtils.DOM_RANGES
	        );
    	if (result.getExitCode() != 0) Messages.fatal(INVALID_MULTIPGM_MISSING2);
		String op = result.getOutput();
		String[] lines = op.split("\n");
		for(String l : lines) {
			List<String> parts = Utils.tokenize(l);
			String tagName = tagASet.get(Integer.parseInt(parts.get(1)));
			String domName = domASet.get(Integer.parseInt(parts.get(3)));
			int startRange = Integer.parseInt(parts.get(4));
			int endRange = Integer.parseInt(parts.get(5));
			Pair<Integer,Integer> pr = new Pair<Integer,Integer>(startRange, endRange);
			HashMap<String, ArrayList<Pair<Integer, Integer>>> perTagRange = LogicBloxUtils.domRanges.get(tagName);
			if (perTagRange == null) {
				perTagRange = new HashMap<String, ArrayList<Pair<Integer, Integer>>>();
				LogicBloxUtils.domRanges.put(tagName, perTagRange);
			}
			ArrayList<Pair<Integer, Integer>> alist = perTagRange.get(domName);
			if (alist == null) {
				alist = new ArrayList<Pair<Integer, Integer>>();
				perTagRange.put(domName, alist);
			}
			alist.add(pr);
		}
    }
    
    public static void loadSubTagRelation() {
    	ArraySet<String> tagASet = LogicBloxUtils.getTagASet();
    	ProcessExecutor.Result result = OutDirUtils.executeCaptureWithWarnOnError(
	            Config.logicbloxCommand,
	            "print",
	            Config.logicbloxWorkspace,
	            LogicBloxUtils.SUB_TAGS
	        );
    	if (result.getExitCode() != 0) return;
		String op = result.getOutput();
		String[] lines = op.split("\n");
		for(String l : lines) {
			List<String> parts = Utils.tokenize(l);
			String tagName = tagASet.get(Integer.parseInt(parts.get(1)));
			String childTag = tagASet.get(Integer.parseInt(parts.get(3)));
			LogicBloxUtils.subTags.put(tagName, childTag);		
		}
    }
}
