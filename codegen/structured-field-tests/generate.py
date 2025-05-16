#!/usr/bin/env python3

import json

ALL_CHARS = range(0x00, 0x7F + 1)
WHITESPACE = [0x20]
DIGITS = list(range(0x30, 0x39 + 1))
LCALPHA = list(range(0x61, 0x7A + 1))
UCALPHA = list(range(0x41, 0x5A + 1))
ALPHA = LCALPHA + UCALPHA

allowed_string_chars = (
    [0x20, 0x21] + list(range(0x23, 0x5B + 1)) + list(range(0x5D, 0x7E + 1))
)
escaped_string_chars = [0x22, 0x5C]
allowed_token_chars = (
    DIGITS
    + ALPHA
    + [
        ord(c)
        for c in [
            ":",
            "/",
            "!",
            "#",
            "$",
            "%",
            "&",
            "'",
            "*",
            "+",
            "-",
            ".",
            "^",
            "_",
            "`",
            "|",
            "~",
        ]
    ]
)
allowed_token_start_chars = ALPHA + [ord("*")]
allowed_key_chars = DIGITS + LCALPHA + [ord(c) for c in ["_", "-", ".", "*"]]
allowed_key_start_chars = LCALPHA + [ord("*")]


def write(name, data):
    fh = open("%s-generated.json" % name, "w")
    json.dump(data, fh, indent=4)
    fh.close()


### strings
tests = []

## allowed characters
for c in ALL_CHARS:
    test = {
        "name": "0x%02x in string" % c,
        "raw": ['" %s "' % chr(c)],
        "header_type": "item",
    }
    if c in allowed_string_chars:
        test["expected"] = [" %s " % chr(c), []]
    else:
        test["must_fail"] = True
    tests.append(test)

## escaped characters
for c in ALL_CHARS:
    test = {
        "name": "Escaped 0x%02x in string" % c,
        "raw": ['"\\%s"' % chr(c)],
        "header_type": "item",
    }
    if c in escaped_string_chars:
        test["expected"] = [chr(c), []]
    else:
        test["must_fail"] = True
    tests.append(test)
write("string", tests)

### string serialisation failures
tests = []

## unallowed characters
for c in ALL_CHARS:
    if c in allowed_string_chars:
        continue
    if c in escaped_string_chars:
        continue
    test = {
        "name": "0x%02x in string - serialise only" % c,
        "expected": ["%s" % chr(c), []],
        "header_type": "item",
        "must_fail": True,
    }
    tests.append(test)
write("serialisation-tests/string", tests)

### tokens
tests = []

## allowed characters
for c in ALL_CHARS:
    test = {
        "name": "0x%02x in token" % c,
        "raw": ["a%sa" % chr(c)],
        "header_type": "item",
    }
    if c in allowed_token_chars:
        test["expected"] = [{"__type": "token", "value": "a%sa" % chr(c)}, []]
    elif c == 0x3B:
        test["expected"] = [{"__type": "token", "value": "a"}, [["a", True]]]
    else:
        test["must_fail"] = True
    tests.append(test)

## allowed starting characters
for c in ALL_CHARS:
    test = {
        "name": "0x%02x starting a token" % c,
        "raw": ["%sa" % chr(c)],
        "header_type": "item",
    }
    if c in WHITESPACE:
        test["expected"] = [
            {"__type": "token", "value": "a"},
            [],
        ]  # whitespace is always stripped.
        test["canonical"] = ["a"]
    elif c in allowed_token_start_chars:
        test["expected"] = [{"__type": "token", "value": "%sa" % chr(c)}, []]
    else:
        test["must_fail"] = True
    tests.append(test)
write("token", tests)

### token serialisation failures
tests = []

## unallowed characters
for c in ALL_CHARS:
    if c in allowed_token_chars:
        continue
    test = {
        "name": "0x%02x in token - serialise only" % c,
        "header_type": "item",
        "expected": [{"__type": "token", "value": "a%sa" % chr(c)}, []],
        "must_fail": True,
    }
    tests.append(test)

## unallowed starting characters
for c in ALL_CHARS:
    if c in allowed_token_start_chars:
      continue
    test = {
        "name": "0x%02x starting a token - serialise only" % c,
        "header_type": "item",
        "expected": [{"__type": "token", "value": "%sa" % chr(c)}, []],
        "must_fail": True,
    }
    tests.append(test)
write("serialisation-tests/token", tests)

### keys
tests = []

## single-character dictionary keys
for c in ALL_CHARS:
    test = {
        "name": "0x%02x as a single-character dictionary key" % c,
        "raw": ["%s=1" % chr(c)],
        "header_type": "dictionary",
    }
    if c in WHITESPACE:
        test["raw"] = ["=1"] # whitespace is always stripped.
        test["must_fail"] = True
    elif c in allowed_key_start_chars:
        test["expected"] = [["%s" % chr(c), [1, []]]]
    else:
        test["must_fail"] = True
    tests.append(test)

## dictionary keys
for c in ALL_CHARS:
    test = {
        "name": "0x%02x in dictionary key" % c,
        "raw": ["a%sa=1" % chr(c)],
        "header_type": "dictionary",
    }
    if c == 0x2C:
        test["expected"] = [["a", [1, []]]]
        test["canonical"] = ["a=1"]
    elif c == 0x3B:
        test["expected"] = [["a", [True, [["a", 1]]]]]
    elif c in allowed_key_chars:
        key = "a%sa" % chr(c)
        test["expected"] = [[key, [1, []]]]
    else:
        test["must_fail"] = True
    tests.append(test)

## allowed dictionary key starting characters
for c in ALL_CHARS:
    test = {
        "name": "0x%02x starting a dictionary key" % c,
        "raw": ["%sa=1" % chr(c)],
        "header_type": "dictionary",
    }
    if c in WHITESPACE:
        test["expected"] = [["a", [1, []]]]  # whitespace is always stripped.
        test["canonical"] = ["a=1"]
    elif c in allowed_key_start_chars:
        test["expected"] = [["%sa" % chr(c), [1, []]]]
    else:
        test["must_fail"] = True
    tests.append(test)

## parameterised list keys
for c in ALL_CHARS:
    test = {
        "name": "0x%02x in parameterised list key" % c,
        "raw": ["foo; a%sa=1" % chr(c)],
        "header_type": "list",
    }
    if c == 0x3B:
        test["expected"] = [[{"__type": "token", "value": "foo"}, [["a", 1]]]]
        test["canonical"] = ["foo;a=1"]
    elif c in allowed_key_chars:
        key = "a%sa" % chr(c)
        test["expected"] = [[{"__type": "token", "value": "foo"}, [[key, 1]]]]
        test["canonical"] = ["foo;a%sa=1" % chr(c)]
    else:
        test["must_fail"] = True
    tests.append(test)

## allowed parameterised list key starting characters
for c in ALL_CHARS:
    test = {
        "name": "0x%02x starting a parameterised list key" % c,
        "raw": ["foo; %sa=1" % chr(c)],
        "header_type": "list",
    }
    if c in WHITESPACE:
        test["expected"] = [
            [{"__type": "token", "value": "foo"}, [["a", 1]]]
        ]  # whitespace is always stripped.
        test["canonical"] = ["foo;a=1"]
    elif c in allowed_key_start_chars:
        test["expected"] = [[{"__type": "token", "value": "foo"}, [["%sa" % chr(c), 1]]]]
        test["canonical"] = ["foo;%sa=1" % chr(c)]
    else:
        test["must_fail"] = True
    tests.append(test)
write("key", tests)

### key serialisation failures
tests = []

## bad dictionary keys
for c in ALL_CHARS:
    if c in allowed_key_chars:
        continue
    test = {
        "name": "0x%02x in dictionary key - serialise only" % c,
        "expected": [["a%sa" % chr(c), [1, []]]],
        "header_type": "dictionary",
        "must_fail": True,
    }
    tests.append(test)

## bad dictionary key starting characters
for c in ALL_CHARS:
    if c in allowed_key_start_chars:
        continue
    test = {
        "name": "0x%02x starting a dictionary key - serialise only" % c,
        "header_type": "dictionary",
        "expected": [["%sa" % chr(c), [1, []]]],
        "must_fail": True,
    }
    tests.append(test)

## bad parameterised list keys
for c in ALL_CHARS:
    if c in allowed_key_chars:
        continue
    test = {
        "name": "0x%02x in parameterised list key - serialise only" % c,
        "header_type": "list",
        "expected": [[{"__type": "token", "value": "foo"}, [["a%sa" % chr(c), 1]]]],
        "must_fail": True,
    }
    tests.append(test)

# bad parameterised list key starting characters
for c in ALL_CHARS:
    if c in allowed_key_start_chars:
        continue
    test = {
        "name": "0x%02x starting a parameterised list key" % c,
        "header_type": "list",
        "expected": [[{"__type": "token", "value": "foo"}, [["%sa" % chr(c), 1]]]],
        "must_fail": True,
    }
    tests.append(test)
write("serialisation-tests/key", tests)

### large types
tests = []

## large dictionaries
dict_members = 1024
tests.append(
    {
        "name": "large dictionary",
        "raw": [", ".join(["a%s=1" % i for i in range(dict_members)])],
        "header_type": "dictionary",
        "expected": [["a%s" % i, [1, []]] for i in range(dict_members)],
    }
)

## large dictionary key
key_length = 64
tests.append(
    {
        "name": "large dictionary key",
        "raw": ["%s=1" % ("a" * key_length)],
        "header_type": "dictionary",
        "expected": [[("a" * key_length), [1, []]]],
    }
)

## large lists
list_members = 1024
tests.append(
    {
        "name": "large list",
        "raw": [", ".join(["a%s" % i for i in range(list_members)])],
        "header_type": "list",
        "expected": [
            [{"__type": "token", "value": "a%s" % i}, []] for i in range(list_members)
        ],
    }
)

## large parameterised lists
param_list_members = 1024
tests.append(
    {
        "name": "large parameterised list",
        "raw": [", ".join(["foo;a%s=1" % i for i in range(param_list_members)])],
        "header_type": "list",
        "expected": [
            [{"__type": "token", "value": "foo"}, [["a%s" % i, 1]]]
            for i in range(param_list_members)
        ],
    }
)

## large number of params
param_members = 256
tests.append(
    {
        "name": "large params",
        "raw": ["foo;%s" % ";".join(["a%s=1" % i for i in range(param_members)])],
        "header_type": "list",
        "expected": [
            [
                {"__type": "token", "value": "foo"},
                [["a%s" % i, 1] for i in range(param_members)],
            ]
        ],
    }
)

## large param key
tests.append(
    {
        "name": "large param key",
        "raw": ["foo;%s=1" % ("a" * key_length)],
        "header_type": "list",
        "expected": [[{"__type": "token", "value": "foo"}, [[("a" * key_length), 1]]]],
    }
)

## large strings
string_length = 1024
tests.append(
    {
        "name": "large string",
        "raw": ['"%s"' % ("=" * string_length)],
        "header_type": "item",
        "expected": ["=" * string_length, []],
    }
)
tests.append(
    {
        "name": "large escaped string",
        "raw": ['"%s"' % ('\\"' * string_length)],
        "header_type": "item",
        "expected": ['"' * string_length, []],
    }
)

## large tokens
token_length = 512
tests.append(
    {
        "name": "large token",
        "raw": ["%s" % ("a" * token_length)],
        "header_type": "item",
        "expected": [{"__type": "token", "value": "a" * token_length}, []],
    }
)

write("large", tests)


## Number types
tests = []

## integer sizes
number_length = 15
for i in range(1, number_length + 1):
    tests.append(
        {
            "name": f"{i} digits of zero",
            "raw": ["0" * i],
            "header_type": "item",
            "expected": [0, []],
            "canonical": ["0"],
        }
    )
    tests.append(
        {
            "name": f"{i} digit small integer",
            "raw": ["1" * i],
            "header_type": "item",
            "expected": [int("1" * i), []],
        }
    )
    tests.append(
        {
            "name": f"{i} digit large integer",
            "raw": ["9" * i],
            "header_type": "item",
            "expected": [int("9" * i), []],
        }
    )

## decimal sizes
integer_length = 12
fractional_length = 3
for i in range(1, integer_length + 1):
    for j in range(1, fractional_length + 1):
        k = i + j
        tests.append(
            {
                "name": f"{k} digit 0, {j} fractional small decimal",
                "raw": ["0" * i + "." + "1" * j],
                "header_type": "item",
                "expected": [float("0" * i + "." + "1" * j), []],
                "canonical": ["0." + "1" * j],
            }
        )
        tests.append(
            {
                "name": f"{k} digit, {j} fractional 0 decimal",
                "raw": ["1" * i + "." + "0" * j],
                "header_type": "item",
                "expected": [float("1" * i + "." + "0" * j), []],
                "canonical": ["1" * i + ".0"],
            }
        )
        tests.append(
            {
                "name": f"{k} digit, {j} fractional small decimal",
                "raw": ["1" * i + "." + "1" * j],
                "header_type": "item",
                "expected": [float("1" * i + "." + "1" * j), []],
            }
        )
        tests.append(
            {
                "name": f"{k} digit, {j} fractional large decimal",
                "raw": ["9" * i + "." + "9" * j],
                "header_type": "item",
                "expected": [float("9" * i + "." + "9" * j), []],
            }
        )

tests.append(
    {
        "name": f"too many digit 0 decimal",
        "raw": ["0" * (number_length) + "." + "0"],
        "header_type": "item",
        "must_fail": True,
    }
)
tests.append(
    {
        "name": f"too many fractional digits 0 decimal",
        "raw": [
            "0" * (number_length - fractional_length)
            + "."
            + "0" * (fractional_length + 1)
        ],
        "header_type": "item",
        "must_fail": True,
    }
)
tests.append(
    {
        "name": f"too many digit 9 decimal",
        "raw": ["9" * (number_length) + "." + "9"],
        "header_type": "item",
        "must_fail": True,
    }
)
tests.append(
    {
        "name": f"too many fractional digits 9 decimal",
        "raw": [
            "9" * (number_length - fractional_length)
            + "."
            + "9" * (fractional_length + 1)
        ],
        "header_type": "item",
        "must_fail": True,
    }
)


write("number", tests)
