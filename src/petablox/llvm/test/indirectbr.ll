define void @main(i32 %x) #0 {
  %addr = alloca i8*, align 8
  store i8* blockaddress(@main, %dest), i8** %addr, align 8
  indirectbr i8** %addr, [label %dest]
  ret void

dest:
  ret void 

}

