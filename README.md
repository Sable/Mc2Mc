# Mc2Mc

Mc2Mc is a MATLAB to MATLAB source code translator designed for MATLAB vectorization.

## Configuration

Platforms:

- Tamer framework (Developed at Sable/McGill)
- IntelliJ IDEA (tested under v15.0.4)

Jar (mc2mc.jar)

- `out/artifacts/mc2mc_jar/mc2mc.jar`
- How to build it? [StackOverflow](http://stackoverflow.com/questions/1082580/how-to-build-jars-from-intellij-properly)

Scripts

- `mc2mc_jar.sh`: run with the jar file
- `runOstrich2.sh`: run with `mc2mc_jar.sh` to test all benchmarks
    + **opt** can be either *plus* or *tir*
    + *tir* generates TameIR before vectorization
    + *plus* generates aggregated human-readable code after vectorization


## Options


```
> java -jar mc2mc.jar --help

Usage: Mc2Mc [options] 
  Options:
    -h, --help
       List all commands
       Default: false
    -v, --view
       Tamer viewer
       Default: false
    -args
       List all optimizations
       Default: <empty string>
    -noplus
       Vectorized TameIR
       Default: false
    -out
       Output directory
       Default: <empty string>
    -plus
       TamePlus with check (default)
       Default: false
    -tir
       Output tameIR
       Default: false
```


## Benchmarks

### Source

- Source folder: `data/ostrich2`
- Benchmark testing repository (lcpc16-analysis: https://github.com/Sable/lcpc16-analysis)


### Benchmark list

- Back Propagation
- Black-Scholes
- Capr
- Crni
- FFT
- Monte Carlo simulation
- NW
- Page Rank
- SPMV

## An example

Generate vectorization code for `Black-Scholes` benchmark with the following command line.
(Please check `runOstrich2.sh` for more examples)

```
./mc2mc_jar.sh data/ostrich2/blackscholes/runBlkSchls_new.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL" \
-out "data/ostrich2/blackscholes/plus" -plus
```

## Development notes

**TamerViewer** is used to present the structure of Tame IR.

```java
TamerViewer tv = new TamerViewer(node);
tv.GetViewer();
```


More detail can be found at [wiki](https://github.com/Sable/Mc2Mc/wiki/TamerViewer).
