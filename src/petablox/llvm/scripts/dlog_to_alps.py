#!/usr/bin/env python
import sys

"""We need to track:

    1. List of instructions
    2. All of the rules

"""

tracker = {}

def get_relations(raw_list):
    stripped_list = [x.strip() for x in raw_list]
    relations = filter(lambda x: not x.startswith("%") and x != "", stripped_list)
    return relations

def print_domain(relations, domain, start):
    global tracker
    instructions = filter(lambda x: x.startswith(start), relations)

    if not instructions:
        return

    start_type = start[:-1]
    if not start_type in tracker:
        tracker[start_type] = []

    instruction_ids = [x.split("(")[1].split(")")[0] for x in instructions]
    if domain == "T":
        instruction_ids += ["i1", "i8", "i16", "i32"]
    print "{}: {}.".format(domain, ",".join(instruction_ids))
    for x in instruction_ids:
        tracker[start_type].append(x)

def print_relations(relations):

    def process_domains(k, values):
        for val in values:
            items = val.split(", ")
            ret = []
            try:
                for item in items:
                    if item in tracker["instruction"]:
                        ret.append("I")
                    elif item in tracker["operand"]:
                        ret.append("O")
                    #elif item in tracker["variable"]:
                    #    ret.append("V")
                    elif item in tracker["constant"]:
                        ret.append("C")
                    elif item in tracker["type"]:
                        ret.append("T")
                    elif item in tracker["function"]:
                        ret.append("F")
                    elif item in tracker["linkage"]:
                        ret.append("L")
                    else:
                        ret.append("Error")
            except KeyError:
                continue
        return ret

    def print_r(k,v):
        inputs = len(v[0].split(","))
        types = process_domains(k, v)
        print "*{}({})\n{}\n.".format(k, ",".join(types), "\n".join(v))

    relations_map = {}
    for r in relations:
        relation = r.split("(")
        name = relation[0]
        value = relation[1].split(")")[0]
        if name in relations_map:
            relations_map[name].append(value)
        else:
            relations_map[name] = [value]

    for k,v in relations_map.iteritems():
        print_r(k,v)

def main():
    if len(sys.argv) != 2:
        print "USAGE: ./dlog_to_alps.py input-file.dlog"
        sys.exit(1)

    with open(sys.argv[1]) as f:
        input_lines = f.readlines()
        relations = get_relations(input_lines)

        print_domain(relations, "F", "function(")
        print_domain(relations, "I", "instruction(")
        print_domain(relations, "C", "constant(")
        print_domain(relations, "O", "operand(")
        print_domain(relations, "V", "variable(")
        print_domain(relations, "T", "type(")
        print_domain(relations, "L", "linkage(")
        print_domain(relations, "X", "other(")
        print
        print_relations(relations)

if __name__ == "__main__":
    main()
