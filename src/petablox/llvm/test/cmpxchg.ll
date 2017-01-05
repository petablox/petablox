define void @main() {
    %ptr = alloca i32
    %1 = alloca i32
    %2 = cmpxchg i32* %ptr, i32 2, i32 0 acq_rel monotonic ; yields  { i32, i1 }
    %3 = cmpxchg weak volatile i32* %ptr, i32 2, i32 0 singlethread acq_rel monotonic ; yields  { i32, i1 }
    ret void
}
