package petablox.analyses.datarace;

import java.util.HashMap;

import petablox.analyses.type.DomT;
import petablox.project.Petablox;
import petablox.project.ClassicProject;
import petablox.project.analyses.ProgramRel;

/**
 * Relation containing types that will filter out races on objects of these types.
 * If the system property "petablox.datarace.exclude.typeflag" is set to true, then the filter will be applied. (Default: false)
 * If the above flag is set to true, the types that must be filtered out is read from
 * the relation excludeType
 */
@Petablox(
    name = "excludeType",
    consumes = { "T" },
    sign = "T1:T1"
)
public class RelExcludeType extends ProgramRel {
    public void fill() {
        String tlst = System.getProperty("petablox.datarace.exclude.type", "");
        if (tlst.equals("")) return;

        DomT domT = (DomT) ClassicProject.g().getTrgt("T");
        HashMap<String,Integer> typeName2Id = new HashMap<String,Integer>();
        for (int i = 0; i < domT.size(); i++)
            typeName2Id.put(domT.get(i).toString(), i);
        String[] types = tlst.split(",");
        for (String t : types) {
            String tTrim = t.trim();
            if (typeName2Id.containsKey(tTrim)) {
                int idx = typeName2Id.get(tTrim);
                add(idx);
            }
        }
    }
}
