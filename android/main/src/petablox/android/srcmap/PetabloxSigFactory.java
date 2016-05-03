package petablox.android.srcmap;

import java.util.*;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;


public class PetabloxSigFactory
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

	public static String toPetabloxType(ITypeBinding type)
	{
		return toPetabloxTypeInternal(type.getErasure());
	}
	
	private static String toPetabloxTypeInternal(ITypeBinding type)
	{
		if(type.isPrimitive()){
			return type.getBinaryName();
		}
		
		if(type.isArray()){
			String typeStr = toPetabloxTypeInternal(type.getElementType());
			int dim = type.getDimensions();
			for(int i = 0; i < dim; i++)
				typeStr = "[".concat(typeStr);
			return typeStr;
		}
			
		return "L".concat(toPetabloxRefType(type)).concat(";");
	}
	
	private static String toPetabloxRefType(ITypeBinding type)
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
			return toPetabloxRefType(declKlass).concat("$").concat(cname);
			
		}
		return type.getBinaryName().replace('.', '/');
	}
	
}
