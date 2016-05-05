package stamp.reporting.controller;

import javax.xml.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;

import java.util.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static stamp.reporting.Common.*;

import stamp.reporting.QueryResults;

public class BaseCallReportController 
{
    public static class SrcPosition
    {
        private final String srcfile;
        public String getFile() { return srcfile; }
        private final int linenum;
        public int getLine() { return linenum; }
        
        public SrcPosition(String srcfile, int linenum)
        {
            this.srcfile = srcfile;
            this.linenum = linenum;
        }
        
        public int hashCode() 
        {
            return new HashCodeBuilder(17, 31). // randomly chosen prime numbers
                append(srcfile).
                append(linenum).
                toHashCode();
        }

        public boolean equals(Object obj) 
        {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (!(obj instanceof SrcPosition))
                return false;

            SrcPosition rhs = (SrcPosition) obj;
            return new EqualsBuilder().
                append(srcfile, rhs.srcfile).
                append(linenum, rhs.linenum).
                isEquals();
        }
        
        public String toString() 
        {
            return srcfile + " (" + linenum + ")";
        }
    }
    
    public static class MethodParameter
    {
        private String type;
        public final String getType() { return type; }
        private int keyNum;
        public final int getKeyNumber() { return keyNum; }
        private Set<String> values;
        public Set<String> getValues() { return values; }
        
        public MethodParameter(String type, int keyNum)
        {
            this.type = type;
            this.keyNum = keyNum;
            values = new HashSet<String>();
        }
        
        public MethodParameter(String type)
        {
            this(type, 0);
        }
        
        public void addValue(String value) 
        {
            if(type.equals("boolean")) {
                if(value.equals("0")) value = "false";
                else if(value.equals("1")) value = "true";
            }
            values.add(value);
        }
        
        public boolean compatible(MethodParameter other)
        {
            return (this.getType() == other.getType() &&
                    this.getKeyNumber() == other.getKeyNumber());
        }
        
        public static MethodParameter combine(Collection<MethodParameter> parameters)
        {
            if(parameters.isEmpty())
            {
                throw new RuntimeException(
                    "Can't combine empty collection of MethodParameter objects.");
            }
            
            MethodParameter newParam = null;
            
            for(MethodParameter param : parameters)
            {
                if(newParam == null)
                {
                    newParam = new MethodParameter(param.getType(), 
                                                   param.getKeyNumber());
                }
                else
                {
                    if(!param.compatible(newParam))
                    {
                        throw new RuntimeException(
                            "Can't combine incompatible MethodParameter objects.");
                    }
                }
                for(String value : param.getValues())
                {
                    newParam.addValue(value);
                }
            }
            return newParam;
        }
    }
    
    public static class MethodInvocation
    {
        private final SrcPosition position;
        public SrcPosition getPosition() { return position; }
        public String getFile() { return position.getFile(); }
        public int getLineNum() { return position.getLine(); }
        
        private List<MethodParameter> parameters;
        public List<MethodParameter> getParameters() { return parameters; }
        
        private MethodParameter returnParameter;
        public MethodParameter getReturnParameter() { return returnParameter; }
        
        public MethodInvocation(SrcPosition position, 
                                List<MethodParameter> parameters, 
                                MethodParameter returnParameter)
        {
            this.position = position;
            this.parameters = parameters;
            this.returnParameter = returnParameter;
        }
        
        public boolean compatible(MethodInvocation invocation)
        {
            if(!this.getReturnParameter().compatible(
                invocation.getReturnParameter())) return false;
            if(this.getParameters().size() != 
               invocation.getParameters().size()) return false;
            for(int i = 0; i < this.getParameters().size(); i++)
            {
                if(!this.getParameters().get(i).compatible(
                    invocation.getParameters().get(i))) return false;
            }
            return true;
        }
    }
    
    public static class Method
    {
        private final String name;
        public String getName() { return name; }
        
        private final SrcPosition position;
        public SrcPosition getPosition() { return position; }
        public String getFile() { return position.getFile(); }
        public int getLineNum() { return position.getLine(); }
        
        private List<MethodInvocation> invocations;
        public List<MethodInvocation> getInvocations() { return invocations; }
        
        public List<MethodParameter> getParameters()
        {
            if(invocations.size() == 0) return new ArrayList<MethodParameter>();
            
            List<List<MethodParameter>> unfoldedParams = 
                                         new ArrayList<List<MethodParameter>>();
            int pCount = this.getInvocations().get(0).getParameters().size();
            for(int i = 0; i < pCount; i++)
            {
                unfoldedParams.add(new ArrayList<MethodParameter>());
            }
            for(MethodInvocation invocation : this.getInvocations())
            {
                List<MethodParameter> invParams = invocation.getParameters();
                for(int i = 0; i < invParams.size(); i++)
                {
                    unfoldedParams.get(i).add(invParams.get(i));
                }
            }
            List<MethodParameter> parameters = new ArrayList<MethodParameter>();
            for(List<MethodParameter> pList : unfoldedParams)
            {
                MethodParameter foldedParam = MethodParameter.combine(pList);
                parameters.add(foldedParam);
            }
            return parameters;
        }
        
        public MethodParameter getReturnParameter() 
        { 
            List<MethodParameter> retParams = new ArrayList<MethodParameter>();
            for(MethodInvocation invocation : this.getInvocations())
            {
                retParams.add(invocation.getReturnParameter());
            }
            return MethodParameter.combine(retParams);
        }
        
        public Method(String name, SrcPosition position)
        {
            this.name = name;
            this.position = position;
            this.invocations = new LinkedList<MethodInvocation>();
        }
        
        public void addInvocation(MethodInvocation invocation)
        {
            if(invocations.size() > 0)
            {
                // Verify that this invocation is compatible with the first 
                // 'pattern' invocation detected.
                MethodInvocation pattern = invocations.get(0);
                if(!pattern.compatible(invocation))
                {
                    throw new RuntimeException(
                        "Invocations of the same method must be compatible " +
                        "in the numbers and types of their parameters, " +
                        "as well as in the parameter's key numbers");
                }
            }
            this.invocations.add(invocation);
        }
        
        public Map<SrcPosition, Method> filterByCallsite()
        {
            Map<SrcPosition, List<MethodInvocation>> ics; // Invocations by Call Site
            ics = new HashMap<SrcPosition, List<MethodInvocation>>();
            for(MethodInvocation invocation : this.getInvocations())
            {
                SrcPosition invPos = invocation.getPosition();
                if(!ics.containsKey(invPos))
                {
                    ics.put(invPos, new ArrayList<MethodInvocation>());
                }
                ics.get(invPos).add(invocation);
            }
            Map<SrcPosition, Method> mcs; // Methods by Call Site
            mcs = new HashMap<SrcPosition, Method>();
            for(SrcPosition pos : ics.keySet())
            {
                Method m = new Method(this.name, pos);
                for(MethodInvocation invc : ics.get(pos))
                    m.addInvocation(invc);
                mcs.put(pos, m);
            }
            return mcs;
        }
    }
    
    protected Element dataNode;
    private QueryResults qr;
    
    private Method method;
    public Method getMethod() { return method; }
    
    protected Map<String, Element> getElementsMap(Element dataElem, String tag)
    {
        List<Element> elements = getChildrenByTagName(dataElem, tag);
		Map<String, Element> elementsMap = new HashMap<String, Element>();
		for(int i = 0; i < elements.size(); i++)
		{
		    Element elem = elements.get(i);
		    String key = elem.getAttribute("key");
		    elementsMap.put(key, elem);
		}
		return elementsMap;
    }
    
    protected Map<String, Element> getRecordElementsMap(Element dataElem)
    {
        return getElementsMap(dataElem, "record");
    }
    
    protected Map<String, Element> getRecordListElementsMap(Element dataElem)
    {
        return getElementsMap(dataElem, "recordlist");
    }
    
    protected List<MethodInvocation> parseMethodInvocationsInfo(Element paramsE, 
                                Element paramsNumMapE, Element invocationsE, 
                                MethodParameter retParam)
    {
        // TODO: Read method return parameter and values from XML (remove 
        //       retParam from this method's signature).
        
        List<String> parameterTypes = new ArrayList<String>();
        List<Element> paramElements = getChildrenByTagName(paramsE, "record");
		for(int i = 0; i < paramElements.size(); i++)
		{
		        parameterTypes.add(paramElements.get(i).getAttribute("value"));
		}
		
		Map<Integer, Integer> parameterKeyNumbers = new HashMap<Integer, Integer>();
		// callbacks report has a paramsNumMap element, api calls report doesn't
		if(paramsNumMapE != null)
		{
		    List<Element> paramNumMapElements = getChildrenByTagName(paramsNumMapE, "record");
		    for(int i = 0; i < paramNumMapElements.size(); i++)
		    {
		        int key = Integer.parseInt(paramNumMapElements.get(i).getAttribute("key"));
		        int value = Integer.parseInt(paramNumMapElements.get(i).getAttribute("value"));
		        parameterKeyNumbers.put(key, value + 1);
		    }
		}
		
		List<MethodInvocation> invocations = new ArrayList<MethodInvocation>();
		List<Element> invocElements = getChildrenByTagName(invocationsE, "recordlist");
		for(int i = 0; i < invocElements.size(); i++)
		{
		    Element invocationE = invocElements.get(i);
		    Map<String, Element> recordElemsMap = getRecordElementsMap(invocationE);
		    Map<String, Element> recordListElemsMap = getRecordListElementsMap(invocationE);
		    
		    String srcFile = recordElemsMap.get("srcfile").getAttribute("value");
            int linenum = Integer.parseInt(recordElemsMap.get("linenum").getAttribute("value"));
            SrcPosition position = new SrcPosition(srcFile, linenum);
            
            List<MethodParameter> parameters = new ArrayList<MethodParameter>();
            for(int j = 0; j < parameterTypes.size(); j++)
            {
                MethodParameter p;
                String pType = parameterTypes.get(j);
                if(parameterKeyNumbers.containsKey(j))
                {
                    int pKeyNum = parameterKeyNumbers.get(j);
                    p = new MethodParameter(pType, pKeyNum);
                }
                else
                {
                    p = new MethodParameter(pType);
                }
                parameters.add(p);
            }
		    
		    Element invocParamsE = recordListElemsMap.get("parameters");
		    List<Element> invocPEs = getChildrenByTagName(invocParamsE, "record");
		    int modifier;
		    if(parameters.size() == invocPEs.size()) 
		        modifier = 0; 
		    else if((parameters.size()-1) == invocPEs.size()) 
		        modifier = 1; // static method
		    else 
		        throw new RuntimeException("Invalid invocations record: ");
		    for(int j = 0; j < invocPEs.size(); j++)
		    {
		        String value = invocPEs.get(j).getAttribute("value");
		        value = value.substring(value.indexOf(":") + 1);
		        parameters.get(j + modifier).addValue(value);
		    }
		    
		    invocations.add(
		        new MethodInvocation(position, parameters, retParam));
		}
		
		return invocations;
    }
    
    protected Method parseMethodRecord(Map<String, Element> recordElemsMap,
                                     Map<String, Element> recordListElemsMap)
    {
        String methodName = 
                    recordElemsMap.get("method_name").getAttribute("value");
        String srcFile = 
                    recordElemsMap.get("srcfile").getAttribute("value");
        int linenum = Integer.parseInt(
                        recordElemsMap.get("linenum").getAttribute("value"));
		SrcPosition position = new SrcPosition(srcFile, linenum);
		
		Method method = new Method(methodName, position);
		
		String returnType = recordElemsMap.get("method_return_type").getAttribute("value");
		MethodParameter retParam = new MethodParameter(returnType);
		
		Element paramsE = recordListElemsMap.get("parameters");
		Element paramsNumMapE = recordListElemsMap.get("param_num_map");
		Element invocationsE = recordListElemsMap.get("invocations");
		List<MethodInvocation> invocations = 
		    parseMethodInvocationsInfo(paramsE, paramsNumMapE, invocationsE, 
		                              retParam);
		
		for(MethodInvocation invocation : invocations)
		{
		    method.addInvocation(invocation);
		}
		
		return method;
    }
    
    public BaseCallReportController(QueryResults qr, String filepath, String id)
    {
        this.qr = qr;
        Element node = qr.queryForElement(filepath, id);
        
        String nodeName = node.getNodeName();
		if(!nodeName.equals("tuple"))
		    throw new RuntimeException("Unexpected node type.");
		
		Element valueElem = getFirstChildByTagName(node, "value");
		dataNode = getFirstChildByTagName(valueElem, "data");
		    
		Map<String, Element> recordElemsMap = getRecordElementsMap(dataNode);
		Map<String, Element> recordListElemsMap = getRecordListElementsMap(dataNode);
		
		this.method = parseMethodRecord(recordElemsMap, recordListElemsMap);
    }
}
