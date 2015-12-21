// EdgeLabeler.java, created May 17, 2004 3:14:06 PM 2004 by jwhaley
// Copyright (C) 2004 John Whaley <jwhaley@alum.mit.edu>
// Licensed under the terms of the GNU LGPL; see COPYING for details.
package jwutil.graphs;

/**
 * EdgeLabeler
 * 
 * @author jwhaley
 * @version $Id: EdgeLabeler.java,v 1.1 2004/09/27 22:42:32 joewhaley Exp $
 */
public interface EdgeLabeler {
    
    public Object getLabel(Object from, Object to);
    
}
