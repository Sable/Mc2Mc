#!/usr/bin/env bash
# sh runOstrich2.sh > runOstrich2.log

dataPath=data/ostrich2
#gitPath=/Users/wukefe/Documents/GitHub/wu/Ostrich2/benchmarks
proc=processing
#opt="tir"
opt="plus"
run="mc2mc_jar.sh"

# backprop
echo $proc backprop
./$run $dataPath/backprop/bp_core.m -args "\
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&?*?&REAL \
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&1*1&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&?*?&REAL DOUBLE&?*?&REAL \
DOUBLE&?*?&REAL " \
-out "data/ostrich2/backprop/$opt" -$opt


# blackscholes
echo $proc blackscholes
./$run $dataPath/blackscholes/runBlkSchls_new.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL" \
-out "data/ostrich2/blackscholes/$opt" -$opt

# capr
echo $proc capr
./$run $dataPath/capr/capacitor.m -args \
"DOUBLE&1*1&REAL \
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&1*1&REAL \
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&1*1&REAL" \
-out "data/ostrich2/capr/$opt" -$opt

# crni
echo $proc crni
./$run $dataPath/crni/crnich.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&1*1&REAL \
DOUBLE&1*1&REAL DOUBLE&1*1&REAL" \
-out "data/ostrich2/crni/$opt" -$opt

# fft
echo $proc fft
./$run $dataPath/fft/fft2D.m -args "\
DOUBLE&?*?&REAL DOUBLE&?*?&REAL DOUBLE&1*1&REAL" \
-out "data/ostrich2/fft/$opt" -$opt

# nw
echo $proc nw
./$run $dataPath/nw/needle.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&1*1&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&?*?&REAL \
DOUBLE&?*?&REAL DOUBLE&24*24&REAL" \
-out "data/ostrich2/nw/$opt" -$opt

# pagerank
echo $proc pagerank
./$run $dataPath/pagerank/pagerank.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&?*?&REAL \
DOUBLE&?*1&REAL DOUBLE&?*1&REAL DOUBLE&1*1&REAL" \
-out "data/ostrich2/pagerank/$opt" -$opt

# spmv
echo $proc spmv
./$run $dataPath/spmv/spmv_core.m -args "\
DOUBLE&1*1&REAL DOUBLE&1*1&REAL DOUBLE&1*?&REAL \
DOUBLE&1*?&REAL DOUBLE&1*?&REAL DOUBLE&1*?&REAL" \
-out "data/ostrich2/spmv/$opt" -$opt

# mc
echo $proc mc
./$run $dataPath/mc/MonteCarlo.m -args "\
DOUBLE&2*?&REAL" \
-out "data/ostrich2/mc/$opt" -$opt