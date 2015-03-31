/*
 * Copyright (C) 2000 by ETHZ/INF/CS
 * All rights reserved
 * 
 * @version $Id$
 * @author Florian Schneider
 */

public class TspSolver extends Thread {

    static final boolean debug = Tsp.debug;

    static int[][] weights = new int[Tsp.MAX_TOUR_SIZE][Tsp.MAX_TOUR_SIZE];
    static int TourStackTop;
    static int Done;
    static int PrioQLast;
    static int MinTourLen;
    static int[] MinTour = new int[Tsp.MAX_TOUR_SIZE];
    static Integer MinLock = new Integer(0);
    static Integer TourLock = new Integer(0);
    static Integer barrier = new Integer(0);
    static PrioQElement[] PrioQ = new PrioQElement[Tsp.MAX_NUM_TOURS];
    static int[] TourStack = new int[Tsp.MAX_NUM_TOURS];
    static TourElement[] Tours = new TourElement[Tsp.MAX_NUM_TOURS];

    /* non-static member variables, used by recusive_solve and visit_nodes */
    int CurDist, PathLen;
    int[] Visit = new int[Tsp.MAX_TOUR_SIZE];
    int[] Path = new int[Tsp.MAX_TOUR_SIZE];
    int visitNodes;

    public void run() {
	Worker();
    }

    void Worker() {
	int curr = -1;
	for ( ; ; ) {
	    /* Continuously get a tour and split. */
	    curr = get_tour(curr);
	    if (curr < 0) {
		if (debug) 
		    System.out.println("Worker: curr = " + curr); 
		break;
	    }
	    recursive_solve(curr);
	    if (debug) 
		DumpPrioQ();
	}
    }

    /*
     * new_tour():
     *
     *    Create a new tour structure given an existing tour structure and
     *  the next edge to add to the tour.  Returns the index of the new structure.
     *
     */
    static int new_tour(int prev_index, int move) {
	    int index = 0;
	    int i;
	    TourElement curr, prev;
	    
	    synchronized(TourLock) {
		if (debug) 
		    System.out.println("new_tour");

		if (TourStackTop >= 0) 
		    index = TourStack[TourStackTop--];
		else {
		    System.out.println("TourStackTop: " + TourStackTop);
		    System.exit(-1);
		}

		curr = Tours[index];
		prev = Tours[prev_index];
       
		for (i = 0; i < Tsp.TspSize; i++) {
		    curr.prefix[i] = prev.prefix[i];
		    curr.conn = prev.conn;
		}
		curr.last = prev.last;
		curr.prefix_weight = prev.prefix_weight + 
		    weights[curr.prefix[curr.last]][move];
		curr.prefix[++curr.last] = move;

		if (debug) 
		    MakeTourString(curr.last, curr.prefix);

		curr.conn |= 1 << move;
	
		return calc_bound(index);
	    }
	}


    /*
     * set_best():
     *
     *  Set the global `best' value.
     *
     */
    static void set_best(int best, int[] path) {
	if (best >= MinTourLen) {
	    if (debug) 
		System.out.println("set_best: " + best + " <-> " + MinTourLen);
	    return;
	}
	synchronized(MinLock) {
	    if (best < MinTourLen) {
		if (debug) {
		    System.out.print("(" + Thread.currentThread().getName() + ") ");
		    System.out.println("set_best MinTourLen: " + best + " (old: " + MinTourLen + ")");
		}
		MinTourLen = best;
		for (int i = 0; i < Tsp.TspSize; i++)
		    MinTour[i] = path[i];
		if (debug) 
		    MakeTourString(Tsp.TspSize, MinTour);
	    }
	}
    }


    static void MakeTourString(int len, int[] path) {
	int i;
	String tour_str="";

	for (i=0; i < len; i++) {
	    System.out.print(path[i]+" - ");
	    if (path[i] >= 10) System.out.print("   ");
	    else System.out.print("  ");
	}
	System.out.println(path[i]);
    }


    /*
     *  Add up min edges connecting all unconnected vertixes (AHU p. 331-335)
     *  At some point, we may want to use MST to get a lower bound. This
     *  bound should be higher (and therefore more accurate) but will take
     *  longer to compute. 
     */
    static int calc_bound(int curr_index) {
	int i, j, wt, wt1, wt2;
	TourElement curr = Tours[curr_index];
	
	synchronized(TourLock) {
	    if (debug) 
		System.out.println("calc_bound");

	    /*
	     * wt1: the value of the edge with the lowest weight from the node
	     *	    we're testing to another unconnected node.
	     * wt2: the value of the edge with the second lowest weight
	     */

	    /* if we have one unconnected node */
	    if (curr.last == (Tsp.TspSize - 2)) {
		for (i = 0; i < Tsp.TspSize; i++) {
		    if ((curr.conn & (1<<i))==0) {
			/* we have found the one unconnected node */
			curr.prefix[Tsp.TspSize-1] = i;
			curr.prefix[Tsp.TspSize] = Tsp.StartNode;
		    
			/* add edges to and from the last node */
			curr.prefix_weight += weights[curr.prefix[Tsp.TspSize-2]][i] +
			    weights[i][curr.prefix[Tsp.StartNode]];
		    
			if (curr.prefix_weight < MinTourLen) {
			    /* Store our new best path and its weight. */
			    set_best(curr.prefix_weight, curr.prefix);
			}
		    
			/* De-allocate this tour so someone else can use it */
			curr.lower_bound = Tsp.BIGINT;
		    
			TourStack[++TourStackTop] = curr_index; /* Free tour. */
			return(Tsp.END_TOUR);
		    }
		}
	    }

	    curr.mst_weight = 0;

	    /*
	     * Add up the lowest weights for edges connected to vertices
	     * not yet used or at the ends of the current tour, and divide by two.
	     * This could be tweaked quite a bit.  For example:
	     *   (1) Check to make sure that using an edge would not make it
	     *       impossible for a vertex to have degree two.
	     *   (2) Check to make sure that the edge doesn't give some
	     *       vertex degree 3.
	     */

	    if (curr.last != Tsp.TspSize - 1) {
		for (i = 0; i < Tsp.TspSize; i++) {
		    if ((curr.conn & 1<<i)!=0) continue;
		    for (j = 0, wt1 = wt2 = Tsp.BIGINT; j < Tsp.TspSize; j++) {
			/* Ignore j's that are not connected to i (global->weights[i][j]==0), */
			/* or that are already in the tour and aren't either the      */
			/* first or last node in the current tour.		      */
			wt = weights[i][j];
			if ((wt==0) || (((curr.conn&(1<<i))!=0)&&(j != curr.prefix[0])&&
					(j != curr.prefix[curr.last])))
			    continue;
		    
			/* Might want to check that edges go to unused vertices */
			if (wt < wt1) {
			    wt2 = wt1;
			    wt1 = wt;
			} else if (wt < wt2) wt2 = wt;
		    }

		    /* At least three unconnected nodes? */
		    if (wt2 != Tsp.BIGINT) curr.mst_weight += (wt1 + wt2) >> 1;
		    /* Exactly two unconnected nodes? */
		    else if (wt1 != Tsp.BIGINT) curr.mst_weight += wt1;
		}
		curr.mst_weight += 1;
	    }
	    curr.lower_bound = curr.mst_weight + curr.prefix_weight;
	    return(curr_index);
	}
    }

    static int LEFT_CHILD(int x) {
	return (x<<1);
    }

    static int RIGHT_CHILD(int x) {
	return (x<<1)+1;
    }

    static boolean less_than(PrioQElement x, PrioQElement y) {
	return ((x.priority  < y.priority) || 
		(x.priority == y.priority) && 
		(Tours[x.index].last > Tours[y.index].last));
    }

    /*
     * DumpPrioQ():
     *
     * Dump the contents of PrioQ in some user-readable format (for debugging).
     *
     */
    static void DumpPrioQ() {
	int pq, ind;
	
	System.out.println("DumpPrioQ");
	for (pq = 1; pq <= PrioQLast; pq++) {
	    ind = PrioQ[pq].index;
	    System.out.print(ind+"("+PrioQ[pq].priority+"):\t");
	    if ((LEFT_CHILD(pq)<PrioQLast) && (PrioQ[pq].priority>PrioQ[LEFT_CHILD(pq)].priority))
		System.out.print(" left child wrong! ");
	    if ((LEFT_CHILD(pq)<PrioQLast) && (PrioQ[pq].priority>PrioQ[RIGHT_CHILD(pq)].priority)) 
		System.out.print(" right child wrong! ");
	    MakeTourString(Tours[ind].last, Tours[ind].prefix);
	}
    }


    /*
     * split_tour():
     *
     *  Break current tour into subproblems, and stick them all in the priority
     *  queue for later evaluation.
     *
     */
    static void split_tour(int curr_ind) {
	int n_ind, last_node, wt;
	int i, pq, parent, index, priority;
	TourElement curr;
	PrioQElement cptr, pptr;

	synchronized(TourLock) {
	    if (debug) 
		System.out.println("split_tour");

	    curr = Tours[curr_ind];
	
	    if (debug)
		MakeTourString(curr.last, curr.prefix);

	    /* Create a tour and add it to the priority Q for each possible
	       path that can be derived from the current path by adding a single
	       node while staying under the current minimum tour length. */
	
	    if (curr.last != (Tsp.TspSize - 1)) {
		boolean t1, t2, t3;
		TourElement new_;
	    
		last_node = curr.prefix[curr.last];
		for (i = 0; i < Tsp.TspSize; i++) {
		    /*
		     * Check: 1. Not already in tour
		     *	      2. Edge from last entry to node in graph
		     *	and   3. Weight of new partial tour is less than cur min.
		     */
		    wt = weights[last_node][i];
		    t1 = ((curr.conn & (1<<i)) == 0);
		    t2 = (wt != 0);
		    t3 = (curr.lower_bound + wt) <= MinTourLen;
		    if (t1 && t2 && t3) {
			if ((n_ind = new_tour(curr_ind, i)) == Tsp.END_TOUR) {
			    continue;
			}
			/*
			 * If it's shorter than the current best tour, or equal
			 * to the current best tour and we're reporting all min
			 * length tours, put it on the priority q.
			 */
			new_ = Tours[n_ind];
		    
			if (PrioQLast >= Tsp.MAX_NUM_TOURS-1) {
			    System.out.println("pqLast "+PrioQLast);
			    System.exit(-1);
			}

			if (debug)
			    MakeTourString(new_.last, new_.prefix);
		    
			pq = ++PrioQLast;
			cptr = PrioQ[pq];
			cptr.index = n_ind;
			cptr.priority = new_.lower_bound;
		    
			/* Bubble the entry up to the appropriate level to maintain
			   the invariant that a parent is less than either of it's
			   children. */
			for (parent = pq >> 1, pptr = PrioQ[parent];
			     (pq > 1) && less_than(cptr, pptr);
			     pq = parent,  cptr = pptr, parent = pq >> 1, pptr = PrioQ[parent]) {
			    /* PrioQ[pq] lower priority than parent -> SWITCH THEM. */
			    index = cptr.index;
			    priority = cptr.priority;
			    cptr.index = pptr.index;
			    cptr.priority = pptr.priority;
			    pptr.index = index;
			    pptr.priority = priority;
			}
		    } else if (debug) 
			System.out.println(" [" + curr.lower_bound + " + " + wt + " > " + MinTourLen);
		}
	    }
	}
    }

    /*
     * find_solvable_tour():
     *
     * Used by both the normal TSP program (called by get_tour()) and
     * the RPC server (called by RPCserver()) to return the next solvable
     * (sufficiently short) tour.
     *
     */
    static int find_solvable_tour() {
	int curr, i, left, right, child, index;
	int priority, last;
	PrioQElement pptr, cptr, lptr, rptr;
	
	synchronized(TourLock) {
	    if (debug) 
		System.out.println("find_solvable_tour");

	    if (Done != 0) { 
		if (debug) 
		    System.out.println("(" + Thread.currentThread().getName() + ") - done"); 
		return -1; 
	    }
	
	    for ( ; PrioQLast != 0; ) {
		pptr = PrioQ[1];
		curr = pptr.index;
		if (pptr.priority >= MinTourLen) {
		    /* We're done -- there's no way a better tour could be found. */
		    if (debug) {
			System.out.print("\t" + Thread.currentThread().getName() + ": ");		
			MakeTourString(Tours[curr].last, Tours[curr].prefix);
		    }
		    Done = 1;
		    return -1;
		}

		/* Bubble everything maintain the priority queue's heap invariant. */
		/* Move last element to root position. */
		cptr = PrioQ[PrioQLast];
		pptr.index  = cptr.index;
		pptr.priority = cptr.priority;
		PrioQLast--;

		/* Push previous last element down tree to restore heap structure. */
		for (i = 1; i <= (PrioQLast >> 1); ) {
		
		    /* Find child with lowest priority. */
		    left  = LEFT_CHILD(i);
		    right = RIGHT_CHILD(i);
	    
		    lptr = PrioQ[left];
		    rptr = PrioQ[right];
    	
		    if (left == PrioQLast || less_than(lptr,rptr)) {
			child = left;
			cptr = lptr;
		    } else {
			child = right;
			cptr = rptr;
		    }
    
    
		    /* Exchange parent and child, if necessary. */
		    if (less_than(cptr,pptr)) {
			/* PrioQ[child] has lower prio than its parent - switch 'em. */
			index = pptr.index;
			priority = pptr.priority;
			pptr.index = cptr.index;
			pptr.priority = cptr.priority;
			cptr.index = index;
			cptr.priority = priority;
			i = child;
			pptr = cptr;
		    } else break;
		}
	    
		last = Tours[curr].last;
	    
		/* If we're within `NodesFromEnd' nodes of a complete tour, find
		   minimum solutions recursively.  Else, split the tour. */
		if (last < Tsp.TspSize || last < 1) {
		    if (last >= (Tsp.TspSize - Tsp.NodesFromEnd - 1)) return(curr);
		    else split_tour(curr);	/* The current tour is too long, */
		}				/* to solve now, break it up.	 */
		else {
		    /* Bogus tour index. */
		    if (debug) {
			System.out.print("\t" + Thread.currentThread().getName() + ": ");
			MakeTourString(Tsp.TspSize, Tours[curr].prefix);
		    }
		}
		TourStack[++TourStackTop] = curr; /* Free tour. */

	    }
	    /* Ran out of candidates - DONE! */
	    Done = 1;
	    return(-1);
	}
    }

    static int get_tour(int curr) {

	if (debug) 
	    System.out.println("get_tour");

	synchronized(TourLock) {
	    if (curr != -1) TourStack[++TourStackTop] = curr;
	    curr = find_solvable_tour();
	}
	return(curr);
    }


    /*
     *   recursive_solve(curr_ind)
     *
     *	We're now supposed to recursively try all possible solutions
     *	starting with the current tour.  We do this by copying the
     *	state to local variables (to avoid unneeded conflicts) and
     *	calling visit_nodes to do the actual recursive solution.
     *
     */
    void recursive_solve(int index) {
	int i, j;
	TourElement curr = Tours[index];

	if (debug) 
	    System.out.println("recursive_solve");

	CurDist = curr.prefix_weight;
	PathLen = curr.last + 1;
	
	for (i = 0; i < Tsp.TspSize; i++) Visit[i] = 0;
	
	for (i = 0; i < PathLen; i++) {
	    Path[i] = curr.prefix[i];
	    Visit[Path[i]] = 1;
	}

	if (PathLen > Tsp.TspSize) {
	    System.out.println("Pathlen: "+ PathLen);
	    System.exit(0);
	}
	
	if (CurDist == 0 || debug) {
	    if (debug) 
		System.out.print("\t" + Thread.currentThread().getName() + ": Tour " + index + " is ");
	    for (i = 0; i < PathLen-1; i++) {
		System.out.print(Path[i]+" - ");
	    }
	    if (debug) { 
		System.out.println(Path[i]);
		System.out.println("\t" + Thread.currentThread().getName() + ": Cur: " + CurDist + ", Min " + MinTourLen + 
				   ", Len:" + (PathLen - 1) + ", Sz: " + Tsp.TspSize + "\n");
	    }
	}
	
	visit_nodes(Path[PathLen-1]);
    }

    /*
     *   visit_nodes()
     *
     *       Exhaustively visits each node to find Hamilton cycle.
     *       Assumes that search started at node from.
     *
     */
    void visit_nodes(int from) {
	int i;
	int dist, last;

	visitNodes++;	
	
	for (i = 1; i < Tsp.TspSize; i++) {
	    if (Visit[i]!=0) continue;	/* Already visited. */
	    if ((dist = weights[from][i]) == 0) 
		continue; /* Not connected. */
	    if (CurDist + dist > MinTourLen) 
		continue; /* Path too long. */
	    
	    /* Try next node. */
	    Visit[i] = 1;
	    Path[PathLen++] = i;
	    CurDist += dist;
	    
	    if (PathLen == Tsp.TspSize) {
		/* Visiting last node - determine if path is min length. */
		if ((last = weights[i][Tsp.StartNode]) != 0 &&
		    (CurDist += last) < MinTourLen) {
		    set_best(CurDist, Path);
		}
		CurDist -= last;
	    } /* if visiting last node */
	    else if (CurDist < MinTourLen) 
		visit_nodes(i);	/* Visit on. */
	    
	    /* Remove current try and loop again at this level. */
	    CurDist -= dist;
	    PathLen--;
	    Visit[i] = 0;
	}
    }
}

		     




