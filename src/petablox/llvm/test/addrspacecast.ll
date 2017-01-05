define i32 addrspace(1)* @main() {
    %1 = alloca i32
    %2 = addrspacecast i32* %1 to i32 addrspace(1)*
    ret i32 addrspace(1)* %2
}
