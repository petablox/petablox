#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import re
import string
import sys

""" @file
Infrastructure for producing the functions in @ref Generated from a
Context-Free Grammar.

The accepted format for the input grammar is as follows:
- Except otherwise noted, the parser is insensitive to whitespace, including
  extra blank lines.
- Comments are written using the `#` character, and extend to the end of the
  line.
- A @Symbol can be any alphanumeric string (including underscores). The first
  character must be a letter. Symbols are case-sensitive.
- Symbols don't need to be declared before use.
- Terminal symbols are represented by strings starting with a lowercase
  character. Conversely, names for non-terminals start with an uppercase
  character.
- Each non-terminal may have one or more @Production%s associated with it. The
  RHS of a production can be any sequence of symbols (terminals and/or
  non-terminals) separated by whitespace, e.g. `B c D`. Productions cannot span
  multiple lines. Empty productions can be declared using a single `-`
  character.
- A set of productions is associated with some non-terminal `A` using the
  notation `A :: ...`. The right-hand side of `::` must contain one or more
  productions (as defined above) separated by `|` marks. A series of
  productions may extend to subsequent lines, as long as each line starts a new
  production (the line must begin with `|`). A series of productions ends at
  the first blank line, or at the start of another `::` declaration (there is
  no dedicated end-of-production mark). The same non-terminal may be associated
  with any number of `::` declarations.
- A symbol on the RHS of a `::` declaration may be prefixed with `_` to signify
  that the corresponding Edge should be traversed in reverse during the
  CFL-Reachability computation.
- The result of a production and/or any symbol used in a production may be
  associated with an index, using the notation `[i]`, where `i` can be any
  single latin letter. A production must contain either 0 or 2+ indexed
  symbols, and they must all use the same index character. The resulting code
  will only trigger an indexed production if the indices on the edges match.
  Leaving out the index expression for some symbol, or using the `[*]`
  expression for it, causes the generated solver code to ignore indices when
  matching it.

Example of a valid grammar specification:

    S :: - | koo # this is a comment
       | A[j] _A[j]
    A[i] :: foo[i] S _S bar[*]

- To specify weights for terminal symbols, include the line

    .weights symbol0 weight0 symbol1 weight1 ...

  Weights must be integers, and currently cannot be specified for
  individual indices.

Example of a weight specification:

    .weights A 1 B 2
"""

class ListDict:
    """
    An append-only dictionary, where keys are associated with lists of values.

    The default value for a key is the empty list.
    """

    def __init__(self):
        """
        Create an empty dictionary (where every key maps to the empty list).
        """
        self._dict = {}

    def append(self, key, value):
        """
        Append a value to the list associated with some key.
        """
        if not key in self._dict:
            self._dict[key] = []
        self._dict[key].append(value)

    def get(self, key):
        """
        Get the list associated with a key.

        Returns the empty list if the key is not present in the dictionary.
        """
        return self._dict.get(key, [])

    def __iter__(self):
        """
        Iterate over those keys that map to non-empty lists.
        """
        for k in self._dict:
            yield k

    def __str__(self):
        return '\n'.join(['\n'.join(['%s:' % k] +
                                    ['\t%s' % v for v in self._dict[k]])
                          for k in self._dict])

class Symbol:
    """
    A symbol in the input grammar.
    """
    _num_temps = 0
    _num_symbols = 0
    _symbol_list = []
    _symbol_dict = {}

    def __init__(self, name=None):
        """
        Create a symbol with the specified name, or generate a unique
        non-terminal if no name is provided.

        Generated symbols are guaranteed to never clash with any user-defined
        symbol.
        """
        if name is None:
            name = '%' + str(Symbol._num_temps)
            Symbol._num_temps += 1
        else:
            assert re.match(r'^[a-zA-Z]\w*$', name) is not None, \
                "Invalid symbol: %s" % name
        ## The @Symbol<!-- -->'s string in the input grammar.
        self.name = name
        if self not in Symbol._symbol_dict:
            Symbol._symbol_dict[self] = Symbol._num_symbols
            Symbol._symbol_list.append(self)
            Symbol._num_symbols += 1

    def is_terminal(self):
        """
        Check if this is a terminal symbol.

        I.e., if its name starts with a lowercase character.
        """
        return self.name[0] in string.ascii_lowercase

    def __key__(self):
        return self.name

    def __eq__(self, other):
        return type(self) == type(other) and self.__key__() == other.__key__()

    def __hash__(self):
        return hash(self.__key__())

    def __str__(self):
        return self.name

    @staticmethod
    def num_symbols():
        """
        Get the number of distinct @Symbol%s encountered so far.
        """
        return Symbol._num_symbols

    @staticmethod
    def symbols():
        """
        Iterate over all @Symbol%s encountered so far, sorted by their @Kind.
        """
        return Symbol._symbol_list

    @staticmethod
    def symbol2kind(symbol):
        """
        Map a @Symbol to its @Kind.
        """
        return Symbol._symbol_dict[symbol]

    @staticmethod
    def kind2symbol(kind):
        """
        Get the @Symbol corresponding to some @Kind.
        """
        return Symbol._symbol_list[kind]

class Literal:
    """
    An instance of some @Symbol in a @Production.

    May optionally contain a 'reverse' modifier and/or an @Index expression.
    """

    def __init__(self, symbol, indexed, reversed=False):
        ## The @Symbol represented by this @Literal.
        self.symbol = symbol
        ## Whether this @Literal contains an @Index expression.
        #  We don't have to store the actual index character, since all the
        #  indexed @Literal%s in the same @Production must use the same
        #  character anyway.
        self.indexed = indexed
        ## Whether this @Literal has a 'reverse' modifier.
        self.reversed = reversed

    def __str__(self):
        return (('_' if self.reversed else '') + str(self.symbol) +
                ('[i]' if self.indexed else ''))

class Production:
    """
    A production of the input grammar.
    """

    def __init__(self, result, used):
        Production._check_production(result, used)
        ## The @Literal on the LHS of this @Production.
        self.result = result
        ## An ordered list of the @Literal%s on the RHS of this @Production.
        self.used = used

    def split(self):
        """
        Split this into a list of @Production%s with up to 2 @Literal%s on the
        RHS.

        Our splitting strategy works as follows:

            S :: a b c d e =>
            S :: ((((a b) c) d) e) =>
            T0 :: a b
            T1 :: T0 c
            T2 :: T1 d
            S  :: T2 e
        """
        # TODO: Could optimize the number of indexed intermediates (and even
        #       the total number of intermediates?) by choosing a different
        #       splitting strategy according to the current input (e.g.
        #       right-to-left grouping, tree-style splitting).
        if len(self.used) <= 2:
            return [self]
        r_used = self.used[1:]
        num_temps = len(self.used) - 2
        temp_symbols = [Symbol() for _ in range(0, num_temps)]
        temp_indexed = [True for _ in temp_symbols]
        # The only intermediate symbols that need to be indexed are those
        # between the first and the last indexed literals in the original
        # production (the result of the production counts as the rightmost
        # literal for this purpose).
        if not self.result.indexed:
            for i in range(num_temps-1, -1, -1):
                if r_used[i+1].indexed:
                    break
                else:
                    temp_indexed[i] = False
        if not self.used[0].indexed:
            for i in range(0, num_temps):
                if r_used[i].indexed:
                    break
                else:
                    temp_indexed[i] = False
        temps = [Literal(s, i) for (s, i) in zip(temp_symbols, temp_indexed)]
        l_used = [self.used[0]] + temps
        results = temps + [self.result]
        return [Production(r, [ls, rs])
                for (r, ls, rs) in zip(results, l_used, r_used)]

    def get_rev_prods(self):
        """
        Get all the @ReverseProduction%s corresponding to this @Production.

        Only works for @Production%s with up to 2 @Literal%s in the RHS, bigger
        ones should be @link Production::split() split@endlink first.
        """
        num_used = len(self.used)
        if num_used == 0:
            return [ReverseProduction(self.result, None)]
        elif num_used == 1:
            return [ReverseProduction(self.result, self.used[0])]
        elif num_used == 2:
            rev_l = ReverseProduction(self.result, self.used[0], self.used[1],
                                      True)
            rev_r = ReverseProduction(self.result, self.used[1], self.used[0],
                                      False)
            return [rev_l, rev_r]
        else:
            assert False, "Productions must be split before further processing"

    def __str__(self):
        rhs = ('-' if self.used == []
               else ' '.join([str(s) for s in self.used]))
        return str(self.result) + ' :: ' + rhs

    @staticmethod
    def _check_production(result, used):
        assert not result.symbol.is_terminal(), "Can't produce non-terminals"
        assert not result.reversed, "Can't produce reversed literals"
        indexed_literals = [s for s in [result] + used if s.indexed]
        assert indexed_literals == [] or len(indexed_literals) >= 2, \
            "At least two indexed literals required per production"

class ReverseProduction:
    """
    A @Production as seen from the point of view of some element on the RHS.

    A regular @Production (e.g. `S :: T R`) specifies what we can combine (a
    `T` followed by an `R`) to synthesize the LHS (an `S`). Conversely, a
    @ReverseProduction (e.g. `T + (* R) => S`) assumes we already have the
    'base' (a `T`), and specifies what additional elements are required (a
    subsequent `R`) to produce the 'result' (an `S`).

    This implementation allows up to one additional required @Literal.

    The public API exposed by this class translates the abstract, grammar-level
    relations between @Symbol%s to the corresponding low-level solver
    instructions that implement those relations. In the context of the solver,
    each @Literal is represented by an Edge of the appropriate @Kind, and the
    'base' @Literal corresponds to an Edge of the appropriate @Kind which we
    are currently processing.

    We will use the @ReverseProduction `B[i] + (A[i] *) => C[i]` as a running
    example to illustrate the functionality of the methods in this class. As
    part of this example, we assume we are currently processing an Edge
    compatible with the 'base' of this @ReverseProduction, i.e., an Edge for
    @Symbol `B`.
    """

    def __init__(self, result, base, reqd=None, comes_after=True):
        assert not(base is None and reqd is not None), \
            "Empty productions can't take a required literal"
        used = (([] if base is None else [base]) +
                ([] if reqd is None else [reqd]))
        Production._check_production(result, used)
        ## The @Literal generated by this production.
        self.result = result
        ## The @Literal we assume to be present.
        #
        #  Is `None` if this corresponds to an empty production.
        self.base = base
        ## The additional @Literal we need to complete the production.
        #
        #  Is `None` if this corresponds to a single-element production.
        self.reqd = reqd
        ## Whether the required @Literal needs to come after or before the one
        #  we already have.
        self.comes_after = comes_after

    def __check_need_to_search(self):
        assert self.base is not None, \
            "No need to search for empty productions"
        assert self.reqd is not None, \
            "No need to search for single-element productions"

    # TODO: The following methods return strings, which are tied to a
    #       particular variable and function naming. It might be more robust to
    #       return True/False and have the caller pick the strings to use.

    def search_endpoint(self):
        """
        On which endpoint of the Edge being processed we should search for an
        Edge that can complete this @ReverseProduction.

        In our running example, we need to search on the source Node.
        """
        self.__check_need_to_search()
        if self.base.reversed ^ self.comes_after:
            return 'to'
        else:
            return 'from'

    def search_direction(self):
        """
        On which set of Edge%s (incoming or outgoing) of the
        @link ReverseProduction::search_endpoint() search endpoint@endlink
        we should search for an Edge that can complete this @ReverseProduction.

        In our running example, we need to search within the incoming Edge%s.
        """
        self.__check_need_to_search()
        if self.reqd.reversed ^ self.comes_after:
            return 'Out'
        else:
            return 'In'

    def result_source(self):
        """
        Where we should place the source Node of any Edge produced by this
        @ReverseProduction.

        In our running example, we would place it on the source Node of the
        'other' Edge (the one representing @Symbol `A`).
        """
        if self.reqd is None:
            edge = 'base'
            endpoint = 'to' if self.base.reversed else 'from'
        else:
            edge = 'base' if self.comes_after else 'other'
            if (not self.base.reversed and self.comes_after or
                not self.reqd.reversed and not self.comes_after):
                endpoint = 'from'
            else:
                endpoint = 'to'
        return edge + '.' + endpoint

    def result_target(self):
        """
        Where we should place the target Node of any Edge produced by this
        @ReverseProduction.

        In our running example, we would place it on the target Node of the
        'base' Edge (the one representing @Symbol `B`).
        """
        if self.reqd is None:
            edge = 'base'
            endpoint = 'from' if self.base.reversed else 'to'
        else:
            edge = 'other' if self.comes_after else 'base'
            if (not self.base.reversed and not self.comes_after or
                not self.reqd.reversed and self.comes_after):
                endpoint = 'to'
            else:
                endpoint = 'from'
        return edge + '.' + endpoint

    def result_indices(self):
        """
        How we should fill the IndexSet of any Edge produced by this
        @ReverseProduction.

        In our running example, we would add only those @Indices that are
        present on both the `A`-Edge and the `B`-Edge (because, according to
        the @ReverseProduction, the @Indices on the two combined Edge%s must
        match).
        """
        if self.reqd is None:
            if not self.base.indexed and not self.result.indexed:
                return 'false'
            elif self.base.indexed and self.result.indexed:
                return 'true'
            else:
                assert False
        else:
            base_idx = self.base.indexed
            reqd_idx = self.reqd.indexed
            res_idx = self.result.indexed
            if not base_idx and not reqd_idx and not res_idx:
                return 'false'
            elif not base_idx and reqd_idx and res_idx:
                return 'true'
            elif base_idx and not reqd_idx and res_idx:
                return 'true'
            elif base_idx and reqd_idx and not res_idx:
                return 'false'
            elif base_idx and reqd_idx and res_idx:
                return 'true'
            else:
                assert False

    def must_check_for_common_index(self):
        """
        Whether we need to add an additional @Index compatibility check for the
        two combined Edge%s.

        In our running example, we don't need to add any additional check,
        since we are already taking the intersection of the IndexSet%s on the
        `A`-Edge and the `B`-Edge.
        """
        return (self.base.indexed and self.reqd.indexed)

    def __str__(self):
        have = '-' if self.base is None else str(self.base)
        if self.reqd is None:
            need = ''
        elif self.comes_after:
            need = ' + (* %s)' % self.reqd
        else:
            need = ' + (%s *)' % self.reqd
        return have + need + ' => ' + str(self.result)

class CFGParser:
    """
    Parser for our input grammar format.
    """

    def __init__(self):
        self._lhs = None
        self._lhs_index = None
        ## All the @Production%s encountered so far, grouped by result @Symbol.
        self.prods = ListDict()
        ## All the @ReverseProduction%s encountered so far, grouped by result
        #  @Symbol.
        self.rev_prods = ListDict()
        ## All the 
        self.outNonTerminals = []
        self.weights = {}

    def parse_line(self, line):
        """
        Parse the next line of the grammar specification.
        """
        line_wo_comment = (line.split('#'))[0]
        toks = line_wo_comment.split()
        if toks == []:
            self._lhs = None
            self._lhs_index = None
            return
        if toks[0] == '.output':
            toks = toks[1:]
            for ont in toks:
                self.outNonTerminals.append(ont)
            return
        elif toks[0] == '.weights':
            toks = toks[1:]
            assert len(toks)%2 == 0, "weights not paired up correctly"
            for i in range(0, len(toks), 2):
                self.weights[toks[i]] = toks[i+1]
            return
        elif toks[0] == '|':
            assert self._lhs is not None, "| without preceding production"
            toks = toks[1:]
        elif len(toks) >= 2 and toks[1] == '::':
            (self._lhs, self._lhs_index) = self.__parse_literal(toks[0])
            toks = toks[2:]
        else:
            assert False, "Malformed production"
        while '|' in toks:
            split_pos = toks.index('|')
            self.__parse_production(toks[:split_pos])
            toks = toks[split_pos+1:]
        self.__parse_production(toks)

    def __parse_production(self, toks):
        assert toks != [], u"Empty production not marked with '-'"
        used = []
        if toks != ['-']:
            prod_index = self._lhs_index
            for t in toks:
                (lit, i) = self.__parse_literal(t)
                used.append(lit)
                if i is not None:
                    assert prod_index is None or i == prod_index, \
                        "Production contains more than one distinct indices"
                    prod_index = i
        prod = Production(self._lhs, used)
        for p in prod.split():
            self.prods.append(p.result.symbol, p)
            for rp in p.get_rev_prods():
                base_symbol = None if rp.base is None else rp.base.symbol
                self.rev_prods.append(base_symbol, rp)

    def __parse_literal(self, str):
        matcher = re.match(r'^(_?)([a-zA-Z]\w*)(?:\[([a-zA-Z\*])\])?$', str)
        assert matcher is not None, "Malformed symbol: %s" % str
        reversed = matcher.group(1) != ''
        symbol = Symbol(matcher.group(2))
        index = matcher.group(3)
        if index == '*':
            index = None
        indexed = index is not None
        return (Literal(symbol, indexed, reversed), index)

def parse(fin, fout, className):
    """
    Read a grammar specification from @a fin and print out the corresponding
    solver code to @a fout.

    @param [in] fin A File-like object to read from.
    @param [out] fout A File-like object to write to.
    """
    parser = CFGParser()
    fout.write('package stamp.missingmodels.grammars;\n')
    fout.write('import stamp.missingmodels.util.jcflsolver.*;\n')
    fout.write('\n')

    fout.write('/* Original Grammar:\n')
    for line in fin:
        parser.parse_line(line)
        fout.write(line)
    fout.write('*/\n')
    fout.write('\n')

    fout.write('/* Normalized Grammar:\n')
    fout.write('%s\n' % parser.prods)
    fout.write('*/\n')
    fout.write('\n')

    fout.write('/* Reverse Productions:\n')
    fout.write('%s\n' % parser.rev_prods)
    fout.write('*/\n')
    fout.write('\n')
    
    fout.write('public class %s extends Graph {\n' % className) 
    fout.write('\n')

    fout.write('public boolean isTerminal(int kind) {\n')
    fout.write('  switch (kind) {\n')
    for s in Symbol.symbols():
        if s.is_terminal():
            fout.write('  case %s:\n' % Symbol.symbol2kind(s))
    fout.write('    return true;\n')
    fout.write('  default:\n')
    fout.write('    return false;\n')
    fout.write('  }\n')
    fout.write('}\n')
    fout.write('\n')

    fout.write('public int numKinds() {\n')
    fout.write('  return %s;\n' % Symbol.num_symbols())
    fout.write('}\n')
    fout.write('\n')

    fout.write('public int symbolToKind(String symbol) {\n')
    for s in Symbol.symbols():
        fout.write('  if (symbol.equals("%s")) return %s;\n'
                   % (s, Symbol.symbol2kind(s)))
    fout.write('  throw new RuntimeException("Unknown symbol "+symbol);\n')
    fout.write('}\n')
    fout.write('\n')

    fout.write('public String kindToSymbol(int kind) {\n')
    fout.write('  switch (kind) {\n')
    for s in Symbol.symbols():
        fout.write('  case %s: return "%s";\n' % (Symbol.symbol2kind(s), s))
    fout.write('  default: throw new RuntimeException("Unknown kind "+kind);\n')
    fout.write('  }\n')
    fout.write('}\n')
    fout.write('\n')

    fout.write('public void process(Edge base) {\n')
    fout.write('  switch (base.kind) {\n')
    for base_symbol in Symbol.symbols():
        rev_prods = parser.rev_prods.get(base_symbol)
        if rev_prods == []:
            # This symbol doesn't appear on the RHS of any production.
            continue
        fout.write('  case %s: /* %s */\n'
                   % (Symbol.symbol2kind(base_symbol), base_symbol))
        # TODO: Might happen that two separate productions have:
        #       - the same search endpoint
        #       - the same search direction
        #       - the same kind for the required edge
        #       Then, we could combine their two loops into one.
        for rp in rev_prods:
            fout.write('    /* %s */\n' % rp)
            res_src = rp.result_source()
            res_trgt = rp.result_target()
            res_kind = Symbol.symbol2kind(rp.result.symbol)
            res_idx = rp.result_indices()
            if rp.reqd is None:
                fout.write('    addEdge(%s, %s, %s, base, %s);\n'
                           % (res_src, res_trgt, res_kind, res_idx))
            else:
                search_node = 'base.' + rp.search_endpoint()
                search_dir = rp.search_direction()
                reqd_kind = Symbol.symbol2kind(rp.reqd.symbol)

                fout.write('    for(Edge other : %s.get%sEdges(%s)){\n'
                           % (search_node, search_dir, reqd_kind))

                fout.write('      addEdge(%s, %s, %s, base, other, %s);\n'
                           % (res_src, res_trgt, res_kind, res_idx))
                fout.write('    }\n')
        fout.write('    break;\n')
    fout.write('  }\n')
    fout.write('}\n')
    fout.write('\n')

    fout.write('public String[] outputRels() {\n');
    outRels = ['"' + e + '"' for e in parser.outNonTerminals]
    fout.write('    String[] rels = {%s};\n' % ", ".join(outRels));
    fout.write('    return rels;\n');
    fout.write('}\n');
    fout.write('\n');

    usereps = 'true'
    fout.write('public short kindToWeight(int kind) {\n');
    fout.write('  switch (kind) {\n')
    for s in Symbol.symbols():
        if s.name in parser.weights:
            usereps = 'false'
            fout.write('  case %s:\n' % Symbol.symbol2kind(s))
            fout.write('    return (short)%s;\n' % parser.weights[s.name])
    fout.write('  default:\n')
    fout.write('    return (short)0;\n')
    fout.write('  }\n')
    fout.write('}\n')
    fout.write('\n')

    fout.write('public boolean useReps() { return %s; }\n' % usereps)

    fout.write('\n')
    fout.write('}');

# TODO: More user-friendly error output than assertion failure
# TODO: We treat our custom objects as immutable, but we don't have any
#       guarantees on that.
# TODO: More structured way to synthesize code: specialized C-code synthesis
#       class, or put base program text in a large triple-quoted string and
#       leave %s's for places to fill in.

## Help message describing the calling convention for this script.
usage_string = """Usage: %s <input-file> [<output-dir>]
Produce CFL-Reachability solver code for a Context-Free Grammar.
<input-file> must contain a grammar specification (see the main project docs
for details), and have a .cfg extension.
Output is printed to a file inside <output-dir> with the same name as
<input-file>, but with the .cfg extension stripped.
If no output directory is given, print generated code to stdout.
"""

def _main():
    if (len(sys.argv) < 2 or sys.argv[1] == '-h' or sys.argv[1] == '--help' or
        os.path.splitext(sys.argv[1])[1] != '.cfg'):
        script_name = os.path.basename(__file__)
        sys.stderr.write(usage_string % script_name)
        exit(1)
    with open(sys.argv[1], 'r') as fin:
        if len(sys.argv) >= 3:
            base_outfile = os.path.basename(os.path.splitext(sys.argv[1])[0])
            outfile = os.path.join(sys.argv[2], base_outfile)
            className = os.path.splitext(base_outfile)[0]
            with open(outfile, 'w') as fout:
                parse(fin, fout, className)
        else:
            parse(fin, sys.stdout, className)

if __name__ == '__main__':
    _main()
