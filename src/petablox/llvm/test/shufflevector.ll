define void @main() {
    %1 = insertelement <3 x i32> undef, i32 1, i32 0
    %2 = insertelement <3 x i32> undef, i32 2, i32 0
    %3 = shufflevector <3 x i32> %1, <3 x i32> %2, <3 x i32> <i32 0, i32 1, i32 2>
    ret void
}
