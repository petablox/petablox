// Trivial double free

#include <stdlib.h>
int main(int argc, char *argv[]) {
  int *x = malloc(8); 
  free(x);
  free(x);
  return 0; 
}