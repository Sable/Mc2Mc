function runner(dim,density,normal_stdev,iterations)
% Example: runner_loop(5000,2000,0.01,100);
% random seed

% feature accel off;
% feature accel on;
rand('seed',1234579); %octave

% rand_csr
csr_num_rows     = dim;
csr_num_cols     = dim;
csr_density_perc = density/10000;
csr_nz_per_row   = dim * density / 1000000;
csr_num_nonzeros = round(csr_nz_per_row * dim);
csr_stddev       = normal_stdev * csr_nz_per_row;
csr_Ap           = ones(1,csr_num_rows + 1);
csr_Aj           = ones(1,csr_num_nonzeros);

disp(sprintf('Approximate number of non-zero elements: %d\n', csr_num_nonzeros));

high_bound       = min(csr_num_cols, 2 * csr_nz_per_row);
used_cols        = zeros(1, csr_num_cols);
used_cols        = blanks(csr_num_cols);
update_interval  = round(csr_num_rows / 10);
if update_interval == 0
    update_interval = csr_num_rows;
end

for i = 1:csr_num_rows
    if mod(i, update_interval) == 0
        disp(sprintf('\t%d of %d (%5.1f%%) Rows Generated. Continuing...\n',i,csr_num_rows,i/csr_num_rows*100));
    end
    nnz_ith_row_double  = randn() * csr_stddev + csr_nz_per_row;

    if mod(i, update_interval) == 0
        disp(sprintf('\tRandomly chosen number of non-zero elements in row %d: %5.1f\n',i,nnz_ith_row_double));
    end

    if nnz_ith_row_double < 0
        nnz_ith_row = 0;
    elseif nnz_ith_row_double > high_bound
        nnz_ith_row = high_bound;
    else
        nnz_ith_row = round(nnz_ith_row_double);
    end

    if mod(i, update_interval) == 0
        disp(sprintf('\tActual number of non-zero elements in row %d: %d\n',i,nnz_ith_row));
    end

    csr_Ap(i+1) = csr_Ap(i) + nnz_ith_row;
    if csr_Ap(i+1) > csr_num_nonzeros + 1
        csr_Aj = ones(1, csr_Ap(i+1));
    end
    used_cols(:) = 0;
    for j = 1:nnz_ith_row
        rand_col = randi([1,csr_num_cols]); %rand from [1,csr_num_cols]
        while used_cols(rand_col)
            rand_col = randi([1,csr_num_cols]);
        end
        csr_Aj(csr_Ap(i) + j) = rand_col;
        used_cols(rand_col) = 1;
    end
    sta = csr_Ap(i);
    len = length(csr_Aj);
    csr_Aj(sta:len) = sort(csr_Aj(sta:len));
end
nz_error = abs(csr_num_nonzeros - csr_Ap(csr_num_rows + 1)) / csr_num_nonzeros;
disp(sprintf('Actual number of non-zero elements: %d\n',csr_Ap(csr_num_rows + 1)));
if nz_error >= 0.05
    error('WARNING: Actual NNZ differs from Theoretical NNZ by %5.2f%%!\n',nz_error*100);
end
csr_num_nonzeros = csr_Ap(csr_num_rows + 1);
csr_density_perc = csr_num_nonzeros * 100 / csr_num_cols / csr_num_rows;
csr_density_ppm  = round(csr_density_perc * 10000);
csr_Ax           = zeros(1,csr_num_nonzeros);
for i = 1:csr_num_nonzeros
    csr_Ax(i) = 1 - 2 * rand();
    while csr_Ax(i) == 0
        csr_Ax(i) = 1 - 2 * rand();
    end
end
% the end of rand_csr

vec = rand(1,dim);

tic
for i = 1:iterations
    res = spmv_core(dim,csr_num_rows,csr_Ap,csr_Ax,csr_Aj,vec);
end
elapsedTime = toc;

%disp(sprintf('The first value of the result is %f\n', res(1)));
msg = sprintf('{ \"status\": %d, \"options\": \"-n %d -d %d -s %f\", \"time\": %f }\n', 1, dim, density, normal_stdev, elapsedTime);
disp(msg);

end
