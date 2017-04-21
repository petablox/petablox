
// Double freeing via an aliased pointer with non-trivial control flow

#include <stdlib.h> 
#include <stdio.h>

int main(int argc, char *argv[]) { 
  int *x = malloc(8);
  int a,b; scanf("%d", &a);

  scanf("%d", &b);

  if (12345 == a) {
    if (54321 == b) {
      int *y = x;
      free(y); 
    }
  } free(x);

  return 0; 
}
