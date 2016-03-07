function [r] = simplefor(k)
  n = k + 1;
  x = 0;
  for i = 1:n
    x = x * i + 1;
  end
  r = x;
end