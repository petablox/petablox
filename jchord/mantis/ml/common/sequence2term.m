function poly_terms = sequence2term(multi_seqs, inds, const)

% Each row of multi_seqs contains the degrees of different features
% in a single term of the polynomial expansion.

d = length(inds);
if (const == 1)
    S = cell(1, d+1);
    S{1} = '1';
    for i = 2:(d+1)
        S{i} = strcat('f', int2str(inds(i-1)));
    end
else
    S = cell(1, d);
    for i = 1:d
        S{i} = strcat('f', int2str(inds(i)));
    end    
end

D =  size(multi_seqs, 1);
poly_terms = cell(1, D);
for i = 1:D
    degrees = multi_seqs(i, :);
    % Assuming the first column of the data is constant 1
    poly_terms{i} = '';
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
