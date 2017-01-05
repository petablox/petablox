define void @main() {
    %1 = alloca i32
    store i32 3, i32* %1
    %2 = load i32, i32* %1
    %3 = load atomic volatile i32, i32* %1 monotonic, align 4
    ret void
}
