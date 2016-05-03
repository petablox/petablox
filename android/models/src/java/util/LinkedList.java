class LinkedList<E>
{
	private E f;

	@STAMP(flows = {@Flow(from="object",to="this")}) 
	public boolean add(E object){
		this.f = object;
		return true;  
	}
	
	@STAMP(flows = {@Flow(from="object",to="this")}) 
	public void add(int location, E object) {
		this.f = object;
	}

	@STAMP(flows = {@Flow(from="object",to="this")}) 
	public void addFirst(E object) {
		this.f = object;
    }

	@STAMP(flows = {@Flow(from="object",to="this")}) 
    public void addLast(E object) {
		this.f = object;
    }

	public E get(int location) {
		return this.f;
    }

    public E getFirst() {
		return this.f;
    }

    public E getLast() {
		return this.f;
    }

    public E remove(int location) {
		return this.f;
    }

    public E removeFirst() {
		return this.f;
    }

    public E removeLast() {
		return this.f;
    }

	public java.lang.Object[] toArray() {      
		Object[] a = new Object[1];
		a[0] = f;
		return a;
	}

    public E peekFirst() {
        return this.f;
    }

    public E peekLast() {
        return this.f;
    }

    public E pollFirst() {
        return this.f;
    }

    public E pollLast() {
        return this.f;
    }

    public E pop() {
        return this.f;
    }

	@STAMP(flows = {@Flow(from="e",to="this")}) 
    public void push(E e) {
		this.f = e;
    }
	
	@STAMP(flows = {@Flow(from="object",to="this")}) 
	public E set(int location, E object) {
		this.f = object;
		return this.f;
    }

	@STAMP(flows = {@Flow(from="o",to="this")}) 
    public boolean offer(E o) {
		this.f = o;
		return true;
    }

    public E poll() {
		return this.f;
    }

    public E remove() {
		return this.f;
    }

    public E peek() {
		return this.f;
    }

    public E element() {
		return this.f;
    }

}


