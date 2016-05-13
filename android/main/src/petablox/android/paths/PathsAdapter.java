package petablox.android.paths;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Schema;
import org.xml.sax.SAXException;

import petablox.analyses.alias.Ctxt;
import petablox.analyses.alias.DomC;
import petablox.analyses.field.DomF;
import petablox.analyses.var.DomV;
import petablox.android.analyses.DomU;
import petablox.android.analyses.DomCLbl;
import petablox.android.util.DomMap;
import petablox.util.tuple.object.Pair;
import petablox.android.util.PropertyHelper;
import petablox.android.util.StringHelper;
import soot.Local;

public class PathsAdapter {
	private final DomMap doms = new DomMap();
	private final JAXBContext jc;
	private final Schema schema;

	public PathsAdapter(String schemaFile) throws TranslationException {
		try {
			// Feed JAXB the package where schema-generated classes live.
			jc = JAXBContext.newInstance("petablox.android.paths.raw");
			// Specify a schema file, to validate the paths XML file against.
			// This should be the same schema that generated the classes in the
			// above package.
			SchemaFactory sf =
				SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
			schema = sf.newSchema(new File(schemaFile));
		} catch (JAXBException exc) {
			throw new TranslationException(exc);
		} catch (SAXException exc) {
			throw new TranslationException(exc);
		}
	}

	public List<Path> getFlatPaths(String rawPathsFile)
		throws TranslationException {
		try {
			petablox.android.paths.raw.Paths rawPaths = readRawPaths(rawPathsFile);
			flattenRawPaths(rawPaths, true);
			return convertPaths(rawPaths);
		} catch (JAXBException exc) {
			throw new TranslationException(exc);
		}
	}

	public void normalizeRawPaths(String rawPathsFile,
								  String normalPathsFile)
		throws TranslationException  {
		try {
			petablox.android.paths.raw.Paths rawPaths = readRawPaths(rawPathsFile);
			flattenRawPaths(rawPaths, false);
			translateNodeNames(rawPaths, true);
			writeRawPaths(rawPaths, normalPathsFile);
		} catch (JAXBException exc) {
			throw new TranslationException(exc);
		} catch (FileNotFoundException exc) {
			throw new TranslationException(exc);
		}
	}

	public static class TranslationException extends Exception {
		public TranslationException(String msg) {
			super(msg);
		}

		public TranslationException(Throwable cause) {
			super(cause);
		}
	}

	private petablox.android.paths.raw.Paths readRawPaths(String inFile)
		throws JAXBException {
		Unmarshaller u = jc.createUnmarshaller();
		u.setSchema(schema);
		// Fail at the first validation error.
		u.setEventHandler(new DefaultValidationEventHandler());
		return (petablox.android.paths.raw.Paths) u.unmarshal(new File(inFile));
	}

	private void writeRawPaths(petablox.android.paths.raw.Paths rawPaths, String outFile)
		throws FileNotFoundException, JAXBException {
        Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		// TODO: We don't validate the rawPaths tree, because the calling code
		// alters the tree in such a way that it no longer adheres to some of
		// the non-structural constraints in the schema.
        //m.setSchema(schema);
		//m.setEventHandler(new DefaultValidationEventHandler());
        m.marshal(rawPaths, new FileOutputStream(outFile));
	}

	// Expects rawPaths to be fully flat.
	private List<Path> convertPaths(petablox.android.paths.raw.Paths rawPaths)
		throws TranslationException{
		List<Path> paths = new ArrayList<Path>();

		for (petablox.android.paths.raw.Edge rawEdge : rawPaths.getEdges()) {
			Point start = rawNodeToPoint(rawEdge.getFrom());
			Point end = rawNodeToPoint(rawEdge.getTo());
			for (petablox.android.paths.raw.Path rawPath : rawEdge.getPaths()) {
				petablox.android.paths.raw.Step topRawStep = rawPath.getTopStep();

				List<Step> steps = new ArrayList<Step>();
				for (petablox.android.paths.raw.Step s : topRawStep.getSubSteps()) {
					// The sub-steps of the top step should be completely flat.
					assert(rawStepIsTerminal(s));
					String tgtNode = s.isReverse() ? s.getFrom() : s.getTo();
					steps.add(new Step(s.getSymbol(), s.isReverse(),
									   rawNodeToPoint(tgtNode)));
				}
				paths.add(new Path(start, end, steps));
			}
		}

		doms.clear();
		return paths;
	}

	// TODO: The following methods modify raw Paths in-place, and conceptually
	// belong in the classes of petablox.android.paths.raw, but we'd need to edit the JAXB
	// binding scheme to add them.

	private void flattenRawPaths(petablox.android.paths.raw.Paths rawPaths,
								 boolean fully) {
		for (petablox.android.paths.raw.Edge rawEdge : rawPaths.getEdges()) {
			for (petablox.android.paths.raw.Path rawPath : rawEdge.getPaths()) {
				petablox.android.paths.raw.Step topRawStep = rawPath.getTopStep();
				flattenRawSubSteps(topRawStep, fully);
			}
		}
	}

	private void flattenRawSubSteps(petablox.android.paths.raw.Step step, boolean fully) {
		List<petablox.android.paths.raw.Step> subSteps = step.getSubSteps();
		List<petablox.android.paths.raw.Step> flatSubSteps =
			new ArrayList<petablox.android.paths.raw.Step>();

		for (petablox.android.paths.raw.Step ss : subSteps) {
			flattenRawSubSteps(ss, fully);
			if (rawStepIsTerminal(ss) ||
				!fully && !rawStepIsIntermediate(ss)) {
				// Steps corresponding to non-temporary symbols are normally
				// retained.
				// TODO: Could also skip some non-terminals that don't offer
				// much information, e.g. in the case of transitive rules.
				flatSubSteps.add(ss);
			} else {
				// We skip this step, and instead record its sub-steps
				// directly.
				if (ss.isReverse()) {
					// If the step to skip was traversed in reverse, we need to
					// reverse the order in which we record its sub-steps.
					reverseRawStepsList(ss.getSubSteps());
				}
				flatSubSteps.addAll(ss.getSubSteps());
			}
		}

		// Replace the original sub-steps list with the flattened one.
		// TODO: The JAXB-generated class for raw Steps doesn't allow us to
		// simply swap out its sub-steps list for another. We instead have to
		// make a copy of the flattened list.
		subSteps.clear();
		subSteps.addAll(flatSubSteps);
	}

	private boolean rawStepIsIntermediate(petablox.android.paths.raw.Step step) {
		// TODO: Should get this information from the output of the CFG parser.
		return step.getSymbol().startsWith("%");
	}

	private boolean rawStepIsTerminal(petablox.android.paths.raw.Step step) {
		// TODO: Should get this information from the output of the CFG parser.
		char firstLetter = step.getSymbol().charAt(0);
		return firstLetter >= 'a' && firstLetter <= 'z';
	}

	private void reverseRawStepsList(List<petablox.android.paths.raw.Step> rawSteps) {
		// Reverse the order that we traverse the sub-steps.
		Collections.reverse(rawSteps);
		// Also switch the 'reverse' modifier on each of the sub-steps.
		for (petablox.android.paths.raw.Step s : rawSteps) {
			s.setReverse(!s.isReverse());
		}
	}

	private void translateNodeNames(petablox.android.paths.raw.Paths rawPaths,
									boolean useShortNames)
		throws TranslationException {
		for (petablox.android.paths.raw.Edge rawEdge : rawPaths.getEdges()) {
			Point from = rawNodeToPoint(rawEdge.getFrom());
			rawEdge.setFrom(useShortNames ? from.toShortString()
							: from.toString());
			Point to = rawNodeToPoint(rawEdge.getTo());
			rawEdge.setTo(useShortNames ? to.toShortString() : to.toString());

			for (petablox.android.paths.raw.Path rawPath : rawEdge.getPaths()) {
				petablox.android.paths.raw.Step topRawStep = rawPath.getTopStep();
				translateNodeNames(topRawStep, useShortNames);
			}
		}

		doms.clear();
	}

	private void translateNodeNames(petablox.android.paths.raw.Step rawStep,
									boolean useShortNames)
		throws TranslationException {
		Point from = rawNodeToPoint(rawStep.getFrom());
		rawStep.setFrom(useShortNames ? from.toShortString()
						: from.toString());
		Point to = rawNodeToPoint(rawStep.getTo());
		rawStep.setTo(useShortNames ? to.toShortString() : to.toString());

		for (petablox.android.paths.raw.Step ss : rawStep.getSubSteps()) {
			translateNodeNames(ss, useShortNames);
		}
	}

	// TODO: In the following, we have hardcoded information regarding the node
	// naming scheme.

	private Point rawNodeToPoint(String rawNode) throws TranslationException {
		char tag = rawNode.charAt(0);
		switch (tag) {
		case 'v':
			Pair<Integer,Integer> vc = getTwoDomIndices(rawNode);
			int v = vc.val0.intValue();
			int c_v = vc.val1.intValue();
			return new CtxtVarPoint(((DomC) doms.get("C")).get(c_v),
									(Local)((DomV) doms.get("V")).get(v));
		case 'u':
			Pair<Integer,Integer> uc = getTwoDomIndices(rawNode);
			int u = uc.val0.intValue();
			int c_u = uc.val1.intValue();
			return new CtxtVarPoint(((DomC) doms.get("C")).get(c_u),
									((DomU) doms.get("U")).get(u));
		case 'o':
			int o = getSingleDomIndex(rawNode);
			return new CtxtObjPoint(((DomC) doms.get("C")).get(o));
		case 'f':
			int f = getSingleDomIndex(rawNode);
			return new StatFldPoint(((DomF) doms.get("F")).get(f));
		case 'l':
			int cl = getSingleDomIndex(rawNode);
			Pair<String,Ctxt> ctxtLabel = ((DomCLbl) doms.get("CLbl")).get(cl);
			return new CtxtLabelPoint(ctxtLabel.val1, ctxtLabel.val0);
		default:
			throw new TranslationException("Invalid node name: " + rawNode);
		}
	}

	private static int getSingleDomIndex(String rawNode) {
		return Integer.parseInt(rawNode.substring(1));
	}

	private static Pair<Integer,Integer> getTwoDomIndices(String rawNode)
		throws TranslationException {
		List<String> indexStrs = StringHelper.split(rawNode.substring(1), "_");
		if (indexStrs.size() != 2) {
			throw new TranslationException("Invalid node name: " + rawNode);
		}
		return new Pair<Integer,Integer>(new Integer(indexStrs.get(0)),
										 new Integer(indexStrs.get(1)));
	}
}
