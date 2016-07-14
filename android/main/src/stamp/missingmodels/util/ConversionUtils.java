package stamp.missingmodels.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import shord.analyses.DomU;
import shord.analyses.DomV;
import shord.analyses.LocalVarNode;
import shord.analyses.ParamVarNode;
import shord.analyses.RetVarNode;
import shord.analyses.ThisVarNode;
import shord.analyses.VarNode;
import shord.project.ClassicProject;
import soot.Local;
import soot.SootMethod;
import stamp.analyses.DomCL;
import stamp.missingmodels.analysis.JCFLSolverRunner.RelationAdder;
import stamp.missingmodels.util.Relation.IndexRelation;
import stamp.missingmodels.util.Relation.StubIndexRelation;
import stamp.missingmodels.util.Util.MultivalueMap;
import stamp.missingmodels.util.jcflsolver.Graph;
import stamp.srcmap.Expr;
import stamp.srcmap.sourceinfo.RegisterMap;
import stamp.srcmap.sourceinfo.SourceInfo;

/*
 * This class contains code to convert to and from the
 * Shord representation and the JCFLSolver representation.
 * 
 * @author Osbert Bastani
 */
public class ConversionUtils {
	/*
	 * 
	 * DOMAIN INFORMATION:
	 * 
	 * a) Shord domains:
	 * variables = V.dom = Register
	 * methods = M.dom = jq_Method
	 * sources/sinks = SRC.dom/SINK.dom = String
	 * contexts = C.dom = Ctxt
	 * invocation quads = I.dom = Quad
	 * method arg number = Z.dom = Integer
	 * integers = K.dom = Integer
	 * 
	 * b) Stamp domains:
	 * labels (sources + sinks) = L.dom = String
	 */
	
	/*
	 * An implementation of the chord relation lookup interface.
	 */
	public static class ChordRelationAdder implements RelationAdder {
		/*
		 * Returns relations for which no Chord relation is found.
		 */
		@Override
		public Collection<String> addEdges(Graph g, StubLookup s, StubModelSet m) {
			Set<String> relationsNotFound = new HashSet<String>();
			for(int k=0; k<g.numKinds(); k++) {
				if(g.isTerminal(k)) {
					Collection<Relation> relations = getChordRelationsFor(g.kindToSymbol(k));
					if(relations.isEmpty()) {
						relationsNotFound.add(g.kindToSymbol(k));
					}
					for(Relation rel : relations) {
						rel.addEdges(g.kindToSymbol(k), g, s, m);
					}
				}
			}
			return relationsNotFound;
		}
	}

	/*
	 * The set of all relations used by the various grammars.
	 */	
	private static final MultivalueMap<String,Relation> relations = new MultivalueMap<String,Relation>();
	static {
		/*
		 * The following are points-to analysis information.
		 */
		// ref assign
		relations.add("cs_refAssign", new IndexRelation("AssignCtxt", "V", 2, 0, "V", 1, 0));
		relations.add("cs_refAssign", new IndexRelation("LoadStatCtxt", "F", 2, null, "V", 1, 0));
		relations.add("cs_refAssign", new IndexRelation("StoreStatCtxt", "V", 2, 0, "F", 1, null));

		relations.add("cs_refAssignArg", new IndexRelation("AssignArgCtxt", "V", 3, 2, "V", 1, 0));
		relations.add("cs_refAssignRet", new IndexRelation("AssignRetCtxt", "V", 3, 2, "V", 1, 0));

		// ref alloc
		relations.add("cs_refAlloc", new IndexRelation("AllocCtxt", "C", 2, null, "V", 1, 0));

		// ref load/store
		relations.add("cs_refLoad", new IndexRelation("LoadCtxt", "V", 2, 0, "V", 1, 0, 3));
		relations.add("cs_refStore", new IndexRelation("StoreCtxt", "V", 3, 0, "V", 1, 0, 2));

		// cross load/store
		relations.add("cs_primStore", new IndexRelation("StorePrimCtxt", "U", 3, 0, "V", 1, 0, 2));
		relations.add("cs_primLoad", new IndexRelation("LoadPrimCtxt", "V", 2, 0, "U", 1, 0, 3));

		// prim assign
		relations.add("cs_primAssign", new IndexRelation("AssignPrimCtxt", "U", 2, 0, "U", 1, 0));
		relations.add("cs_primAssign", new IndexRelation("LoadStatPrimCtxt", "F", 2, null, "U", 1, 0));
		relations.add("cs_primAssign", new IndexRelation("StoreStatPrimCtxt", "U", 2, 0, "F", 1, null));

		relations.add("cs_primAssignArg", new IndexRelation("AssignArgPrimCtxt", "U", 3, 2, "U", 1, 0));
		relations.add("cs_primAssignRet", new IndexRelation("AssignRetPrimCtxt", "U", 3, 2, "U", 1, 0));

		/*
		 * The following is the points-to relation, computed by BDDBDDB.
		 */
		relations.add("flowsTo", new IndexRelation("pt", "C", 2, null, "V", 1, 0));
		
		/*
		 * The following are phantom points-to relations.
		 */
		//relations.add("flowsTo", new IndexRelation("phpt", "V", 2, 3, "V", 1, 0));
		
		/*
		 * The following are taint information.
		 */

		// ref taint flow
		relations.add("cs_srcRefFlow", new IndexRelation("SrcArgFlowCtxt", "CL", 1, null, "V", 2, 0));
		relations.add("cs_srcRefFlow", new IndexRelation("SrcRetFlowCtxt", "CL", 1, null, "V", 2, 0));
		relations.add("cs_refSinkFlow", new IndexRelation("ArgSinkFlowCtxt", "V", 1, 0, "CL", 2, null));
		relations.add("cs_refRefFlow", new IndexRelation("ArgArgTransferCtxt", "V", 1, 0, "V", 2, 0));
		relations.add("cs_refRefFlow", new IndexRelation("ArgRetTransferCtxt", "V", 1, 0, "V", 2, 0));

		// prim taint flow
		relations.add("cs_srcPrimFlow", new IndexRelation("SrcRetPrimFlowCtxt", "CL", 1, null, "U", 2, 0));
		relations.add("cs_primSinkFlow", new IndexRelation("ArgSinkPrimFlowCtxt", "U", 1, 0, "CL", 2, null));
		relations.add("cs_primPrimFlow", new IndexRelation("ArgPrimRetPrimTransferCtxt", "U", 1, 0, "U", 2, 0));

		// cross taint flow
		relations.add("cs_primRefFlow", new IndexRelation("ArgPrimArgTransferCtxt", "U", 1, 0, "V", 2, 0));
		relations.add("cs_primRefFlow", new IndexRelation("ArgPrimRetTransferCtxt", "U", 1, 0, "V", 2, 0));
		relations.add("cs_refPrimFlow", new IndexRelation("ArgRetPrimTransferCtxt", "V", 1, 0, "U", 2, 0));

		// ref stub taint flow
		relations.add("cs_passThroughStub", new StubIndexRelation("ArgArgTransferCtxtStub", "V", 1, 0, "V", 2, 0, 3, 4, 5));
		relations.add("cs_passThroughStub", new StubIndexRelation("ArgRetTransferCtxtStub", "V", 1, 0, "V", 2, 0, 3, 4));

		// prim stub taint flow
		relations.add("cs_primPassThroughStub", new StubIndexRelation("ArgPrimRetPrimTransferCtxtStub", "U", 1, 0, "U", 2, 0, 3, 4));
		relations.add("cs_refPrimFlowStub", new StubIndexRelation("ArgRetPrimTransferCtxtStub", "V", 1, 0, "U", 2, 0, 3, 4));

		// cross stub taint flow
		relations.add("cs_primRefFlowStub", new StubIndexRelation("ArgPrimArgTransferCtxtStub", "U", 1, 0, "V", 2, 0, 3, 4, 5));
		relations.add("cs_primRefFlowStub", new StubIndexRelation("ArgPrimRetTransferCtxtStub", "U", 1, 0, "V", 2, 0, 3, 4));

		/*
		 * The following are for source/sink inference purposes.
		 */

		/*
		// ref stub source/sink taint flow
		relations.add("cs_srcFlowStub", new StubIndexRelation("cfl_cs_srcArgFlowStub", "M", 2, 3, "V", 1, 0, 2, 3));
		relations.add("cs_srcFlowStub", new StubIndexRelation("cfl_cs_srcRetFlowStub", "M", 2, null, "V", 1, 0, 2));

		relations.add("cs_sinkFlowStub", new StubIndexRelation("cfl_cs_sinkFlowStub", "V", 1, 0, "M", 2, 3, 2, 3));

		// prim stub source/sink taint flow
		relations.add("cs_primSrcFlowStub", new StubIndexRelation("cfl_cs_primSrcFlowStub", "M", 2, null, "U", 1, 0, 2));
		relations.add("cs_primSinkFlowStub", new StubIndexRelation("cfl_cs_primSinkFlowStub", "U", 1, 0, "M", 2, 3, 2, 3));

		// ref source/sink taint flow
		relations.add("cs_srcRefFlowNew", new StubIndexRelation("cfl_cs_fullSrcArgFlow_new", "M", 3, 4, "V", 2, 0, 3, 4));
		relations.add("cs_srcRefFlowNew", new StubIndexRelation("cfl_cs_fullSrcRetFlow_new", "M", 3, null, "V", 2, 0, 3));

		relations.add("cs_refSinkFlowNew", new StubIndexRelation("cfl_cs_fullSinkFlow_new", "V", 1, 0, "M", 3, 4, 3, 4));

		// prim source/sink taint flow
		relations.add("cs_srcPrimFlowNew", new StubIndexRelation("cfl_cs_primFullSrcFlow_new", "M", 3, null, "U", 2, 0, 3));
		relations.add("cs_primSinkFlowNew", new StubIndexRelation("cfl_cs_primFullSinkFlow_new", "U", 1, 0, "M", 3, 4, 3, 4));
		*/
		
		/*
		 * The following are relations for the new taint analysis.
		 */
		
		// source annotations: src2RefT, src2PrimT
		relations.add("src2RefT", new IndexRelation("Src2RefT", "CL", 1, null, "V", 2, 0));
		relations.add("src2PrimT", new IndexRelation("Src2PrimT", "CL", 1, null, "U", 2, 0));		
		
		// sink annotations: sink2RefT, sink2PrimT, sinkF2RefF, sinkF2PrimF
		relations.add("sink2RefT", new IndexRelation("Sink2RefT", "CL", 1, null, "V", 2, 0));
		relations.add("sink2PrimT", new IndexRelation("Sink2PrimT", "CL", 1, null, "U", 2, 0));
		relations.add("sinkF2RefF", new IndexRelation("SinkF2RefF", "CL", 1, null, "V", 2, 0));
		relations.add("sinkF2PrimF", new IndexRelation("SinkF2PrimF", "CL", 1, null, "U", 2, 0));
		
		// transfer annotations: ref2RefT, ref2PrimT, prim2RefT, prim2PrimT
		relations.add("ref2RefT", new IndexRelation("Ref2RefT", "V", 1, 0, "V", 2, 0));
		relations.add("ref2PrimT", new IndexRelation("Ref2PrimT", "V", 1, 0, "U", 2, 0));
		relations.add("prim2RefT", new IndexRelation("Prim2RefT", "U", 1, 0, "V", 2, 0));
		relations.add("prim2PrimT", new IndexRelation("Prim2PrimT", "U", 1, 0, "U", 2, 0));
		
		// flow annotations: ref2RefF, ref2PrimF, prim2RefF, prim2PrimF
		relations.add("ref2RefF", new IndexRelation("Ref2RefF", "V", 1, 0, "V", 2, 0));
		relations.add("ref2PrimF", new IndexRelation("Ref2PrimF", "V", 1, 0, "U", 2, 0));
		relations.add("prim2RefF", new IndexRelation("Prim2RefF", "U", 1, 0, "V", 2, 0));
		relations.add("prim2PrimF", new IndexRelation("Prim2PrimF", "U", 1, 0, "U", 2, 0));
		
		// pt: pt, fptArr
		relations.add("pt", new IndexRelation("pt", "V", 1, 0, "O", 2, null));
		relations.add("fptArr", new IndexRelation("fptArr", "O", 0, null, "O", 1, null));
		
		// field: fpt
		relations.add("fpt", new IndexRelation("fpt", "O", 0, null, "O", 2, null, 1));

		// helper: assignPrimCtxt, assignPrimCCtxt, loadPrimCtxtArr, storePrimCtxtArr
		relations.add("assignPrimCtxt", new IndexRelation("AssignPrimCtxt", "U", 1, 0, "U", 2, 0));
		relations.add("assignPrimCCtxt", new IndexRelation("AssignPrimCCtxt", "U", 1, 0, "U", 3, 2));
		relations.add("loadPrimCtxtArr", new IndexRelation("LoadPrimCtxtArr", "U", 1, 0, "V", 2, 0));
		relations.add("storePrimCtxtArr", new IndexRelation("StorePrimCtxtArr", "V", 1, 0, "U", 2, 0));

		// field helper: loadPrimCtxt, loadStatPrimCtxt, storePrimCtxt, storeStatPrimCtxt
		relations.add("loadPrimCtxt", new IndexRelation("LoadPrimCtxt", "U", 1, 0, "V", 2, 0, 3));
		relations.add("storePrimCtxt", new IndexRelation("StorePrimCtxt", "V", 1, 0, "U", 3, 0, 2));
		relations.add("loadStatPrimCtxt", new IndexRelation("LoadStatPrimCtxt", "U", 1, 0, "F", 2, null));
		relations.add("storeStatPrimCtxt", new IndexRelation("StoreStatPrimCtxt", "F", 1, null, "U", 2, 0));

		// ref stub taint flow
		relations.add("refArg2RefArgTStub", new StubIndexRelation("RefArg2RefArgTStub", "V", 1, 0, "V", 2, 0, 3, 4, 5));
		relations.add("refArg2RefRetTStub", new StubIndexRelation("RefArg2RefRetTStub", "V", 1, 0, "V", 2, 0, 3, 4));
		
		// cross stub taint flow
		relations.add("primArg2RefArgTStub", new StubIndexRelation("PrimArg2RefArgTStub", "U", 1, 0, "V", 2, 0, 3, 4, 5));
		relations.add("primArg2RefRetTStub", new StubIndexRelation("PrimArg2RefRetTStub", "U", 1, 0, "V", 2, 0, 3, 4));
		
		// prim stub taint flow
		relations.add("refArg2PrimRetTStub", new StubIndexRelation("RefArg2PrimRetTStub", "V", 1, 0, "U", 2, 0, 3, 4));
		relations.add("primArg2PrimRetTStub", new StubIndexRelation("PrimArg2PrimRetTStub", "U", 1, 0, "U", 2, 0, 3, 4));
		
		// partial pt
		relations.add("preFlowsTo", new IndexRelation("PreFlowsTo", "O", 1, null, "V", 2, 0));
		relations.add("postFlowsTo", new IndexRelation("PostFlowsTo", "V", 1, 0, "V", 3, 2));
		relations.add("midFlowsTo", new IndexRelation("MidFlowsTo", "V", 1, 0, "V", 3, 2));
		
		// partial pt helper
		relations.add("storeCtxt", new IndexRelation("StoreCtxt", "V", 1, 0, "V", 2, 0, 3));
		relations.add("storeArrCtxt", new IndexRelation("StoreArrCtxt", "V", 1, 0, "V", 2, 0));
		
	}
	
	/*
	 * Returns the Shord relations associated with a given
	 * JCFLSolver relation. 
	 */
	private static Set<Relation> getChordRelationsFor(String relation) {
		return relations.get(relation);		
	}

	/*
	 * A cache of register maps.
	 */
	private static Map<String,RegisterMap> registerMaps = new HashMap<String,RegisterMap>();

	/*
	 * Gets the node info and concatenates into a String.
	 */
	public static String getNodeInfo(SourceInfo sourceInfo, String node) {
		String[] tokens = getNodeInfoTokens(sourceInfo, node);
		return tokens[0] + (tokens.length >= 2 ? tokens[1] : ""); /*+ (tokens.length == 3 ? "_" + tokens[2] : "")*/
	}

	/*
	 * Returns source information about the node.
	 * a) If the node name is in DomCL (starts with CL), then
	 * maps "CL1" -> ["CL", 1].
	 * b) If the node name is in DomV (starts with V), then
	 * returns [hyper link to containing method, hyper link
	 * to variable, contextId]. 
	 * c) If the node name is in DomU, then similar to (b).
	 * d) In any other case, just returns the given string in
	 * a length one array.
	 * NOTE: if source information for the variable can't be
	 * found, then the variableId is returned in place of
	 * the second hyper link.
	 */
	public static String[] getNodeInfoTokens(SourceInfo sourceInfo, String node) {
		try {
			// STEP 1: tokenize the node name
			String[] tokens = tokenizeNodeName(node);
			
			// STEP 2: parse labels, reference variables, and primitive variables
			if(tokens[0].equals("CL")) {
				// STEP 2a: if it is a label, then get the string
				DomCL dom = (DomCL)ClassicProject.g().getTrgt("CL");
				tokens[1] = dom.get(Integer.parseInt(tokens[1])).val0;
			} else if(tokens[0].equals("V") || tokens[0].equals("U")) {
				System.out.println("here3");
				// STEP 2b: if it is a variable, then get the variable and method information
				
				// get the register from the domain
				VarNode register;
				if(tokens[0].equals("V")) {
					DomV dom = (DomV)ClassicProject.g().getTrgt(tokens[0].toUpperCase());
					register = dom.get(Integer.parseInt(tokens[1]));
				} else {
					DomU dom = (DomU)ClassicProject.g().getTrgt(tokens[0].toUpperCase());
					register = dom.get(Integer.parseInt(tokens[1]));
				}
	
				// look up the method and local from the register
				SootMethod method = null;
				Local local = null;
				if(register instanceof LocalVarNode) {
					LocalVarNode localRegister = (LocalVarNode)register;
					local = localRegister.local;
					method = localRegister.meth;
				} else if(register instanceof ThisVarNode) {
					ThisVarNode thisRegister = (ThisVarNode)register;
					method = thisRegister.method;
				} else if(register instanceof ParamVarNode) {
					ParamVarNode paramRegister = (ParamVarNode)register;
					method = paramRegister.method;
				} else if(register instanceof RetVarNode) {
					RetVarNode retRegister = (RetVarNode)register;
					method = retRegister.method;
				}
	
				// HTML hyper link to the method
				String sourceFileName = method == null ? "" : sourceInfo.filePath(method.getDeclaringClass());
				int methodLineNum = sourceInfo.methodLineNum(method);
	
				String methStr = "<a onclick=\"showSource('" + sourceFileName + "','false','" + methodLineNum + "')\">" + "[" + method.getName() + "]</a> ";
	
				// HTML hyper link to the register
				RegisterMap regMap = getRegisterMap(sourceInfo, method);
				Set<Expr> locations = regMap.srcLocsFor(local);
				Integer registerLineNum = null;
				String text = null;
				if(locations != null) {
					for(Expr location : locations) {
						if(location.start() >= 0 && location.length() >= 0 && location.text() != null) {
							registerLineNum = location.line();
							text = location.text();
							break;
						}
					}
				}
	
				// store the links in the tokens and return
				if(registerLineNum != null) {
					tokens[0] = methStr;
					tokens[1] = "<a onclick=\"showSource('" + sourceFileName + "','false','" + registerLineNum + "')\">" + text + "</a>";
				} else {
					tokens[0] = methStr+tokens[0];
				}
	
				// if the context exists, get the context
				/*
				if(tokens.length == 3) {
					DomC domC = (DomC)ClassicProject.g().getTrgt("C");
					tokens[2] = domC.toUniqueString(Integer.parseInt(tokens[2]));
				}
				*/
			}
			return tokens;
		} catch(Exception e) {
			System.out.println("Error parsing node \"" + node + "\"!");
			e.printStackTrace();
			String[] tokens = {node};
			return tokens;
		}
	}

	/*
	 * Returns a register map, caching them as they are requested.
	 */
	private static RegisterMap getRegisterMap(SourceInfo sourceInfo, SootMethod method) {
		RegisterMap registerMap = registerMaps.get(method.toString());
		if(registerMap == null) {
			registerMap = sourceInfo.buildRegMapFor(method);
			registerMaps.put(method.toString(), registerMap);
		}
		return registerMap;
	}

	/*
	 * This function tokenizes graph node names in one of two ways:
	 * a) "V1_2" -> ["V", 1, 2]
	 * b) "CL1" -> ["CL", 1]
	 */
	private static String[] tokenizeNodeName(String node) {
		String[] result;
		String dom = node.replaceAll("[^a-zA-Z]", "");
		if(!node.startsWith(dom)) {
			throw new RuntimeException("Invalid node name " + node + "!");
		}
		if(node.contains("_")) {
			result = new String[3];
			String[] tokens = node.split("_");
			if(tokens.length != 2) {
				System.out.println("Invalid node name " + node + "!");
			}
			result[1] = tokens[0].substring(dom.length());
			result[2] = tokens[1];
		} else {
			result = new String[2];
			result[1] = node.substring(dom.length());
		}
		result[0] = dom;
		return result;
	}

}
