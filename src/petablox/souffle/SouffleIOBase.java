package petablox.souffle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import petablox.bddbddb.Dom;
import petablox.bddbddb.Rel;
import petablox.project.Config;
import petablox.project.PetabloxException;

public abstract class SouffleIOBase {
	protected String workDir = Config.souffleWorkDirName;
	
	protected PrintWriter createPrintWriter(File outFile) { return createPrintWriter(outFile, false); }
    protected PrintWriter createPrintWriter(File outFile, boolean autoFlush) {
        try {
            return new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8"), autoFlush);
        } catch (IOException e) {
            throw new PetabloxException(e);
        }
    }	
	
	public void setWorkDir(String dirName) {
		workDir = Config.souffleWorkDirName;
	}
	
	protected String getDomainDefinition(Dom<?> dom) {
		StringBuilder sb = new StringBuilder();
		sb.append(".symbol_type ");
		sb.append(dom.getName());
		return sb.toString();
	}
	
	protected String getDomainInclude(Dom<?> dom) {
		StringBuilder sb = new StringBuilder();
		sb.append("#ifdef ");
		sb.append(dom.getName() + "\n");
		sb.append("#include ");
		sb.append(dom.getName() + ".dl" + "\n");
		sb.append("#endif\n");
		return sb.toString();
	}

	protected String getRelationDefinition(Rel rel) {
		StringBuilder sb = new StringBuilder();
		sb.append(".decl ");
		sb.append(rel.getName());
		sb.append("(");
		
		int i = 0;
		for (Dom<?> d : rel.getDoms()) {
			sb.append(rel.getName() + i);
			sb.append(":");
			sb.append(d.getName());
			i++;
			
			if (i != rel.getDoms().length) sb.append(",");
		}
		
		sb.append(")");
		
		return sb.toString();
	}
	
}
