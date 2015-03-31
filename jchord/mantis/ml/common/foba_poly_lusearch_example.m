clear all;

nu = 0.5;
degree = 3;
threshold = 0.01;
f_scale = 0.1;
max_terms = 15;

key = 'lusearch';
dir_name = strcat('./');
file_name = strcat(dir_name, key, '-time-features');
org_data = load(file_name);

[num_data, cols] = size(org_data);
num_feats = cols - 1;
costs = ones(1, num_feats);
%%%%%%%%%%%%%%%%%%%%%%%% end of pre-processing %%%%%%%%%%%%%%%%%%%%%%%%%%

% data for feature selection
features = normalization(org_data(:, 2:end));

portion = 0.1;
%% Randomly split data into training and testing sets
rand_indics = randperm(num_data);
num_train = floor(portion*num_data);
train_indics = rand_indics(1:num_train);
test_indics  = rand_indics(num_train+1:num_data);
y      = org_data(train_indics, 1);
data   = features(train_indics, :);
y_test    = org_data(test_indics, 1);
data_test = features(test_indics, :);


%% Foba polynomial regression
[err_sp_nl, num_feats, num_terms] = foba_poly_fitting_testing(y, data, y_test, data_test, costs, degree, threshold, nu, f_scale, max_terms, 0);
