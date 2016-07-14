package stamp.srcmap;

import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.dom.*;
import java.util.*;

/*
  @author Saswat Anand
  Most of the code copied from one of the eclipse's classes
  copied to avoid dependency on yet another jar
*/
public class EclipseUtil
{
	public static List<IMethodBinding> findOverridenMethods(IMethodBinding overridingMethod)
	{
		List<IMethodBinding> ret = null;
		for(ITypeBinding supType : getAllSuperTypes(overridingMethod.getDeclaringClass())){
			IMethodBinding overriddenMethod = findOverriddenMethodInType(supType, overridingMethod);
			if(overriddenMethod != null){
				if(ret == null)
					ret = new ArrayList();
				ret.add(overriddenMethod);
			}
		}
		if(ret == null)
			return Collections.EMPTY_LIST;
		else
			return ret;
	}

	/**
	 * Finds the method in the given <code>type</code> that is overridden by the specified <code>method<code>.
	 * Returns <code>null</code> if no such method exits.
	 * @param type The type to search the method in
	 * @param method The specified method that would override the result
	 * @return the method binding of the method that is overridden by the specified <code>method<code>, or <code>null</code>
	 */
	public static IMethodBinding findOverriddenMethodInType(ITypeBinding type, IMethodBinding method) {
		IMethodBinding[] methods= type.getDeclaredMethods();
		for (int i= 0; i < methods.length; i++) {
			if (isSubsignature(method, methods[i])){
				return methods[i];
			}
		}
		return null;
	}

	/**
	 * Returns all super types (classes and interfaces) for the given type.
	 * @param type The type to get the supertypes of.
	 * @return all super types (excluding <code>type</code>)
	 */
	public static ITypeBinding[] getAllSuperTypes(ITypeBinding type) {
		Set result= new HashSet();
		collectSuperTypes(type, result);
		result.remove(type);
		return (ITypeBinding[]) result.toArray(new ITypeBinding[result.size()]);
	}

	private static void collectSuperTypes(ITypeBinding curr, Set collection) {
		if (collection.add(curr)) {
			ITypeBinding[] interfaces= curr.getInterfaces();
			for (int i= 0; i < interfaces.length; i++) {
				collectSuperTypes(interfaces[i], collection);
			}
			ITypeBinding superClass= curr.getSuperclass();
			if (superClass != null) {
				collectSuperTypes(superClass, collection);
			}
		}
	}

	/**
	 * @param overriding overriding method (m1)
	 * @param overridden overridden method (m2)
	 * @return <code>true</code> iff the method <code>m1</code> is a subsignature of the method <code>m2</code>.
	 * This is one of the requirements for m1 to override m2.
	 * Accessibility and return types are not taken into account.
	 * Note that subsignature is <em>not</em> symmetric!
	 */
	public static boolean isSubsignature(IMethodBinding overriding, IMethodBinding overridden) {
		//TODO: use IMethodBinding#isSubsignature(..) once it is tested and fixed (only erasure of m1's parameter types, considering type variable counts, doing type variable substitution
		return overriding.isSubsignature(overridden);
		/*
		if (!overriding.getName().equals(overridden.getName()))
			return false;

		ITypeBinding[] m1Params= overriding.getParameterTypes();
		ITypeBinding[] m2Params= overridden.getParameterTypes();
		if (m1Params.length != m2Params.length)
			return false;

		ITypeBinding[] m1TypeParams= overriding.getTypeParameters();
		ITypeBinding[] m2TypeParams= overridden.getTypeParameters();
		if (m1TypeParams.length != m2TypeParams.length
			&& m1TypeParams.length != 0) //non-generic m1 can override a generic m2
			return false;

		//m1TypeParameters.length == (m2TypeParameters.length || 0)
		if (m2TypeParams.length != 0) {
			//Note: this branch does not 100% adhere to the spec and may report some false positives.
			// Full compliance would require major duplication of compiler code.

			//Compare type parameter bounds:
			for (int i= 0; i < m1TypeParams.length; i++) {
				// loop over m1TypeParams, which is either empty, or equally long as m2TypeParams
				Set m1Bounds= getTypeBoundsForSubsignature(m1TypeParams[i]);
				Set m2Bounds= getTypeBoundsForSubsignature(m2TypeParams[i]);
				if (! m1Bounds.equals(m2Bounds))
					return false;
			}
			//Compare parameter types:
			if (equals(m2Params, m1Params))
				return true;
			for (int i= 0; i < m1Params.length; i++) {
				ITypeBinding m1Param= m1Params[i];
				if (containsTypeVariables(m1Param))
					m1Param= m1Param.getErasure(); // try to achieve effect of "rename type variables"
				else if (m1Param.isRawType())
					m1Param= m1Param.getTypeDeclaration();
				if (! (equals(m1Param, m2Params[i].getErasure()))) // can erase m2
					return false;
			}
			return true;

		} else {
			// m1TypeParams.length == m2TypeParams.length == 0
			if (equals(m1Params, m2Params))
				return true;
			for (int i= 0; i < m1Params.length; i++) {
				ITypeBinding m1Param= m1Params[i];
				if (m1Param.isRawType())
					m1Param= m1Param.getTypeDeclaration();
				if (! (equals(m1Param, m2Params[i].getErasure()))) // can erase m2
					return false;
			}
			return true;
			}*/
	}

	private static Set getTypeBoundsForSubsignature(ITypeBinding typeParameter) {
		ITypeBinding[] typeBounds= typeParameter.getTypeBounds();
		int count= typeBounds.length;
		if (count == 0)
			return Collections.EMPTY_SET;

		Set result= new HashSet(typeBounds.length);
		for (int i= 0; i < typeBounds.length; i++) {
			ITypeBinding bound= typeBounds[i];
			if ("java.lang.Object".equals(typeBounds[0].getQualifiedName())) //$NON-NLS-1$
				continue;
			else if (containsTypeVariables(bound))
				result.add(bound.getErasure()); // try to achieve effect of "rename type variables"
			else if (bound.isRawType())
				result.add(bound.getTypeDeclaration());
			else
				result.add(bound);
		}
		return result;
	}
	
	/**
	 * Checks if the two bindings are equals. Also works across binding environments.
	 * @param b1 first binding treated as <code>this</code>. So it must
	 *  not be <code>null</code>
	 * @param b2 the second binding.
	 * @return boolean
	 */
	public static boolean equals(IBinding b1, IBinding b2) {
		return b1.isEqualTo(b2);
	}
	
	/**
	 * Checks if the two arrays of bindings have the same length and
	 * their elements are equal. Uses
	 * <code>Bindings.equals(IBinding, IBinding)</code> to compare.
	 * @param b1 the first array of bindings. Must not be <code>null</code>.
	 * @param b2 the second array of bindings.
	 * @return boolean
	 */
	public static boolean equals(IBinding[] b1, IBinding[] b2) {
		assert b1 != null;
		if (b1 == b2)
			return true;
		if (b2 == null)
			return false;
		if (b1.length != b2.length)
			return false;
		for (int i= 0; i < b1.length; i++) {
			if (!equals(b1[i], b2[i]))
				return false;
		}
		return true;
	}
	
	private static boolean containsTypeVariables(ITypeBinding type) {
		if (type.isTypeVariable())
			return true;
		if (type.isArray())
			return containsTypeVariables(type.getElementType());
		if (type.isCapture())
			return containsTypeVariables(type.getWildcard());
		if (type.isParameterizedType())
			return containsTypeVariables(type.getTypeArguments());
		if (type.isTypeVariable())
			return containsTypeVariables(type.getTypeBounds());
		if (type.isWildcardType() && type.getBound() != null)
			return containsTypeVariables(type.getBound());
		return false;
	}
	
	private static boolean containsTypeVariables(ITypeBinding[] types) {
		for (int i= 0; i < types.length; i++)
			if (containsTypeVariables(types[i]))
				return true;
		return false;
	}
}