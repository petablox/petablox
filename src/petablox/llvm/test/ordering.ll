define void @main() {
    fence acquire
    fence release
    fence acq_rel
    fence seq_cst
    ret void
}
