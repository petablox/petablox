package chord.bddbddb;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import chord.logicblox.LogicBloxUtils;
import chord.project.Config;
import chord.project.Config.DatalogEngineType;
import chord.util.IndexMap;
import chord.util.Utils;

/**
 * Generic implementation of a BDD-based domain.
 * <p>
 * Typical usage is as follows:
 * <ul>
 * <li> The domain is initialized by calling {@link #setName(String)} which sets the name of the domain. </li>
 * <li> The domain is next built in memory by repeatedly calling {@link #getOrAdd(Object)} with the argument in each call being a value
 * to be added to the domain.  If the value already exists in the domain then the call does not have any effect.  Otherwise, the value
 * is mapped to integer K in the domain where K is the number of values already in the domain. </li>
 * <li> The domain built in memory is reflected onto disk by calling {@link #saveToBDD(String,boolean)}. </li>
 * <li> The domain on disk can be read by a Datalog program. </li>
 * <li> The domain in memory can be read by calling any of the following:
 * <ul>
 * <li> {@link #iterator()}, which gives an iterator over the values in the domain in memory in the order in which they were added, </li>
 * <li> {@link #get(int)}, which gives the value mapped to the specified integer in the domain in memory, and </li>
 * <li> {@link #indexOf(Object)}, which gives the integer mapped to the specified value in the domain in memory. </li>
 * </ul>
 * </li>
 * </ul>
 *
 * @param <T> The type of values in the domain.
 * 
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class Dom<T> extends IndexMap<T> {
    protected String name;
    public void setName(String name) {
        assert (name != null);
        assert (this.name == null);
        this.name = name;
    }
    public String getName() {
        return name;
    }
    
    /**
     * Reflects the domain in memory onto disk.
     */
    public void saveToBDD(String dirName, boolean saveDomMap) throws IOException {
        String mapFileName = "";
        if (saveDomMap) {
            mapFileName = name + ".map";
            File file = new File(dirName, mapFileName);
            PrintWriter out = new PrintWriter(file);
            int size = size();
            for (int i = 0; i < size; i++) {
                T val = get(i);
                out.println(toUniqueString(val));
            }
            out.close();
        }
        String domFileName = name + ".dom";
        File file = new File(dirName, domFileName);
        PrintWriter out = new PrintWriter(file);
        int size = size();
        out.println(name + " " + size + " " + mapFileName);
        out.close();
    }
    
    /**
     *  Saves this domain in a format suitable for loading into LogicBlox.
     *  For a domain named <tt>N</tt>, two files will be created in <tt>chord.logicblox.work.dir</tt>: 
     *  <tt>N.type</tt>, which contains predicate definitions and <tt>N.csv</tt>, which 
     *  contains <tt>chord.logicblox.delim</tt>-delimited (index, string) pairs.
     *  
     *  @throws IOException if the information cannot be saved successfully
     */
    public void saveToLogicBlox(String dirName) throws IOException {
        Utils.mkdirs(dirName);
        final String DELIM = Config.logicbloxInputDelim;
        final boolean isLb3 = Config.datalogEngine == DatalogEngineType.LOGICBLOX3;

        String intType = isLb3 ? "uint[64]" : "int";

        // save the data values
        File factsFile = new File(dirName, name + ".csv");
        if (!factsFile.getParentFile().exists()) {
            System.err.println("WARN: No parent directory: "
                + factsFile.getParentFile().getAbsolutePath());
        }
        PrintWriter out = new PrintWriter(factsFile);
        for (int i = 0, size = this.size(); i < size; ++i) {
            out.print(i);
            out.print(DELIM);
            out.println(this.toUniqueString(i));
        }
        out.flush();
        out.close();
        if (out.checkError()) {
            throw new IOException("An error occurred writing domain " + name
                + " to " + factsFile.getAbsolutePath());
        }

        // and the type declaration
        File typeFile = new File(dirName, name + ".type");
        out = new PrintWriter(typeFile);
        out.println(getLogicBloxType());
        out.flush();
        out.close();
        if (out.checkError()) {
            throw new IOException("An error occurred writing type information for " + name
                + " to " + typeFile.getAbsolutePath());
        }
        
        // and an import file
        File importFile = new File(dirName, name + ".import");
        out = new PrintWriter(importFile);
        out.println("_in(" + (isLb3 ? "" : "offset; ") + "id, val) -> " + (isLb3 ? "" : intType + "(offset), ") + intType + "(id), string(val).");
        out.println("lang:physical:filePath[`_in] = \"" + factsFile.getAbsolutePath() + "\".");
        if (isLb3)
            out.println("lang:physical:storageModel[`_in] = \"DelimitedFile\".");
        else
            out.println("lang:physical:fileMode[`_in] = \"import\".");
        out.println("lang:physical:delimiter[`_in] = \"" + DELIM + "\".");
        out.println();
        out.println("+" + name + "(x), +" + name + "_values[id, val] = x <- _in(" + (isLb3 ? "" : "_; ") + "id, val).");
        out.flush();
        out.close();
        if (out.checkError()) {
            throw new IOException("An error occurred writing import logic for " + name
                + " to " + importFile.getAbsolutePath());
        }
        
        LogicBloxUtils.addBlock(typeFile);
        LogicBloxUtils.execFile(importFile);
    }
    
    /**
     * Returns LogicBlox type definitions and constraints for this domain type.
     * 
     * @return the type string, suitable for passing to the LB engine
     */
    public String getLogicBloxType() {
        final boolean isLb3 = Config.datalogEngine == DatalogEngineType.LOGICBLOX3;

        String intType = isLb3 ? "uint[64]" : "int";

        StringBuilder type = new StringBuilder();
        // a new entity type
        type.append(name).append("(x) -> .\n");

        // with a constructor of (index, uniquestring)
        type.append(name).append("_values[id, val] = x -> ").append(name)
            .append("(x), ").append(intType).append("(id), string(val).\n")
            .append("lang:constructor(`").append(name)
            .append("_values).\n");
        if (isLb3) {
            // have to make scalable, values other than ScalableSparse are not
            // documented
            type.append("lang:physical:storageModel[`").append(name)
                .append("] = \"ScalableSparse\".\n")
                .append("lang:physical:capacity[`").append(name).append("] = ").append(getLogicBloxCapacity()).append(".\n");
        }

        // enforce uniqueness
        // FIXME disabled for now, makes import slow to a crawl
//        type.append(name).append("_values[id, val1] = x1, ").append(name)
//            .append("_values[id, val2] = x2 -> ")
//            .append("val1 = val2, x1 = x2.\n");
//        type.append(name).append("_values[id1, val] = x1, ").append(name)
//            .append("_values[id2, val] = x2 -> ")
//            .append("id1 = id2, x1 = x2.\n");

        return type.toString();
    }
    
    /**
     * Returns the capacity of LogicBlox predicates for LB 3, which 
     * is the size of this domain by default.
     * 
     * @return the entity capacity
     */
    public long getLogicBloxCapacity() {
        return this.size();
    }
    
    // subclasses may override
    public String toUniqueString(T val) {
        return val == null ? "null" : val.toString();
    }
    public String toUniqueString(int idx) {
        T val = get(idx);
        return toUniqueString(val);
    }
    /**
     * Prints the values in the domain in memory to the standard output stream.
     */
    public void print() {
        print(System.out);
    }
    /**
     * Prints the values in the domain in memory to the specified output stream.
     * 
     * @param out The output stream to which the values in the domain in memory must be printed.
     */
    public void print(PrintStream out) {
        for (int i = 0; i < size(); i++)
            out.println(get(i));
    }
    public int hashCode() {
        return System.identityHashCode(this);
    }
    public boolean equals(Object o) {
        return this == o;
    }
}
