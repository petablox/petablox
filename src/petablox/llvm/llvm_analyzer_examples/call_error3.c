int main(int argc, char** argv) {
  void (*foo)(void);
  foo = 0;
  foo(); // warn: function pointer is null
}
