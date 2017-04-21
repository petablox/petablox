
// Figure 3.9: Example: failed return value check

int main(int argc, char *argv[]) {
   int w,x,y,z;
   w = foo();
   x = foo();
   y = foo();
   if(0!=y){
   return 0;
   }

   z = x + y;
   return z;
  }

