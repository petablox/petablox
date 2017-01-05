define void @main() {
    %1 = alloca i32
    %2 = alloca i32
    store i32 3, i32* %1
    store atomic volatile i32 3, i32* %2 unordered, align 8
    ret void
}
