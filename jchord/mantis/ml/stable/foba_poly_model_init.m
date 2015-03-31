%function [num_orig_feats] = foba_poly_model_init(dir_input, key, runs)

%% Parsing argument list
arg_list = argv ();
%for i = 1:nargin
%    printf ('%s\n', arg_list{i});
%end
dir_input = arg_list{1};
key = '';
runs = str2num(arg_list{3});

%% Input files
file_time   = strcat(dir_input, key, '/exectime.mat');
file_data   = strcat(dir_input, key, '/feature_data.mat');
file_var    = strcat(dir_input, key, '/varying_features.mat');
file_costly = strcat(dir_input, key, '/costly_features.txt');

%% Reading data
% Execution time file
file_exectime = strcat(dir_input, key, '/exectime.txt');
raw_time = load(file_exectime);
runtime = sum(raw_time, 2)/runs;
% Cost file
file_feature_cost = strcat(dir_input, key, '/feature_cost.txt');
costs = load(file_feature_cost);
costs = costs';
% Feature file
file_feature_data = strcat(dir_input, key, '/feature_data.txt');
raw_data = load(file_feature_data);
raw_data = raw_data';
num_orig_feats = size(raw_data, 2);

%% Intermediate files
% Get features with variability
var_data = var(raw_data);
var_f = find(var_data > 0);
% Remove redudant features
[org_data, unique_f] = remove_identical_cols(raw_data(:, var_f), costs(var_f));
var_f = var_f(unique_f);

raw_data = normalization(raw_data);
costly_f = [0];

[num_data, D] = size(raw_data);
rand_indics = randperm(num_data);
var_data = raw_data(rand_indics, var_f);
runtime = runtime(rand_indics);

% For storing intermediate results
save(file_time, 'runtime');
save(file_data, 'var_data');
save(file_var, 'var_f', 'num_orig_feats');

% For costly features, initialized to be empty file.
fid = fopen(file_costly, 'w');
fprintf(fid, '%d ', costly_f);
fclose(fid);

