package petablox.nichrome;

import java.io.File;
import java.io.IOException;

import petablox.bddbddb.BDDBDDBParser;
import petablox.bddbddb.Solver;
import petablox.core.DatalogMetadata;
import petablox.core.IDatalogParser;
import petablox.logicblox.LogicBloxParser;
import petablox.logicblox.LogicBloxUtils;
import petablox.project.Config;
import petablox.project.Messages;
import petablox.project.PetabloxException;
import petablox.project.Config.DatalogEngineType;

/**
 * Provide a unique Datalog engine interface for petablox
 * 
 * @author Xujie Si  (xsi@cis.upenn.edu)
 */

public class NichromeEngine {
    private IDatalogParser parser;
    private DatalogEngineType datalogEngine;
    private DatalogMetadata metadata;

	public NichromeEngine(DatalogEngineType engineType) {
		System.out.println("NichromeEngine is called.");
		if (engineType == null)
			throw new NullPointerException("engineType is null");
		this.datalogEngine = engineType;
		switch (engineType) {
		case BDDBDDB:
			parser = new BDDBDDBParser();
			break;
		case LOGICBLOX3:
		case LOGICBLOX4:
			parser = new LogicBloxParser();
			break;
		default:
			throw new PetabloxException("Unhandled datalog engine type: " + engineType);
		}
	}

    public DatalogMetadata parse(String fileName) throws IOException {
        metadata = parser.parseMetadata(new File(fileName));
        return metadata;
    }

	
    public void run() {
        switch (datalogEngine) {
        case BDDBDDB:
            Solver.run(metadata.getFileName());
            break;
        case LOGICBLOX3:
        case LOGICBLOX4:
            if (Config.verbose >= 1)
                Messages.log("Adding block from: %s", metadata.getFileName());
            LogicBloxUtils.addBlock(new File(metadata.getFileName()));
            break;
        default:
            throw new PetabloxException("FIXME: Unhandled datalog engine type: " + datalogEngine);
        }
    }

	
}
