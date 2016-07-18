function runner(size,penalty,seq1_file,seq2_file,nw_file)
% Example: runner(4096,1);

% feature accel off;
% feature accel on;
% s = RandStream('mcg16807','Seed',49734321);
% RandStream.setGlobalStream(s);
setRandomSeed();

bound_value = [0, 10000];
input_seq_1_size = size;
input_seq_2_size = size;
nb_possible_seq_items = 10;
print_results = 0;

if or(nb_possible_seq_items < 1, nb_possible_seq_items > 24)
    error('The number of different items to generate should be between 1 and 24.\n');
end

max_rows = input_seq_1_size + 1;
max_cols = input_seq_2_size + 1;
reference= zeros(max_rows, max_cols);
input_itemsets = zeros(max_rows, max_cols);
input_seq_1 = ones(1, max_rows);
input_seq_2 = ones(1, max_cols);
aligned_seq_size = input_seq_1_size + input_seq_2_size;
aligned_seq_1 = zeros(1, aligned_seq_size) - 1;
aligned_seq_2 = zeros(1, aligned_seq_size) - 1;

for i = 2:max_rows
    %input_seq_1(i) = 1 + mod(randi(bound_value), nb_possible_seq_items);
    input_seq_1(i) = 1 + mod(abs(commonRandom()), nb_possible_seq_items);
end

for i = 2:max_cols
    %input_seq_2(i) = 1 + mod(randi(bound_value), nb_possible_seq_items);
    input_seq_2(i) = 1 + mod(abs(commonRandom()), nb_possible_seq_items);
end

fp_seq1 = fopen(seq1_file, 'r');
expected_aligned_seq_1_chars = fscanf(fp_seq1, '%c');
expected_aligned_seq_1 = expected_aligned_seq_1_chars - 'A';
expected_aligned_seq_1(expected_aligned_seq_1<0 | expected_aligned_seq_1>=26) = -1;
fclose(fp_seq1);
expected_aligned_seq_1_size = length(expected_aligned_seq_1);

fp_seq2 = fopen(seq2_file, 'r');
expected_aligned_seq_2_chars = fscanf(fp_seq2, '%c');
expected_aligned_seq_2 = expected_aligned_seq_2_chars - 'A';
expected_aligned_seq_2(expected_aligned_seq_2<0 | expected_aligned_seq_2>=26) = -1;
fclose(fp_seq2);
expected_aligned_seq_2_size = length(expected_aligned_seq_2);

if print_results
    disp('Computing dynamic programming results');
end

fileID = fopen(nw_file, 'r');
blosum62 = reshape(fscanf(fileID,'%d'),24,24);
fclose(fileID);

tic
[input_itemsets,reference] = needle(penalty, max_rows, max_cols, ...
    input_seq_1, input_seq_2, reference, input_itemsets, blosum62);
elapsedTime = toc;

aligned_index_1 = aligned_seq_size;
aligned_index_2 = aligned_seq_size;
if print_results
    disp('Trace solution back');
end


i = max_rows;
j = max_cols;
while or(i>1, j>1)
    if and(i>1, j>1)
        nw = input_itemsets(i-1, j-1) + reference(i, j);
        w  = input_itemsets(i, j-1) - penalty;
        n  = input_itemsets(i-1, j) - penalty;
        n_limit = 0;
        w_limit = 0;
        traceback = max(nw, max(w, n));
    elseif i == 1
        n_limit = 1;
        w_limit = 0;
    elseif j == 1
        n_limit = 0;
        w_limit = 1;
    else
        fprintf(2, 'ERROR1');
    end

    if and(n_limit == 0, and(w_limit == 0, traceback == nw))
        aligned_seq_1(aligned_index_1) = input_seq_1(i) - 1;
        aligned_seq_2(aligned_index_2) = input_seq_2(j) - 1;
        aligned_index_1 = aligned_index_1 - 1; i = i - 1;
        aligned_index_2 = aligned_index_2 - 1; j = j - 1;
    elseif or(n_limit == 1, traceback == w)
        aligned_index_1 = aligned_index_1 - 1;
        aligned_seq_2(aligned_index_2) = input_seq_2(j) - 1;
        aligned_index_2 = aligned_index_2 - 1; j = j - 1;
    elseif or(w_limit == 1, traceback == n)
        aligned_index_2 = aligned_index_2 - 1;
        aligned_seq_1(aligned_index_1) = input_seq_1(i) - 1;
        aligned_index_1 = aligned_index_1 - 1; i = i - 1;
    else
        fprintf(2, 'ERROR2');
    end
end

if print_results
    disp('Input Seq 1  :');
    char(input_seq_1(2:max_rows));
    disp('Input Seq 2  :');
    char(input_seq_2(2:max_cols));
    disp('Aligned Seq 1:');
    char(aligned_seq_1);
    disp('Aligned Seq 2:');
    char(aligned_seq_2);
    if print_intermediary_results
        op = ['-', '+'];
        for i = 1:max_rows
            for j = 1:max_cols
                fprintf('%c%.2d ',op(1 + (input_itemsets(i,j) >= 0)), abs(input_itemsets(i,j)));
            end
            fprintf('\n');
        end
    end
end


if and(input_seq_1_size == 4096, and(input_seq_2_size == 4096, and(penalty == 1, nb_possible_seq_items == 10)))
    if or(aligned_seq_size ~= expected_aligned_seq_1_size, 0 < sum(aligned_seq_1 ~= expected_aligned_seq_1))
        fprintf(2, 'the aligned sequence 1 is different from the values expected.\n');
    end
    if or(aligned_seq_size ~= expected_aligned_seq_2_size, 0 < sum(aligned_seq_2 ~= expected_aligned_seq_2))
        fprintf(2, 'the aligned sequence 2 is different from the values expected.\n');
    end
else
    fprintf('No self-checking for dimension %d, penalty %d, and number of possible items %d\n',input_seq_1_size,penalty,nb_possible_seq_items);
end

fprintf(1, '{ \"status\": %d, \"options\": \"-n %d -g %d\", \"time\": %f }\n', 1, input_seq_1_size, penalty, elapsedTime);
end
