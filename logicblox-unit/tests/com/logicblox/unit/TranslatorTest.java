package com.logicblox.unit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.logicblox.unit.translator.Translator;


/**
 * Some simple unit tests for the translator
 * 
 * @author Thiago T. Bartolomei
 */
public class TranslatorTest {

	protected Translator t = null;
	
	@Before
	public void setUp() {
		t = new Translator(false);
	}
	
	@After
	public void tearDown() {
		t = null;
	}
	
	@Test
	public void testComputeFilenameEmpty() {
		Assert.assertEquals(
				"Test0.lb", 
				t.computeFilename(0, null));
		Assert.assertEquals(
				"Test0.lb", 
				t.computeFilename(0, ""));
	}
	
	@Test
	public void testComputeFilenameBasic() {
		Assert.assertEquals(
				"Test0_simple.lb", 
				t.computeFilename(0, "simple"));
		Assert.assertEquals(
				"Test0_primitive_types.lb", 
				t.computeFilename(0, "primitive types"));
	}
	
	@Test
	public void testComputeFilenameLong() {
		Assert.assertEquals(
				"Test0_Class_initializer.lb", 
				t.computeFilename(0, "Class initializer of java.lang.Object"));
	}
	
	@Test
	public void testComputeFilenameArrays() {
		Assert.assertEquals(
				"Test0_Cast_int.lb", 
				t.computeFilename(0, "Cast int[] to Cloneable"));
		Assert.assertEquals(
				"Test0_Cast_Integer.lb", 
				t.computeFilename(0, "Cast Integer[][] to Object[]"));
		Assert.assertEquals(
				"Test0_java_io_FileInputStream_overrides.lb", 
				t.computeFilename(0, "java.io.FileInputStream overrides finalize"));
	}
	
	@Test
	public void testComputeFilenameDots() {
		Assert.assertEquals(
				"Test0_java_io_FileInputStream_overrides.lb", 
				t.computeFilename(0, "java.io.FileInputStream overrides finalize"));
	}
}
