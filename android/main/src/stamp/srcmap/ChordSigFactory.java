package stamp.srcmap;

import java.util.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


public class ChordSigFactory
{
	private static Map<ITypeBinding,String> anonymousClasses = new HashMap();

	public static void newAnonymousClass(ITypeBinding anonymTypeBinding, String syntheticName)
	{
		//System.out.println("naming anonymous class " + anonymTypeBinding);
		anonymousClasses.put(anonymTypeBinding, syntheticName);
	}
	
	public static String getSyntheticName(ITypeBinding anonymTypeBinding)
	{
		return anonymousClasses.get(anonymTypeBinding);
	}

	public static String toChordType(ITypeBinding type)
	{
		return toChordTypeInternal(type.getErasure());
	}
	
	private static String toChordTypeInternal(ITypeBinding type)
	{
		if(type.isPrimitive()){
			return type.getBinaryName();
		}
		
		if(type.isArray()){
			String typeStr = toChordTypeInternal(type.getElementType());
			int dim = type.getDimensions();
			for(int i = 0; i < dim; i++)
				typeStr = "[".concat(typeStr);
			return typeStr;
		}
			
		return "L".concat(toChordRefType(type)).concat(";");
	}
	
	private static String toChordRefType(ITypeBinding type)
	{
		ITypeBinding declKlass = type.getDeclaringClass();
		if(declKlass != null){
			String cname;
			if(type.isAnonymous()){
				cname = anonymousClasses.get(type);
				if(cname == null){
					System.out.println("type: " + type.toString());
					System.out.println("declKlass: "+declKlass.toString());
				}
			}
			else
				cname = type.getName();
			return toChordRefType(declKlass).concat("$").concat(cname);
			
		}
		return type.getBinaryName().replace('.', '/');
	}
	
}