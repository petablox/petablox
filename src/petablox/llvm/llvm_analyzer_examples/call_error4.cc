class C {
public:
  void f();
};

int main(int argc, char** argv) {
  C *pc;
  pc->f(); // warn: object pointer is uninitialized
}
