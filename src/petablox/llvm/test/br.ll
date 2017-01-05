define void @test_cond() #0 {
    br i1 true, label %IfEqual, label %IfUnequal
IfEqual:
    ret void
IfUnequal:
    ret void
}

define void @main() #0 {
    br label %Return
Return:
    ret void
}
