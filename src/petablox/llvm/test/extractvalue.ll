define void @main() {
    %1 = insertvalue {i32, i32} undef, i32 1, 0
    %2 = insertvalue {i32, i32} %1, i32 2, 1
    %3 = insertvalue {i32, {i32, i32}} undef, i32 2, 1, 0
    %4 = insertvalue {i32, {i32, {i32, i32}}} undef, i32 2, 1, 1, 0
    %5 = extractvalue {i32, {i32, {i32, i32}}} %4, 1, 1, 1
    ret void
}
