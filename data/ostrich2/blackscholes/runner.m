function [elapsedTime] = runner(nthread, numOptions, outFile)
% numOptions = str2num(numOptions);

% feature accel off;
[otype,sptprice,strike,rate,volatility,otime,DGrefval] = inputData(numOptions);
elapsedTime = runBlkSchls_new(numOptions,otype,sptprice,strike,rate,volatility,otime,DGrefval);

disp('{ "status": 1,');
disp('"options": ');
disp(numOptions);
disp(', "time": ');
disp(elapsedTime);
disp('}');
end