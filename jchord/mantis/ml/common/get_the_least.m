function [err_rm, ind_least] = get_the_least(y, data)
% Remove the column that degrades the accuracy the least.
% rm_errr: regression error after removing the column
% ind_least: the column should be removed

% Assuming column 1 are all 1's and should not be removed
[n, m] = size(data);
error  = 9.99*ones(1, m);
for i = 2:m
    %inds_cand = setdiff(1:m, i);
    inds_cand = [1:(i-1) (i+1):m];
    [coeff, confidence, residuals] = regress_relative(y, data(:, inds_cand));
    error(i) = mean_error(residuals, y);  
end
[err_rm, ind_least] =  min(error);


% bug: order of data does not match to feature order.