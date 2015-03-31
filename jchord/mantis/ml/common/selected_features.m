function feat_usage = selected_features(terms, num_feats)

feat_usage = zeros(1, num_feats);
pattern = 'f(\d+)';
D = length(terms);
for i = 1:D
    term_c = char(terms(i));
    s = regexp(term_c, pattern, 'tokens');
    n_f = length(s);
    for j = 1:n_f
        ind_c = str2num(char(s{j}));
        feat_usage(ind_c) = feat_usage(ind_c) + 1;
    end
end
