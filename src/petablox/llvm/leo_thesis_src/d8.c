
// Interprocedural double free

#include <stdlib.h>
void foo(int *ptr) { 
  free(ptr);
}

int main(int argc, char *argv[]) {
  int *x = malloc(8); 
  foo(x);
  free(x);
  return 0; 
}
