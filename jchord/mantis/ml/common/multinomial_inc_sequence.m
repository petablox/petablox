% Adding 1 more variables to existing m ones, what are the 
% additional nolinear terms resulting from polynomial expansion
% It is based on multinomial coefficients for the
% polynomial expansion according to
% http://en.wikipedia.org/wiki/Multinomial_theorem
function sequences =  multinomial_inc_sequence(m, d)

sequences = [];
for i = 1:d
    res = multinomial_sequence(m, d-i);
    res(:, m+1) = i;
    sequences = [sequences; res];
end
