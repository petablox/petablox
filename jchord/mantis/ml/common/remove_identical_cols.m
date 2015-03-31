function [new_data, inds_u] = remove_identical_cols(data, costs)
[rows, cols] = size(data);

costly_f = find(costs > 99);
data = normalization(data);
remove = zeros(1, cols);
for i = 1:cols
    if (remove(i) == 0)
        temp_mat = repmat(data(:, i), [1, cols-i]);
        diff_mat = abs(data(:, (i+1):cols) - temp_mat);
        inds = find(sum(diff_mat) < 1e-5) + i;
        % Remove only costly features
        inds = intersect(inds, costly_f);
        remove(inds)=1;
    end
end

inds_u = find(remove == 0);
new_data = data(:, inds_u);