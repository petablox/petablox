function [A, poly_terms, weights, multi_seqs] = polynomial_features_ling(data, costs, inds, degree, const, normalize)
% The poly_terms are not right if not adding const '1'.

% % n data samples and d features
% [n, d] = size(data);
% if (const == 1)
%     % Add a const feature with very small cost
%     data  = [ones(n,1) data];
%     costs = [1e-8 costs];
%     d = d + 1;
% end
% 
% % Work out the polynomial terms
% % The multinomial sequences: each row is the degrees of a single term
% % in the polynomial expansion.
% multi_seqs = multinomial_sequence(d, degree);
% 
% poly_terms = sequence2term(multi_seqs, inds, const);
% weights    = sequence2weight(multi_seqs, costs);
% A          = sequence2value(multi_seqs, data);
% 
% if (normalize)
%     A = normalization(A);
% end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% The previous correct version.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% n data samples and m features
[n, d] = size(data);

if (const == 1)
    % Add a const feature with very small cost
    data  = [ones(n,1) data];
    costs = [1e-8 costs];
    S = cell(1, d+1);
    S{1} = '1';
    for i = 2:(d+1)
        S{i} = strcat('f', int2str(inds(i-1)));
    end
    d = d + 1;
else
    S = cell(1, d);
    for i = 1:d
        S{i} = strcat('f', int2str(inds(i)));
    end    
end

% Work out the polynomial terms
% The multinomial sequences: each row is the degrees of a single term
% in the polynomial expansion.
multi_seqs = multinomial_sequence(d, degree);
D =  size(multi_seqs, 1);
poly_terms = cell(1, D);
for i = 1:D
    degrees = multi_seqs(i, :);
    % Assuming the first column of the data is constant 1
    poly_terms{i} = '1';
    for j = 2:length(degrees)
        if (degrees(j) ~= 0)
            if (degrees(j) == 1)
                poly_terms{i} = strcat(poly_terms{i}, '*', S{j});
            else
                poly_terms{i} = strcat(poly_terms{i}, '*', S{j}, '^', int2str(degrees(j)));
            end
        end
    end
end

weights = zeros(1, D);
for i = 1:D
    degrees = multi_seqs(i, :);
    % count appearance, not frequency, so 0 and 1
    inds_n0 = find(degrees>0);
    degrees(inds_n0) = 1;
    weights(i) = degrees * costs';
end

A = ones(n, D);
for i = 1:D
    degrees = multi_seqs(i, :);
    for j = 1:length(degrees)
        if (degrees(j) ~= 0)
            if (degrees(j) == 1)
                A(:, i) = A(:, i) .* data(:, j);
            else
                A(:, i) = A(:, i) .* data(:, j) .^ degrees(j);
            end
        end
    end
end
% Adding exponential features
A = [exp(A)];

if (normalize)
    A = normalization(A);
end
