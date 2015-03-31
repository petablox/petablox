function [err_sp_nl, num_chosen_feats, num_chosen_terms, x_sp_nl, chosen_seqs, y_predict] = ...
    foba_poly_fitting_testing(y, data, y_test, data_test, costs, degree, threshold, nu, f_scale, max_terms, do_norm)

features = [data; data_test];
[num_data, num_feats] = size(features);
num_train = size(data, 1);

% Train the model and output the selected nonlinear terms
[chosen_data, chosen_terms, chosen_seqs, prev_err] = foba_poly(y, data, costs, degree, threshold, nu, f_scale, max_terms, do_norm);
% Including the constant term
num_chosen_terms = size(chosen_seqs, 1);
% Excluding the constant feature
num_chosen_feats = length(find(sum(chosen_seqs)>0)) - 1;

% I did not store the model coefficent of the terms, so I redo the
% regression on selected nonlinear terms again here
% From original data to selected nolinear terms
A_all = sequence2value(chosen_seqs, [ones(num_data, 1), features]);
A = A_all(1:num_train, :);
[x_sp_nl, confidence, residuals] = regress_relative(y, A);

% Testing
A_test = A_all(num_train+1:end, :);
y_predict = A_test*x_sp_nl;
err_sp_nl = mean(abs((y_predict-y_test) ./ y_test));
%err_sp_nl = sum(abs(y_predict-y_test)) ./ sum(abs(y_test));     