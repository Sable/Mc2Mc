function runner(layer_size)
% Example: runner(2850000);

% feature accel off;
% feature accel on;

sum_of_hidden_weights = 0;
expected_sum_of_hidden_weights = 10.855641469359398;
expected_layer_size = 2850000;
input_n = layer_size;
hidden_n= 16;
output_n= 1;
in = input_n + 1;
hid= hidden_n + 1;
out= output_n + 1;

% set rand seed
%s = RandStream('mcg16807','Seed',49734321);
%RandStream.setGlobalStream(s);
setRandomSeed();

% bpnn_create
n_in     = input_n + 1;
n_hidden = hidden_n + 1;
n_out    = output_n + 1;
input_weights       = createMatrixRandJS( in, hid); %rand
hidden_weights      = createMatrixRandJS(hid, out); %rand
input_prev_weights  = zeros(in, hid);
hidden_prev_weights = zeros(hid, out);
target              = ones(1,out) * 0.1; %vector
hidden_units = zeros(1,hid);
output_units = zeros(1,out);
hidden_delta = zeros(1,hid);
output_delta = zeros(1,out);

% load(net)
nr = in;
%input_units = createMatrixRandJS(1, nr); %C skips [0]
input_units = zeros(1, nr);
input_units(2:nr) = createMatrixRandJS(1, nr-1);
%for i=2:nr
%    input_units(i) = commonRandomJS(); %rand
%end

%hidden_weights

tic
hidden_weights = bp_core(input_units,hidden_units,input_weights,...
    in,hid,out,output_delta,target,output_units,hidden_delta,...
    hidden_prev_weights,input_prev_weights,hidden_weights);
elapsedTime  = toc;

%hidden_weights

if layer_size == expected_layer_size
    for i=2:hidden_n+1
        for j=2:output_n+1
            sum_of_hidden_weights = sum_of_hidden_weights + hidden_weights(i,j);
        end
    end
    valueA = floor(sum_of_hidden_weights*10e5);
    valueB = floor(expected_sum_of_hidden_weights*10e5);
    if valueA ~= valueB
        error('ERROR: expected a sum of hidden weights of %f for an input size of %d but got %f instead\n',...
            expected_sum_of_hidden_weights, expected_layer_size, sum_of_hidden_weights);
    end
else
    warning('WARNING: no self-checking for input size of %d\n', layer_size);
end

disp('{');
disp('"options":');
disp(layer_size);
disp(', "time": ');
disp(elapsedTime);
disp('}');
end
