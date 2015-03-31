function [A, poly_terms, weights, multi_seqs] = polynomial_features_inc(data, costs, inds, degree, const, normalize)
% The increased polynomial feature data because column of data increased by one.
% The poly_terms are not right if not adding const '1'.

% n data samples and d features
[n, d] = size(data);
if (const == 1)
    % Add a const feature with very small cost
    data  = [ones(n,1) data];
    costs = [1e-8 costs];
    d = d + 1;
end
% Work out the polynomial terms
% The multinomial incremental sequences: 
% each row is the degrees of a single term
% in the polynomial expansion.
% The increased sequences because # of features increase by one.
multi_seqs = multinomial_inc_sequence(d-1, degree);
A          = sequence2value(multi_seqs, data);
% % Adding exponential features
% A = [A exp(A)];
% weights = [weights weights];
% poly_terms = [poly_terms poly_terms];
% multi_seqs = [multi_seqs; multi_seqs]
if (normalize)
    A = normalization(A);
end
if (nargout > 1)
    poly_terms = sequence2term(multi_seqs, inds, const);
end
if (nargout > 2)
    weights    = sequence2weight(multi_seqs, costs);
end
