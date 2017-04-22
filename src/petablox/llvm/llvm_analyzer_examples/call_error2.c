int main(int argc, char** argv) {
  void (*foo)(void);
  foo(); // warn: function pointer is uninitialized
}
