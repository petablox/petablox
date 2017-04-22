int main(int argc, char** argv) {
  int x;
  x |= 1; // warn: left expression is unitialized
}
