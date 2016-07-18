function runner(n)
    % feature accel off;
    % feature accel on;
    rdata = createMatrixRandJS(2,n);
    n = 10;
    tic();
    piValue = MonteCarlo(rdata);
    elapsedTime=toc();
    fprintf('{ "time": %f, "pi": %f }\n',elapsedTime,piValue);
end
