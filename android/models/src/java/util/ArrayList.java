class ArrayList<E>
{
    private E f;

	@STAMP(flows = {@Flow(from="object",to="this")})
    public E set(int index, E object) {
		this.f = object;
		return this.f;
    }

	@STAMP(flows = {@Flow(from="object",to="this")})
    public  boolean add(E object)
    {
		this.f = object;
		return true;
    }

	@STAMP(flows = {@Flow(from="object",to="this")})
    public  void add(int index, E object)
	{
		this.f = object;
	}

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public 	E get(int index)
    {
		return f;
    }

	@STAMP(flows = {@Flow(from="this",to="@return")})
    public 	E remove(int index)
    {
		return f;
    }

    public  java.util.Iterator<E> iterator()
    {
		return new ArrayListIterator();
    }

	public java.lang.Object[] toArray() {
		java.lang.Object[] res = {f};
		return res;
	}

    private class ArrayListIterator implements Iterator<E>
    {
		public boolean hasNext()
		{
			throw new RuntimeException("Stub!");
		}

		public E next()
		{
			return ArrayList.this.f;
		}

		public void remove()
		{
			throw new RuntimeException("Stub!");
		}
    }
}
