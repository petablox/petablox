package com.logicblox.unit.translator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.logicblox.unit.AssertTest;
import com.logicblox.unit.Query;
import com.logicblox.unit.Test;
import com.logicblox.unit.Tuple;
import com.logicblox.unit.Variable;

/**
 * A very basic and inefficient code generator for the translator.
 * 
 * <p>Very similar to CodeGenerator, but generates a single file for all tests.</p>
 * 
 * @author Thiago T. Bartolomei
 */
public class SingleFileCodeGenerator extends CodeGenerator {

	/**
	 * Generates some code to put in the set up and tear down files.
	 *
	 * <p>Currently the code just opens and closes the database.</p>
	 * 
	 * @param hasQueryTest
	 * @param hasAssertTrueTest
	 * @param outputDir
	 * @throws IOException
	 */
	@Override
	public void generateSetUpTearDown(String db, File outputDir) throws IOException {
		
		StringBuilder b = new StringBuilder();
		b.append("open ").append(db).append("\n");
		b.append(declareTesting());
		createFile(outputDir, "setUp.lb", b.toString());
		
		b = new StringBuilder();
		b.append(removeTesting());
		b.append("close");
		createFile(outputDir, "tearDown.lb", b.toString());
	}
	
	/**
	 * Declare predicates in a testing block.
	 */
	@Override
	public String declareTesting() {
		return new Transaction("Add testing predicates.", "addBlock -B testing") { public void code() {
			// We support up to arity 4, I think it's enough...
			e("  testFlag[k] = c -> int[32](k), int[32](c).\n");
			e("  results_1(i, r) -> int[32](i), string(r).\n");
			e("  results_2(i, r1, r2) -> int[32](i), string(r1), string(r2).\n");
			e("  results_3(i, r1, r2, r3) -> int[32](i), string(r1), string(r2), string(r3).\n");
			e("  results_4(i, r1, r2, r3, r4) -> int[32](i), string(r1), string(r2), string(r3), string(r4).\n");
		}}.toString();
	}

	@Override
	public String startAssertionTest(AssertTest test) {
		return "// Test " + index + " => " + test.getDescription() + "\n";
	}
	
	/**
	 * Generates a test case for this query test, using this index.
	 * 
	 * @param test
	 * @return
	 */
	@Override
	public String generateQuery(final Test test) {
		final StringBuilder b = new StringBuilder();
		final Query q = test.getQuery();
		
		b.append("// Test ").append(index).append(" => ").append(test.getDescription()).append("\n");
		
		// is the expected result empty?
		final boolean empty = test.getExpectedResult().isEmpty();
		
		if (! empty)
			new Transaction(b, "Populate results predicate.") { public void code() {
				for (Tuple tuple : test.getExpectedResult()) {
					e("  +results_").e(tuple.getTerms().size()).e("(").e(index).e(", ").csslist(tuple.getTerms()).e(").\n");
				}
			}};
		
		new Transaction(b, "Count number of actual results.") { public void code() {
			e("  +testFlag[").e(index).e("] = c <- agg<<c = count()>> \n      ");
			// Cleanup projections from the aggregation
			String query = q.getQueryString().replaceAll("\n", "\n    ");
			for (Variable v : q.getVariablesOfResult())
				query = query.replaceAll("\\?" + v.getName(), "_");
			e(query).e(".\n");
		}};
		
		new Transaction(b, "Verify constraints.") { public void code() {
			if (empty)
				e("  -> ! testFlag[").e(index).e("] = _.\n");
			else {
				//p(v) -> results(v).
				List<Variable> vars = projectedVariables(q);
				e("  ").e(q.getQueryString()).e(" -> results_").e(vars.size()).e("(").e(index).e(", ").csvlist(vars).e(").\n");
				e("  -> testFlag[").e(index).e("] = ").e(vars.size()).e(".\n");
				index++;
			}
		}};
		
		return b.toString();
	}
}
