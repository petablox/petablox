class PhantomReference<T> {
    public  PhantomReference(T r, java.lang.ref.ReferenceQueue<? super T> q) { referent = r; }
    public  T get() { return referent; }
}
