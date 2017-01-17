@x = global i32 1
@y = alias i32, i32* @x

define void @main() {
    ret void
}
