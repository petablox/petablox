package petablox.project.analyses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import CnCHJ.api.ItemCollection;
import petablox.bddbddb.BDDBDDBParser;
import petablox.bddbddb.RelSign;
import petablox.bddbddb.Solver;
import petablox.core.DatalogMetadata;
import petablox.core.IDatalogParser;
import petablox.logicblox.LogicBloxExporter;
import petablox.logicblox.LogicBloxParser;
import petablox.logicblox.LogicBloxUtils;
import petablox.project.PetabloxException;
import petablox.project.Config;
import petablox.project.IDataCollection;
import petablox.project.IStepCollection;
import petablox.project.Messages;
import petablox.project.ModernProject;
import petablox.project.Config.DatalogEngineType;
import petablox.util.Utils;

/**
 * Generic implementation of a Dlog task (a program analysis expressed in Datalog and
 * solved using BDD-based solver <a href="http://bddbddb.sourceforge.net/">bddbddb</a>).
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class DlogAnalysis extends JavaAnalysis {
    
    private DatalogEngineType datalogEngine;
    private DatalogMetadata metadata;
    
    private IDatalogParser parser;
    
    public DlogAnalysis() { 
        this(Config.datalogEngine); 
    }
    
    public DlogAnalysis(DatalogEngineType engineType) {
        if( engineType == null ) throw new NullPointerException("engineType is null");
        this.datalogEngine = engineType;
        switch (engineType) {
        case BDDBDDB:
            parser = new BDDBDDBParser();
            break;
        case LOGICBLOX3:
        case LOGICBLOX4:
            parser = new LogicBloxParser();
            break;
        default:
            throw new PetabloxException("Unhandled datalog engine type: " + Config.datalogEngine);
        }
    }
    
    /**
     * Provides the name of this Datalog analysis.
     * It is specified via a line of the form "# name=..." in the file containing the analysis.
     * 
     * @return    The name of this Datalog analysis.
     */
    public String getDlogName() {
        return metadata != null ? metadata.getDlogName() : null;
    }
    /**
     * Provides the file containing this Datalog analysis.
     * 
     * @return    The file containing this Datalog analysis.
     */
    public String getFileName() {
        return metadata != null ? metadata.getFileName() : null;
    }
    
    public DatalogMetadata parse(String fileName) throws IOException {
        metadata = parser.parseMetadata(new File(fileName));
        if (Config.populate)
        	modifyRelationNames();
        return metadata;
    }
    
    private void modifyRelationNames() {
		Map<String, RelSign> modConsumedRels = new HashMap<String, RelSign>();
		for (Map.Entry<String, RelSign> e : metadata.getConsumedRels().entrySet()) {
			String name = e.getKey();
			RelSign sign = e.getValue();
			name = Config.multiTag + name;
			modConsumedRels.put(name, sign);
		}
		metadata.setConsumedRels(modConsumedRels);
		Map<String, RelSign> modProducedRels = new HashMap<String, RelSign>();
		for (Map.Entry<String, RelSign> e : metadata.getProducedRels().entrySet()) {
			String name = e.getKey();
			RelSign sign = e.getValue();
			name = Config.multiTag + name;
			modProducedRels.put(name, sign);
		}
		metadata.setProducedRels(modProducedRels);
		return;
    }
    
    /*private void error(String errMsg) {
        Messages.log("ERROR: DlogAnalysis: " + fileName + ": line " + lineNum + ": " + errMsg);
        hasNoErrors = false;
    }*/

	private void multiPrgmDlogGenBDD(String origFile, String newFile){
		// List of tags to be used in the dlog
		// 0th tag is always the tag for the output
		List<String> tags = new ArrayList<String>();
		tags.add(Config.multiTag);
		HashMap<String,Integer> domNdxMap = LogicBloxUtils.getDomNdxMap();
		HashMap<String,Integer> newDomNdxMap = LogicBloxExporter.getNewDomNdxMap();
		try{
			BufferedReader br = new BufferedReader(new FileReader(origFile));
			PrintWriter pw = new PrintWriter(newFile);
			while(br.ready()){
				String line = br.readLine();
				//System.out.println(line);
				if(line.equals(""))
					continue;
				if(line.startsWith("#")||line.startsWith(".")){
					pw.println(line);
					continue;
				}
				if(!line.contains(":-")){
					// Relation definitions
					String[] parsed = line.split(" ");
					String rel = parsed[0];
					String[] relParsed = rel.split("\\(");
					String relName = tags.get(0)+relParsed[0];
					String lineBuild = relName+"("+relParsed[1]+" ";
					if(parsed.length >1)
						lineBuild = lineBuild + parsed[1];
					pw.println(lineBuild);
					// TODO : for analyze phase, add similar lines for all tags
				}else if(line.contains(":-")){
					StringBuilder sb = new StringBuilder();

					String[] parsed = line.split(":-");
					String rel = parsed[0].trim();
					String[] relParsed = rel.split("\\(");
					String relName = tags.get(0)+relParsed[0];

					sb.append(relName+"("+relParsed[1]+" ");
					sb.append(":-");
					List<String> tokens = parseRule(parsed[1]);
					for(int i=0;i<tokens.size();i++){
						String token = tokens.get(i);
						if(token.equals(""))
							continue;
						String temp = token;
						temp = temp.trim();
						if(temp.contains("=")){
							int _indx = temp.indexOf('_');
							String domName = temp.substring(0, _indx);
							int eqIndx = temp.indexOf('=');
							String offsetStr = temp.substring(eqIndx+1);
							offsetStr = offsetStr.trim();
							int offset = Integer.parseInt(offsetStr);
							if(domNdxMap.containsKey(domName)){
								offset = offset+domNdxMap.get(domName);
							}
							String l = temp.substring(0,eqIndx);
							sb.append(" ");
							sb.append(l+" = "+offset);
						}else{
							relParsed = temp.split("\\(");
							relName = relParsed[0];
							if(newDomNdxMap.containsKey(relName)){
								sb.append(" "+temp);
							}else{
								relName = tags.get(0)+relParsed[0];
								sb.append(" "+relName+"("+relParsed[1]);
							}
						}
						if(i!=(tokens.size()-1)){
							sb.append(",");
						}
					}
					sb.append('.');
					pw.println(sb.toString());
				}else{
					pw.println(line);
				}
			}
			br.close();
			pw.close();
		}catch(Exception e){
			System.out.println("Exception "+e);
			throw new PetabloxException("Exception in generating multi program dlog");
		}
	}

	private List<String> parseRule(String rule){
		List<String> tokens = new ArrayList<String>();
		int state = 0;// 0 - normal, 2- ( encountered, 1 - ) encountered
		int start = 0;
		for(int i=0;i<rule.length();i++){
			if(rule.charAt(i)== ' ')
				continue;
			if(rule.charAt(i)=='('){
				state = 2;
			}else if(rule.charAt(i)==')'){
				state = 1;
			}else if(rule.charAt(i)==',' && state < 2){
				String temp = rule.substring(start,i);
				tokens.add(temp);
				start = i+1;
			}else if(rule.charAt(i)=='.'){
				String temp = rule.substring(start,i);
				tokens.add(temp);
			}
		}
		return tokens;
	}
	private void multiPrgmDlogGenLogic(String origFile, String newFile){
		// List of tags to be used in the dlog
		// 0th tag is always the tag for the output
		List<String> tags = new ArrayList<String>();
		tags.add(Config.multiTag);
		boolean analyze = !Config.analyze.equals("");
		if(analyze){
			//Analyze mode, add the tags to the list
			String[] list = Config.analyze.split(",");
			for(String t : list){
				tags.add(t+"_");
			}
		}
		HashMap<String,Integer> domNdxMap = LogicBloxUtils.getDomNdxMap();
		HashMap<String,Integer> newDomNdxMap = LogicBloxExporter.getNewDomNdxMap();
		Map<String,RelSign> consumedRels = metadata.getConsumedRels();
		try{
			BufferedReader br = new BufferedReader(new FileReader(origFile));
			PrintWriter pw = new PrintWriter(newFile);
			while(br.ready()){
				String line = br.readLine();
				//System.out.println(line);
				if(line.startsWith("//")){
					pw.println(line);
					continue;
				}
				if(line.contains("->")){
					// Relation definitions
					String[] parsed = line.split("->");
					String rel = parsed[0];
					String[] relParsed = rel.split("\\(");
					String relName = tags.get(0)+relParsed[0];
					String lineBuild = relName+"("+relParsed[1]+"->"+parsed[1];
					pw.println(lineBuild);
				}else if(line.contains("<-")){
					StringBuilder sb = new StringBuilder();

					String[] parsed = line.split("<-");
					String rel = parsed[0].trim();
					String[] relParsed = rel.split("\\(");
					String relName = tags.get(0)+relParsed[0];

					sb.append(relName+"("+relParsed[1]+" ");
					sb.append("<-");
					List<String> tokens = parseRule(parsed[1]);
					for(int i=0;i<tokens.size();i++){
						String token = tokens.get(i);
						if(token.equals(""))
							continue;
						String temp = token;
						temp = temp.trim();
						if(temp.contains("=")){
							int _indx = temp.indexOf('_');
							String domName = temp.substring(0, _indx);
							int eqIndx = temp.indexOf('=');
							String offsetStr = temp.substring(eqIndx+1);
							offsetStr = offsetStr.trim();
							int offset = Integer.parseInt(offsetStr);
							if(domNdxMap.containsKey(domName)){
								offset = offset+domNdxMap.get(domName);
							}
							String l = temp.substring(0,eqIndx);
							sb.append(" ");
							sb.append(l+" = "+offset);
						}else if(temp.contains("<") || temp.contains(">")){
							sb.append(" "+temp);
						}else{
							relParsed = temp.split("\\(");
							relName = relParsed[0];
							if(newDomNdxMap.containsKey(relName) || domNdxMap.containsKey(relName)){
								sb.append(" "+temp);
							}else{
								relName = tags.get(0)+relParsed[0];
								sb.append(" "+relName+"("+relParsed[1]);
							}
						}
						if(i!=(tokens.size()-1)){
							sb.append(",");
						}
					}
					sb.append('.');
					pw.println(sb.toString());
				}
			}
			// Generate union constraints
			if(analyze){
				for(String name : consumedRels.keySet()){
					RelSign r = consumedRels.get(name);
					List<String> cons = buildUnionCons(name, r, tags);
					for(String c : cons){
						pw.println(c);
					}
				}
			}
			br.close();
			pw.close();
		}catch(Exception e){
			System.out.println("Exception "+e);
			throw new PetabloxException("Exception in generating multi program dlog");
		}
	}
	private List<String> buildUnionCons(String relName,RelSign sign,List<String> tags){
		String[] doms = sign.getDomNames();
		StringBuilder relDefLsb = new StringBuilder();
		StringBuilder relDefRsb = new StringBuilder();
		//relDefL.append(tags.get(0));
		relDefLsb.append(relName);
		relDefLsb.append('(');
		
		relDefRsb.append(" -> ");
		
		HashMap<String,Integer> domVars= new HashMap<String,Integer>();
		for(int i=0;i<doms.length;i++){
			String dom = doms[i];
			for(int j=0;j<dom.length();j++){
				if(Character.isDigit(dom.charAt(j))){
					dom = dom.substring(0, j);
					break;
				}
			}
			if(!domVars.containsKey(dom))
				domVars.put(dom, -1);
			String domVar = dom.toLowerCase();
			int indx = domVars.get(dom)+1;
			domVar = domVar+indx;
			domVars.put(dom, indx);
			relDefLsb.append(domVar);
			
			relDefRsb.append(dom);
			relDefRsb.append('(');
			relDefRsb.append(domVar);
			relDefRsb.append(')');
			
			if(i!=(doms.length-1)){
				relDefLsb.append(',');
				relDefRsb.append(',');
			}
		}
		relDefLsb.append(')');
		String relDefL = relDefLsb.toString();
		String relDefR = relDefRsb.toString();
		
		List<String> cons = new ArrayList<String>();
		String predDecl = tags.get(0)+relDefL+relDefR+".";
		
		cons.add(predDecl);
		
		for(int i=1;i<tags.size();i++){
			String rule = tags.get(0)+relDefL+" <- "+tags.get(i)+relDefL+".";
			cons.add(rule);
		}
		
		
		return cons;
		
	}
	private void multiPrgmDlogGen(){
		if(!Config.multiPgmMode)
			return;
		String origFile = metadata.getFileName();
		int lastSep = 0;
		for(int i=0;i<origFile.length();i++){
			if(origFile.charAt(i)==File.separatorChar){
				lastSep = i;
			}
		}
		String path = origFile.substring(0,lastSep);
		String fileName = origFile.substring(lastSep+1);
		String[] nameExt = fileName.split("\\.");
		String newFile = path+File.separator+nameExt[0]+"_"+Config.multiTag+"."+nameExt[1];
		switch (datalogEngine) {
	    case BDDBDDB:
	        //multiPrgmDlogGenBDD(origFile, newFile);
	        break;
	    case LOGICBLOX3:
	    case LOGICBLOX4:
	        multiPrgmDlogGenLogic(origFile, newFile);
	        break;
	    default:
	        throw new PetabloxException("FIXME: Unhandled datalog engine type: " + datalogEngine);
	    }
		metadata.setFileName(newFile);
	}

    /**
     * Executes this Datalog analysis.
     */
    public void run() {
    	multiPrgmDlogGen();
        switch (datalogEngine) {
        case BDDBDDB:
            Solver.run(metadata.getFileName());
            break;
        case LOGICBLOX3:
        case LOGICBLOX4:
            if (Config.verbose >= 1)
                Messages.log("Adding block from: %s", metadata.getFileName());
            LogicBloxUtils.addBlock(new File(metadata.getFileName()));
            break;
        default:
            throw new PetabloxException("FIXME: Unhandled datalog engine type: " + datalogEngine);
        }
    }

    public void run(Object ctrl, IStepCollection sc) {
        ModernProject p = ModernProject.g();
        Object[] consumes = p.runPrologue(ctrl, sc);
        List<ProgramDom> allDoms = new ArrayList<ProgramDom>();
        for (Object o : consumes) {
            if (o instanceof ProgramDom)
                allDoms.add((ProgramDom) o);
        }
        run();
        List<IDataCollection> pdcList = sc.getProducedDataCollections();
        for (IDataCollection pdc : pdcList) {
            ItemCollection pic = pdc.getItemCollection();
            String relName = pdc.getName();
            RelSign sign = p.getSign(relName);
            String[] domNames = sign.getDomNames();
            ProgramDom[] doms = new ProgramDom[domNames.length];
            for (int i = 0; i < domNames.length; i++) {
                String domName = Utils.trimNumSuffix(domNames[i]);
                for (ProgramDom dom : allDoms) {
                    if (dom.getName().equals(domName)) {
                        doms[i] = dom;
                        break;
                    }
                }
                assert (doms[i] != null);
            }
            ProgramRel rel = new ProgramRel();
            rel.setName(relName);
            rel.setSign(sign);
            rel.setDoms(doms);
            pic.Put(ctrl, rel);
        }
    }

    /**
     * Provides the names of all domains of relations consumed/produced by this Datalog analysis.
     * 
     * @return    The names of all domains of relations consumed/produced by this Datalog analysis.
     */
    public Set<String> getDomNames() {
        return metadata != null ? metadata.getMajorDomNames() : null;
    }
    /**
     * Provides the names and signatures of all relations consumed by this Datalog analysis.
     * 
     * @return    The names and signatures of all relations consumed by this Datalog analysis.
     */
    public Map<String, RelSign> getConsumedRels() {
        return metadata != null ? metadata.getConsumedRels() : null;
    }
    /**
     * Provides the names and signatures of all relations produced by this Datalog analysis.
     * 
     * @return    The names and signatures of all relations produced by this Datalog analysis.
     */
    public Map<String, RelSign> getProducedRels() {
        return metadata != null ? metadata.getProducedRels() : null;
    }
}
