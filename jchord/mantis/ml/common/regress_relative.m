function [coeff, confidence, residuals] = regress_relative(y, data)
 
[n, d] = size(data);
 
% response = ones(n, 1);
% A = data ./ repmat(y, [1 d]);
% 
% [coeff, confidence, residuals] = regress(response, A);
% residuals = residuals .* y;

% For matlab
%b = ridge(y, data(:, 2:end), 0.01, 0);
XX = data' * data + 0.001*eye(d);
XY = data'*y;
coeff = XX \ XY;

confidence = [];
residuals = y - data*coeff;