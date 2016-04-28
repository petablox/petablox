package petablox.project.analyses;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        if (Config.multiPgmMode) {
        	modifyDomNames();
        	modifyRelationNames();
	}
        return metadata;
    }
    
    private void modifyRelationNames() {
		Map<String, RelSign> modConsumedRels = new HashMap<String, RelSign>();
		for (Map.Entry<String, RelSign> e : metadata.getConsumedRels().entrySet()) {
			String name = e.getKey();
			RelSign sign = e.getValue();
			name = Config.multiTag + name;
			modConsumedRels.put(name, getModifiedRelSign(sign));
		}
		metadata.setConsumedRels(modConsumedRels);
		Map<String, RelSign> modProducedRels = new HashMap<String, RelSign>();
		for (Map.Entry<String, RelSign> e : metadata.getProducedRels().entrySet()) {
			String name = e.getKey();
			RelSign sign = e.getValue();
			name = Config.multiTag + name;
			modProducedRels.put(name, getModifiedRelSign(sign));
		}
		metadata.setProducedRels(modProducedRels);
		return;
    }
    
    private RelSign getModifiedRelSign (RelSign rs) {
    	String[] domNames = rs.getDomNames();
    	String[] modDomNames = new String[domNames.length];
    	for (int j = 0; j < domNames.length; j++) {
        	modDomNames[j] = Config.multiTag + domNames[j];
        }
    	String domOrder = rs.getDomOrder();
    	String modOrder = RelSign.fixMultiPgmVarOrder(domOrder);
    	RelSign newRs = new RelSign(modDomNames, modOrder);
    	return newRs;
    }
    
    private void modifyDomNames() {
  		HashSet<String> modDoms = new HashSet<String>();
  		for (String d : metadata.getMajorDomNames()) {
  			d = Config.multiTag + d;
  			modDoms.add(d);
  		}
  		metadata.setMajorDomNames(modDoms);
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
							String nm = domName.substring(Config.multiTag.length());
							if(domNdxMap.containsKey(nm)){
								offset = offset+domNdxMap.get(nm);
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
		boolean analyze = Config.analyze;
		if(analyze){
			//Analyze mode, add the tags to the list
			String[] list = Config.tagList.split(",");
			for(String t : list){
				tags.add(t);
			}
		}
		HashSet<String> ignoredDoms = new HashSet<String>();
		ignoredDoms.add("Tags");
		ignoredDoms.add("string");
		ignoredDoms.add("int");
		ignoredDoms.add("AnnotationName");
		HashMap<String,Integer> domNdxMap = LogicBloxUtils.getDomNdxMap();
		HashMap<String,Integer> newDomNdxMap = LogicBloxExporter.getNewDomNdxMap();
		Map<String,RelSign> consumedRels = metadata.getConsumedRels();
		boolean first = true;
		try{
			BufferedReader br = new BufferedReader(new FileReader(origFile));
			PrintWriter pw = new PrintWriter(newFile);
			while(br.ready()){
				String line = br.readLine();
				//System.out.println(line);
				if(line.startsWith("//") || line.equals("")){
					if(!line.contains(":name:"))
						pw.println(line);
					continue;
				}else if(first && analyze){
					first = false;
					HashMap<String,Integer> domsMap = null;
					if(Config.populate)
						domsMap = newDomNdxMap;
					else
						domsMap = domNdxMap;
					int testIdx = 1;
					for(String domTagged : domsMap.keySet()){
						String dom = null;
						if(Config.populate)
							dom = domTagged.substring(Config.multiTag.length());
						else
							dom = domTagged;
						if(ignoredDoms.contains(dom))
							continue;
						String newDom = tags.get(0)+dom;
						
						StringBuilder type = new StringBuilder(newDom).append("(x), ")
					            .append(newDom).append("_index(x:index) -> ").append("int").append("(index).\n");  
						 type.append(newDom).append("_string[x] = s -> ").append(newDom).append("(x), string(s).\n");
						 
					     pw.print(type.toString());
					     for(int i=1;i<tags.size();i++){
					    	 String othDom = tags.get(i)+dom;
					    	 StringBuilder unionDomSB = new StringBuilder();
					    	 String testRel = tags.get(0)+"test"+testIdx;
					    	 testIdx++;
					    	 unionDomSB.append(testRel).append("(id,s) -> int(id), string(s). \n");
					    	 unionDomSB.append(testRel).append("(id,s) <- ");
					    	 unionDomSB.append(othDom+"(y), "+othDom+"_index[y] = id, "+othDom+"_string[y] = s.\n");
					    	 //unionDomSB.append("+"+newDom+"(x), +"+newDom+"_index[x] = id, +"+newDom+"_string[x] = s <-");
					    	 unionDomSB.append("+"+newDom+"(x), +"+newDom+"_index[x] = id, +"+newDom+"_string[x] = s <- ");
					    	 unionDomSB.append("+"+testRel).append("(id,s).");
					    	 
					    	 pw.println(unionDomSB.toString());
					     }
					     pw.println();
					}
					// Generate union constraints
					for(String name : consumedRels.keySet()){
						RelSign r = consumedRels.get(name);
						name = name.substring(Config.multiTag.length());
						List<String> cons = buildUnionCons(name, r, tags,ignoredDoms);
						for(String c : cons){
							pw.println(c);
							pw.println();
						}
					}
				}
				if(line.contains("->")){
					// Relation definitions
					String[] parsed = line.split("->");
					String rel = parsed[0];
					String[] relParsed = rel.split("\\(");
					String relName = tags.get(0)+relParsed[0];
					String[] domsParsed = parsed[1].split(",");
					String lineBuild = relName+"("+relParsed[1]+"-> ";
					for(int i=0;i<domsParsed.length;i++){
						String domDecl = domsParsed[i];
						domDecl = domDecl.trim();
						String domName = domDecl.substring(0, domDecl.indexOf('('));
						if(ignoredDoms.contains(domName))
							lineBuild = lineBuild+domDecl;
						else
							lineBuild = lineBuild+tags.get(0)+domDecl;
						if(i!=(domsParsed.length-1))
							lineBuild = lineBuild +",";
					}
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
							if(_indx == -1){
								sb.append(" "+temp);
								if(i!=(tokens.size()-1)){
									sb.append(",");
								}
								continue;
							}
							String domName = temp.substring(0, _indx);
							int eqIndx = temp.indexOf('=');
							String l = temp.substring(0,eqIndx);
							String offsetStr = temp.substring(eqIndx+1);
							offsetStr = offsetStr.trim();
							if(Config.populate){
								if(offsetStr.indexOf('_')==-1){
									int offset = Integer.parseInt(offsetStr);
									if(domNdxMap.containsKey(domName)){
										offset = offset+domNdxMap.get(domName);
									}
									sb.append(" ");
									if(ignoredDoms.contains(domName))
										sb.append(l+" = "+offset);
									else
										sb.append(tags.get(0)+l+" = "+offset);
								}else{
									if(ignoredDoms.contains(domName))
										sb.append(l+" = "+offsetStr);
									else
										sb.append(tags.get(0)+l+" = "+tags.get(0)+offsetStr);
								}
							}else{
								if(offsetStr.indexOf('_')==-1){
									if(ignoredDoms.contains(domName))
										sb.append(" "+temp);
									else
										sb.append(" "+tags.get(0)+temp);
								}else{
									if(ignoredDoms.contains(domName))
										sb.append(" "+temp);
									else
										sb.append(" "+tags.get(0)+l+" = "+tags.get(0)+offsetStr);
								}
							}
						}else{
							relParsed = temp.split("\\(");
							relName = relParsed[0];
							if(ignoredDoms.contains(relName)){
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
			br.close();
			pw.close();
		}catch(Exception e){
			System.out.println("Exception "+e);
			e.printStackTrace();
			throw new PetabloxException("Exception in generating multi program dlog");
		}
	}
	private List<String> buildUnionCons(String relName,RelSign sign,List<String> tags,Set<String> ignoredDoms){
		// %1$s - output tag
		// %2$s - input tag
		
		String outFormat = "%1$s",inFormat = "%2$s";
		
		String[] doms = sign.getDomNames();
		StringBuilder relDefLsb = new StringBuilder(); 
		StringBuilder relDefRsb = new StringBuilder();
		StringBuilder relRuleRsbCons = new StringBuilder();
		StringBuilder relRuleRsbUnion = new StringBuilder();
		boolean annotRel = relName.contains("Annot");
		relDefLsb.append(outFormat);
		relDefLsb.append(relName);
		relDefLsb.append('(');
		
		relDefRsb.append(" -> ");
		
		relRuleRsbUnion.append(inFormat+relName+"(");
		
		HashMap<String,Integer> domVars= new HashMap<String,Integer>();
		for(int i=0;i<doms.length;i++){
			String dom = doms[i];
			dom = dom.substring(Config.multiTag.length());
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
			relDefLsb.append(outFormat+domVar);
			
			if(ignoredDoms.contains(dom))
				relDefRsb.append(dom);
			else{
				if(!annotRel)
					relDefRsb.append(outFormat+dom);
				else
					relDefRsb.append("int");
			}
			relDefRsb.append('(');
			relDefRsb.append(outFormat+domVar);
			relDefRsb.append(')');
			
			if(ignoredDoms.contains(dom)){
				relRuleRsbCons.append(dom).append("("+outFormat+domVar+"), ");
				relRuleRsbCons.append(dom).append("("+inFormat+domVar+"), ");
				if(!(dom.equals("string")|| dom.equals("int")))
					relRuleRsbCons.append(dom).append("_index["+outFormat+domVar+"] = "+dom+"_index["+inFormat+domVar+"]");
				else
					relRuleRsbCons.append(outFormat+domVar+" = "+inFormat+domVar);
			}else{
				if(!annotRel){
					relRuleRsbCons.append(outFormat+dom).append("("+outFormat+domVar+"), ");
					relRuleRsbCons.append(inFormat+dom).append("("+inFormat+domVar+"), ");
				}
				if(!(dom.equals("string")|| dom.equals("int"))){
					if(!annotRel)
						relRuleRsbCons.append(outFormat+dom).append("_index["+outFormat+domVar+"] = "+inFormat+dom+"_index["+inFormat+domVar+"]");
					else
						relRuleRsbCons.append(outFormat+domVar+" = "+inFormat+domVar);
				}else
					relRuleRsbCons.append(outFormat+domVar+" = "+inFormat+domVar);
			}
			relRuleRsbUnion.append(inFormat+domVar);
			
			if(i!=(doms.length-1)){
				relDefLsb.append(',');
				relDefRsb.append(',');
				relRuleRsbCons.append(',');
				relRuleRsbUnion.append(',');
			}
		}
		relDefLsb.append(')');
		relRuleRsbUnion.append(')');
		
		String relDefL = relDefLsb.toString();
		String relDefR = relDefRsb.toString();
		String relRuleRCons = relRuleRsbCons.toString();
		String relRuleRUnion = relRuleRsbUnion.toString();
		
		List<String> cons = new ArrayList<String>();
		String predDecl = String.format(relDefL,tags.get(0))+String.format(relDefR, tags.get(0))+".";
		
		cons.add(predDecl);
		
		for(int i=1;i<tags.size();i++){
			String rule = String.format(relDefL,tags.get(0))+" <- "+String.format(relRuleRCons, tags.get(0),tags.get(i))+","+String.format(relRuleRUnion, tags.get(0), tags.get(i))+".";
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
    	if(!Config.multiPgmMode || (Config.multiPgmMode && !Config.generateOnly)){
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
