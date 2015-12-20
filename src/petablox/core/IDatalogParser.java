package petablox.core;

import java.io.File;
import java.io.IOException;

/**
 * An interface for classes that parse metadata from 
 * specific variants of Datalog.
 */
public interface IDatalogParser {
    /**
     * Parses a datalog file and returns the extracted metadata.
     * 
     * @param file the file to parse, must exist and be readable
     * @return the extracted metadata
     * @throws IOException if there is a problem reading the file
     */
    public DatalogMetadata parseMetadata(File file) throws IOException;
}
