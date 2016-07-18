function runner(twoExp, version, verify, debug)
% 'twoExp' is the exponent of two that determines the size of the array
% 'version' is the version of the algorith to use
%        0  is the base version that does not use complex number support in MATLAB 
%        1  is the idiomatic version that uses complex number support in MATLAB
%        2  is the native version that directly calls the fft function from the standard library of MATLAB
% 'verify' checks the output of the algorithm to ensure it is correct
% 'debug'  prints extra information for debugging purposes
%
% Example: runner(10)

% feature accel off;
% feature accel on;

expected_input_values = [...
0.481573149786131970984 0.301748808388446920770 0.340183073820948256305 ...
0.180649106785259638830 0.431881071961759344102 0.436196878628739070916 ...
0.269685438940934441021 0.310521185448687342401 0.332082255015251515129 ...
0.405592626389080113114];

if nargin < 2
    version = 0;
end
if nargin < 3
    verify = 0;
end
if nargin < 4
    debug = 0;
end

n = power(2,twoExp);
if or(twoExp < 0, twoExp > 30)
    error('ERROR: invalid exponent of %d for input size\n',n);
    exit(1);
end

if version == 0
    filename = strcat('data-', num2str(twoExp), '.mat');
    mR = createMatrixFromRandom(n);
    mI = createMatrixFromRandom(n);

    if debug
        fprintf('Input: \n');
        disp(mR);
        disp(mI);
    end
    tic;
    [resR,resI] = fft2D(mR,mI,n);
else
    filename = strcat('data-', num2str(twoExp), '-complex.mat');
    m = complex(createMatrixFromRandom(n),createMatrixFromRandom(n));
    if version == 1
        tic;
        res = fft2DComplex(m,n);
    end
end
elapsedTime = toc;

if debug
    fprintf('Result: \n');
    if version == 0
        disp(resR);
        disp(resI);
    else 
        disp(res)
    end
end

fprintf('{ \"status\": %d, \"options\": \"%d\", \"time\": %f }\n', 1, twoExp, elapsedTime);
end
