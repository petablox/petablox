package stamp.missingmodels.viz.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;
import stamp.missingmodels.util.FileManager.FileType;
import stamp.missingmodels.util.FileManager.StampOutputFile;

public class StampRelationOutputFile implements StampOutputFile {
	private final String relationName;
	
	public StampRelationOutputFile(String relationName) {
		this.relationName = relationName;
	}
	
	@Override
	public String getName() {
		return "relations/" + this.relationName + ".rel";
	}

	@Override
	public FileType getType() {
		return FileType.OUTPUT;
	}

	@Override
	public String getContent() {
		StringBuilder sb = new StringBuilder();
		
		final ProgramRel rel = (ProgramRel)ClassicProject.g().getTrgt(this.relationName);
		rel.load();
		List<int[]> res = new ArrayList<int[]>();
		for(int[] tuple : rel.getAryNIntTuples()) {
			res.add(tuple);
		}
		
		Collections.sort(res, new Comparator<int[]>() {
			@Override
			public int compare(int[] arg0, int[] arg1) {
				if(arg0.length != arg1.length) {
					throw new RuntimeException("Error in comparator argument lengths: " + arg0.length + " vs. " + arg1.length + "!");
				}
				for(int i=0; i<arg0.length; i++) {
					if(arg0[i] < arg1[i]) {
						return -1;
					} else if(arg0[i] > arg1[i]) {
						return 1;
					}
				}
				return 0;
			}			
		});
		
		for(int[] tuple : res) {
			for(int i=0; i<tuple.length; i++) {
				sb.append(tuple[i] + ",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("\n");
		}
		
		return sb.toString();
	}

}
