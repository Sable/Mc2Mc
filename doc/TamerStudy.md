
## Source

- Tamer thesis by Anton Dubrau
- [Writing analyses using Tamer](https://github.com/Sable/mclab-core/wiki/Writing-analyses-using-Tamer)
- [Using McLab for development](https://github.com/Sable/mclab-core/wiki/Using-McLab-for-development)

### General ideas

- Tamer is designed for static languages, such as FORTRAN
- Tamer is built on McSAF
- TIRNode is valid in AST node
- Check D.1,D.2,D.3 IR structures (page 132/136)

Value analysis

- Interprocedural analysis
- Simple range analysis

### Chapter 4, Tame IR

- TIR only involves statements and function calls
- Check section 4.1 for each statement
- Basic statements
- Control flow statements

### Chapter 6, Interprocedural value analysis

- Sophisticated flow analysis
- Support more built-in functions than McFor


### Benchmark issues

- See section 6.5, "half of the benchmarks from the FALCON project"
