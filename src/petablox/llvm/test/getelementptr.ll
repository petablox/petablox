%struct.ST = type { i32, double, %struct.RT }
%struct.RT = type { i8, [10 x [20 x i32]], i8 }

; Function Attrs: nounwind uwtable
define signext i8 @main(%struct.ST* %s) #0 {
  %1 = alloca %struct.ST*, align 8
  store %struct.ST* %s, %struct.ST** %1, align 8
  %2 = load %struct.ST*, %struct.ST** %1, align 8
  %3 = getelementptr inbounds %struct.ST, %struct.ST* %2, i32 0, i32 2
  %4 = getelementptr inbounds %struct.RT, %struct.RT* %3, i32 0, i32 0
  %5 = load i8, i8* %4, align 8
  ret i8 %5
}
