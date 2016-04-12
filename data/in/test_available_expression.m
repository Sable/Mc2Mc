function [res] = test_available_expression(n)
% case 1
  for i = 1:100
    y = i + i + 1;
    z = i + i + 1;
    k = y * z;
  end
% case 2
  res = zeros(1,n);
  for i = 1:n
    if(i+1 < n)
      res(i+1) = res(i) + 1;
    end
  end
end