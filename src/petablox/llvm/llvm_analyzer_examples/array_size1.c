int main(int argc, char** argv) {
  int x;
  int vla1[x]; // warn: garbage as size
}
