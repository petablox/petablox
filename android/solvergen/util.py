class FinalAttrs(object):
    """
    A class that enforces a single assignment to any instance attribute.

    Any object of this class (or any subclass of it) is prohibited from
    assigning to the same attribute twice. If this happens, an exception is
    raised.

    "Private" attributes, i.e. those whose name begins with an underscore, are
    excluded from this constraint (these should only be manipulated within the
    class code anyway). Any attributes found in `self._mutables` (which should
    contain a list of attribute names) are also excluded.

    To make use of this class, have your class inherit from it instead of
    @e object, and make sure you initialize all public attributes in the
    constructor. You can whitelist a custom set of mutable attributes by
    storing their names (as strings) in `self._mutables`.
    """

    def __init__(self):
        raise NotImplementedError() # abstract class

    def __setattr__(self, name, value):
        if (not hasattr(self, name) or name.startswith('_') or
            hasattr(self, '_mutables') and name in self._mutables):
            super(FinalAttrs, self).__setattr__(name, value)
        else:
            raise Exception('Attribute %s is final' % name)

class MultiDict(FinalAttrs):
    """
    An append-only dictionary, where a single key can be associated with more
    than one value. By default, a key is mapped to nothing.

    This is an abstract base class, that doesn't specify the container to use
    for storing the values associated with each key. Concrete subclasses should
    define that container, by implementing MultiDict#_empty_container() and
    MultiDict#_append_to_container().
    """

    def __init__(self):
        """
        Create an empty dictionary (where every key maps to no value).
        """
        self._dict = {}

    def append(self, key, value):
        """
        Add a value to the container associated with some key.
        """
        if not key in self._dict:
            self._dict[key] = self._empty_container()
        self._append_to_container(self._dict[key], value)

    def get(self, key):
        """
        Get all the values associated with some key.
        """
        return self._dict.get(key, self._empty_container())

    def __iter__(self):
        """
        Iterate over those keys that map to at least one value.
        """
        for k in self._dict:
            yield k

    def __str__(self):
        return '\n'.join(['\n'.join(['%s:' % k] +
                                    ['\t%s' % v for v in self._dict[k]])
                          for k in self._dict])

    def _empty_container(self):
        """
        Create a new empty container of the kind used for storing dictionary
        values (abstract method).
        """
        raise NotImplementedError()

    def _append_to_container(self, container, value):
        """
        Append a value to a container of the kind used for storing dictionary
        values (abstract method).
        """
        raise NotImplementedError()

class OrderedMultiDict(MultiDict):
    """
    A MultiDict variant where the values associated with each key are ordered
    by insertion time.

    The underlying implementation uses a simple list as a container for values.
    """

    def __init__(self):
        MultiDict.__init__(self)

    def _empty_container(self):
        return []

    def _append_to_container(self, list, value):
        list.append(value)

class CodePrinter(FinalAttrs):
    """
    A helper class for pretty-printing C code.
    """
    # TODO: Should rename functions to 'emit'.

    def __init__(self, out):
        """
        @param [in] out A file-like object to print to.
        """
        self._out = out
        self._level = 0

    def write(self, line, newline=True):
        if line.startswith('}'):
            self._level -= 1
        is_case_start = line.startswith('case') or line.startswith('default')
        real_level = self._level - (1 if is_case_start else 0)
        self._out.write('  ' * real_level + line + ('\n' if newline else ''))
        if line.endswith('{'):
            self._level += 1

def to_c_bool(py_bool):
    return 'true' if py_bool else 'false'

def all_same(elems):
    if elems == []:
        return True
    for e in elems[1:]:
        if e != elems[0]:
            return False
    return True
