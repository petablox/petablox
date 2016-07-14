package stamp.reporting.controller;

import javax.xml.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;

import java.util.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import static stamp.reporting.Common.*;

import stamp.reporting.QueryResults;

public class DynamicCallbacksReportController extends BaseCallReportController
{
    public Method getCallbackMethod() { return getMethod(); }
    
    private List<Method> registrationMethods;
    public List<Method> getRegistrationMethods() { return registrationMethods; }
    
    private List<Method> relatedMethods;
    public List<Method> getRelatedMethods() { return relatedMethods; }
    
    private void parseRelatedMethodsData(Element relatedListE)
    {
        List<Element> relatedMethodElements = getChildrenByTagName(relatedListE, "record");
        for(int i = 0; i < relatedMethodElements.size(); i++)
		{
		    Element relatedE = relatedMethodElements.get(i);
		    
		    Map<String, Element> recordElemsMap = getRecordElementsMap(relatedE);
		    Map<String, Element> recordListElemsMap = getRecordListElementsMap(relatedE);
		    Method related = parseMethodRecord(recordElemsMap, recordListElemsMap);
		    
		    String relationType = recordElemsMap.get("relation_type").getAttribute("value");
		    if(relationType.equals("registration"))
		        registrationMethods.add(related);
		    else
		        relatedMethods.add(related);
		}
    }
    
    public DynamicCallbacksReportController(QueryResults qr, String filepath, String id)
    {
        super(qr, filepath, id);
        
        registrationMethods = new ArrayList<Method>();
        relatedMethods = new ArrayList<Method>();
        
        Element relatedListE = getRecordListElementsMap(dataNode).get("related_methods");
		parseRelatedMethodsData(relatedListE);
    }
}
