
# Learn Tamer

## Introduction

See [Tamer study](TamerStudy.md).

## Start Tamer

### Parameter passing

Tamer requires specific parameters if main functions need parameters.
The type and shape of parameters are used to initialize the
interprocedual value analysis.
  
The format of the shape and type of input parameters can be as follows:

```
"DOUBLE&1*1&REAL"
```

***Don't forget the two double quotes***

### Study Tame IR

In the introduction, Tamer is built on McSAF which provides basic
operations on AST tree structures.
 
- What are statements and function calls?

**A:** The Tame IR reduces the total number of possible AST nodes. In
particular, we remove all expression nodes, and express their operations
in terms of statements and function calls.

