package stamp.reporting;

import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.*;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

public class Common
{
	public static String getLabel(Element value)
	{
	    Element labelElem = getFirstChildByTagName(value, "label");
		return escapeHtml4(labelElem.getFirstChild().getNodeValue());
	}

	public static String linkToSrc(String label, Element value) 
	{
		String srcFile = value.getAttribute("srcFile");
		String lineNum = value.getAttribute("lineNum");
		return linkToSrc(label, srcFile, lineNum);
	}
	
	public static String linkToSrc(String label, String srcFile, String lineNum)
	{
		if(srcFile == null) {
			return label;
		} else {
			int ln = lineNum.length() == 0 ? 0 : Integer.parseInt(lineNum);
			String newLineNum = String.valueOf(ln > 0 ? ln : 0);
			StringBuilder builder = new StringBuilder();
			builder.append(label).append('#').append(srcFile).append('#').append(newLineNum);
			return builder.toString();
		}
	}

	public static List<Element> getChildrenByTagName(Element parent, String name) {
		List<Element> nodeList = new ArrayList<Element>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && 
				name.equals(child.getNodeName())) {
				nodeList.add((Element) child);
			}
		}
		return nodeList;
    }
	
	public static Element getFirstChildByTagName(Element parent, String name) 
	{
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && 
				name.equals(child.getNodeName())) {
				return (Element) child;
			}
		}
		return null;
    }
	
    public static String sha1sum(String s)
    {
        String result = "";
        byte[] b;
        try 
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(s.getBytes("utf8"));
            b = crypt.digest();
        }
        catch (java.security.NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA1 message digest algorithm not found.");
        }
        catch (java.io.UnsupportedEncodingException e)
        {
            throw new RuntimeException("UTF-8 encoding not supported.");
        }
        for (int i=0; i < b.length; i++) {
            result +=
                Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
}
