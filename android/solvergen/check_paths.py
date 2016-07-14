import cfg_parser
import xml.etree.ElementTree as ET
import os
import os.path
import sys

if len(sys.argv) != 3:
    print 'Usage: %s <grammar> <files-dir>' % sys.argv[0]
    sys.exit(1)

grammar = cfg_parser.Grammar()
with open(sys.argv[1]) as f:
    for line in f:
        grammar.parse_line(line)

def to_py_bool(xml_bool):
    if xml_bool == 'true':
        return True
    elif xml_bool == 'false':
        return False
    else:
        assert False

def parse_step_node(step_node):
    assert step_node.tag == 'step'
    symbol = grammar.symbols.find_symbol(step_node.attrib['symbol'])
    assert symbol is not None
    reverse = to_py_bool(step_node.attrib['reverse'])
    src = step_node.attrib['from']
    tgt = step_node.attrib['to']
    index = step_node.get('index')
    assert not (symbol.parametric and index is None)
    sub_steps = list(step_node)
    assert len(sub_steps) <= 2
    return (symbol, reverse, src, tgt, index, sub_steps)

def check_step_node(parent_node):
    (e_symbol, _, e_src, e_tgt, e_idx, sub_steps) = parse_step_node(parent_node)

    if sub_steps == []:
        # Accept partially expanded steps (and terminal steps)
        return
    (l_symbol, l_rev, l_src, l_tgt, l_idx, _) = parse_step_node(sub_steps[0])
    if len(sub_steps) == 2:
        (r_symbol, r_rev, r_src, r_tgt, r_idx, _) = parse_step_node(sub_steps[1])
    else:
        (r_symbol, r_rev, r_src, r_tgt, r_idx) = (None, None, None, None, None)

    for p in grammar.prods.get(e_symbol):
        if p.left is None:
            continue # empty derivations would have been accepted already
        if p.left.symbol != l_symbol or p.left.reversed != l_rev:
            continue
        if p.left.indexed and (l_idx is None or
                               e_idx is not None and e_idx != l_idx):
            continue
        if p.right is None:
            if r_symbol is not None:
                continue
            if (not l_rev and e_src == l_src and e_tgt == l_tgt or
                l_rev and e_src == l_tgt and e_tgt == l_src):
                return
        else:
            if r_symbol is None:
                continue
            if p.right.symbol != r_symbol or p.right.reversed != r_rev:
                continue
            if p.right.indexed and (r_idx is None or
                                    e_idx is not None and e_idx != r_idx or
                                    p.left.indexed and l_idx != r_idx):
                continue
            if (not l_rev and not r_rev and e_src == l_src and l_tgt == r_src and r_tgt == e_tgt or
                l_rev     and not r_rev and e_src == l_tgt and l_src == r_src and r_tgt == e_tgt or
                not l_rev and r_rev     and e_src == l_src and l_tgt == r_tgt and r_src == e_tgt or
                l_rev     and r_rev     and e_src == l_tgt and l_src == r_tgt and r_src == e_tgt):
                return
    assert False

def check_node(node):
    if node.tag == 'paths':
        for edge_node in node:
            assert edge_node.tag == 'edge'
            check_node(edge_node)
    elif node.tag == 'edge':
        for path_node in node:
            assert path_node.tag == 'path'
            check_node(path_node)
    elif node.tag == 'path':
        children = list(node)
        assert len(children) == 1
        check_step_node(children[0])
    else:
        assert False

def check_file(fname):
    tree = ET.parse(fname)
    check_node(tree.getroot())
    print "Done with %s" % fname

if os.path.isdir(sys.argv[2]):
    for fname in os.listdir(sys.argv[2]):
        check_file(os.path.join(sys.argv[2], fname))
else:
    check_file(sys.argv[2])
