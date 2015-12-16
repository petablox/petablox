#!/usr/bin/env python
from __future__ import print_function
import sys
import re
import datetime
from collections import OrderedDict
import argparse


def convert(dlog_path, logic_path=None, verbose=0):
    v = verbose >= 1; vv = verbose >= 2
    if logic_path is None:
        logic_path = re.sub(r'\.dlog$', '.logic', dlog_path)
    if logic_path == dlog_path:
        print('Skipping file: {}'.format(dlog_path), file=sys.stderr)
        return
    print('Converting {} to {}'.format(dlog_path, logic_path))

    with open(dlog_path) as dfile, open(logic_path, 'w') as lfile:
        def output(msg, *args, **kwargs):
            if args or kwargs:
                msg = msg.format(*args, **kwargs)
            if vv:
                print('Output: {}'.format(msg))
            print(msg, file=lfile)

        output('// Created by convert.py from {} on {}'.format(dlog_path, datetime.datetime.now()))
        for line in dfile:
            # skip var order
            if line.startswith('.bddvarorder'):
                continue
            # pass comments through, but catch special name comment
            m = re.search(r'^\s*#( name=(?P<name>.+))?', line)
            if m:
                name = m.group('name')
                if name:
                    output('// :name: {}'.format(name))
                else:
                    output('// {}'.format(line.rstrip('\n')))
                continue

            # convert domain includes
            m = re.search(r'\.include "(?P<dom>\w+)\.dom"', line)
            if m:
                if v: print('Found domains: {}'.format(line.rstrip()))
                output('// :domains: {}'.format(m.group('dom')))
                continue

            # convert input, output and intermediate relation declarations
            m = re.search(r'^(?P<relname>[^(]+)\((?P<relsig>[^)]+)\) ?(?P<type>input|output|)\s*(#|$)', line)
            if m:
                if v: print('Found relation: {}'.format(line.rstrip()))
                rtype = m.group('type')
                if not rtype:
                    output('// convert.py: following intermediate relation converted to output relation')
                    rtype = 'output'
                relname = m.group('relname')
                sig = m.group('relsig')
                varToDom = OrderedDict(parseSigParts(sig))
                output('// :{}s: {}({})'.format(rtype, relname, ','.join(varToDom.itervalues())))

                if rtype == 'output':
                    reldoms = ', '.join([ dom + '(' + var + ')' for var, dom in varToDom.iteritems() ])
                    relvars = ','.join(varToDom.iterkeys())
                    typesig = '{relname}({relvars}) -> {reldoms}.'.format(relname=relname, relvars=relvars, reldoms=reldoms)
                    output(typesig)
                continue

            # everything else assumed to be a rule definition or blank
            outline = line.replace(':-', '<-').rstrip()
            if outline.endswith('. split'):
                outline = outline[:-len('split')]
            output(outline)



def parseSigParts(relsig):
    parts = [s.strip().split(':') for s in relsig.split(',')]
    parts = [[p[0], re.sub(r'[0-9]+$', '', p[1])] for p in parts]

    # can't assume variable names are unique in input file, make sure they are in the output
    suffixes = {}
    for i, part in enumerate(parts):
        var, dom = part
        if var not in suffixes:
            suffixes[var] = 1
        else:
            suffix = suffixes[var]
            parts[i][0] = var + str(suffix)
            suffixes[var] = suffix + 1

    return parts

def _parser():
    p = argparse.ArgumentParser(description='Convert BDD-style datalog files to LogicBlox format.')
    p.add_argument('files', nargs='+')
    p.add_argument('--verbose', '-v', action='count')
    return p

def main(args=None):
    if args is None:
        args = sys.argv[1:]
    args = _parser().parse_args(args)
    for arg in args.files:
        convert(arg, verbose=args.verbose)

if __name__ == '__main__':
    main()
