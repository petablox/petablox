
// Double freeing occurs following goto statement

#include <stdlib.h> #include <stdio.h>
int main(int argc, char* argv[]) { 
  int *x = malloc(8);
  int a;
  scanf("%d", &a);
  if (12345 == a) { 
    free(x);
    goto cleanup; 
  }
  
  int b;
  scanf("%d", &b);
  if (54321 == b) { 
    goto cleanup;
  } 

  free(x);
  return 0;

 cleanup:
  free(x);
  return 1; 

}

