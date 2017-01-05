define float @main() {
    %1 = fptrunc double 10.0 to float
    ret float %1
}
