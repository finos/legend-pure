# Writing PCT function documentation

This standard governs the `'''…'''` documentation on `<<PCT.function>>` declarations and
`<<PCT.test>>` cases under `legend-pure-m3-core/src/main/resources/platform/pure/**`.

These strings are **published to end users**. Write for someone using Pure to get work done, not for
someone maintaining the compiler.

For the language mechanics — where documentation attaches, how content is processed, the conflict with
an explicit `doc.doc` — see
[Pure Language Reference § Documentation](../reference/pure-language-reference.md#documentation). This
document covers only *what to write*.

---

## Why it is worth writing

Documentation is sugar over the `meta::pure::profiles::doc` `doc` tagged value, and the pipeline that
publishes it already exists end to end:

```
.pure  '''markdown'''
  → doc.doc tagged value
  → PCTTools.getDoc()
  → Signature.documentation             (per signature)
  → FunctionDefinition.tests[].documentation   (per test)
  → FUNCTIONS_<module>.json
  → DocumentationGeneration.buildDocumentation()
  → consumed downstream for published documentation
```

---

## The template

````pure
'''
One-sentence summary, ending in a period.

Optional paragraph on semantics, edge cases, and empty/`[0..1]` behaviour.

**Parameters**
- `source` — what it is.
- `toReplace` — what it is.

**Returns** what comes back.

**Examples**
```pure
'abcde'->replace('ab', 'fg')      // 'fgcde'
'hello'->replace('z', 'y')        // 'hello'
```

**See also** `split(String[1], String[1]):String[*]`,
`substring(String[1], Integer[1]):String[1]`
'''
native function
    <<PCT.function>>
    meta::pure::functions::string::replace(source:String[1], toReplace:String[1], replacement:String[1]):String[1];
````

The live reference implementation is
[`platform/pure/essential/string/transformation/replace.pure`](../../legend-pure-core/legend-pure-m3-core/src/main/resources/platform/pure/essential/string/transformation/replace.pure).

Put the delimiters on their own lines, even for a one-liner — `'''One line.'''` does not canonicalize
the way you would expect:

```pure
'''
Replacing a leading substring rewrites only that occurrence.
'''
function <<PCT.test>> meta::pure::functions::string::tests::replace::testReplace<Z|y>(…):Boolean[1]
```

Every function gets **at least one runnable example**. Prefer examples showing a normal case and at
least one boundary — no match, empty collection, zero.

---

## Formatting rules

1. **Keep the opening delimiter, the body and the closing delimiter at one indentation.** Content
   indentation is measured relative to the closing delimiter, so a `'''` pulled left of the body leaves
   that difference on every line — and 4+ spaces renders as a Markdown code block.

2. **Use `-` for bullets.** Consistent with the corpus, and avoids any confusion with emphasis.

3. **Never write `'''` inside the content** — it closes the literal. This is the only sequence that
   needs avoiding; ordinary single quotes in Pure examples are fine.

4. **Content is literal** — never unescaped, so `\n` is a backslash and an `n`. Write real line breaks.

5. **Tag fenced blocks `pure`** so the published renderer can highlight them.

6. **One documentation literal per declaration.** Two in a row is a parse error, and the message points
   at the literal rather than at the duplication — easy to misread when it happens.

---

## Overloads

`Signature.documentation` is per-signature, and the REPL shows the **first** signature that has
documentation.

> Put the **full** documentation on the first, most general signature — usually the one carrying the
> stereotype block. Give each other overload a short comment stating only how it differs.

Make the delta self-contained: name the signature being contrasted rather than writing "as above", since
a renderer may show each signature on its own.

```pure
'''
As `indexOf(String[1], String[1]):Integer[1]`, but begins searching at `fromIndex` rather than at the
start of the string. Still returns `-1` when there is no occurrence at or after that position.
'''
```

Note `greaterThanEqual` tags `(Number[0..1], Number[0..1])` where the other comparison operators tag
`[1],[1]` — check which signature actually carries the stereotype block before assuming.

---

## Cross-references (`See also`)

Reference other functions by **full signature**, never bare name, so a renderer can resolve exactly one
target:

```
**See also** `split(String[1], String[1]):String[*]`
```

Two reasons from the current corpus: `substring` has two overloads, and `contains` exists in both
`meta::pure::functions::string` and `meta::pure::functions::collection`.

**Format** — match the `simple` field of `Signature` in `FUNCTIONS_*.json`: parameter types comma-space
separated, return type after the colon. Strip the package when the target is in the same package as the
function you are documenting; keep it otherwise.

**Do not hand-write signatures.** Copy them from the generated JSON:

```bash
python3 -c "
import json; d=json.load(open('legend-pure-core/legend-pure-m3-core/target/classes/pct-reports/FUNCTIONS_essential.json'))
print('\n'.join(s['simple'] for f in d['functionDefinitions'] for s in f['signatures']))"
```

Hand-written references are wrong surprisingly often — `split` returns `String[*]` not `String[1]`, and
`instanceOf` lives in `meta`, not `lang`.

---

## Before you document a file: check for position assertions

Adding documentation **shifts every line below it**, and some tests assert the line and column of
elements in `platform/pure`. The failure names a line number, not your documentation, so it reads as
unrelated.

The most productive grep is for `assertError` calls with numeric position arguments — those are
**self-referential**, asserting the line they are written on:

```bash
grep -rn --include='*.pure' "assertError(.*',[[:space:]]*[0-9]" platform/pure
```

Java-side assertions come in two further shapes that a single pattern will not find:

```bash
grep -rn --include='*.java' "<file> line:" .                                    # error-message strings
grep -rn --include='*.java' 'getSourceInformation()\.\(getLine\|getColumn\)' .  # integer assertions
```

Files carrying position assertions, as of writing:

| File(s) | Asserted from |
|---|---|
| `essential/tests/assert.pure`, `fail.pure` | `TestPCTTools`, `TestFunctionExecutionStart` |
| `essential/io/print.pure` | `TestSourceNavigation` (`getLine()`) |
| `essential/meta/source/sourceInformation.pure` | itself — positions of elements **below** the declaration |
| `essential/collection/index/at.pure` | itself, via `assertError` |
| `essential/date/creation/date.pure`, `date/extract/{dayOfMonth,hour,minute,second}.pure` | themselves |
| `essential/tests/{assert,assertEquals,assertError,assertFalse,assertNotEquals,assertSameElements}.pure` | themselves |

Update the numbers in the same commit. Do not compute the offset by hand — make the edit, then re-read
the file for the actual new positions. For a self-referential `assertError`, the expected value is simply
the assertion's own new line number.

---

## Verifying: m3-core alone is not enough

Documentation lives in `m3-core`, but **the tests that exercise it do not**. Both of these run only in
the runtime engine modules:

- `Test_*_PCT` — the `<<PCT.test>>` functions
- `TestCoreFunctions` — the platform `<<test.Test>>` functions, and **it does not match `*_PCT`**, so a
  filtered run silently skips it

A green m3-core build therefore proves nothing about the position assertions above. Run both, in this
order, and **never concurrently** — a second Maven `clean` deletes `target/` under the first and produces
a flood of misleading "code repository can't be found" errors:

```bash
mvn clean install -pl legend-pure-core/legend-pure-m3-core

mvn test -pl legend-pure-runtime/legend-pure-runtime-java-engine-interpreted
```

Run the whole interpreted module rather than filtering — filtering is what hid `TestCoreFunctions`.

**Never verify with `-DskipTests`.** It proves the documentation parses and serializes; it cannot detect
a broken position assertion.

Then check the emitted JSON, which is the thing actually published:

```bash
python3 -c "
import json; d=json.load(open('legend-pure-core/legend-pure-m3-core/target/classes/pct-reports/FUNCTIONS_essential.json'))
print(json.dumps([f for f in d['functionDefinitions'] if f.get('name')=='replace'], indent=2))"
```

Confirm `signatures[].documentation` holds the intended Markdown with newlines preserved, and that tests
appear under `tests` with their documentation attached.

Documentation is parse-time sugar, so **no PCT result should change**. Treat any diff as a real
regression.
