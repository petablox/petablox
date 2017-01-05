define void @main() #0 {
    switch i32 1, label %onone [ i32 0, label %onzero ]

onzero:
    ret void

onone:
    ret void
}
