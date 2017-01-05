define void @main() {
    %1 = insertvalue {i32, i32} undef, i32 1, 0
    %2 = insertvalue {i32, i32} %1, i32 2, 1
    %3 = insertvalue {i32, {i32}} undef, i32 1, 1, 0
    ret void
}
