#!/usr/bin/python

## Usage:
# dexdump app.apk > dexdump.log
# python dexdumpy.py dexdump.log

import sys
import re

class_re = re.compile(r"^  Class descriptor  : 'L(?P<classname>.*);'$")
inst_re = re.compile(r"^.*insns size    : (?P<num>\d+) 16-bit code units$")

codecount = {}
current_class = "dummy"

def add_count(class_name, count):
    namespace_components = class_name.split('/')
    simple_class_name = namespace_components[-1] + "(class)"
    namespace_components = namespace_components[:-1]
    current_node = codecount
    for ns in namespace_components:
        if ns not in current_node: current_node[ns] = {}
        current_node = current_node[ns]
    if simple_class_name not in current_node:
        current_node[simple_class_name] = count
    else:
        current_node[simple_class_name] += count

def sum_counts(current_node):
    count = 0
    for name, n in current_node.items(): 
        if "(class)" in name: count += n
        else: count += sum_counts(n)
    return count

def print_counts_rec(current_node, ns_level, ns_prefix):
    if(ns_level == 0):
        # Print counts for this node
        print ns_prefix + "*\t" + str(sum_counts(current_node))
        return
    for ns_name, ns in current_node.items():
        # Print counts for this node if it contains any classes
        if "(class)" in ns_name:
            print ns_prefix + "*\t" + str(sum_counts(current_node))
            return
    for ns_name, ns in current_node.items():  
        new_ns_prefix = ns_prefix + ns_name + "."
        print_counts_rec(ns, ns_level - 1, new_ns_prefix)

def print_counts(namespace_level = 3):
    print_counts_rec(codecount, namespace_level, "")

def count_total_app_rec(current_node, ns_prefix):
    if "android.support.v4" in ns_prefix:
        return 0
    count = 0
    for name, n in current_node.items(): 
        if "(class)" in name: count += n
        else: 
            new_ns_prefix = ns_prefix + name + "."
            count += count_total_app_rec(n, new_ns_prefix)
    return count

def count_total_app():
    return count_total_app_rec(codecount, "")

with open(sys.argv[1]) as f:
    for l in f:
        classMatch = class_re.match(l)
        instCntMatch = inst_re.match(l)
        if classMatch:
            current_class = classMatch.group("classname")
        elif instCntMatch:
            count = 2*int(instCntMatch.group("num"))
            add_count(current_class, count)            
            
print_counts()
print "Total (w/o android.support.v4):", count_total_app(), "bytes of dex bytecode"
