function [piValue] = MonteCarlo(inputData)
n = size(inputData, 2);
sampleOut = zeros(1,n);
for k=1:n
    sampleOut(k) = SampleFunc(inputData(1,k),inputData(2,k));
end
reduceCount = 0;
for k=1:n
    reduceCount = reduceCount + sampleOut(k);
end
piValue = 4 * reduceCount / n;
end

