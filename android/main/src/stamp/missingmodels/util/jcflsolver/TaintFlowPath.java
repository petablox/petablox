package stamp.missingmodels.util.jcflsolver;

import java.io.*;
import java.util.*;

public class TaintFlowPath
{
	private class ValueRef extends Value
	{
		Map<String,Value> fieldVals = new HashMap();

		Value getFld(String fldName)
		{
			Value f = fieldVals.get(fldName);
			if(f == null){
				f = newValue(false);
				fieldVals.put(fldName, f);
			}
			return f;
		}
		
		void setFld(String fldName, Value f)
		{
			fieldVals.put(fldName, f);
		}
	}
	
	private static class Value
	{
		static int count = 0;
		
		String name;
		Value passThroughTarget;
		boolean stubPassThrough;
		List<Edge> matchedEdges;

		Value()
		{
			this("val"+count++);
		}

		Value(String name)
		{
			setName(name);
		}
		
		void setName(String name)
		{
			this.name = name;
		}
		
		Value setPassThroughTarget(Value target, boolean stubEdge)
		{
			this.passThroughTarget = target;
			this.stubPassThrough = stubEdge;
			return target;
		}
		
		void setMatchedEdges(List<Edge> edges)
		{
			this.matchedEdges = edges;
		}
	} 

	private static class Edge
	{
		String type;
		String src;
		String tgt;
		String lab;

		Edge(String edgeStr)
		{
			String[] tokens = edgeStr.split(" ");
			type = tokens[0];
			src = tokens[1];
			tgt = tokens[2];
			lab = tokens.length > 3 ? tokens[3] : null;
		}
	}

	private Value srcValue;
	private Value value;
	private List<Edge> allEdges = new LinkedList();
	private List<Edge> matchedEdges;
	
	private Value newValue(boolean refType)
	{
		Value ret;
		if(refType)
			ret = new ValueRef();
		else
			ret = new Value();
		return ret;
	}

	private Value newValue(boolean refType, String name)
	{
		Value ret;
		if(refType)
			ret = new ValueRef();
		else
			ret = new Value(name);
		return ret;
	}

	void processEdge(String edgeStr)
	{
		System.out.println("//"+edgeStr);

		Edge edge = new Edge(edgeStr);
		allEdges.add(edge);
		matchedEdges.add(edge);

		String type = edge.type;

		if(type.equals("cs_primAssign")){
		} 
		else if(type.equals("cs_primLoad")){
			value = ((ValueRef) value).getFld(edge.lab);
		} 
		else if(type.equals("cs_primStore")){
			//b.f = a, a -> b
			ValueRef newValue = (ValueRef) newValue(true);
			newValue.setFld(edge.lab, value);
			value = newValue;
		} 
		else if(type.equals("cs_refStoreBar")){
			//b.f = a, b -> a
			ValueRef newValue = (ValueRef) newValue(true);			
			((ValueRef) value).setFld(edge.lab, newValue);
			value = newValue;
		} 
		else if(type.equals("cs_srcPrimFlow")){
			srcValue = newValue(false, edge.src);
			value = srcValue.setPassThroughTarget(newValue(false), false);
		}
		else if(type.equals("cs_refSinkFlow")){
			Value sinkValue = newValue(false, edge.tgt);
			value.setPassThroughTarget(sinkValue, false);
		}
		else if(type.equals("cs_primRefFlow")){
			value = value.setPassThroughTarget(newValue(true),false);
		}
		else if(type.equals("cs_refPrimFlow")){
			//create a new prim value
			value = value.setPassThroughTarget(newValue(false),false);
		}
		else if(type.equals("cs_primPrimFlow")){
			//create a new prim value
			value = value.setPassThroughTarget(newValue(false),false);
		}
		else if(type.equals("cs_primRefFullFlowStub")){
			value = value.setPassThroughTarget(newValue(true),true);
		}
		else if(type.equals("cs_fullPassThroughStub")){
			value = value.setPassThroughTarget(newValue(true),true);
		}
		else if(type.equals("cs_refRefFlow")){
			value = value.setPassThroughTarget(newValue(true),false);
		}
		else if(type.equals("FlowsToBar")){
			//v -> o
			value.setName(edge.tgt);
		}
		else if(type.equals("FlowsTo")){
		}
		else
			throw new RuntimeException("unhandled "+edgeStr);
		
		//System.out.println("** "+ value.name);
	}

	void printGraph()
	{
		System.out.println("digraph G{");
		List<Value> path = new LinkedList();
		Value val = srcValue;
		while(val != null){
			path.add(val);
			Value newVal = null;
			if(val instanceof ValueRef){
				for(Map.Entry<String,Value> entry : ((ValueRef) val).fieldVals.entrySet()){
					if(newVal != null) throw new RuntimeException("multiple outgoing edge!");
					newVal = entry.getValue();
					System.out.println(val.name + " -> " +newVal.name+" [label="+entry.getKey()+"];");
				}
			}
			if(val.passThroughTarget != null){
				if(newVal != null) throw new RuntimeException("multiple outgoing edge!");
				newVal = val.passThroughTarget;
				System.out.println(val.name + " -> " + newVal.name + " [style=dotted"+(val.stubPassThrough ? ",color=red" : "")+"];");
			}
			val = newVal;
		}
		/*
		for(Value val : allValues){
			//if(val instanceof ValueRef){
			//	for(Map.Entry<String,Value> entry : ((ValueRef) val).fieldVals.entrySet()){
			//		System.out.println(val.name + " -> " +entry.getValue().name+" [label="+entry.getKey()+"];");
			//	}
			//}
			if(val.passThroughTarget != null){
				System.out.println(val.name + " -> " + val.passThroughTarget.name + " [style=dotted"+(val.stubPassThrough ? ",color=red" : "")+"];");
			}
		}
		*/
		System.out.println("}");

		map();
	}

	private void map()
	{
		for(Edge edge : allEdges){
			String type = edge.type;
			
			
		}
	}

	public static void process(String pathStr)
	{
		TaintFlowPath vfg = new TaintFlowPath();
		String[] edgeStrs = pathStr.split(",");
		for(String edgeStr : edgeStrs){
			vfg.processEdge(edgeStr);
		}
		vfg.printGraph();
	}

	public static void main(String[] args) throws IOException
	{
		String pathFileName = args[0];
		BufferedReader reader = new BufferedReader(new FileReader(pathFileName));
		String pathStr = reader.readLine();
		reader.close();
		process(pathStr);
	}
}