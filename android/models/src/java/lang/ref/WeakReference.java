class WeakReference<T> {
    public  WeakReference(T r) { referent = r; }
    public  WeakReference(T r, java.lang.ref.ReferenceQueue<? super T> q) { referent = r; }
}


