struct S {
  int x;
};

void f(struct S s);

int main(int argc, char** argv) {
  struct S s;
  f(s); // warn: passed-by-value arg contain uninitialized data
}
