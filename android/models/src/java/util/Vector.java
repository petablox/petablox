class Vector<E>
{
    private E f;

    @STAMP(flows = {@Flow(from="object",to="this")})
    public  void add(int location, E object) {
	    this.f = object;
	}

    @STAMP(flows = {@Flow(from="object",to="this")})
    public boolean add(E object) {
		this.f = object;
		return true;
	}
    
    @STAMP(flows = {@Flow(from="object",to="this")})
	public  void addElement(E object) {
		this.f = object;
	}
	
	public synchronized  java.lang.Object clone() {
			return this;
	}
	
    @STAMP(flows = {@Flow(from="elements",to="this")})
    public synchronized  void copyInto(java.lang.Object[] elements) {
    	    elements[0] = this.f;
    	}
	
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public synchronized  E elementAt(int location) {
    	    return this.f;
    	}

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public synchronized  E firstElement() {
    	    return this.f;
    	}
    
    @STAMP(flows = {@Flow(from="this",to="@return")})
    public  E get(int location) {
    	return this.f;
    }

    @STAMP(flows = {@Flow(from="object",to="this")})
    public synchronized  void insertElementAt(E object, int location) {
    	    this.f = object;
    	}

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public synchronized  E lastElement() {
    	    return this.f;
    	}
	
    @STAMP(flows = {@Flow(from="object",to="this")})    
    public synchronized  E set(int location, E object) {
    	    this.f = object;
    	    return this.f;
    	}
	
    @STAMP(flows = {@Flow(from="object",to="this")})    
    public synchronized  void setElementAt(E object, int location) {
    	    this.f = object;
    	}
	
    @STAMP(flows = {@Flow(from="this",to="@return")})
	public synchronized  java.util.List<E> subList(int start, int end) {
     	    List<E> l = new ArrayList<E>();
     	    l.add(this.f);
     	    return l;
    }

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public synchronized  java.lang.Object[] toArray() {
    	    java.lang.Object[] o = {this.f};
    	    return o;
		}

    @STAMP(flows = {@Flow(from="this",to="@return")})
    public synchronized  java.lang.String toString() {
			return new String();
    	}

    @STAMP(flows = {@Flow(from="this",to="@return")})
	public  java.util.Enumeration<E> elements() { 
		return new StampEnumeration<E>(this.f);
	}
}