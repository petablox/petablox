package petablox.souffle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import petablox.bddbddb.Rel;
import petablox.project.PetabloxException;
import petablox.util.Utils;

public class SouffleImporter extends SouffleIOBase {

	public void importRelation(Rel rel) {
		File f = new File(rel.getName() + ".csv");
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				rel.add(parseIntRow(line));
			}
			Utils.close(br);
		} catch (IOException e) {
			throw new PetabloxException(e);
		}
		
	}
	
	private int[] parseIntRow(String line) {
        String[] parts = line.split("\\s+");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; ++i)
            result[i] = Integer.parseInt(parts[i], 10);
        return result;
    }

}
