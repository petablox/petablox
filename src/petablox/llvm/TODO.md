# TODO

## LLVM Translation
This list contains all of the LLVM operations/features.
An `X` next to the item indicates that it has been translated
into a relational representation.

### High level structure
- [ ] Calling convention
- [ ] Global variables
- [ ] Functions
- [ ] Attributes
- [ ] Operands
- [ ] Control flow
    - [x] Basic block
    - [x] Instructions
    - [ ] Procedure calls

### Types
- [ ] General type
- [ ] Void
- [ ] Function
- [ ] First class
    - [ ] Single value
        - [ ] Integer
        - [ ] Floating point
        - [ ] X86_mmx
        - [ ] Pointer
        - [ ] Vector
    - [ ] Label
    - [ ] Token
    - [ ] Metadata
    - [ ] Aggregate
        - [ ] Array
        - [ ] Structure
        - [ ] Opaque

### Constants
- [ ] General constant
- [ ] Simple
- [ ] Complex
- [ ] Global
- [ ] Function addresses
- [ ] Undefined
- [ ] Poison values
- [ ] Addresses of basic blocks
- [ ] Constant expressions

### Other Values
- [ ] Inline assembly (?)

### Instructions
- [ ] General instruction
- [ ] Terminator instructions
    - [x] `ret`
    - [x] `br`
    - [ ] `switch`
    - [ ] `indirectbr`
    - [ ] `invoke`
    - [ ] `resume`
    - [ ] `catchswitch`
    - [ ] `catchret`
    - [ ] `cleanupret`
    - [x] `unreachable`
- [x] Binary operations
    - [x] `add`
    - [x] `fadd`
    - [x] `sub`
    - [x] `fsub`
    - [x] `mul`
    - [x] `fmul`
    - [x] `udiv`
    - [x] `sdiv`
    - [x] `fdiv`
    - [x] `urem `
    - [x] `srem`
    - [x] `frem`
- [x] Bitwise binary operations
    - [x] `shl`
    - [x] `lshr`
    - [x] `ashr`
    - [x] `and`
    - [x] `or`
    - [x] `xor`
- [ ] Vector operations
    - [ ] `extractelement`
    - [ ] `insertelement`
    - [ ] `shufflevector`
- [ ] Aggregate Operations
    - [ ] `extractvalue`
    - [ ] `insertvalue`
- [x] Memory access/addressing operations
    - [x] `alloca`
    - [x] `load`
    - [x] `store`
    - [x] `fence`
    - [x] `cmpxchg`
    - [x] `atomicrmw`
    - [x] `getelementptr`
- [x] Conversion operations
    - [x] `trun`
    - [x] `zext`
    - [x] `sext`
    - [x] `fptrunc`
    - [x] `fpext`
    - [x] `fptoui`
    - [x] `fptosi`
    - [x] `uitofp`
    - [x] `sitofp`
    - [x] `ptrtoint`
    - [x] `inttoptr`
    - [x] `bitcast`
    - [x] `addrspacecast`
- [ ] Other operations
    - [x] `icmp`
    - [x] `fcmp`
    - [x] `phi`
    - [x] `select`
    - [ ] `call`
    - [ ] `var_arg`
    - [ ] `landingpad`
    - [ ] `catchpad`
    - [ ] `cleanuppad`
