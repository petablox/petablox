define i32 @main() {
    %1 = alloca i32
    %2 = ptrtoint i32* %1 to i32
    ret i32 %2
}
