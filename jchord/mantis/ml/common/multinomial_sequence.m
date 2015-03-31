% function flag = multinomial_sequence(m, d)
% 
%     for i = 0:d
%         if (m == 1)
%             flag = 1;
%             fprintf('%d\n', d);
%             return;
%         else
%             fprintf('%d ', i);
%         end
%         d = d - i;
%         m = m - 1;
%         if (d > 0)
%             flag = multinomial_sequence(m, d);
%         end
%     end
% end
% 

% function multinomial_sequence(m, s, sofar)
% if m == 0
%     sofar
%     return;
% end
% 
% if m == 1
%     multinomial_sequence(m-1,s, [sofar s])
%     return;
% end
% 
% for i = 0:s
%     multinomial_sequence(m-1, s-i, [sofar i]);
% end

% Compute all sequences (multinomial coefficients) for the
% polynomial expansion according to
% http://en.wikipedia.org/wiki/Multinomial_theorem

function res =  multinomial_sequence(m, s)

% if m == 1
%     res = s;
%     return
% end
% 
% res = [];
% for i = 0:s
%     lower_res = multinomial_sequence(m-1, s-i);
%     res = [res; repmat(i,size(lower_res,1),1), lower_res];
% end


% Only higher-order single terms
res = zeros(m*s, m);
for i = 1:m
    inds = (s*(i-1)+1):(s*(i-1)+s);
    res(inds, i) = 1:s;
end