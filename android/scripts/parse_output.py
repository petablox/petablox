#!/usr/bin/env python

import re
import fileinput
from sets import Set
import sys

# app directory
dir = sys.argv[1]
dir = dir + '/chord_output/'

# parse inputs
def parse(filename, num_args):
    # construct the parse string
    parse_string = '<'
    for i in range(num_args):
        parse_string = parse_string + '(?P<g' + repr(i) + '>.*),'
    parse_string = parse_string[:-1] + '>'

    # parse the file
    result = []
    for line in fileinput.input(filename):
        # parse the line
        entry = []
        for i in range(num_args):
            entry.append(re.search(parse_string, line).group('g'+repr(i)))
        # add the line to the result
        result.append(entry)
    return result

def print_list(list, filename):
    f = open(filename, 'w')
    for x in list:
        f.write(repr(x)+'\n')
    f.close()

tainted_stubs = parse(dir + 'taintedStub.txt', 2) # tainted stubs
print_list(tainted_stubs, 'tainted_stubs.txt')
