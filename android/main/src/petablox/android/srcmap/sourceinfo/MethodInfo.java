package petablox.android.srcmap.sourceinfo;

import java.util.List;

import petablox.android.srcmap.Marker;

/**
 * @author Saswat Anand 
 */
public interface MethodInfo {
	public abstract List<Marker> markers(int line, String markerType, String sig);
}	
