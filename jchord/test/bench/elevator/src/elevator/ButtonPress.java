package elevator;

/*
 * Copyright (C) 2000 by ETHZ/INF/CS
 * All rights reserved
 * 
 * @version $Id$
 * @author Roger Karrer
 */

// class to represent a press of a call button
class ButtonPress {
    // floor on which the button is pressed
    public int onFloor; 
    
    // floor to which the person wishes to travel
    public int toFloor;
    
    // tick at which the button is pressed 
    public int time; 

    public ButtonPress(int t, int from, int to) {
        onFloor = from;
        toFloor = to;
        time = t;
    }
}

