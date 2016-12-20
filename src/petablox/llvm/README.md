# VIVAS_

## LLVM Pass
An LLVM pass to generate a relational representation of `C/C++` programs.

### Source files
The file `src/llvmToDatalog.c` contains the source code for the LLVM pass.
The file `src/vivas-clang.c` is the source code for a drop in substitute for clang 
that is inspired and adapted from AFL's instrumentation framework [1].

### Setup
To set up the LLVM pass, run `./setup.sh`. 
This script will download LLVM/Clang and compile both the `llvmToDatalog` pass
and the custom `vivas-clang` drop in compiler.

### Usage
To run the pass, execute the command: `./generate-facts.sh /path/to/input/program.c`

The pass can also analyze programs that are  compiled using the `make` and `configure` systems.
In the program root directory, run `CC=/path/to/vivas-clang CXX=/path/to/vivas-clang++ ./configure`.
Then, to execute the analysis, run `IS_MAKE=1 /path/to/generate-facts.sh`.

## References
[1] Michal Zalewski, AFL. [https://github.com/mirrorer/afl](https://github.com/mirrorer/afl).
