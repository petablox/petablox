function [err_add, ind_best] =  get_the_best(y, orig_data, cand_data, chosen)

% Add 1 column of data from cand_data to orig_data 
% that achieves the best regression accuracy.

[N, num_cand] = size(cand_data);

err_sp = 9.99*ones(1, num_cand);
for i = 1:num_cand
    if (~ismember(i, chosen))
        inds_new = [chosen i];
        data = [orig_data cand_data(:, inds_new)];
        [coeff, confidence, residuals] = regress_relative(y, data);
        err_sp(i) = mean_error(residuals, y);  
        %err_sp(i) = mean(abs(residuals ./ y));  
        %err_sp(i) = sum(abs(residuals)) / sum(abs(y));  
    end
end

[err_add, ind_best] =  min(err_sp);