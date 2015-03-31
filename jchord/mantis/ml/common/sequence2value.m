function A = sequence2value(multi_seqs, data)

% Each row of multi_seqs contains the degrees of different features
% in a single term of the polynomial expansion.

n = size(data, 1);
D = size(multi_seqs, 1);

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
