function whole_seqs = subset2whole(subseqs, inds, m)

% The first column of subseqs is for constant 1 vector,
% which is not include in inds.

% inds contain of indices of subset in the whole set
% m is the length of the whole sequence
[n, d] = size(subseqs);
whole_seqs = zeros(n, m);

% The column 1 vector
whole_seqs(:, 1) = subseqs(:, 1);
for i = 2:d
    whole_seqs(:, inds(i-1)+1) = subseqs(:, i);
end
