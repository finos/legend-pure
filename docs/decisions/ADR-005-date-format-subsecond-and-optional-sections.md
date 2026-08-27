# ADR-005: Sub-Second Fields and Optional Sections in the Date Format Language

**Status:** Accepted. Implemented in `legend-pure-m4` and `legend-pure-m3-core`.
**Date:** 2026-08-27
**Deciders:** Legend Pure core team

## Context

`meta::pure::functions::string::format` renders a date from a format string: `%t{yyyy-MM-dd}`,
optionally opening with a time zone. One method does the work, `DateFormat.format` in
`legend-pure-m4`, and everything routes through it. `PureDate.format(String)` delegates to it, and
the Pure native reaches it from both engines by carving the `%t{...}` payload out of the larger
format string. A change there lands on both engines at once.

Two gaps in that language prompted this work. Both are visible from inside Pure, without reference
to anything downstream.

### Gap 1: the language cannot describe Pure's own type

A Pure date comes at seven granularities, from a bare year to a sub-second of arbitrary precision. A
format string could address exactly one of them, because every control character was mandatory:
`yyyy-MM-dd` was a run-time error for a `Year`, and `yyyy` silently discarded everything a
`DateWithSubsecond` knew. There was no way to write a format string that renders a collection of
dates whose granularities differ, which is the ordinary case for data drawn from more than one
source.

That is a language under-powered for its own type system, and nothing about the design forced it.
The renderer already knew, per component, whether the date carried it, since that is what it tested
before throwing. The information was there and could not be reached.

The workaround is visible in shipped Pure. Every SQL literal writer in legend-engine branches on
granularity and keeps a format string per branch; the Oracle one has five branches and five format
strings for what is one string under this change.

### Gap 2: the sub-second field's count meant two different things

Repeating a control character is documented as widening the field, and for `S` it did so for one,
two, or three letters. At four letters the count stopped meaning anything and the field wrote the
whole stored fraction, however long.

| Field | `.070004235` | `.07` | What the count did |
|---|---|---|---|
| `SS` | `07` | `07` | capped the width at 2 |
| `SSS` | `070` | `07` | capped the width at 3 |
| `SSSS` | `070004235` | `07` | nothing |
| `SSSSSSSSS` | `070004235` | `07` | nothing |

So `SSS` and `SSSS` differed in kind rather than in degree, and `SSSS` and `SSSSSSSSS` were the same
field written two ways. Alongside that, the field could not write a **fixed-width** fraction at all,
which ISO 8601 and every wire format built on it require.

This was not a theoretical complaint. Of the fourteen sub-second fields written in legend-engine's
`.pure` sources, **ten are spelled with four letters or more, and not one of them gets the fixed
width its author plainly meant**. `formatDate`'s "ISO 8601 nanosecond precision" writes nine digits
only for a date whose fraction already has nine; every SQL literal writer spells its fraction
`SSSSSS` and means microseconds.

## Decisions

### 1. A sub-second field is a width: a minimum, a maximum, a policy at each end, and a fill

The field says how many digits to write and what to do when the date does not fit:

| Parameter | Range | Meaning |
|---|---|---|
| `min` | 0, 1, 2, ... | fewest digits the field will write |
| `max` | `max(min, 1)` to unbounded | most digits the field will write |
| `below` | fill or throw | what happens when the stored fraction is shorter than `min` |
| `above` | truncate or throw | what happens when it is longer than `max` |
| `fill` | one character | what padding writes, default `0` |

The general form states all of it, `S(min,max)` with `*` for an unbounded maximum, `!` after either
bound to throw rather than fill or truncate there, and an optional third part naming a fill other
than zero. Five shorthands cover the cases worth a short spelling: `SN` exactly N, `S<N` at most N,
`S>N` at least N and everything beyond, `S*` however many the date has, and `S!N` exactly N and
refusing a shorter fraction.

Rendering applies the maximum before the minimum, so a field can cut a long fraction down and pad a
short one out without the two ever meeting, and the minimum is tested against what the maximum left,
which is what makes `S(3,3)` a fixed width rather than a contradiction. Fill goes on the right and
truncation takes a prefix, because sub-second digits run most significant first: `.07` padded to
three is `.070`, not `.007`.

### 2. Padding is worth marking. Truncating is not

`S!N` exists and there is no matching short spelling for refusing to truncate. That asymmetry is
principled rather than arbitrary, and it follows from what a Pure date is.

**A Pure date is a span of time rather than an instant**, and the number of digits it stores is the
precision of that span. `.07` *contains* `.070`, `.071`, and `.0712`. So:

- **Truncating widens the span.** It loses precision and stays true. There is little reason to want
  it refused.
- **Padding narrows it.** It claims a precision the date does not have. Refusing is a real choice,
  and it deserves a short spelling.

Refusing to truncate is a validation concern rather than a formatting one, and validation concerns
spell out: `S(N!,N!)` rather than a second `!` prefix, whose reading would be ad hoc and whose use
case is thin.

The same reasoning is why the fallback that invents a fraction is written out rather than
abbreviated. `?[.S9|".000000000"]` is four characters longer than a hypothetical `.S=9` and says
what it does; the nine zeros stay visible, which is the point.

### 3. A sub-second field always fails on a date with no fraction

**Presence is not a field concern.** How many digits to write is the field's business; whether there
are any to write is not, and belongs to the optional section around it, which is where every other
control character already leaves the question.

Two readings were rejected, both of which have the field answer for an absent fraction itself:

- **Write the fill out to the minimum width**, so `S3` gives `000`. This makes the field silently
  claim millisecond precision for a date that has none, and makes the claim invisible at the call
  site. It is the reading legend-engine's forked implementation took, and it is why that fork and
  this one cannot be told apart by reading a format string.
- **Write nothing.** This leaves the decimal point behind, and the point is not the field's to
  remove. Only a section can take the separator with it.

Beyond taste, the decision earns its place structurally. **With the precondition, every sub-second
field can fail**, so every one of them composes with an optional section. Without it, a field
carrying no throwing policy could never fail, so `?[.S3]` would render `.000` forever and
`?[.S*|".000"]` could never reach its fallback. Both look right and quietly are not. The precondition
removes that trap entirely rather than documenting it, and collapses an axis out of the parameter
space: a renderable date has at least one digit, so a minimum of 0 and a minimum of 1 are the same
field.

A field and a section do not overlap, and the pair that looks as though it might is worth knowing:

| Format | `.070004235` | `.070` | `.07` | no fraction |
|---|---|---|---|---|
| `?[SSS\|"000"]` | `070` | `070` | `07` | `000` |
| `S3` | `070` | `070` | `070` | fails |
| `?[S3\|"000"]` | `070` | `070` | `070` | `000` |

Nothing a field can say answers for a date with no fraction, and nothing a section can say pads a
short one. Saying both takes both.

### 4. The repetition form is frozen, and every legacy form gains an exact modern equivalent

`S` through `SSS` are `S<1` through `S<3`. `SSSS` and longer are `S*`. Same semantics, no migration,
and the discontinuity at four letters becomes visible as the point where `S<N` turns into `S*`. The
older spelling can therefore be deprecated later without anything losing a way to be said.

Every character the new forms recruit (`<`, `>`, `*`, `!`, `(`, `)`, `?`, `|`, `]`, and a digit after
`S`) was an error before this change, so **no format string that worked before changes meaning**.
That claim was checked rather than asserted: the implementation this replaced was run head to head
with the new one over 4,377,492 pairs of a format string and a date, covering every control character
at widths one to five, every pair and triple drawn from a pool of runs, separators, and quoted text,
against seven time zones and twelve dates. Every difference is one deliberate change, `a` now
consuming its run of repetitions like every other control character, so `aa` writes `PM` rather than
`PMPM`. A fifty-row table of that comparison is checked in as `TestDateFormat`'s compatibility suite.

`DateFormatPattern.pattern()` writes the modern spelling and never the repetition form, which makes
reading a format string and writing it back the mechanical half of a migration.

**The one hazard, named rather than buried:** `S3` and `SSS` look like the same field and are not.
`S3` is exactly three digits; `SSS` is at most three. On `.07` the first gives `070` and the second
`07`, and the difference is in the unsound direction, since the padded form claims precision the date
lacks. `S+N` was considered as a spelling that could not be confused with `SSS` and rejected, because
`+` and `>` would both read as "at least", making `S+3` and `S>3` near-synonyms with different
meanings: a worse collision, between two new forms rather than between a new form and a deprecated
one.

### 5. Optional sections are `?[...]`, with full alternation

A run of the format string in `?[...]` is written where the date can carry it and left out where it
cannot. Within one, `|` separates alternatives: the first alternative every element of which can
write the date is the one written, and where none can, the section writes nothing.

`?` prefixes the brackets so there is no collision with the leading `[zone]`, and brackets for
optionality have precedent in Java's `DateTimeFormatter`. Braces were rejected because `%t{...}` is
bounded by scanning for the first unquoted `}`.

Alternation is full rather than body-plus-default because the viability check is a loop either way,
and a literal-only alternative is exactly a default.

**Sections nest, and a nested section asks the date for nothing on its own account**, so it adds no
requirement to the alternative holding it. `?[" at "?[HH:mm]]` on a date with no hour writes `" at "`
and stops, because the outer alternative holds a literal and a section and both of those can always
write. Hoisting the literal out, as `?[" at "HH:mm]`, is what makes the two rise and fall together.
The alternative, letting a nested section refuse on its parent's behalf, was rejected because it
makes viability depend on a whole subtree and would make `?[X]` and `?[X|""]` mean different things.

An alternative may not be empty. One at the front would make the whole section write nothing whatever
the date, which is a silent bug; one at the back says what the section without it already says. `""`
spells an empty alternative where one is meant, and is therefore an element that writes nothing
rather than nothing at all.

**What a section costs:** it never fails, which is what it is for, and therefore a throwing
sub-second bound inside one selects the next alternative where outside one it would raise. So
"refuse over-precise data *and* tolerate coarse data" is not sayable. That is the one place this
design closes off something a user might reasonably want, and it is the price of a construct that can
be relied on never to fail. In exchange it makes a new shape of fallback available:
`?[.S(0,3!)|".(truncated)"]` takes its fallback because the date carries too much rather than too
little, which nothing else in the language can ask for.

### 6. Validation rejects only the incoherent, never the merely inert

Format strings are frequently built at run time, so a string's validity must not depend on how it was
authored. A generator that uniformly emits `S(min!,max)` and passes `min = 1` writes a field with a
dead marker, and it renders rather than failing: the field means something perfectly clear, namely
the same thing without the marker. Two validation standards, with the compiler stricter than the
renderer, would make a literal format string fail where the identical computed one succeeds.

So the parse rejects a maximum below one, a minimum above the maximum, a fill that is not exactly one
character, and syntactic malformation, and nothing else.

The fill is the one place the rule bites, and it shows what the rule actually says. A multi-character
fill has **two** defensible meanings: write the fill once per missing digit, or write it repeatedly
until the gap is closed. Nothing in the model prefers either. Admitting the unambiguous does not
oblige choosing arbitrarily among meanings, so a longer fill is rejected while inert markers are not.

### 7. A literal format string is checked when it is compiled

A format string was only ever found to be wrong by running it. `FormatTools.validate` checks a format
string against the specifier grammar the two engines implement, and `FormatValidator` calls it from
`FunctionExpressionValidator` wherever parameter 0 of `format` is a literal.

The check covers the whole string rather than only its date patterns, because a specifier that does
not exist is as much a property of the string alone as a pattern that could never write a date. It
does **not** check the specifiers against the arguments, in number or in kind: `args` is an `Any[*]`
that is frequently computed, so a format string has to be free to be right for arguments the compiler
cannot see, and getting that wrong would turn a nuisance into an outage.

Only a literal can be checked this way, which is why the run-time check stays. Every SQL literal
writer in legend-engine splices its time zone into its format string, so none of them is a literal at
any call site.

## Consequences

**Positive:**

- One format string renders a date at whatever precision it carries, across all seven granularities:
  `yyyy?[-MM?[-dd?["T"HH?[:mm?[:ss?[.S*]]]]]]`. That closes gap 1.
- A fixed-width fraction is sayable, so `?[.S9|".000000000"]` replaces a branch, two `format` calls,
  a concatenation, and a second pass over the date. That closes gap 2.
- The format string is read once into a `DateFormatPattern` that can be held and reused, and a
  pattern can be built directly by a caller that has no format string to hand.
- Resolving the time zone moved to parse time, which makes a zoned pattern **faster** than the loop
  it replaced: that called `ZoneId.of` on every single call.
- A malformed literal format string now fails to compile rather than waiting for its line to run.

**Negative:**

- Parsing per call costs about three times the old loop for an unzoned pattern. A pattern hoisted out
  of a loop is at parity or better, and a zoned one is faster either way.
- `S3` and `SSS` are confusable, as decision 4 records.
- A throwing bound inside a section stops throwing, as decision 5 records.
- `StrictTimeFormat` is now visibly a second implementation of an overlapping language, with the
  sub-second rule this change replaced. It has no Pure surface, so nothing reaches it except
  `PureStrictTime.format(String)` from Java, and it was deliberately left alone.

**Mitigation:**

- The confusable pair is documented in `DateFormat`'s javadoc and in `format`'s doc string, and
  deprecating the repetition form retires it.
- `pattern()` writes the modern spelling of any field it reads, so the migration is mechanical for
  every format string whose meaning is already right.

## Rejected

- **Trimming trailing zeros.** Designed as a `#` suffix and cut. It cancels out against the minimum,
  and which of them wins depends on an ordering nothing in the notation settles: trim before padding
  and `SN#` means exactly `SN`; trim after and the minimum is dead whenever the fill is a zero. A
  feature whose two readings differ on every field carrying both parameters is one to take up on its
  own. It has a real use case, matching PostgreSQL's own timestamp rendering, so it will come back.
- **`S=N`, "exactly N digits, inventing them when the date has none".** A category error: it is a
  shorthand for a field *and* a section, and should not wear an `S` spelling that implies otherwise.
- **Rounding rather than truncating above the maximum.** Recommended against permanently. Rounding
  does not widen the span a date stands for, it *moves* it, so the rendered value can name a range the
  date never covered, and `.9999` at width three would carry into the second.
- **Text fields** (month names, day-of-week names, eras). They drag in locale, which this formatter
  has never had and which is a design commitment rather than a feature addition. `MMM` writing `003`
  is the worst behaviour in the language and predates this work; it wants a lint, not a silent fix.

## See also

- `legend-pure-m4/.../primitive/date/DateFormatPattern.java` and its javadoc, which carries the
  rendering order, the viability rule, and the interval argument.
- `legend-pure-m4/.../primitive/date/DateFormat.java`, whose `format` javadoc is the user-facing
  reference for the whole language.
- `legend-pure-m3-core/.../platform/pure/essential/string/toString/format.pure`, the Pure surface.
- `TestDateFormat`, whose `LEGACY_COMPATIBILITY` table is the compatibility suite, and
  `TestDateFormatPattern`, which carries the worked tables of this decision as tests.
- [ADR-004: Date Difference Semantics](ADR-004-date-difference-semantics.md), which reads a Pure date
  as a span in the same way and for the same reasons.
