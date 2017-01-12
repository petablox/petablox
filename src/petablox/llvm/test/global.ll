@foo = external thread_local(initialexec) global i32
@bar = weak hidden addrspace(5) constant float 1.0, section "foo", align 4

define void @main() {
    ret void
}
