; This struct is different for every platform. For most platforms,
; it is merely an i8*.
%struct.va_list = type { i8* }

; For Unix x86_64 platforms, va_list is the following struct:
; %struct.va_list = type { i32, i32, i8*, i8* }

define i32 @test(i32 %X, ...) {
    ; Initialize variable argument processing
    %ap = alloca %struct.va_list
    %ap2 = bitcast %struct.va_list* %ap to i8*
    call void @llvm.va_start(i8* %ap2)

    ; Read a single integer argument
    %tmp = va_arg i8* %ap2, i32

    ; Demonstrate usage of llvm.va_copy and llvm.va_end
    %aq = alloca i8*
    %aq2 = bitcast i8** %aq to i8*
    call void @llvm.va_copy(i8* %aq2, i8* %ap2)
    call void @llvm.va_end(i8* %aq2)

    ; Stop processing of arguments.
    call void @llvm.va_end(i8* %ap2)
    ret i32 %tmp
}

declare void @llvm.va_start(i8*)
declare void @llvm.va_copy(i8*, i8*)
declare void @llvm.va_end(i8*)

define void @main() {
    ret void
}
