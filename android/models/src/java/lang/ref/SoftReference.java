class SoftReference<T> {
    public  SoftReference(T r) { referent = r; }
    public  SoftReference(T r, java.lang.ref.ReferenceQueue<? super T> q) { referent = r; }
}
