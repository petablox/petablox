function [chosen_data, chosen_terms, chosen_seqs, prev_err] = foba_poly(y, data, costs, degree, threshold, nu, f_scale, max_terms, do_norm)

[N, num_feats] = size(data);

existing_all  = zeros(num_feats, max_terms);
selecting_all = zeros(num_feats, max_terms);

%% Initialization
% The chosen features
chosen_feats = [];

% Using constant 1 vector for regression
% The chosen (nonlinear) terms
num_chosen_terms = 1;
chosen_terms{1} = '1';
% The sequences of degrees for generating the (nonlinear) terms
% The first column is constant 1
chosen_seqs = zeros(1, num_feats+1);
chosen_seqs(1, 1) = 1;
% The data values of chosen nonlinear terms
chosen_data(:, 1) = ones(N, 1);
[coeff, confidence, residuals] = regress_relative(y, chosen_data);
prev_err = mean_error(residuals, y);
%prev_err = mean(abs(residuals ./ y));
%prev_err = sum(abs(residuals)) / sum(abs(y));

err_sp = 9.99*ones(1, num_feats);
while (num_chosen_terms < max_terms)
    % Determin the best feature to add
    err_sp(1:num_feats) = 9.99;
    for i = 1:num_feats
        % Go through features one by one. For each feature, choose
        % the best terms, and compute the resulting error
        if (~ismember(i, chosen_feats))
            % if this features is not selected, consider its by doing:
            % Generate candidate dataset using nonlinear terms of its own 
            % and its interaction with existing data
            inds_new = [chosen_feats i];
            %[cand_data, cand_terms, cand_weights, multi_seqs] = polynomial_features_inc(data(:, inds_new), ...
            %                                        costs(inds_new), inds_new, degree, 1, do_norm);       
            [cand_data] = polynomial_features_inc(data(:, inds_new), ...
                          costs(inds_new), inds_new, degree, 1, do_norm);       
            [existing, selecting, error] = foba_linear(y, chosen_data, ...
                                                prev_err, cand_data, f_scale*threshold, nu, max_terms);
            err_sp(i) = error;
            num_existing(i)  = length(existing);
            num_selecting(i) = length(selecting);
             existing_all(i,  1:num_existing(i)) = existing;
            selecting_all(i, 1:num_selecting(i)) = selecting;
        end        
    end
    
    % Choose best feature and its nonlinear terms according to the error
    [curr_err, ind_best] =  min(err_sp);    
    gain = prev_err - curr_err;

    %% %%%%%%%%%%%% This part could be further optimized %%%%%%%%%%%%%%%%%%%
    if (gain > threshold)
        chosen_feats = [chosen_feats ind_best];
        % Recompute the terms
        [cand_data, cand_terms, cand_weights, multi_seqs] = polynomial_features_inc(data(:, chosen_feats), ...
                                                costs(chosen_feats), chosen_feats, degree, 1, do_norm);       
         existing =  existing_all(ind_best,  1:num_existing(ind_best));
        selecting = selecting_all(ind_best, 1:num_selecting(ind_best));
        chosen_data  = [chosen_data(:, existing) cand_data(:, selecting)];
        chosen_terms = {chosen_terms{existing} cand_terms{selecting}};

        added_seqs = subset2whole(multi_seqs(selecting, :), chosen_feats, num_feats+1);
        chosen_seqs = [chosen_seqs(existing, :); added_seqs];
        
        % Figure out the original features selectecd
        feat_usage = selected_features(chosen_terms, num_feats);
        rmd_inds = find(feat_usage == 0);
        chosen_feats = setdiff(chosen_feats, rmd_inds);

        num_chosen_terms = length(existing) + length(selecting);
        prev_err = curr_err;
    else
        break;
    end
    
end
