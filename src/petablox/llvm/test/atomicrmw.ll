define void @main() {
    %ptr = alloca i32
    %1 = atomicrmw add i32* %ptr, i32 1 acquire
    %2 = atomicrmw volatile min i32* %ptr, i32 1 acquire
    %3 = atomicrmw and i32* %ptr, i32 1 acquire
    ret void
}
