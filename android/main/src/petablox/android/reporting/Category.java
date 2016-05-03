package petablox.reporting;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import soot.SootClass;
import soot.SootMethod;
import petablox.android.srcmap.sourceinfo.SourceInfo;

/*
 * @author Saswat Anand
**/
public class Category extends Tuple {
	protected Map<Object,Category> subCategories = new LinkedHashMap();
	protected List<Tuple> tuples = new ArrayList();
	protected String type;

	public Category() {
		this(null);
	}

	public Category(Object key) {
		addValue(key);
		if(key instanceof SootClass)
			type = "class";
		else if(key instanceof SootMethod)
			type = "method";
	}
	
	public Category lastSubCat() {
		ArrayList<Object> al = new ArrayList<Object>(subCategories.keySet());
		return subCategories.get(al.get(al.size()-1));
	}

	public Object lastSubCatKey() {
		ArrayList<Object> al = new ArrayList<Object>(subCategories.keySet());
		return al.get(al.size()-1);
	}


	public Category findSubCat(Object key) {
		if (subCategories.isEmpty()) {
			return null;
		}

		ArrayList<Object> al = new ArrayList<Object>(subCategories.keySet());
		Object obj = al.get(al.size()-1);
		Category c = subCategories.get(obj);
		if (key.equals(obj)) {
			return this;
		} 
		return c.findSubCat(key);
	}


	public Category makeOrGetSubCat(Object key) {
		Category sc = subCategories.get(key);
		if(sc == null){
			sc = new Category(key);
			//sc.setSourceInfo(this.sourceInfo);
			subCategories.put(key, sc);
		}
		return sc;
	}

	/**
	 * Place ane existing subcategory. In a Category. Use with care.
	 */
	/*pkgpvt*/ void putSubCat(Object key, Category subCat) {
		subCategories.put(key, subCat);
	}

	/**
	 * Add a supercategory of key above subcategory sub. Sub
	 * Should be a subcategory of this.
	 */
	public Category makeOrGetSupCat(Object sub, Object key) {
		Category oldCat = subCategories.remove(sub);
		if (oldCat == null) {
			return null;
		}

		Category newCat = new Category(key);
		//newCat.setSourceInfo(this.sourceInfo);
		newCat.putSubCat(sub, oldCat);
		subCategories.put(key, newCat);
		return newCat;
	}
	
	public void write(PrintWriter writer) {
		if(type != null)
			writer.println("<category type=\""+type+"\">");
		else
			writer.println("<category>");
		writer.println(str);
		for(Tuple t : tuples)
			t.write(writer);
		
		for(Category c : sortSubCats())
			c.write(writer);

		writer.println("</category>");
	}


	public void writeInsertionOrder(PrintWriter writer) {
		if(type != null)
			writer.println("<category type=\""+type+"\">");
		else
			writer.println("<category>");
		writer.println(str);
		for(Tuple t : tuples)
			t.write(writer);
		
		for(Category c : subCategories.values())
			c.writeInsertionOrder(writer);

		writer.println("</category>");
	}
	
	public Tuple newTuple() {
		Tuple tuple = new Tuple();
		//tuple.setSourceInfo(this.sourceInfo);
		return addTuple(tuple);
	}
	
	public Tuple addTuple(Tuple t) {
		tuples.add(t);
		return t;
	}

	public Category makeOrGetPkgCat(SootClass klass) {
		String name = klass.getName();
		return makeOrGetPkgCat(name, klass);
	}

	public Category makeOrGetPkgCat(SootMethod method) {
		SootClass klass = method.getDeclaringClass();
		return makeOrGetPkgCat(klass).makeOrGetSubCat(method);
	}
	
	private Category makeOrGetPkgCat(String pkg, SootClass klass) {
		int index = pkg.indexOf('.');
		Category ret;
		if(index < 0)
			ret = makeOrGetSubCat(klass);
		else {
			ret = makeOrGetSubCat(pkg.substring(0,index));
			ret = ret.makeOrGetPkgCat(pkg.substring(index+1), klass);
		}
		return ret;
	}
	
	protected List<Category> sortSubCats() {
		//sort
		Map<String,Category> strToSubcat = new HashMap();
		List<String> strs = new ArrayList();
		for(Category sc : subCategories.values()){
			strs.add(sc.str);
			strToSubcat.put(sc.str, sc);
		}
		Collections.sort(strs);
		
		List<Category> ret = new ArrayList();
		for(String str : strs){
			Category c = strToSubcat.get(str);
			ret.add(c);
		}
		return ret;
	}
}
