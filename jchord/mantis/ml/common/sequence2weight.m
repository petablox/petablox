function weights = sequence2weight(multi_seqs, costs)

% Each row of multi_seqs contains the degrees of different features
% in a single term of the polynomial expansion.

D = size(multi_seqs, 1);
weights = zeros(1, D);
for i = 1:D
    degrees = multi_seqs(i, :);
    % count appearance, not frequency, so 0 and 1
    inds_n0 = find(degrees>0);
    degrees(inds_n0) = 1;
    weights(i) = degrees * costs';
end
