/*
 * Copyright (C) 2000 by ETHZ/INF/CS
 * All rights reserved
 * 
 * @version $Id$
 * @author Florian Schneider
 */

public class TourElement {
    int[] prefix=new int[Tsp.MAX_TOUR_SIZE];
    int conn;
    int last;
    int prefix_weight;
    int lower_bound;
    int mst_weight;
}
