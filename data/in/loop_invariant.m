function [r] = loop_invariant(k)
  n = k + 1;
  y = exp(3);
  z = 3.14;
  a = zeros(1, n);
  for i = 1:n
    x = y + z;
    a(i) = i * i + x * x;
  end
  r = x;
end