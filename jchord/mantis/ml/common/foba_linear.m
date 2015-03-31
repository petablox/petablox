function [existing, chosen, prev_err] = foba_linear(y, orig_data, err_existing, cand_data, threshold, nu, max_terms)

[N, num_orig] = size(orig_data);

existing = 1:num_orig;
chosen = [];
err_add  = 1.0;
prev_err = err_existing;
num_terms = length(existing) + length(chosen);
while (num_terms < max_terms)
    % Forward step, consider each feature 1 by 1
    [curr_err, ind_best] =  get_the_best(y, orig_data(:, existing), cand_data, chosen);
    gain = prev_err - curr_err;
    if (gain > threshold)
        chosen   = [chosen ind_best];
        err_add  = curr_err;
        prev_err = curr_err;        
    else
        break;
    end
    
    %% Adaptive backward steps
    % Consider removing chosen_data one column by one, if it
    % does not destroy nu portion of the accuracy gain.
    while(1)
        % New data consists of the existing data and the chosen data
        data_new = [orig_data(:, existing) cand_data(:, chosen)];
        % Figure out the best one to remove, and the resulting error
        [curr_err, ind_least] = get_the_least(y, data_new);
        % If the error loss is too much, stop
        loss = curr_err - err_add;
        if (loss > nu*gain)
            break;
        end
        % If the error loss is small, remove the ind_least
        % from either existing, or chosen
        if (ind_least <= length(existing))
            existing(ind_least) = [];
        else
            chosen(ind_least-length(existing)) = [];
        end
        prev_err = curr_err;
    end    
    
    num_terms = length(existing) + length(chosen);
end    
