package stamp.missingmodels.jimplesrcmapper;

/* Soot - a J*va Optimization Framework
 * Copyright (C) 2003 Ondrej Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import soot.AttributesUnitPrinter;
import soot.Body;
import soot.BriefUnitPrinter;
import soot.LabeledUnitPrinter;
import soot.Local;
import soot.Modifier;
import soot.NormalUnitPrinter;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Type;
import soot.Unit;
import soot.UnitPrinter;
import soot.options.Options;
import soot.tagkit.JimpleLineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;
import soot.util.DeterministicHashMap;

/**
 * Prints out a class and all its methods.
 */
public class Printer {

	/** An visitor to visit while the code is printed */
	private final JimpleVisitor visitor;

	public Printer() {
		this.visitor = new JimpleVisitor();
	}

	public Printer(JimpleVisitor visitor) {
		this.visitor = visitor;
	}

	public static final int USE_ABBREVIATIONS = 0x0001, ADD_JIMPLE_LN = 0x0010;

	public boolean useAbbreviations() {
		return (options & USE_ABBREVIATIONS) != 0;
	}

	public boolean addJimpleLn() {
		return (options & ADD_JIMPLE_LN) != 0;
	}

	int options = 0;
	public void setOption(int opt) {
		options |= opt;
	}
	public void clearOption(int opt) {
		options &= ~opt;
	}

	int jimpleLnNum = 0; // actual line number

	public int getJimpleLnNum() {
		return jimpleLnNum;
	}
	public void setJimpleLnNum(int newVal) {
		jimpleLnNum = newVal;
	}
	public void incJimpleLnNum() {
		jimpleLnNum++;
		//G.v().out.println("jimple Ln Num: "+jimpleLnNum);
	}

	/**
	 * Prints all the files in the given output directory.
	 */
	public void printAll(String outputPath) throws IOException {
		System.out.println("ENTERING JIMPLE PRINTER");
		if(!System.getProperty("line.separator").equals("\n")) {
			throw new RuntimeException("Bad line separator!");
		}
		for(SootClass cl : Scene.v().getClasses()) {
			//System.out.println("PRINTING: " + cl.getName());

			// Get file name.
			StringBuffer b = new StringBuffer();
			b.append(outputPath);
			b.append(cl.getPackageName().replace('.', '/') + '/');
			File folder = new File(b.toString());
			
			b = new StringBuffer();
			b.append(cl.getName());
			b.append(".jimple");
			File file = new File(folder, b.toString());
			
			//System.out.println("TO FILE: " + file.getCanonicalPath());
			
			// Get print writer for the file.
			//final int format = Options.v().output_format();
			//String fileName = SourceLocator.v().getFileNameFor(cl, format);
			file.getParentFile().mkdirs();

			// Print the class to the file.
			printTo(cl, file);

		}
		System.out.println("EXITING PRINTER");
	}
	
	public void printTo(SootClass cl, File file) throws IOException {
		OutputStream streamOut = new FileOutputStream(file);
		this.printTo(cl, streamOut);
		streamOut.close();
	}
	
	/**
	 * Prints the file in the given output directory.
	 * Resets the escaped writer after each file.
	 */
	private EscapedWriter escapedWriter = null;
	public void printTo(SootClass cl, OutputStream streamOut) throws IOException {
		this.escapedWriter = new EscapedWriter(new OutputStreamWriter(streamOut));
		PrintWriter writerOut =  new PrintWriter(this.escapedWriter);

		// Print the class to the writer.
		printTo(cl, writerOut);
		
		// Clean up.
		writerOut.flush();
	}

	private void printTo(SootClass cl, PrintWriter out) {
		/** START VISIT */
		this.visitor.start(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		/** START VISIT CLASS */
		this.visitor.startVisit(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		// add jimple line number tags
		setJimpleLnNum(1);

		// Print class name + modifiers
		/** START VISIT CLASS DECLARATION */
		this.visitor.startVisitDeclaration(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
		{
			StringTokenizer st =
					new StringTokenizer(Modifier.toString(cl.getModifiers()));
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				if( cl.isInterface() && tok.equals("abstract") ) continue;
				out.print(tok + " ");
			}

			String classPrefix = "";

			if (!cl.isInterface()) {
				classPrefix = classPrefix + " class";
				classPrefix = classPrefix.trim();
			}

			out.print(classPrefix + " ");

			out.print(Scene.v().quotedNameOf(cl.getName()).toString());
		}

		// Print extension
		{
			if (cl.hasSuperclass())
				out.print(
						" extends "
								+ Scene.v().quotedNameOf(cl.getSuperclass().getName())
								+ "");
		}

		// Print interfaces
		{
			Iterator interfaceIt = cl.getInterfaces().iterator();

			if (interfaceIt.hasNext()) {
				out.print(" implements ");

				out.print(
						""
								+ Scene.v().quotedNameOf(
										((SootClass) interfaceIt.next()).getName())
										+ "");

				while (interfaceIt.hasNext()) {
					out.print(",");
					out.print(
							" "
									+ Scene.v().quotedNameOf(
											((SootClass) interfaceIt.next()).getName())
											+ "");
				}
			}
		}

		/** END VISIT CLASS DECLARATION */
		this.visitor.endVisitDeclaration(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		out.println();
		incJimpleLnNum();

		out.println("{");
		incJimpleLnNum();
		if (Options.v().print_tags_in_output()){
			Iterator cTagIterator = cl.getTags().iterator();
			while (cTagIterator.hasNext()) {
				Tag t = (Tag) cTagIterator.next();
				out.print("/*");
				out.print(t.toString());
				out.println("*/");
			}
		}

		/** START VISIT CLASS BODY */
		this.visitor.startVisitBody(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		// Print fields
		{
			Iterator fieldIt = cl.getFields().iterator();

			if (fieldIt.hasNext()) {
				while (fieldIt.hasNext()) {
					SootField f = (SootField) fieldIt.next();

					if (f.isPhantom())
						continue;

					if (Options.v().print_tags_in_output()){
						Iterator fTagIterator = f.getTags().iterator();
						while (fTagIterator.hasNext()) {
							Tag t = (Tag) fTagIterator.next();
							out.print("/*");
							out.print(t.toString());
							out.println("*/");
						}
					}

					out.print("    ");

					/** START VISIT FIELD DECLARATION */
					this.visitor.startVisit(f, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
					out.print(f.getDeclaration());
					/** END VISIT FIELD DECLARATION */
					this.visitor.endVisit(f, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

					out.println(";");

					if (addJimpleLn()) {
						setJimpleLnNum(addJimpleLnTags(getJimpleLnNum(), f));		
					}

					//incJimpleLnNum();
				}
			}
		}

		// Print methods
		{
			Iterator methodIt = cl.methodIterator();

			if (methodIt.hasNext()) {
				if (cl.getMethodCount() != 0) {
					out.println();
					incJimpleLnNum();
				}

				while (methodIt.hasNext()) {
					SootMethod method = (SootMethod) methodIt.next();

					if (method.isPhantom())
						continue;

					/** START VISIT METHOD */
					this.visitor.startVisit(method, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

					if (!Modifier.isAbstract(method.getModifiers())
							&& !Modifier.isNative(method.getModifiers())) {
						if (!method.hasActiveBody())
							throw new RuntimeException("method " + method.getName() + " has no active body!");
						else
							if (Options.v().print_tags_in_output()){
								Iterator mTagIterator = method.getTags().iterator();
								while (mTagIterator.hasNext()) {
									Tag t = (Tag) mTagIterator.next();
									out.print("/*");
									out.print(t.toString());
									out.println("*/");
								}
							}
						printTo(method.getActiveBody(), out);

						if (methodIt.hasNext()) {
							out.println();
							incJimpleLnNum();
						}
					} else {

						if (Options.v().print_tags_in_output()){
							Iterator mTagIterator = method.getTags().iterator();
							while (mTagIterator.hasNext()) {
								Tag t = (Tag) mTagIterator.next();
								out.print("/*");
								out.print(t.toString());
								out.println("*/");
							}
						}

						out.print("    ");

						/** START VISIT (ABSTRACT) METHOD DECLARATION */
						this.visitor.startVisitDeclaration(method, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
						out.print(method.getDeclaration());
						/** END VISIT (ABSTRACT) METHOD DECLARATION */
						this.visitor.endVisitDeclaration(method, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());


						out.println(";");
						incJimpleLnNum();
						if (methodIt.hasNext()) {
							out.println();
							incJimpleLnNum();
						}
					}

					/** END VISIT METHOD */
					this.visitor.endVisit(method, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
				}
			}
		}

		/** END VISIT CLASS BODY */
		this.visitor.endVisitBody(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		out.println("}");
		incJimpleLnNum();

		/** END VISIT CLASS */
		this.visitor.endVisit(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
		
		/** END VISIT */
		this.visitor.end(cl, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
	}

	/**
	 *   Prints out the method corresponding to b Body, (declaration and body),
	 *   in the textual format corresponding to the IR used to encode b body.
	 *
	 *   @param out a PrintWriter instance to print to.
	 */
	public void printTo(Body b, PrintWriter out) {
		b.validate();

		boolean isPrecise = !useAbbreviations();

		String decl = b.getMethod().getDeclaration();

		out.print("    ");
		/** START VISIT METHOD DECLARATION */
		this.visitor.startVisitDeclaration(b.getMethod(), this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
		out.println(decl);
		/** END VISIT METHOD DECLARATION */
		this.visitor.endVisitDeclaration(b.getMethod(), this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		if (addJimpleLn()) {
			setJimpleLnNum(addJimpleLnTags(getJimpleLnNum(), b.getMethod()));
		}

		out.println("    {");
		incJimpleLnNum();

		UnitGraph unitGraph = new soot.toolkits.graph.BriefUnitGraph(b);

		LabeledUnitPrinter up;
		if( isPrecise ) up = new NormalUnitPrinter(b);
		else up = new BriefUnitPrinter(b);

		if (addJimpleLn()) {
			up.setPositionTagger(new AttributesUnitPrinter(getJimpleLnNum()));
		}

		/** START VISIT METHOD BODY */
		this.visitor.startVisitBody(b.getMethod(), this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		printLocalsInBody(b, up);
		printStatementsInBody(b, out, up, unitGraph);

		/** END VISIT METHOD BODY */
		this.visitor.endVisitBody(b.getMethod(), this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());

		out.println("    }");
		incJimpleLnNum();

	}

	/** Prints the given <code>JimpleBody</code> to the specified <code>PrintWriter</code>. */
	private void printStatementsInBody(Body body, java.io.PrintWriter out, LabeledUnitPrinter up, UnitGraph unitGraph ) {
		Chain units = body.getUnits();
		Iterator unitIt = units.iterator();
		Unit currentStmt = null, previousStmt;

		while (unitIt.hasNext()) {

			previousStmt = currentStmt;
			currentStmt = (Unit) unitIt.next();

			// Print appropriate header.
			{
				// Put an empty line if the previous node was a branch node, the current node is a join node
				//   or the previous statement does not have body statement as a successor, or if
				//   body statement has a label on it

				if (currentStmt != units.getFirst()) {
					if (unitGraph.getSuccsOf(previousStmt).size() != 1
							|| unitGraph.getPredsOf(currentStmt).size() != 1
							|| up.labels().containsKey(currentStmt)) {
						up.newline();
					} else {
						// Or if the previous node does not have body statement as a successor.

						List succs = unitGraph.getSuccsOf(previousStmt);

						if (succs.get(0) != currentStmt) {
							up.newline();
						}
					}
				}

				if (up.labels().containsKey(currentStmt)) {
					up.unitRef( currentStmt, true );
					up.literal(":");
					up.newline();
				}

				if (up.references().containsKey(currentStmt)) {
					up.unitRef( currentStmt, false );
				}
			}

			up.startUnit(currentStmt);
			/** START VISIT STATEMENT */
			this.visitor.startVisit(currentStmt, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
			currentStmt.toString(up);
			/** END VISIT STATEMENT */
			this.visitor.endVisit(currentStmt, this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
			up.endUnit(currentStmt);

			up.literal(";");
			up.newline();

			// only print them if not generating attributes files 
			// because they mess up line number
			//if (!addJimpleLn()) {
			if (Options.v().print_tags_in_output()){
				Iterator tagIterator = currentStmt.getTags().iterator();
				while (tagIterator.hasNext()) {
					Tag t = (Tag) tagIterator.next();
					up.noIndent();
					up.literal("/*");
					up.literal(t.toString());
					up.literal("*/");
					up.newline();
				}
				/*Iterator udIt = currentStmt.getUseAndDefBoxes().iterator();
                while (udIt.hasNext()) {
                    ValueBox temp = (ValueBox)udIt.next();
                    Iterator vbtags = temp.getTags().iterator();
                    while (vbtags.hasNext()) {
                        Tag t = (Tag) vbtags.next();
                        up.noIndent();
                        up.literal("VB Tag: "+t.toString());
                        up.newline();
                    }
                }*/
			}
		}

		out.print(up.toString());
		if (addJimpleLn()){
			setJimpleLnNum(up.getPositionTagger().getEndLn());
		}


		// Print out exceptions
		{
			Iterator trapIt = body.getTraps().iterator();

			if (trapIt.hasNext()) {
				out.println();
				incJimpleLnNum();
			}

			while (trapIt.hasNext()) {
				Trap trap = (Trap) trapIt.next();

				out.println(
						"        catch "
								+ Scene.v().quotedNameOf(trap.getException().getName())
								+ " from "
								+ up.labels().get(trap.getBeginUnit())
								+ " to "
								+ up.labels().get(trap.getEndUnit())
								+ " with "
								+ up.labels().get(trap.getHandlerUnit())
								+ ";");

				incJimpleLnNum();

			}
		}

	}

	private int addJimpleLnTags(int lnNum, SootMethod meth) {
		meth.addTag(new JimpleLineNumberTag(lnNum));
		lnNum++;
		return lnNum;
	}

	private int addJimpleLnTags(int lnNum, SootField f) {
		f.addTag(new JimpleLineNumberTag(lnNum));
		lnNum++;
		return lnNum;
	}

	/** Prints the given <code>JimpleBody</code> to the specified <code>PrintWriter</code>. */
	private void printLocalsInBody(
			Body body,
			UnitPrinter up) {
		// Print out local variables
		{
			Map<Type, List> typeToLocals =
					new DeterministicHashMap(body.getLocalCount() * 2 + 1, 0.7f);

			// Collect locals
			{
				Iterator localIt = body.getLocals().iterator();

				while (localIt.hasNext()) {
					Local local = (Local) localIt.next();

					List<Local> localList;

					Type t = local.getType();

					if (typeToLocals.containsKey(t))
						localList = typeToLocals.get(t);
					else {
						localList = new ArrayList<Local>();
						typeToLocals.put(t, localList);
					}

					localList.add(local);
				}
			}

			// Print locals
			{
				Iterator<Type> typeIt = typeToLocals.keySet().iterator();

				while (typeIt.hasNext()) {
					Type type = typeIt.next();

					List localList = typeToLocals.get(type);
					Object[] locals = localList.toArray();
					up.type( type );
					up.literal( " " );

					for (int k = 0; k < locals.length; k++) {
						if (k != 0)
							up.literal( ", " );

						/** START VISIT LOCAL */
						this.visitor.startVisit((Local)locals[k], this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
						up.local( (Local) locals[k] );
						/** END VISIT LOCAL */
						this.visitor.endVisit((Local)locals[k], this.escapedWriter.getNumCharsWritten(), this.escapedWriter.getCurLineNumber());
					}

					up.literal(";");
					up.newline();
				}
			}

			if (!typeToLocals.isEmpty()) {
				up.newline();
			}
		}
	}
}
