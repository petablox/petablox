
// Figure 3.4: Example: out of bounds array access

int main(int argc, char *argv[]) {
  int x[5];
  int i,y;
  scanf("%d", &y);
  if(3==y){
    i = -1;
  }
  else {
    i = 2;
  }
  x[i] = 3;
  x[6] = 2;
  return 0;
}

