define i32 @foo(i32 %x) {
    ret i32 %x
}

define void @main() {
    %1 = call i32 @foo(i32 3)
    %2 = tail call i32 @foo(i32 4)
    %3 = notail call i32 @foo(i32 5)
    %4 = musttail call i32 @foo(i32 6)

    ret void
}
