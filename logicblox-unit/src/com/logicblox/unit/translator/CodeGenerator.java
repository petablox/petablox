package com.logicblox.unit.translator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.logicblox.unit.AssertLiteral;
import com.logicblox.unit.AssertTest;
import com.logicblox.unit.Atom;
import com.logicblox.unit.Literal;
import com.logicblox.unit.Query;
import com.logicblox.unit.Test;
import com.logicblox.unit.Tuple;
import com.logicblox.unit.Variable;

/**
 * A very basic and inefficient code generator for the translator. 
 * 
 * @author Thiago T. Bartolomei
 */
public class CodeGenerator {
	
	/**
	 * The index of the test we are currently generating.
	 * 
	 * <p>In this generator only assertion tests need the index.</p>
	 */
	protected int index = 0;
	
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
	public void generateSetUpTearDown(String db, File outputDir) throws IOException {
		// If there's ever support for initialize and finalize, uncomment these and remove below
		createFile(outputDir, "initialize.lb", "open " + db + "\n");
		createFile(outputDir, "finalize.lb", "close\n");
		
		StringBuilder b = new StringBuilder();
		//b.append("open ").append(db).append("\n");
		b.append(declareTesting());
		b.append(declareDummyResults());
		createFile(outputDir, "setUp.lb", b.toString());
		
		b = new StringBuilder();
		//b.append(removeTesting());
		b.append(removeResults());
		//b.append("close");
		createFile(outputDir, "tearDown.lb", b.toString());
	}
	
	/**
	 * Creates a file with this contents in this directory.
	 *
	 * @param dir
	 * @param name
	 * @param contents
	 * @throws IOException
	 */
	protected void createFile(File dir, String name, String contents) throws IOException {
		FileWriter w = new FileWriter(new File(dir, name));
		w.write(contents);
		w.close();
	}
	
	/**
	 * Declare predicates in a testing block.
	 */
	public String declareTesting() {
		return new Transaction("Add testing predicates.", "addBlock -B testing") { public void code() {
			e("  testFlag[k] = c -> int[32](k), int[32](c).\n");
		}}.toString();
	}
	
	/**
	 * Declares a dummy results block.
	 *
	 * @return
	 */
	public String declareDummyResults() {
		return new Transaction("Declare dummy results block.", "addBlock -B results") { public void code() {
			e("  _dummy_local_predicate(a) -> int[8](a).\n");
		}}.toString();
	}
	
	/**
	 * Removes the testing block.
	 */
	public String removeTesting() {
		return new Transaction("Remove testing predicates.", null) { public void code() {
			e("removeBlock -B testing\n");
		}}.toString();		
	}

	/**
	 * Remove results block.
	 *
	 * @return
	 */
	public String removeResults() {
		return new Transaction("Remove results block.", null) { public void code() {
			e("removeBlock -B results\n");
		}}.toString();
	}
	
	/**
	 * Tells the generator that it will start generating this assertion test.
	 *
	 * @param test
	 * @return
	 */
	public String startAssertionTest(AssertTest test) {
		index = 0;
		return "// Test " + test.getDescription() + "\n";
	}
	
	/**
	 * Generates code to verify the assertion described by this AssertLiteral.
	 * 
	 * <p>If the assertion is negated then a simple constraint is generated.
	 * Otherwise, we use the trick with a count() aggregation. That is, we get
	 * the old assertion and try to count how many facts it returns. Since we
	 * are looking for presence, it must return exactly 1.</p>
	 * 
	 * <p>Note that we should fix this trick with aggregation if the engine
	 * fixes the ref-mode translation (the trick is only actually needed when
	 * ref-mode is used).</p>
	 *
	 * @param i
	 * @param assertion
	 * @return
	 */
	public String generateAssertion(final AssertLiteral assertion) {
		
		final StringBuilder b = new StringBuilder();
		new Transaction(b, "Check assertion") { public void code() {
		
			Literal l = assertion.getLiteral();
			// Negated predicates don't need aggregation
			if (l.isNegated())
				e("  -> !");
			else
				e("  +testFlag[" + index + "] = c <- agg<<c = count()>> ");
			
			Atom atom = l.getAtom();
			e(atom.getPredicate());
			if (atom.isFunction()) e("["); else e("(");
			
			// list terms
			cstlist(atom.getTerms());
			
			if (atom.isFunction()) {
				e("] = ");
				atom.getValueTerm().appendLogicBloxString(b);
				e(".\n");
			} else 
				e(").\n");
			
			// Constraint if aggregation was used
			if (! l.isNegated()) {
				e("  -> testFlag[" + index + "] = 1.\n");
				index++;
			}
		}};
		return b.toString();
	}
	
	
	/**
	 * Generates a test case for this query test.
	 *
	 * <p>The generated code uses a "results" predicate which is populated with the expected
	 * results of the query. The predicate arity varies according to the number of projected 
	 * variables in the query. The testFlag predicate is populated with the count of the 
	 * expected and actual results. Finally, two constraints are used to verify that the
	 * actual results match the expected results.<p> 
	 * 
	 * @param test
	 * @return
	 */
	public String generateQuery(final Test test) {
		final StringBuilder b = new StringBuilder();
		final Query q = test.getQuery();
		
		// is the expected result empty?
		final boolean empty = test.getExpectedResult().isEmpty();
		 
		if (! empty)
			new Transaction(b, "Declare expected results block.", "replaceBlock -B results") { public void code() {
				List<Variable> vars = projectedVariables(q);
				e("  results(").csvlist(vars).e(") -> ");
				boolean c = false;
				for(Variable v : vars) {
					if (c) e(", "); else c = true;
					e("string(?").e(v.getName()).e(")");
				}
				e(".\n");
			}};
		
		if (! empty)
			new Transaction(b, "Populate results predicate.") { public void code() {
				for (Tuple tuple : test.getExpectedResult()) {
					e("  +results(").csslist(tuple.getTerms()).e(").\n");
				}
			}};
		
		new Transaction(b, "Count number of actual results.") { public void code() {
			e("  +testFlag[0] = c <- agg<<c = count()>> \n      ");
			// Cleanup projections from the aggregation
			String query = q.getQueryString().replaceAll("\n", "\n    ");
			for (Variable v : q.getVariablesOfResult())
				query = query.replaceAll("\\?" + v.getName(), "_");
			e(query).e(".\n");
		}};
		
		/*
		 * This is not really need, just put the integer in the constraint!
		if (! empty)
			new Transaction(b, "Count number of expected results.") { public void code() {
				e("  +testFlag[1] = c <- agg<<c = count()>> results(");
				csAnonlist(projectedVariables(q).size());
				e(").\n");
			}};
		*/
		
		new Transaction(b, "Verify constraints.") { public void code() {
			if (empty)
				e("  -> ! testFlag[0] = _.\n");
			else {
				//p(v) -> results(v).
				e("  ").e(q.getQueryString()).e(" -> results(").csvlist(projectedVariables(q)).e(").\n");
				e("  -> testFlag[0] = ").e(projectedVariables(q).size()).e(".\n");
			}
		}};
		
		return b.toString();
	}
	
	/**
	 * Return the variables in the query that are projected.
	 * 
	 * <p>The "project" keyword populates the variablesOfResult List. If
	 * it is not used, we return all variables.</p>
	 *
	 * @param q
	 * @return
	 */
	protected final List<Variable> projectedVariables(Query q) {
		// if there's no projection, list all variables in results
		if (q.getVariablesOfResult().isEmpty())
			return q.getVariables();
		else
			return q.getVariablesOfResult();
	}
}
