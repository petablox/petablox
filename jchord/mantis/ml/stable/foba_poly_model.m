%function foba_poly_model(dir_input, key, round)

%% Parsing argument list
arg_list = argv ();
for i = 1:nargin
    printf ('%s\n', arg_list{i});
end
dir_input = arg_list{1};
key = '';
round = str2num(arg_list{2});
max_terms = str2num(arg_list{3});

%% Hyper-parameters for the algorithm
nu = 0.5;
degree = 3;
threshold = 0.01;
f_scale = 0.1;
portion = 0.1;


%% Input & output file names
file_time   = strcat(dir_input, key, '/exectime.mat');
file_data   = strcat(dir_input, key, '/feature_data.mat');
file_var    = strcat(dir_input, key, '/varying_features.mat');
file_costly = strcat(dir_input, key, '/costly_features.txt');
file_chosen = strcat(dir_input, key, '/currently_chosen_features.txt');
file_out    = strcat(dir_input, key, '/rejecting_costly_features.txt');

%% Read in input files                    
load(file_time);
load(file_data);
load(file_var);

costly_f = load(file_costly);
raw_data = zeros(length(runtime), num_orig_feats);
raw_data(:, var_f) = var_data;

useful_f = setdiff(var_f, costly_f);
features = (raw_data(:, useful_f));
[num_data, D] = size(features);
costs = ones(1, D);

%% Randomly split data into training and testing sets
rand_indics = 1:num_data;
num_train = floor(portion*num_data);
train_indics = rand_indics(1:num_train);
test_indics  = rand_indics(num_train+1:num_data);
y      = runtime(train_indics);
data   = features(train_indics, :);
y_test    = runtime(test_indics, 1);
data_test = features(test_indics, :);

%% Foba polynomial regression
[err_sp_nl, num_chosen_feats, num_chose_terms, x_sp_nl, chosen_seqs, y_predict] = ...
   foba_poly_fitting_testing(y, data, y_test, data_test, costs, degree, threshold, nu, f_scale, max_terms, 0);
non0 = find(sum(chosen_seqs)>0);
chosen_feats = useful_f(non0(2:end)-1);
poly_terms = sequence2term(chosen_seqs, useful_f, 1);

%% output information
%% The model to screen:
coefficients = x_sp_nl';
fprintf('Round %d:\n', round);
fprintf('%% The foba poly prediction error = %.3f, using the following %d terms model:\n', ...
         err_sp_nl, length(x_sp_nl));
fprintf('%% M(f) = %8.3f', coefficients(1));
for i = 2:length(coefficients)
    wterms = strcat(num2str(coefficients(i), '%8.3f'), char(poly_terms(i)));
    if (coefficients(i) > 0)
        formats_str = ' + %s';
    else
        formats_str = ' %s';
    end
    fprintf(formats_str, wterms);
end
fprintf('\n%% with features ');
fprintf('%d ', chosen_feats);
fprintf('\n');

%% The model to file:
fid = fopen(file_out, 'a');   
fprintf(fid, 'Round %d:\n', round);
fprintf(fid, '%% The foba poly prediction error = %.3f, using the following %d terms model:\n', ...
         err_sp_nl, length(x_sp_nl));
fprintf(fid, '%% M(f) = %8.3f', coefficients(1));
for i = 2:length(coefficients)
    wterms = strcat(num2str(coefficients(i), '%8.3f'), char(poly_terms(i)));
    if (coefficients(i) > 0)
        formats_str = ' + %s';
    else
        formats_str = ' %s';
    end
    fprintf(fid, formats_str, wterms);
end
fprintf(fid, '\n%% with features ');
fprintf(fid, '%d ', chosen_feats);
fprintf(fid, '\n');

fclose(fid);

%% The chosen features
fid = fopen(file_chosen, 'w');   
fprintf(fid, '%.3f ', err_sp_nl);
fprintf(fid, '%d ', chosen_feats);
fclose(fid);
