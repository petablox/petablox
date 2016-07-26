package edu.ucla.pls.baseline;
/**
 * Transfer is the canonical example of a deadlock. Transfer tries to transfer
 * some amount from one store to another. The algorithm tries to be safe, by
 * first locking the intial store and then the other, so that no dataraces can 
 * occure. Sadly does another transfer do the same thing (in the oposite
 * order), if unlucky will these two transfers deadlock by waiting for the
 * other store to be free forever.
 *
 * @author Christian Gram Kalhauge
 */
public class Transfer implements Runnable {
    private Store a;
    private Store b;

    public Transfer(Store a, Store b) {
        this.a = a;
        this.b = b;
    }
    
    /**
     */
    public void run() {
        a.transferTo(1, b);
    }

    public static void main(String[] args) {
        Store a = new Store(1),
              b = new Store(1);
        
        Transfer trans1 = new Transfer(a, b),
                 trans2 = new Transfer(b, a);

        Thread[] threads = {
            new Thread(trans1),
            new Thread(trans2)
        };
      
        // Start all threads
        for (Thread t : threads) t.start();
        
        // Wait for them to end
        for (Thread t : threads) 
            try { t.join(); } 
            catch (InterruptedException e) { };

        System.out.println("Transfer Succeded");
    }
    
}

class Store {
    private int var;

    public Store(int var) {
        this.var = var;
    }

    /**
     * @param amount  
     * @param other  
     */
    public synchronized void transferTo(int amount, Store other) {
        var -= amount;
        other.add(amount);
    }
    
    /**
     * @param amount  
     */
    public synchronized void add(int amount) {
        var += amount;
    }
    
}
