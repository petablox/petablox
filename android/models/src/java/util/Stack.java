class Stack<E>
{
    private E f;

    public synchronized  E peek() 
    { 
		return this.f;
    }

    public synchronized  E pop() 
    { 
		return this.f;
    }

    public  E push(E object) 
    { 
		this.f = object;
		return object;
    }
}
