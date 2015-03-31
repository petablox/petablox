function feats_rmd = costy_features(loops_costy, branchs_costy, vars_costy, num_loops, num_branchs)
loops_rmd = loops_costy;
branchs_rmd = branchs_costy + num_loops;

vars_rmd = [];
for i = 1:length(vars_costy)
    s_ind = (vars_costy(i)-1)*5 + 1;
    e_ind = (vars_costy(i)-1)*5 + 5;
    vars_rmd = [vars_rmd s_ind:e_ind];
end
vars_rmd = vars_rmd + num_loops + num_branchs;

feats_rmd = [loops_rmd branchs_rmd vars_rmd];
