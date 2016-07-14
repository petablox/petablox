package stamp.util;

import shord.project.ClassicProject;
import shord.project.analyses.ProgramRel;

/**
 * A map from names to relations, which is filled lazily as new relations are
 * requested.
 *
 * Any changes made to the mapped relations are only stored to disk when the
 * map is {@link #clear() cleared}.
 */
public class RelMap extends LazyMap<String,ProgramRel> {
	private final boolean createNew;

	/**
	 * @param createNew whether to create new, modifiable relations, or use
	 * unmodifiable copies of existing ones
	 */
	public RelMap(boolean createNew) {
		this.createNew = createNew;
	}

	@Override
	public ProgramRel lazyFill(String relName) {
		// TODO: Can this fail for non-existent relations? Need to configure
		// targets / skip Project infrastructure?
		ProgramRel rel =  (ProgramRel) ClassicProject.g().getTrgt(relName);
		if (createNew) {
			if (rel.isOpen() || ClassicProject.g().isTrgtDone(relName)) {
				// TODO: This will cause problems with RelParser if two .dpt
				// files happen to fill in the same relation.
				throw new RuntimeException("Relation " + relName +
										   " already exists");
			}
			rel.zero();
		} else {
			if (!ClassicProject.g().isTrgtDone(relName)) {
				throw new RuntimeException("Relation " + relName +
										   " has not been filled yet");
			}
			if (!rel.isOpen()) {
				rel.load();
			}
		}
		return rel;
	}

	/**
	 * Empty the map, storing all changes to the mapped ProgramRels to disk (if
	 * they were opened for writing) and removing them from memory.
	 */
	@Override
	public void clear() {
		for (ProgramRel rel : values()) {
			if (createNew) {
				// Save and close
				rel.save();
			} else {
				// Don't save any changes if relations were opened in read-only
				// mode.
				// TODO: Ideally, we should disallow changes.
				rel.close();
			}
		}
		super.clear();
	}
}
