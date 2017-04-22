void test(int *p) {
  if (p)
    return;

  int x = p[0]; // warn
}

int main(int argc, char** argv) {
  return 0;
}
