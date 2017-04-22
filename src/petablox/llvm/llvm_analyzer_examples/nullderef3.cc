class C {
public:
  int x;
};

int main(int argc, char** argv) {
  C *pc = 0;
  int k = pc->x; // warn
}
