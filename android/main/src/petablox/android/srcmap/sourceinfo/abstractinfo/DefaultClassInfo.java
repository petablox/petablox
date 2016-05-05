package petablox.android.srcmap.sourceinfo.abstractinfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import petablox.android.srcmap.sourceinfo.ClassInfo;

/**
 * @author Saswat Anand 
 */
public class DefaultClassInfo implements ClassInfo {
	private Map<String, BasicMethodInfo> methInfos = new HashMap<String, BasicMethodInfo>();	
	private String className;
	private File file;
	private int lineNum;
	private Element classElem;

	protected DefaultClassInfo(String className, File f, Element classElem) {
		//System.out.println("reading class info " + className + " " + f);
		this.file = f;
		this.className = className;
		this.classElem = classElem;
		readInfo();
	}

	/*
	public Map<String,MethodInfo> allMethodInfos()
	{
		Map<String,MethodInfo> ret = new HashMap();
		for(String chordSig : methInfos.keySet()){
			ret.put(chordSig, new MethodInfo(chordSig, this));
		}
		return ret;
	}
	*/

	public int lineNum() {
		return lineNum;
	}

	public DefaultMethodInfo methodInfo(String chordSig) {
		BasicMethodInfo bmi = methInfos.get(chordSig);
		if(bmi == null)
			return null;
		return new DefaultMethodInfo(chordSig, this.classElem);
	}

	public int lineNum(String chordMethSig) { 
		BasicMethodInfo bmi = methInfos.get(chordMethSig);
		return bmi == null ? -1 : bmi.lineNum;
	}
	
	public List<String> aliasSigs(String chordMethSig) {
		List<String> ret = methInfos.get(chordMethSig).aliasPetabloxSigs;
		return ret == null ? Collections.EMPTY_LIST : ret;
	}
	
	public Map<String,List<String>> allAliasSigs() {
		Map<String,List<String>> ret = new HashMap();
		for(Map.Entry<String,BasicMethodInfo> bmiEntry : methInfos.entrySet()){
			String chordSig = bmiEntry.getKey();
			List<String> aliases = bmiEntry.getValue().aliasPetabloxSigs;
			if(aliases != null){
				if(ret == null)
					ret = new HashMap();
				ret.put(chordSig, aliases);
			}
		}
		return ret == null ? Collections.EMPTY_MAP : ret;
	}

	public static List<Element> getChildrenByTagName(Element parent, String name)  {
		List<Element> nodeList = new ArrayList<Element>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && 
				name.equals(child.getNodeName())) {
				nodeList.add((Element) child);
			}
		}
		return nodeList;
	}

	private void readInfo() {
		String clsLineNum = classElem.getAttribute("line");
		if(!clsLineNum.equals(""))
			lineNum = Integer.parseInt(clsLineNum);
		else
			lineNum = -1;

		for(Element methElem : getChildrenByTagName(classElem, "method")){
			String chordSig = methElem.getAttribute("chordsig");
			BasicMethodInfo bmi = new BasicMethodInfo();
			methInfos.put(chordSig, bmi);

			//line num
			String lineNum = methElem.getAttribute("line");
			bmi.lineNum = !lineNum.equals("") ? Integer.parseInt(lineNum) : -1;
			
			for(Element aliasElem : getChildrenByTagName(methElem, "alias")){
				String aliasSig = aliasElem.getFirstChild().getNodeValue();
				bmi.addAliasPetabloxSig(aliasSig);
			}
		}
	}

	private static class BasicMethodInfo {
		int lineNum = -1;
		List<String> aliasPetabloxSigs;
		
		void addAliasPetabloxSig(String chordSig) {
			if(aliasPetabloxSigs == null)
				aliasPetabloxSigs = new ArrayList();
			aliasPetabloxSigs.add(chordSig);
		}
	}
}
