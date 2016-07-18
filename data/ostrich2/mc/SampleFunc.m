function [z] = SampleFunc(a, b)
if (a^2 + b^2) < 1
    z = 1;
else
    z = 0;
end
end
