function [a,b,c] = allstructs(value)
global x
  x = value;
  % test all structures
  if(value > 1)
    disp('value is greater than 1');
  end

  n = value * value;
  c = 0;
  for i = 1:n
    c = c + i;
    if c > 100
      break;
    elseif c > 200
      continue;
    else
      c = -1;
    end
  end

  k = value;
  while k > 10
    if k > 20
      k = k - 2;
    else
      k = k - 1;
    end
  end

  a = x - 1;
  b = x + 1;
  c = x * 1;
end