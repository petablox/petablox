package chord.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

import chord.bddbddb.RelSign;

/**
 * A simple container for Datalog analysis metadata.  This data 
 * could be produced from either BDD or LogicBlox style of files.
 */
public class DatalogMetadata {
    // absolute filename of the datalog program
    private String fileName;
    private Set<String> domNames;
    private Map<String, RelSign> consumedRels;
    private Map<String, RelSign> producedRels;
    private String dlogName;
    private List<String> minorDomNames;
    
    /** The BDD variable order, not applicable for LogicBlox. */
    private String bddOrder;
    
    private boolean hasNoErrors = true;

    public DatalogMetadata() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Set<String> getMajorDomNames() {
        return domNames;
    }

    public void setMajorDomNames(Set<String> majorDomNames) {
        this.domNames = majorDomNames;
    }

    public Map<String, RelSign> getConsumedRels() {
        return consumedRels;
    }

    public void setConsumedRels(Map<String, RelSign> consumedRels) {
        this.consumedRels = consumedRels;
    }

    public Map<String, RelSign> getProducedRels() {
        return producedRels;
    }

    public void setProducedRels(Map<String, RelSign> producedRels) {
        this.producedRels = producedRels;
    }

    public String getDlogName() {
        return dlogName;
    }

    public void setDlogName(String dlogName) {
        this.dlogName = dlogName;
    }    
    
    public void setHasNoErrors(boolean hasNoErrors) {
        this.hasNoErrors = hasNoErrors;
    }
    
    public boolean hasNoErrors() { return hasNoErrors; }
    
    public void setMinorDomNames(List<String> minorDomNames) {
        this.minorDomNames = minorDomNames;
    }
    
    public List<String> getMinorDomNames() {
        return minorDomNames;
    }
    
    /**
     * Returns the BDD variable order.  This returns <code>null</code> for 
     * if this is produced for LogicBlox.
     * @return
     */
    public String getBddOrder() {
        return bddOrder;
    }
    
    public void setBddOrder(String bddOrder) {
        this.bddOrder = bddOrder;
    }

}
