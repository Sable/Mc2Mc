function [r] = simplefor(k)
  n = k + 1;
  x = 0;
  for i = 1:n
    t = sqrt(i);
    x = x + t;
  end
  r = x;
end