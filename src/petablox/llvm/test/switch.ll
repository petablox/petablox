define void @main() #0 {
    switch i32 1, label %onone [ i32 5, label %onzero i32 6, label %onone ]

onzero:
    ret void

onone:
    ret void
}
