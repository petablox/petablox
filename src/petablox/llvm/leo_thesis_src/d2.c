// Correct reallocation of a pointer
#include <stdlib.h>
int main(int argc, char *argv[]) {
  int *x = malloc(8); 
  free(x);
  x = malloc(8);
  free(x);
  return 0; 
}
