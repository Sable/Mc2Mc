# Mc2Mc

A MATLAB to MATLAB source code translator.

Mc2Mc is a code translator designed for improving MATLAB code using
static analyses and vectorization.

## Configuration

Platforms:

- Tamer framework (Developed by Sable/McGill)
- IntelliJ IDEA (tested under v15.04)

## Options

```
  Options:
    -a, --all
       Enable all optimizations
       Default: true
    -d, --disable
       Disable all optimization
       Default: false
    -p, --display
       list all optimizations
       Default: false
    -e, --enable
       Enable an optimization
       Default: true
    -h, --help
       list all commands
       Default: false
```

## Development notes

**TamerViewer** is used to present the structure of Tamer programs.

```java
TamerViewer tv = new TamerViewer(node);
tv.GetViewer();
```


More detail can be found at [wiki](https://github.com/Sable/Mc2Mc/wiki/TamerViewer).
