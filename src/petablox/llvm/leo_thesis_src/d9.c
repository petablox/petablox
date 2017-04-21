
//  Global double free


#include <stdlib.h>
int *x;
int main(int argc, char *argv[]) { 
  x = malloc(8);
  free(x); 
  free(x); 

  return 0;
}
