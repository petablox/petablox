import os
import string
import sys

def parse_declaration(s):
    fields = []
    name = s.split('(')

    rest = name[1][:-1]
    parts = rest.split(',')
    for p in parts:
        decl = p.split(':')
        assert(len(decl) == 2)
        fields.append((decl[0], decl[1]))

    toReturn = [name[0], '(']
    for f in fields:
        toReturn.append(f[0])
        toReturn.append(':')
        toReturn.append(f[1].strip(string.digits))
        toReturn.append(',')
    toReturn = toReturn[:-1]
    toReturn.append(')')

    toReturn.append(' //')
    for f in fields:
        toReturn.append(f[1])
        toReturn.append(',')
    toReturn = toReturn[:-1]

    return name[0], ''.join(toReturn)

def read_file(f):
    output = []
    with open(f) as file:
        lines = file.readlines()
        current_line = []
        for line in lines:
            # bddbddb comments can be safely ignored
            if line.startswith("# name"):
                output.append("// name=" + line[7:])
            elif line[0] == '#' or line.startswith('.bdd'):
                continue
            elif not line.strip():
                output.append('\n')
                continue
            elif line.strip()[-1] == '.':
                current_line.append(line)
                output.append(''.join(current_line))
                current_line = []
            elif line.strip()[-1] == '\\':
                current_line.append(line)    
            elif line.strip().endswith('input'):
                name, converted = parse_declaration(line[:-6].strip())
                output.append('.decl ' + converted + '\n')
                output.append('.input ' + name + '()' + '\n')
            elif line.strip().endswith('output'):
                name, converted = parse_declaration(line[:-7].strip())
                output.append('.decl ' + converted + '\n')
                output.append('.output ' + name + '()\n')
            elif line.strip().endswith(')'):
                _, converted = parse_declaration(line.strip())
                output.append('.decl ' + converted + '\n')
            elif line.startswith('.include'):
                # difficulty in this case because we need to know whether the type of the domain
                output.append('.number_type ' + line[10] + '\n')
            else:
                output.append(line)

    return ''.join(output)

def main():
    dirname = sys.argv[1]
    for root, dirs, files in os.walk(dirname):
        for f in files:
            if f.endswith('.dlog'):
                with open(root + '/' + os.path.splitext(f)[0] + '.dl', 'w') as rewritten:
                    rewritten.write(read_file(root + '/' + f))


if __name__ == "__main__":
    main()
