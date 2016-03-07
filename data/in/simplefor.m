function [r] = simplefor(k)
  n = k + 1;
  for i = 1:n
    x = x + i;
  end
  r = x;
end