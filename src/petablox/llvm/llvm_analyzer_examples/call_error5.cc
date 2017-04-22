class C {
public:
  void f();
};

int main(int argc, char** argv) {
  C *pc = 0;
  pc->f(); // warn: object pointer is null
}
