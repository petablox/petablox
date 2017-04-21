
// Double freeing via an aliased pointer

#include <stdlib.h>
int main(int argc, char *argv[]) {
  int *x = malloc(8); 
  int *y = x; 
  free(x);
  free(y);
  return 0; 
}
