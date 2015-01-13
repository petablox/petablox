package com.logicblox.unit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.logicblox.unit.translator.CodeGenerator;

/**
 * Some simple unit tests for the code generator
 * 
 * @author Thiago T. Bartolomei
 */
public class CodeGeneratorTest {
	
	protected CodeGenerator c = null;
	
	@Before
	public void setUp() {
		c = new CodeGenerator();
	}
	
	@After
	public void tearDown() {
		c = null;
	}
	
	@Test
	public void testUnaryAssertion() {
		AssertLiteral a = createSimpleAssertion(false, "PrimitiveType", "boolean");
		
		Assert.assertEquals(
				"transaction\n" +
				"exec <doc>\n" +  
				"  +testFlag[0] = c <- agg<<c = count()>> PrimitiveType(\"boolean\").\n" +
				"  -> testFlag[0] = 1.\n" +
				"</doc>\n" +
				"commit\n\n", 
				c.generateAssertion(a));
	}
	
	public AssertLiteral createSimpleAssertion(boolean negated, String predicate, String term) {
		Atom a = new Atom(predicate);
		a.addKeyTerm(new StringTerm(term));
		
		Literal l = new Literal(a);
		if (negated) l.setNegated();
		
		return new AssertLiteral(l);
	}
}
