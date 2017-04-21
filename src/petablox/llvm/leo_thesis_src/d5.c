
// Double free with non-trivial control flow

#include <stdlib.h> #include <stdio.h>
int main(int argc, char *argv[]) { 
  int *x = malloc(8);
  int a,b; scanf("%d", &a);
  scanf("%d", &b);
  if (12345 == a) {
    if (54321 == b) {
      free(x); }
  } free(x);
  return 0; 
}
