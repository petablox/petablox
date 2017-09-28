package petablox.souffle;

import java.io.File;
import java.io.PrintWriter;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.PetabloxException;
import petablox.util.Utils;

/**
 * A class for exporting domains/relations to files Souffle can interpret.
 * 
 * Implementation is similar to LB Exporter since they both use csv.
 * @author rmzhang
 */
public class SouffleExporter extends SouffleIOBase {	
	/**
	 * Save the given domain to a file.
	 * Implementation copied from code in LogicBloxExporter to export to csv.
	 * 
	 * @param dom Domain to be saved.
	 */
	public void saveDomain(Dom<?> dom) {
		String domName = dom.getName();
		File factsFile = new File(workDir, domName + ".facts");
		saveDomainData(dom, factsFile);
		
		File domainFile = new File(workDir, domName + ".dl");
		saveDomainInfo(dom, domainFile);
	}

	private void saveDomainInfo(Dom<?> dom, File domainFile) {
		PrintWriter out = createPrintWriter(domainFile);
		out.println(getDomainDefinition(dom));
		out.flush();
		Utils.close(out);
		if (out.checkError()) {
			throw new PetabloxException("Error writing " + dom.getName()
				+ " domain info to " + domainFile.getAbsolutePath());
		}
		
	}

	private void saveDomainData(Dom<?> dom, File factsFile) {
		final String DELIM = "\t";
		PrintWriter out = createPrintWriter(factsFile);
        for (int i = 0, size = dom.size(); i < size; ++i) {
            out.print(i);
            out.print(DELIM);
            out.println(dom.toUniqueString(i));
        }
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("Error writing " + dom.getName()
                + " domain facts to " + factsFile.getAbsolutePath());
        }
		
	}

	/**
	 * Save the given relation to a file.
	 * Implementation copied from code for LogicBloxExporter to export to csv.
	 * 
	 * @param relation Relation to be saved.
	 */
	public void saveRelation(Rel relation) {
		String relName = relation.getName();
        File factsFile = new File(workDir, relName + ".facts");
        saveRelationData(relation, factsFile);
        
        File domainFile = new File(workDir, relName + ".dl");
        saveRelationInfo(relation, domainFile);
	}

	private void saveRelationInfo(Rel relation, File domainFile) {
		PrintWriter out = createPrintWriter(domainFile);
		/*
		 * Make sure that we have references to all domains mentioned in the relation
		 */
		for (Dom<?> d : relation.getDoms()) {
			out.write(getDomainInclude(d));
		}
		
		out.write(getRelationDefinition(relation));
		
		out.flush();
		Utils.close(out);
		if (out.checkError()) {
            throw new PetabloxException("Error writing " + relation.getName()
                + " relation info to " + domainFile.getAbsolutePath());
        }
	}

	private void saveRelationData(Rel relation, File dest) {
		final String DELIM = "\t";
		PrintWriter out = createPrintWriter(dest);
	
		StringBuilder sb = new StringBuilder();
        for (int[] row: relation.getAryNIntTuples()) {
            sb.setLength(0);
            for (int col: row)
                sb.append(col).append(DELIM);
            sb.setLength(sb.length() - DELIM.length()); // remove trailing delim
            out.println(sb.toString());
        }
        
        out.flush();
        Utils.close(out);
        if (out.checkError()) {
            throw new PetabloxException("An error occurred writing relation " +
                relation.getName() + " data to " + dest.getAbsolutePath());
        }
		
	}

}
