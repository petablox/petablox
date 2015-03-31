function err = mean_error(residuals, y)

%err = mean(residuals .^ 2);
err = mean(abs(residuals ./ y));
