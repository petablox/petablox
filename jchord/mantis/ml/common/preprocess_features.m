function [data, num_loops, num_branchs, num_vars] = preprocess_features(file_names, banned_feats, out_file)

% Banned loop features, indexed by their columns in the loop data matrix
banned_loops = banned_feats{1}
% Banned branch features, indexed by their columns in the branch data matrix
banned_branchs = banned_feats{2};
% Banned var features, indexed by the variables in the instrumentation. So
% they need a conversion to column indices
banned_var_inds = banned_feats{3};
value_inds = [];
for i = 1:length(banned_var_inds)
    s_ind = (banned_var_inds(i)-1)*5+1;
    e_ind = banned_var_inds(i)*5;
    value_inds = [value_inds s_ind:e_ind]
end
banned_vars = value_inds;

% Loop Features, which is from file_names{1}
file_name = file_names{1};
org_loop = load(file_name);
org_loop(1, :) = []; % remove the first row, artifacts
[N, num_loops] = size(org_loop(:, 2:end));
% find useful with variability
usf_loops = find(var(org_loop(:, 2:end))>1e-8);
% remove banned
usf_loops = setdiff(usf_loops, banned_loops);
loop_feats = org_loop(:, usf_loops+1);

% Branch Features, which is from file_names{2}
file_name = file_names{2};
org_branch = load(file_name);
org_branch(1,:) = []; % remove the first row, artifacts
[N, num_branchs] = size(org_branch(:, 2:end));
% find useful with variability
usf_branchs = find(var(org_branch(:, 2:end))>1e-8);
% remove banned
usf_branchs = setdiff(usf_branchs, banned_branchs);
% Not all of them are allowed
branch_feats = org_branch(:, usf_branchs+1);

% Variable Features, which is from file_names{3}
file_name = file_names{3};
org_var = load(file_name);
org_var(1,:) = []; % remove the first row, artifacts
[N, num_vars] = size(org_var(:, 2:end));
% find useful with variability
usf_vars = find(var(org_var(:, 2:end))>1e-8);
% remove banned
usf_vars = setdiff(usf_vars, banned_vars);
% save and remove the first column, which is the running time
run_time = org_var(:, 1);
org_var(:, 1) = []; 
% Not all of them are allowed
org_var = org_var(:, usf_vars);

% Processing var feature, which has up-to 5 version of values
% Compute statistics (count, mean, std, min, max) to replace 
% the original values
[N, D] = size(org_var);
num_var = D/5;
stat_var = zeros(N, num_var*5);
for i = 1:N
    for j = 1:num_var
        s_ind = (j-1)*5+1;
        e_ind = (j-1)*5+5;
        values = org_var(i, s_ind:e_ind);
        inds = find(values ~= -999999);
        count = length(inds);
        if (count > 0)
            obsv_values = values(inds);
            stat_var(i, s_ind) = count;
            stat_var(i, s_ind+1) = mean(obsv_values);
            stat_var(i, s_ind+2) = std(obsv_values);
            stat_var(i, s_ind+3) = min(obsv_values);
            stat_var(i, s_ind+4) = max(obsv_values);
        else
            stat_var(i, s_ind:e_ind) = 0;
        end
    end
end
var_feats = stat_var;

% All together:
all_data = [run_time loop_feats branch_feats var_feats];
% usf_feats are those allowed and with variability
% They are now indexed by the whole data matrix. That's why we need
% (num_loops+usf_branchs), (num_loops+num_branchs+usf_vars)
usf_feats = [usf_loops (num_loops+usf_branchs) (num_loops+num_branchs+usf_vars)];
% The first row of the data store the useful features in the original data matrix
usf_cols =  [-999 usf_feats];
data = [usf_cols; all_data];

fid = fopen(out_file, 'w');
fprintf(fid, '%% Time; %.4f loop  features; %.4f branch  features %.4f var features;\n ', ...
    num_loops, num_branchs, num_vars);
fprintf(fid, '%.4d ', data(1, :));
fprintf(fid, '%% This row records the indices of the listed features in the original data\n');
for i = 2:N
    fprintf(fid, '%.3f ', data(i, :));
    fprintf(fid, '\n');
end
fclose(fid);