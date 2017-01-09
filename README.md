# Mc2Mc

Mc2Mc is a MATLAB to MATLAB source code translator designed for MATLAB vectorization.

## Configurations

Development platforms:

- Tamer framework (Developed at Sable/McGill)
- IntelliJ IDEA (tested under v15.0.4)

Jar (mc2mc.jar)

- Require [`libs/McLabCore.jar`](https://github.com/Sable/mclab-core/releases)
- `out/artifacts/mc2mc_jar/mc2mc.jar`
    + [Built with IntelliJ IDEA](http://stackoverflow.com/questions/1082580/how-to-build-jars-from-intellij-properly); or
    + Using Ant: `ant jar`; or
    + Download from the [latest release](/releases/latest) 

Scripts

- [`mc2mc_jar.sh`](mc2mc_jar.sh): run with the jar file
- [`runOstrich2.sh`](runOstrich2.sh): run with `mc2mc_jar.sh` to test all benchmarks
    + **opt** can be either *plus* or *tir*
    + *tir*: generates TameIR before vectorization
    + *plus*: generates aggregated human-readable code after vectorization


## Execution

### Run all benchmarks

```bash
sh runOstrich2.sh > runOstrich2.log
```

### Run a benchmark

Generate vectorization code for `Black-Scholes` benchmark with the following command line.
(Please check [`runOstrich2.sh`](runOstrich2.sh) for more examples)

```
./mc2mc_jar.sh data/ostrich2/blackscholes/runBlkSchls_new.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL" \
-out "data/ostrich2/blackscholes/plus" -plus
```

## Benchmarks

### Source

- Source folder: [`data/ostrich2`](data/ostrich2)
- Benchmark testing repository (lcpc16-analysis: https://github.com/Sable/lcpc16-analysis)


### Benchmark list

- Back-Propagation (BP)
- Black-Scholes (BS)
- Capacitance (CAPR)
- Crank-Nicholson (CRNI)
- Fast Fourier Transform (FFT)
- Monte Carlo simulation (MC)
- Needleman-Wunsch (NW)
- Page-Rank (PR)
- Sparse Matrix-Vector Multiplication (SPMV)

## Options


```
> ./mc2mc_jar.sh --help

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


## Development notes

**TamerViewer** is used to present the structure of Tame IR.

```java
TamerViewer tv = new TamerViewer(node);
tv.GetViewer();
```


More detail can be found at [wiki](https://github.com/Sable/Mc2Mc/wiki/TamerViewer).

## Copyright and License

Copyright 2016-2017 Hanfeng Chen, Alexander Krolik, Erick Lavoie, Laurie Hendren 
and McGill University.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this work except in compliance with the License. You may obtain a copy
of the License in the LICENSE file, or at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the LICENSE.