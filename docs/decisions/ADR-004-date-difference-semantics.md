# ADR-004: Date Difference Semantics

**Status:** Proposed. Options analysis; no decision taken.
**Date:** 2026-08-06, revised 2026-08-07
**Deciders:** Legend Pure core team

## Goals

Three, in priority order.

1. **Well defined, usable, and internally coherent.** The rule should be statable in a sentence or
   two, hold without exceptions, agree with the rest of the platform's date functions, and not
   surprise a user who has read the documentation. **Conforming to a standard is the strongest form
   of being well defined**, since it replaces a house convention with one the user may already know
   and can look up independently. ISO 8601 carries particular weight: Pure's date literals, its
   granularity model, and its lexical representation already follow it, so a date function that
   departs from it departs from the platform's own foundation.
2. **Sufficient in the platform.** Pure does not always push computation down. A great deal of work
   runs in the platform's own engines, and that is a first-class execution mode rather than a
   fallback, since the in-memory answer *is* the definition of the function. Users therefore have to
   be able to compute what they actually need without leaving Pure. And where more than one reading
   of "the difference between two dates" is legitimate, letting a user ask for the one they want is
   a feature rather than a complication.
3. **Translatable to external target systems.** Where computation is pushed down, Legend pushes it
   into the data stores and query engines it runs against. Many are relational, but nothing about
   the design requires that. A rule that no target can reproduce is a rule that silently gives
   different answers depending on where a query runs.

Where these conflict, the earlier wins. A function that is wrong in a well defined way is worse than
one that is right and needs emulation, and a function users cannot get the answer they need out of
is worse than one that needs a second call. Most of the options below trade against each other along
these lines, so all three are weighed explicitly throughout.

## Context

`meta::pure::functions::date::dateDiff(d1, d2, unit)` and
`meta::pure::functions::date::adjust(date, n, unit)` are `<<PCT.function>>` natives. Both are
implemented once, in `legend-pure-m4`: `DateFunctions.dateDifference` (delegating to the
package-private `DateDiff`) and the `PureDate.addX` family. Both engines call straight into them,
`CoreHelper.dateDiff` in the compiled engine and the `DateDiff` and `AdjustDate` natives in the
interpreted engine, so the two engines cannot disagree. Every other execution backend translates
them into the target system's own functions, and there they can and do disagree. Most of the
evidence below comes from the relational adapters, because that is where the manifests are, but the
question is general: any store or query engine Legend pushes into has to be able to reproduce the
rule.

This ADR takes the platform as it stands today as its baseline and asks what to do next. It records
what the current semantics are, what they cost us, and what the alternatives are. It does not choose
between them.

The platform is written in Java today, but Pure is a language rather than a Java library and should
be reimplementable in C, Rust, or anything else with the same answers. So what a particular library
does is evidence here, never justification, and "cheap to implement with the library we happen to
use" is a cost, not a reason. Every candidate below is therefore stated as a formula over instants
and calendar fields before any implementation is mentioned.

### Terms

- **Unit**: one of the `DurationUnit` values, YEARS through NANOSECONDS.
- **Grid**: the set of instants at which a unit's counter ticks over. Midnights for DAYS, the first
  of the month for MONTHS, and so on. A date is **aligned** to a unit when it sits exactly on that
  unit's grid.
- **Granularity**: how much of a date is written down. A Pure date stops at the year, the month, the
  day, the hour, the minute, the second, or a subsecond of any precision, and stands for the whole
  span it leaves unsaid. `dateDiff` reads a date as the first instant of that span.

### What the platform does today

| Unit | Rule | Family |
|---|---|---|
| YEARS, MONTHS, DAYS | Floor both operands onto the unit's grid, subtract the grid positions | Boundary counting (A below) |
| WEEKS | Sunday-based grid going forward, Monday-based grid going backward | Boundary counting, inconsistently (B below) |
| HOURS through NANOSECONDS | Elapsed time, truncated toward zero | Elapsed (C below) |

The WEEKS row is usually described as "half open: a boundary landing on `d2` counts, one landing on
`d1` does not." That is true, but there is a sharper way to say it. `weeksBetween` counts
`weekNumber(to) - weekNumber(from)` going forward and `weekNumber(to - 1) - weekNumber(from - 1)`
going backward, and shifting both operands back one day is exactly the same as shifting the grid
origin forward one day. So:

> **`dateDiff(a, b, WEEKS)` counts boundaries on a Sunday-based week grid when `b >= a`, and on a
> Monday-based week grid when `b < a`.**

That is the defect, stated plainly. It is not a half-open convention that happens to be visible for
one unit; it is two different week grids depending on the direction of travel. It is also why the
asymmetry exists: `2015-07-04 -> 2015-07-05` is 1 but `2015-07-05 -> 2015-07-04` is 0.

Four further properties of the function as it stands, which the options below have to preserve or
deliberately change:

- **A coarse operand is read as the start of its span**, so `%2014` behaves as `%2014-01-01T00:00:00`
  and `%2014->dateDiff(%2014-06, MONTHS)` is 5. See axis 4.
- **Subsecond digits count down to the nanosecond** and anything finer is ignored.
- **A span too large for the unit raises an error** rather than wrapping, which NANOSECONDS reaches
  at about 292 years.
- **The calendar is proleptic ISO**, matching `adjust`, so there is no Julian cutover and years at or
  below zero are ordinary years. Most SQL targets support neither, which is a translation limit
  rather than a semantic one.

One gap is worth naming here rather than leaving to the evidence section: **MICROSECONDS and
NANOSECONDS are accepted in memory and translatable nowhere.** `adjust` accepts them, so `dateDiff`
does too, but every dialect's unit list stops at MILLISECONDS and neither unit has a
`<<PCT.test>>`. Whatever is decided below, that pair is currently outside the contract that PCT is
supposed to enforce.

## The design space

Choosing "the semantics of `dateDiff`" is really six independent choices. The literature and the
existing discussion collapse them, which is why the WEEKS question keeps reappearing. The letters
A through E below label positions on the first axis only.

### Axis 1: what is being counted

Let `g_U(t)` be the **grid index** of instant `t` for unit `U`:

```
g_YEARS(t)   = year(t)
g_MONTHS(t)  = 12 * year(t) + month(t)
g_U(t)       = floor((t - origin_U) / length_U)      for WEEKS and every finer unit
```

For DAYS and finer, `origin_U` and `length_U` are fixed by the calendar and the definition of the
second. For WEEKS, `length_U` is seven days but `origin_U` is a free parameter: see axis 3.

| | Name | Definition | `2015-12-31 -> 2016-01-01` in YEARS |
|---|---|---|---|
| **A** | Boundary counting | `g_U(b) - g_U(a)` | 1 |
| **B** | Half-open boundary counting | Grid points in `(a, b]` going forward, minus grid points in `[b, a)` going backward. Equals A plus `[a aligned]` minus `[b aligned]` when `b < a`, and equals A exactly when `b >= a` | 1 |
| **C** | Complete elapsed units | `trunc((b - a) / length_U)` for fixed-length units; for YEARS and MONTHS, the largest `n` such that `a` advanced by `n` units does not pass `b`, mirrored going backward | 0 |
| **D** | Exact fractional | `(b - a) / length_U` exactly for fixed-length units; **undefined for YEARS and MONTHS without a named convention**, see axis 5 | 0.0027... |
| **E** | Compound period | The record of years, months, and days (and optionally a time part) `P` such that `a` advanced by `P` is exactly `b`, computed field by field with borrowing | 0 years, 0 months, 1 day |

Their algebraic properties, which is what "internally coherent" cashes out to:

| | Antisymmetric | Additive | `d(a,b) = 0` means | Derivable from |
|---|---|---|---|---|
| **A** | Yes | **Yes** | `a` and `b` fall in the same grid cell (an equivalence relation) | C, after truncating both operands to the unit; on the fixed-length units, D as well |
| **B** | **No** | No | Nothing uniform | Nothing |
| **C** | Yes | No | `a` and `b` are less than one unit apart | On the fixed-length units, `trunc` of D, or of C in a finer unit. **On YEARS and MONTHS, nothing** |
| **D** | Yes | Yes | `a` and `b` begin at the same instant, which is not the same as being equal: see axis 4 | On the fixed-length units, C in the finest unit, divided. **On YEARS and MONTHS, nothing** |
| **E** | Yes by construction | No | as D | Nothing |

Five of these deserve comment, because they are easy to get wrong.

**A is additive.** `d(a,c) = d(a,b) + d(b,c)` for every `a`, `b`, and `c`, because A is the
difference of a rank function and therefore telescopes. Antisymmetry, additivity, and the fact that
a zero result is an equivalence relation all fall out of that single structural fact rather than
being separate properties that have to be checked. A is the only integer candidate with this
property, and it is the strongest formal argument available for boundary counting. It is easy to
mistake additivity for something only a fractional result can offer; it is not.

**A is derivable, given truncation.** A is not recoverable from a C or D *result*: `HOURS(12:59,
13:01)` is 1 under A while the exact distance is 0.033, and no rounding of 0.033 yields 1. But it is
recoverable by truncating the *operands* first, and the platform already ships those truncations:
`firstDayOfYear`, `firstDayOfMonth`, `firstDayOfWeek`, `firstHourOfDay`, `firstMinuteOfHour`, and
`firstSecondOfMinute`, all in legend-engine's `core/pure/corefunctions/dateExtension.pure`.

**On the fixed-length units, everything reduces to the finest one.** Given an elapsed count in
NANOSECONDS a user can build C in any coarser fixed-length unit by dividing, D by dividing without
truncating, and A by truncating the operands first. So for WEEKS and below the platform needs to
offer only one primitive, and it already does. The one caveat is range: NANOSECONDS overflows at
about 292 years, so long spans have to start from MICROSECONDS or MILLISECONDS instead.

**On YEARS and MONTHS, nothing reduces to anything.** Complete months cannot be built out of
boundary-counted months, because the correction term is anniversary arithmetic with month-end
clamping, which is precisely where hand-written code goes wrong: see the `2015-01-31 -> 2015-02-28`
row below, where boundary counting says one month, complete units say zero, and `adjust` by one
month lands exactly on the target date. Fractional months cannot be built either, since they need
complete months first, plus a convention. **So `dateDiff` is one primitive short: complete years and
months are the only reading a user cannot assemble from what the platform provides.** That is a
goal 2 gap, and it is the same gap under both the complete and the fractional reading.

**C is `trunc` of D, not `floor` of D.** C as defined and as implemented truncates toward zero,
which is what makes it antisymmetric. `floor(-0.5)` is -1 where C gives 0, so deriving C with
`floor` breaks antisymmetry on every backward pair. Anywhere this document or the code says "floor
the fractional result," read "truncate toward zero."

**E is the only candidate that inverts `adjust` off the diagonal.** `a` advanced by `E(a, b)` is `b`
for all `a` and `b`, by construction, which is the property `java.time.Period.between` guarantees. No
scalar candidate can do this, because a scalar in a single unit cannot carry the remainder. E is
listed for completeness and is not proposed: it would require a new Pure type and a new arithmetic,
and no target we push into returns it. But it should be rejected explicitly rather than left out,
because it is the answer to "what is the difference between two dates" that most calendar libraries
actually give.

### What the candidates actually give you

Every candidate produces an answer somebody will call obviously wrong. Stating the rules abstractly
hides that; putting the numbers side by side does not. A uses the Sunday grid, which is what the
platform counts forward on today. D needs a convention for YEARS and MONTHS, so where the Oracle
convention (a flat 31-day month) and the anniversary convention (the fraction of the way to the next
anniversary, measured against the real length of that span) disagree, both are given as
`Oracle / anniversary`. That they disagree at all is the argument of axis 5.

| From | To | Unit | A | B | C | D | The uncomfortable reading |
|---|---|---|---|---|---|---|---|
| `%2015-12-31` | `%2016-01-01` | YEARS | **1** | **1** | 0 | 0.0027 | One day apart is a whole year |
| `%2015-01-01` | `%2015-12-31` | YEARS | 0 | 0 | 0 | 0.9973 | 364 days apart is no years at all |
| `%2016-01-31` | `%2016-03-01` | MONTHS | **2** | **2** | 1 | 1.03 | Thirty days is two months |
| `%2016-02-01` | `%2016-02-29` | MONTHS | 0 | 0 | 0 | 0.90 / 0.97 | Almost all of February is no months, and the two D conventions disagree about how much of it, because February is not 31 days long |
| `%2015-01-31` | `%2015-02-28` | MONTHS | 1 | 1 | **0** | 1 | C says zero months even though `adjust(%2015-01-31, 1, MONTHS)` lands exactly on `%2015-02-28` |
| `%2015-07-04` | `%2015-07-05` | WEEKS | 1 | 1 | 0 | 0.14 | Saturday to Sunday is a whole week. On a Monday grid A gives 0 |
| `%2015-07-05` | `%2015-07-04` | WEEKS | -1 | **0** | 0 | -0.14 | The same pair reversed. B is the only candidate whose magnitude changes with direction |
| `%2015-07-06` | `%2015-07-11` | WEEKS | 0 | 0 | 0 | 0.71 | Monday to Saturday is no weeks, on either grid |
| `%2015-07-07T23:59` | `%2015-07-08T00:01` | DAYS | **1** | **1** | 0 | 0.0014 | Two minutes is a day |
| `%2015-07-07T12:59` | `%2015-07-07T13:01` | HOURS | **1** | **1** | 0 | 0.033 | Two minutes is an hour |
| `%2015-07-07T12:00` | `%2015-07-07T12:59` | HOURS | 0 | 0 | 0 | 0.983 | Fifty-nine minutes is no hours |
| `%2015-07-07T12:59` | `%2015-07-07T13:01` | MINUTES | 2 | 2 | 2 | 2 | All four agree, which is the usual case and the reason the disagreements are easy to miss |

Read down the A column and the complaint is "tiny gaps become whole units." Read down the C column
and it is "large gaps vanish." Neither is a defect to be fixed; they are answers to different
questions, and the last row is why the difference so rarely surfaces.

Three structural consequences follow, each of which the table makes concrete:

- **C is not additive.** From `%2015-01-01T00:00` to `00:59` is zero HOURS, and `00:59` to `01:58` is
  zero HOURS, but `00:00` to `01:58` is one. Two zeroes that sum to one. A gives 0, 1, and 1, which
  do add up.
- **Under A, units do not convert.** The `12:59 -> 13:01` pair is one HOUR and two MINUTES, and two
  minutes divided by sixty is zero, not one. Only D converts exactly.
- **A zero answer never means the dates are equal.** Every candidate returns 0 for `%2014` against
  `%2014-01`, which are different dates that begin at the same instant. See axis 4.

### Axis 2: rounding

Independent of what is being counted, a non-integral result has to be resolved. The options are
truncation toward zero, floor, ceiling, and round to nearest. The trade-off is exact:

- **Truncation toward zero gives antisymmetry** and a plateau of width two units around zero, since
  everything strictly between -1 and 1 maps to 0.
- **Floor gives monotonicity** in both arguments and a uniform plateau of width one, and gives up
  antisymmetry.

You cannot have both. A escapes the choice entirely by flooring both *operands* onto the grid, which
is why it is antisymmetric and monotone at once. C and D must choose, and the platform has chosen
truncation.

Round to nearest is a real user-facing semantics, not only a bug: "about three months" is a sensible
answer to a sensible question. Databricks and Oracle currently produce it by accident, and that is a
translation defect, but a rounding mode is the sort of thing an options record would expose
deliberately. See Option 6.

### Axis 3: the grid origin for WEEKS

Every other unit's grid is fixed by the calendar. WEEKS is not: days are not aligned to weeks, so
the origin is a free parameter, and it is the only reason A and B ever differ in practice.

**The platform currently holds three different answers at once.**

| Function | Week convention | Where |
|---|---|---|
| `dateDiff(..., WEEKS)` | Sunday-based grid forward, Monday-based grid backward | `legend-pure-m4` `DateDiff.weeksBetween` |
| `firstDayOfWeek` | Monday | legend-engine `dateExtension.pure`, `mostRecentDayOfWeek(DayOfWeek.Monday)` |
| `weekOfYear` | `GregorianCalendar.get(WEEK_OF_YEAR)`, so it depends on the **JVM default locale** and uses the Julian cutover calendar | legend-engine `FunctionsHelper.weekOfYear` |

Any decision about WEEKS in `dateDiff` that does not also settle `firstDayOfWeek` and `weekOfYear`
leaves the platform incoherent, which is goal 1. The `weekOfYear` locale dependence is a defect in
its own right and should be fixed regardless.

Mainstream systems treat the origin as a parameter rather than a constant: BigQuery takes it in the
call (`DATE_DIFF(d2, d1, WEEK(MONDAY))`), Snowflake takes it from the `WEEK_START` session
parameter, Tableau takes it as a fourth argument to `DATEDIFF`, and SAS encodes it in the interval
name (`WEEK.2`). SQL Server is the outlier that hard-codes Sunday and documents that `SET DATEFIRST`
deliberately does not affect `DATEDIFF`.

### Axis 4: mixed granularity

`dateDiff` reads a coarse date as the first instant of its span, so `%2014` behaves as
`%2014-01-01T00:00:00`. Alternatives are to reject a unit finer than either operand's granularity,
or to return a range. Three observations:

- **`adjust` already rejects it.** `%2014->adjust(1, DAYS)` throws, while
  `%2014->dateDiff(%2015, DAYS)` cheerfully answers 365. The two halves of the same arithmetic
  disagree about whether a coarse date has a day.
- **`compare` treats a date as a span; `dateDiff` treats it as a point.** `compare(%2014, %2014-01)`
  is -1, because a date that runs out of components sorts before one that carries on, yet
  `dateDiff(%2014, %2014-01, U)` is 0 for every `U`. So a zero difference does not imply equality,
  under any of A through D. This is worth knowing before claiming that a fractional result would
  make zero mean "equal": it would not, because granularity, not precision, is what separates those
  two dates.
- Mixed granularity is almost entirely an in-memory concern, because year and year-month granularity
  do not survive pushdown at all. See the translation evidence below.
- **`%latest` is deliberately handled differently by the two functions.** `LatestDate` has neither
  components nor a fixed position on the time line: what it stands for is decided by the context it
  appears in, so it means different things in different places despite being a singleton. Sorting
  has to be total, so `compare` orders it after every other date, which is a sorting convention
  rather than a claim about when it falls. Differencing does not have to be total, so `dateDiff`
  rejects it in either position, including against itself. Inventing an answer to a question that
  has no meaning would be worse than refusing it.

### Axis 5: conventions for variable-length units

WEEKS and below have a constant length, so C and D need no convention. YEARS and MONTHS do not, and
this is where a fractional answer has to invent something.

The claim that every such convention is arbitrary is too strong. Finance has standardized several,
and they are the reason `YEARFRAC` in Excel takes a `basis` argument: 30/360 bond basis, 30E/360,
ACT/360, ACT/365F, and ACT/ACT in the ISDA and ICMA variants. Oracle's `MONTHS_BETWEEN` picks a
different one, a flat 31-day month for the fractional part, so
`MONTHS_BETWEEN('2016-02-29', '2016-02-01')` is 28/31 even though that February had 29 days. DB2
documents its `TIMESTAMPDIFF` as an approximation using 30-day months and 365-day years.

There is also a convention that needs no arbitrary denominator: complete units by anniversary
arithmetic, plus the fraction of the way to the next anniversary, measured against the actual length
of that partial month or year. TC39 Temporal appears to implement exactly this in
`Duration.prototype.total({ unit, relativeTo })`, where the anchor is what makes the fraction well
defined. That should be confirmed against the specification before being relied on, but the
statement "no system implements it" should not stand unchecked.

The design consequence: **a fractional variant should take the convention as an argument rather than
pin one.** For a platform whose users compute accruals and day counts, one hard-coded denominator is
the wrong answer no matter which one is chosen.

### Axis 6: time zones

Pure dates carry no zone today; every date is understood as UTC. The choice made here constrains
what zone support can look like later:

- **Elapsed time is zone independent. Calendar boundaries are not.** A boundary count needs a zone
  to locate the boundary.
- Under a hybrid (calendar units count boundaries, time units measure elapsed time), only the
  calendar units would need a zone. Under A uniformly, **every** unit would, including HOURS: zones
  with :30 and :45 offsets put the top of the hour in a different place.
- The classic pitfall is DAYS against HOURS across a DST transition: a civil day is 23 or 25 hours
  long, so `DAYS * 24` is not `HOURS`. The current split already gets this right, since DAYS counts
  civil midnights while HOURS measures elapsed time. Temporal makes the same choice deliberately:
  `ZonedDateTime.until` with `largestUnit: 'day'` counts calendar days, not 86,400-second blocks.

### Out of scope

Business-day and holiday-calendar counting (`NETWORKDAYS` and friends), fiscal calendars, and leap
seconds. The first is a real and probably imminent requirement given the
`meta::pure::functions::date::calendar::*` family already in legend-engine, which carries NY and
London calendars and fiscal-day semantics, but it is a different function rather than a different
reading of this one.

## Why the calendar/time split is principled

A calendar unit names a position in a civil calendar. A date has a year, a month, and a day of
month, and those positions are defined by rules people agreed on, not by any quantity of elapsed
time. A time unit names an offset within a day and is a fixed quantity of elapsed time. Counting
boundaries is the natural question to ask of the first kind, and measuring duration is the natural
question to ask of the second, so a rule that treats them differently is following a real
distinction rather than an arbitrary one.

The strongest corroboration is not a library but a standard: **ANSI SQL has two interval families,
year-month and day-time, and forbids mixing them**, for exactly this reason. Four unrelated language
designs land on the same line: Java separates date-based from time-based `ChronoUnit`s and
`Period` from `Duration`, Temporal separates plain dates from instants and gives durations separate
date and time fields, .NET added `DateOnly` and `TimeOnly` alongside its tick-based `TimeSpan`, and
Julia separates calendar periods from fixed-length time periods.

What is not principled is that the boundary-counting family is implemented two different ways. For
YEARS, MONTHS, and DAYS the distinction is invisible, because truncating to the unit already puts
both operands on the unit's grid. WEEKS is the only unit where the grid origin is a choice, and it
is the only unit that got a second implementation.

## How other systems choose

Evidence, not authority. The point of the table is that the two goals pull in opposite directions.

| System | What differencing two dates gives | Semantics |
|---|---|---|
| Python, stdlib `datetime` | Subtraction yields a `timedelta` of days, seconds, and microseconds | D; no calendar-aware difference at all |
| JavaScript `Date` | Subtraction yields milliseconds | D |
| JavaScript Temporal | `until` yields a `Duration` in whichever largest unit the caller asks for; `total` yields a fraction; `roundingMode` is explicit | E and C, with D on demand |
| C# / .NET | Subtraction yields a `TimeSpan` of ticks; NodaTime adds `Period.Between(start, end, units)` | D, with E in NodaTime |
| Go | `Sub` yields a `Duration` of nanoseconds | D |
| Rust `chrono` | Subtraction yields a `TimeDelta`; `years_since` yields complete years | D, with C for years |
| Java | `ChronoUnit.between` yields complete units; `Period.between` yields a compound period | C and E |
| Common Lisp | Universal time is a second count | D |
| SAS | `INTCK(interval, a, b, method)` takes `DISCRETE` or `CONTINUOUS`; intervals are shiftable, as in `WEEK.2` | **A and C, selected by an argument** |
| Excel and Sheets | `DATEDIF`; `DAYS360`; `YEARFRAC(..., basis)` | C, plus D under named conventions |
| SQL Server, Snowflake, BigQuery, DuckDB, Trino, ClickHouse | `DATEDIFF` / `DATE_DIFF` count boundaries | A |
| MySQL and SingleStore | `TIMESTAMPDIFF` counts complete units; `DATEDIFF` is days only | C |
| Oracle | `MONTHS_BETWEEN` is fractional; date subtraction is fractional days | D |
| PostgreSQL | No `DATEDIFF` at all: subtraction yields a day-time interval, `age()` yields a compound interval | D and E |
| SQLite | `julianday()` difference | D |

Two patterns matter.

**Essentially no general-purpose language offers boundary counting.** A is a database idea. Languages
give you an exact duration, whole units, or a compound period, and the ones that offer only exact
durations do not provide a calendar-aware difference at all. That cuts across our goals rather than
along them: choosing A aligns Pure with the databases it pushes down to and away from how languages
think about dates, and choosing C or D does the reverse.

**SAS is the closest existing precedent for this whole document.** `INTCK` has taken a method
argument selecting between boundary counting and complete units for decades, in a language whose
users are doing exactly the kind of work Legend's users do. It is the strongest evidence that
Option 6 is a viable shape rather than a hedge, and that a mode argument can be made to work if it
is named well.

**`adjust` is not universally agreed either.** Java clamps month overflow, so 31 January plus one
month is 28 February; Go normalizes, so the same addition yields 3 March. Pure clamps, and
`adjust.pure` now says so. Note also that clamping costs two algebraic laws that users assume:
`(d + 1 month) + 1 month` is not `d + 2 months`, and `(d + 1 month) - 1 month` is not `d`. NodaTime
documents this loudly; Pure should too.

*Confidence: the relational rows for PostgreSQL, SingleStore, Oracle, and Databricks are verified
below against Legend's own generated SQL. High confidence on Python, JavaScript's `Date`, Go, .NET,
Java, and SQL Server. Moderate on the Temporal, `chrono`, Julia, SAS, and DuckDB/Trino/ClickHouse
specifics, which should be checked against current documentation before being relied on.*

## Evaluation criteria

1. **Internal coherence.** Can the rule be stated briefly and completely; is it antisymmetric and
   additive; does it need special cases; does it agree with `adjust`, `compare`, `firstDayOfWeek`,
   and `weekOfYear`.
2. **Translatability.** How cheaply and reliably it maps onto the systems we target, and whether
   emulation has a uniform shape.
3. **Cost.** Implementation work, plus the downstream cost of changing behavior that already ships.

The third splits into two costs that are easy to conflate and that pull in different directions.

**Retracting a declared expectation.** The `<<PCT.test>>` functions in `dateDiff.pure` are not a
regression net that happens to be checked in. They are the platform's published statement of what
`dateDiff` does: users read them to find out what Legend supports, and adapter authors implement
against them. Changing an assertion is therefore not a test edit, it is a retraction of something
the platform has told people is true, and the cost includes every expectation built on it. That cost
is high, and it depends on **what** is retracted as much as on how many: withdrawing an assertion
that contradicts its own mirror image costs far less than withdrawing one that was defensible on its
own terms, even though both count as one line.

**Changing behavior silently.** An option can move a large share of real answers while changing few
assertions or none. That is not cheap because the declaration held still; it is arguably worse,
because the published statement stays true and stops being a description of the function. Users have
no signal that anything moved. Each option below reports a measured share of ordered pairs whose
answer changes, alongside the assertions it retracts.

The two are reported separately, and neither substitutes for the other.

## Evidence

### The `adjust` relationship

`adjust(a, dateDiff(a, b, U), U) == b` holds for all `a` and `b` exactly when `a` and `b` share a
granularity and `U` names that granularity: the "diagonal." Verified over **46,496** ordered pairs
in `TestDateDifference.testAdjustInvertsDateDifferenceAtMatchingGranularity`.

**This criterion barely discriminates between A, B, C, and D.** On the diagonal both operands are
aligned to the unit's grid, which is precisely the condition under which all four coincide. Every
one of them satisfies it. Off the diagonal none of them do, for reasons fixed by design: `adjust`
refuses units finer than the date's granularity, and a coarser unit loses information through
truncation or month-end clamping.

It does discriminate in two places.

**It penalizes D.** `adjust` takes an `Integer`, so `adjust(a, dateDiff(a, b, U), U)` does not
typecheck under fractional results; the invariant has to be written with an explicit truncation. It
still holds on the diagonal, since a fractional difference between aligned operands is a whole
number, but the relationship stops being expressible without a conversion.

**It rewards E**, which inverts `adjust` for all pairs by construction rather than by coincidence.

C has a *potential* off-diagonal relationship: it can be **defined** as "the largest `n` such that
advancing `a` by `n` units does not overshoot `b`," which would make the two functions inverse where
the granularity permits. That definition has to be built deliberately, because a library's
complete-units function will not give it to you:

```
2015-01-31 plus 1 month                        = 2015-02-28    (adjust clamps to the short month)
complete months from 2015-01-31 to 2015-02-28  = 0             (yet the adjustment landed exactly there)
```

Anyone adopting C should implement it against `adjust` directly and test the pair, in whatever
language the platform is written in.

### What the declared expectations say, and what they leave unsaid

Scoring all 70 `assertEquals` in `dateDiff.pure` against each candidate, after first confirming that
the current implementation reproduces all 70. These counts are what each option would have to
**retract from the platform's published statement of what `dateDiff` does**, not a sample of usage:

| Unit | Cases | A differs | B differs | C differs |
|---|---|---|---|---|
| YEARS | 7 | 0 | 0 | 1 |
| MONTHS | 11 | 0 | 0 | 0 |
| WEEKS | 14 | 3 | 0 | 2 |
| DAYS | 10 | 0 | 0 | 0 |
| HOURS through MILLISECONDS | 28 | 0 | 0 | 0 |
| **Total** | **70** | **3** | **0** | **3** |

**Which three assertions matters more than the number three.** Under A they are:

```
2015-07-05 -> 2015-07-04     0  ->  -1
2015-07-12 -> 2015-07-06     0  ->  -1
2015-08-02 -> 2015-07-06    -3  ->  -4
```

All three are backward pairs whose declared answer disagrees in magnitude with its own forward
counterpart. Retracting them withdraws statements that are self-contradictory. Under C the three are
`%2015-12-31T23:59:59 -> %2016-01-01T00:00:01 = 1 year`, which is the headline example in the
published documentation of `dateDiff`, plus two coherent WEEKS statements. Retracting those
withdraws things the platform is entitled to have said. Same count, very different cost.

What the declarations say is also narrower than it looks, which matters for what these numbers can
and cannot settle:

- **Every `d1` in `dateDiff.pure` is aligned to the unit under test.** That is exactly the condition
  under which A, B, and C agree. All 28 time-unit assertions hold under all three candidates, so the
  platform has never actually declared that the time units measure elapsed time rather than count
  boundaries, and no PCT run can detect a change there.
- **B uniformly would retract nothing**, because forward B is A and the declared backward cases are
  aligned at both ends. Nothing retracted plus a changed function is the worst combination, not the
  best.
- The same gap reaches the adapter manifests below: **they record only the divergence the
  declarations happen to probe.** Oracle's MONTHS is generated as `floor(months_between(d2, d1))`,
  so `dateDiff(%2016-01-31, %2016-02-01, MONTHS)` is -1 on Oracle and 1 in Pure. Nothing declares
  it, so nothing excludes it, and `testDateDiffMonths` is not on Oracle's exclusion list.

That is an argument for **declaring more**, not for discounting what is already declared. Adding
assertions over unaligned operands states the calendar-versus-time split the platform already
implements, which is worth doing on its own terms: an addition costs a user nothing to accept, and
it is the precondition for any PCT run being able to confirm a decision taken here.

### Current translatability

Taken from the PCT manifests in legend-engine
(`**/src/main/resources/pct-manifests/*/EssentialFunctions_manifest.json`), which record the tests
each adapter is known to fail, cross-checked against the generators that produce the target
expression. Twelve adapters carry manifests: eleven relational (ClickHouse, Databricks, DuckDB, H2,
MemSQL, Oracle, PostgreSQL, Snowflake, Spanner, SQL Server, and Trino) and Deephaven, which is not.

**WEEKS does not work on a single target.** `testDateDiffWeeks` is excluded on all ten adapters that
support `dateDiff` at all:

| Signature | Adapters | Cause |
|---|---|---|
| `expected 0, actual -1` | H2, Oracle, SQL Server | Target counts boundaries on a Sunday grid in both directions. Verified for Oracle: `(next_day(d2, 'SUNDAY') - next_day(d1, 'SUNDAY')) / 7` groups days into Sunday-to-Saturday blocks, which is our forward grid. This divergence is exactly our backward Monday grid |
| `expected 1, actual 0` | ClickHouse, DuckDB, MemSQL, PostgreSQL, Snowflake, Trino | **Not one cause.** PostgreSQL generates `TRUNC((d2 - d1) / 7)` and MemSQL generates `timestampdiff(WEEK, d1, d2)`, both of which are semantics C, not a week-start difference. For ClickHouse, DuckDB, Snowflake, and Trino the call goes straight to the native `datediff`, and a Monday-based grid would produce the same signature; that reading is inferred and unverified |
| Not supported | Databricks | The generator has no WEEK branch and fails explicitly |

The distinction matters: **pinning a Sunday week start in the generated SQL cannot fix PostgreSQL or
MemSQL, because week start is not what they are doing.** It would, however, fix them under Option 4.

**Year and year-month granularity do not survive pushdown at all.** `testDateDiffYears`,
`testAdjustByYears`, and `testAdjustByMonths` are excluded on all eleven relational adapters, with
errors like "Ensure the target system understands Year or Year-month semantic" and "Date has no day:
2012-03." No SQL type represents `%2015` or `%2012-03`, and this is one place where the limitation is
genuinely about SQL rather than about targets in general. The mixed-granularity semantics of axis 4
therefore matters almost entirely for in-memory execution.

**Some targets round where we truncate.** Oracle fails `testDateDiffHours` (`expected 2, actual 3`)
and `testDateDiffMinutes` (`expected 0, actual 1`) because its generator produces fractional days
and multiplies, for example `(CAST(d2 AS DATE) - CAST(d1 AS DATE)) * 24`, which is then rounded on
the way to an integer. Databricks fails the same two plus `testDateDiffMonths` because its generator
literally emits `cast(round(months_between(d2, d1)) AS INT)`. This is a translation defect rather
than a semantics choice, but it is the same class of problem.

**Other gaps.** Oracle does not support `dateDiff` in MILLISECONDS (ORA-30081). SQL Server returns
the wrong answer for `adjust` by milliseconds, its legacy `datetime` having about 3.33 ms
resolution. Deephaven and Spanner support neither function. **No generator handles MICROSECONDS or
NANOSECONDS**: every dialect's unit list stops at MILLISECONDS, so those two units are in-memory
only, silently.

**How translation is done.** Dialects with a native `DATEDIFF` map directly to it; SQL Server emits
`datediff(%s, %s, %s)`. Dialects without one use a hand-written generator, such as
`generateDateDiffExpressionForPostgres` and `generateDateDiffExpressionForOracle`. The direct
mappings inherit whatever semantics the target has, without checking that it matches Pure's. That is
the root cause of most of the divergences above.

**The practical consequence:** since WEEKS is already broken on every backend, *changing WEEKS
semantics carries no pushdown regression risk.* It can only reduce the number of exclusions.

### Measured impact

The second cost: how many answers move, over every ordered pair in a dense sweep. None of this is
visible in the declarations, which is exactly why it is reported separately.

| Change | Ordered pairs affected |
|---|---|
| WEEKS from today's rule to A on the Sunday grid | 12.2% of ordered day pairs in 2015 and 2016 |
| WEEKS from today's rule to A on the Monday grid | 12.2% of the same pairs |
| WEEKS from today's rule to complete 7-day spans (C) | 42.7% |
| HOURS from elapsed (C) to boundary counting (A) | 47.1% of ordered minute pairs within a day |

The first two being equal is not a coincidence: today's rule already *is* A on the Sunday grid going
forward and A on the Monday grid going backward, so picking either one keeps half the answers and
changes the other half. Sunday keeps the forward answers; Monday keeps the backward ones.

The last row is the number to keep in view for Option 2. `HOURS(12:59, 13:01) = 1` is not a corner
case; boundary counting and elapsed time disagree for nearly half of all unaligned pairs, and not
one of those pairs is covered by anything the platform currently declares.

## Options

Option to semantics, at a glance: 0 is A + B + C, 1 is A + C, 2 is A, 3 is B, 4 is C, 5 is D, and 6
is more than one of them exposed to the user.

### Option 0: status quo

- **Coherence:** three rules for two families. The calendar and time split is principled; the WEEKS
  variant is not, and is best described as using two different week grids depending on direction.
  WEEKS is not antisymmetric, needs its own paragraph of documentation, and can never invert
  `adjust`. The platform also disagrees with itself about week start across three functions.
- **Translatability:** as measured above. WEEKS broken on all ten adapters that support `dateDiff`;
  time units diverge from boundary-counting targets whenever operands are not aligned; MICROSECONDS
  and NANOSECONDS untranslatable everywhere.
- **Cost:** nothing retracted and nothing moved. This is the only option with zero cost on both
  measures, which is the whole of its case.

### Option 1: hybrid, with the WEEKS fix (A for all calendar units, C for time)

Make WEEKS use a single grid in both directions, so that `dateDiff(a, b, WEEKS)` is
`weekNumber(b) - weekNumber(a)` unconditionally. **Which grid is a real sub-decision, not a detail.**

| | Grid | Declarations retracted | What they said | Answers moved | Fixes | Notes |
|---|---|---|---|---|---|---|
| **1a** | Sunday | 3 | All three self-contradictory: each disagrees in magnitude with its own forward counterpart | 12.2% | H2, Oracle, SQL Server | Keeps today's forward answers; contradicts `firstDayOfWeek` and ISO 8601 |
| **1b** | Monday (ISO 8601) | 2 | Both defensible: `2015-07-04 -> 2015-07-05 = 1` and `2016-01-01 -> 2017-01-01 = 53` | 12.2% | Plausibly ClickHouse, DuckDB, Snowflake, Trino; needs verification | Keeps today's backward answers; agrees with `firstDayOfWeek` and ISO 8601; SQL Server hard-codes Sunday and would need emulation |
| **1c** | Caller supplies it | As its default | As its default | As its default | Whichever matches the default | Signature change to a `<<PCT.function>>`, or a sibling function; matches BigQuery, Snowflake, Tableau, and SAS |

- **Coherence:** two rules, both one sentence, split on the calendar and time line argued for above.
  Antisymmetric everywhere. The calendar units become additive; the time units do not, which is
  inherent to C and is the one structural property this option gives up relative to Option 2. No
  special cases. Preserves the graceful path to zone support: calendar units would take a zone, time
  units would not. 1b additionally makes the platform agree with itself, which is goal 1; 1a leaves
  `dateDiff` and `firstDayOfWeek` disagreeing.
- **Translatability:** strictly better than today under any of the three. Emulation is cheap and
  uniform either way, since a fixed seven-day grid count is
  `floor((epochday(b) - k) / 7) - floor((epochday(a) - k) / 7)` on any target that can produce a day
  number. That is an argument for settling the grid on coherence grounds rather than on adapter
  counts, which are close to a wash.
- **Cost:** the implementation is a deletion under 1a, `weeksBetween` losing its backward branch, and
  the same deletion plus a one-line change to the grid origin constant under 1b. The retractions
  differ in kind, which matters more than the counts. Under 1a:

  ```
  2015-07-05 -> 2015-07-04     0  ->  -1
  2015-07-12 -> 2015-07-06     0  ->  -1
  2015-08-02 -> 2015-07-06    -3  ->  -4
  ```

  Every one of those is a backward pair whose declared answer contradicts its own forward
  counterpart, so what is being withdrawn is an anomaly rather than a commitment. Under 1b the two
  are `2015-07-04 -> 2015-07-05` (1 to 0) and `2016-01-01 -> 2017-01-01` (53 to 52), both of which
  are perfectly coherent things for the platform to have said and which a user could reasonably have
  built on. Either way, 12.2% of ordered day pairs move, and no backend reproduces today's answers,
  so nothing regresses under pushdown. Requires an engine rebuild to re-run PCT.

### Option 2: semantics A uniformly

- **Coherence:** one rule, *floor both dates onto the unit's grid and subtract*. Antisymmetric and
  additive everywhere. No special cases. Simplest of all the options to state, and the only one that
  makes the whole function a single sentence.
- **Translatability:** best. Maps 1:1 onto `DATEDIFF` for SQL Server, Snowflake, BigQuery, and other
  boundary-counting targets, for every unit. Where emulation is needed the shape is uniform, a
  truncation and a subtraction, so one generator template serves all units. Caveat: fine units over
  long spans overflow 32-bit `DATEDIFF` on SQL Server, which is why `DATEDIFF_BIG` exists.
- **Cost:** the worst shape of any integer option. It retracts only the same three anomalous WEEKS
  assertions, so the declared expectations barely move, and then **47.1% of ordered minute pairs
  within a day get a different HOURS answer**. Every published statement about the time units
  survives untouched while ceasing to describe the function, so a user reading `dateDiff.pure` sees
  no change at all and gets different numbers. It also discards the elapsed-time reading entirely,
  which shipped Pure code relies on: `toEpochValue`, `firstMillisecondOfSecond`, the router's timing
  instrumentation, and `tdsEquivalent`'s tolerance comparison all call `dateDiff` in a time unit.
  Under future zone support every unit becomes zone dependent, including HOURS, which the hybrid
  avoids. If this option is taken, the time-unit assertions must be rewritten to state the new rule
  even where they would still pass.

### Option 3: semantics B uniformly

- **Coherence:** one rule, but the wrong one. The half-open convention makes every unit asymmetric
  whenever exactly one endpoint sits on a boundary: `HOURS(13:00:00, 13:30:00)` would be 0 while
  `HOURS(13:30:00, 13:00:00)` would be -1. Operands on a boundary are extremely common. This spreads
  the WEEKS anomaly across the whole function rather than removing it.
- **Translatability:** poor. No target implements B. Every unit on every dialect would need
  hand-written emulation to reproduce the asymmetry.
- **Cost:** it retracts nothing and changes the function everywhere, which is the worst combination
  on offer rather than a point in its favor: the platform's declarations would keep saying exactly
  what they say now, about a function that no longer behaves that way. **Not recommended under any
  weighting.** Listed for completeness.

### Option 4: semantics C uniformly

- **Coherence:** one rule, *how many whole units fit between them*. Antisymmetric but not additive.
  Its headline advantage, being definable in terms of `adjust`, has to be built deliberately. Its
  headline disadvantage is that it abandons the calendar and time distinction argued for above:
  under C, `%2015-12-31T23:59:59 -> %2016-01-01T00:00:01` is zero years, which is defensible as
  duration and indefensible as calendar arithmetic.
- **Translatability:** mixed, and better for WEEKS than previously recorded. Maps 1:1 onto MySQL and
  SingleStore `TIMESTAMPDIFF` and onto `floor(MONTHS_BETWEEN(...))`. **PostgreSQL and MemSQL would
  pass `testDateDiffWeeks` with no change to the generated SQL at all**, since both already emit
  exactly C. But YEARS, MONTHS, and DAYS would then need emulation on SQL Server, Snowflake,
  BigQuery, and every other boundary-counting target, and emulating complete months and years
  requires anniversary logic, the fiddliest of the integer generators.
- **Cost:** three retractions, the same count as Option 1, but far more expensive ones. The first is
  `%2015-12-31T23:59:59 -> %2016-01-01T00:00:01 = 1` in YEARS, which is not an incidental assertion:
  it is the worked example the published documentation of `dateDiff` leads with, and it is the
  clearest statement the platform makes about what the function is for. Withdrawing it is
  withdrawing the function's advertised character. Behaviorally, 42.7% of ordered day pairs change in
  WEEKS, and DAYS, MONTHS, and YEARS all change for any operand carrying a time of day, which the
  declarations do not currently cover.

### Option 5: semantics D, fractional results

Change `dateDiff` to return the exact real-valued distance, `Float[1]` or `Decimal[1]` instead of
`Integer[1]`.

- **Coherence:** the best of any option on the constant-duration units, and the worst on the others.

  For WEEKS and below it is the only option that is *lossless*. It is exactly antisymmetric and
  additive, and unit conversion becomes exact, so `dateDiff(a, b, HOURS)` is
  `dateDiff(a, b, MINUTES) / 60`, which is false under every other option. Note that additivity is
  **not** unique to D: A has it too, for a different reason.

  For YEARS and MONTHS it is the weakest option, because the denominator is a convention. See
  axis 5: the right response is to take the convention as an argument, which a single fixed-arity
  `dateDiff` cannot do.

  A zero result does **not** recover the meaning "the two dates are equal," because Pure dates carry
  granularity: `%2014`, `%2014-01`, and `%2014-01-01` are pairwise unequal and every difference
  between them is exactly zero.

  It also weakens the `adjust` relationship, which no longer typechecks without an explicit
  truncation.

- **Precision:** a real hazard, and a silent one. `Float` in Pure is a floating point number without
  a mandated representation, so the exact limit is implementation defined, but every floating point
  format has finite precision, so for any of them there is a span beyond which
  `dateDiff(a, b, NANOSECONDS)` stops being able to represent whole nanoseconds and quietly loses
  resolution. On the current platform, which uses a Java `double`, that happens at 2^53 nanoseconds,
  about 104 days. Wherever the limit falls, this is worse than the status quo, which now raises an
  overflow error rather than returning a plausible wrong answer. `Decimal[1]` pushes the limit far
  enough out to stop mattering but is slower and translates less cleanly, since decimal arithmetic
  and precision rules differ by dialect.
- **Translatability:** genuinely good on some targets, poor on others, and split along a different
  line than the integer options. Oracle is a natural fit, and Databricks and Spark `months_between`
  also returns a double following Oracle's convention. PostgreSQL, DuckDB, Trino, and ClickHouse can
  all divide an epoch difference to get any constant-duration unit exactly. But SQL Server,
  Snowflake, and BigQuery return integers from `DATEDIFF`, so fractional has to be built from a
  fine-grained integer difference and a division, which runs into exactly the overflow that forced
  SQL Server to add `DATEDIFF_BIG`. And no system has a `YEARS_BETWEEN`, so fractional years need
  emulating everywhere, against whichever convention is picked.
- **Cost:** the highest of any option, and different in kind. This is a **signature change** to a
  `<<PCT.function>>`, not a semantics change. Every Pure query that assigns the result to an
  `Integer` or uses it in integer arithmetic fails to compile, every Java caller changes from `long`
  to `double`, and every generator is rewritten. On the declaration side it is total: all 70
  assertions change shape and need a tolerance, so the platform retracts and restates everything it
  has ever said about this function at once. **Not recommended standalone**, but see Option 6, where
  the same semantics costs far less.

### Option 6: expose more than one reading

Either sibling functions or an options record:

```
dateDiff(d1, d2, unit)                                      -- Integer, unchanged
dateDiffBoundaries(d1, d2, unit)                            -- Integer, A
dateDiffElapsed(d1, d2, unit)                               -- Integer, C
dateDiffExact(d1, d2, unit, convention)                     -- Float or Decimal, D
```

or, following Temporal and SAS,

```
dateDiff(d1, d2, unit, ^DateDiffOptions(mode=..., rounding=..., weekStart=..., dayCount=...))
```

**Which variants are needed is a much smaller question than it looks**, because the readings are not
independent, and the derivability analysis of axis 1 narrows it to one:

- On WEEKS and below, a single elapsed count in the finest unit gives all of A, C, and D by
  truncating operands or dividing. The platform already provides that, so nothing needs adding.
- On YEARS and MONTHS, **complete units are the only primitive missing**. Boundary counting is
  already there; fractional needs complete units plus a convention; and complete units cannot be
  assembled from boundary counting without hand-written anniversary arithmetic.
- B is not derivable from anything and is not wanted.

So the smallest thing that closes the goal 2 gap is **one function, giving complete years and
months**, rather than a menu. A fractional variant is worth more than a boundary-counting one if a
second is ever added, because it is the one that can carry a day-count convention, but it is not
what users are currently unable to express.

- **Coherence:** each function is individually coherent, nothing is retracted, and no existing
  behavior moves. It is the only option that scores zero on both cost measures, which is a real
  argument in its favor rather than a technicality, and **the only one that serves goal 2 directly**:
  it is the difference between a user being able to compute complete months in the platform and
  having to hand-roll anniversary arithmetic. But the incoherence of the default does not go away;
  it acquires companions. Users must now understand a
  distinction that took this team a lengthy investigation to characterize, and must pick correctly.
  Separate names make the choice visible at the call site; an options record makes it invisible but
  extensible, and is the only shape that can carry the week start of axis 3, the rounding mode of
  axis 2, and the day-count convention of axis 5 without a combinatorial explosion of function
  names. Note that a fractional sibling sidesteps Option 5's fatal objection entirely: the signature
  change stops being a change, because the new signature belongs to a new function.
- **Translatability:** genuinely good, and the fractional variant improves it rather than adding
  load. Each variant maps cleanly onto the dialects that implement it natively: boundary counting to
  `DATEDIFF` on SQL Server and Snowflake, elapsed to `TIMESTAMPDIFF` on MySQL and SingleStore and to
  `TRUNC((d2 - d1) / 7)` on PostgreSQL, and fractional to date subtraction and `MONTHS_BETWEEN` on
  Oracle and Databricks. Oracle and Databricks are currently among the worst-served adapters,
  precisely because they are fractional underneath and we force an integer through them.
- **Cost:** highest ongoing cost. Every new function multiplies across the whole PCT surface: a full
  set of `<<PCT.test>>` functions, plus a `dynaFnToSql` mapping and manifest maintenance for each of
  the twelve adapters. Roughly doubles the date-function translation surface permanently. An options
  record is cheaper per reading but harder to translate, since each combination has to be lowered
  separately or rejected.
- **`adjust` does not need variants**, only documentation: add N units, clamping at month end, with
  the two algebraic laws that clamping costs spelled out. Whether clamping should be configurable is
  a separate and much smaller question.
- **A cheaper middle path:** land the second reading as `<<PCT.platformOnly>>` first, in-memory
  engines only with `<<test.Test>>` tests, and add translation per target only where there is
  demand. That also keeps the new reading out of the declared contract until it has settled.

## Comparison

Coherence and translatability rated best (++) to worst (--). The two cost columns are the ones from
the evaluation criteria: what the platform would have to withdraw from its published statement of
what `dateDiff` does, and how much behavior moves without any statement changing.

| | Rules | Antisymmetric | Additive | Translatability | Declarations retracted | Answers moved | Cost |
|---|---|---|---|---|---|---|---|
| **0** Status quo (A + B + C) | 3 | No (WEEKS) | No (WEEKS) | - (WEEKS broken on 10 of 10) | none | none | none |
| **1a** Hybrid, Sunday grid | 2 | Yes | Calendar units only | + | 3, all self-contradictory | 12.2% of day pairs, WEEKS only | low |
| **1b** Hybrid, Monday grid | 2 | Yes | Calendar units only | + | 2, both defensible | 12.2% of day pairs, WEEKS only | low; also agrees with ISO 8601 and `firstDayOfWeek` |
| **1c** Hybrid, week start supplied | 2 | Yes | Calendar units only | ++ | as its default | as its default | medium; signature change |
| **2** Semantics A uniformly | 1 | Yes | Yes | ++ | 3, all self-contradictory | 47.1% of minute pairs, HOURS | medium to high; almost all of it silent |
| **3** Semantics B uniformly | 1 | **No (all units)** | No | -- | none | large | worst shape on offer |
| **4** Semantics C uniformly | 1 | Yes | No | - overall, + for WEEKS | 3, including the documented headline example | 42.7% of day pairs, plus every unaligned operand | high |
| **5** Semantics D (fractional) | 1 | Yes | Yes | mixed: ++ Oracle and Spark, -- SQL Server and Snowflake | all 70 (**signature change**) | all | highest |
| **6** More than one reading | per variant | per variant | per variant | ++ | none | none | high, ongoing |

**Goal 2 cuts across this table in a way the columns do not show.** What a user can compute in the
platform depends only on which readings are primitive, and on the calendar units complete years and
months are the one reading nothing else yields:

- **Options 0, 1, and 2** all leave the gap exactly where it is today. Boundary counting is provided,
  complete units are not, and no amount of truncation recovers them.
- **Option 4** is the only single-reading option that closes it. With complete units provided,
  boundary counting comes back by truncating the operands, so a user can express both. That is a
  real argument for C that has nothing to do with which answer is more natural, and it is worth
  weighing against the objections above.
- **Option 5** closes it only under the anniversary convention, where truncating the fractional
  result gives complete units by construction. Under Oracle's convention it does not:
  `%2015-01-31 -> %2015-02-28` is exactly 1.0, which truncates to 1, while complete months is 0.
- **Option 6** closes it by design, with one function.

## Recommendation

**Take Option 1b: keep the hybrid, and put WEEKS on a single Monday-based ISO 8601 grid in both
directions.** Option 1 removes the only genuinely indefensible part of the current design, is a code
deletion rather than an addition, improves pushdown, and preserves the calendar and time split that
the future zone story depends on. The grid is the whole of the sub-decision, and it turns on
goal 1.

**ISO 8601 defines the week, and Pure already runs on ISO 8601.** Date literals, the granularity
model, and the lexical representation all follow it. A `dateDiff` that counts weeks from Sunday is a
house convention sitting inside a function whose every other aspect is standardized, and a user who
looks up what a week is will get the wrong answer about Legend. Legend's own `firstDayOfWeek` is
already Monday, so ISO is also the convention that makes the platform agree with itself.

The cost is real and runs the other way:

- **1a (Sunday)** retracts three assertions, every one of which declares an answer that contradicts
  its own forward counterpart. What is withdrawn is an anomaly.
- **1b (Monday)** retracts two, but they are `2015-07-04 -> 2015-07-05 = 1` and
  `2016-01-01 -> 2017-01-01 = 53`, both coherent statements a user could reasonably have built on.

Withdrawing a commitment costs more than withdrawing a mistake, so on that measure alone 1a wins.
Three things outweigh it. First, standards conformance is a goal 1 property and transition cost is
not; the end state should decide a semantics meant to last. Second, **1a probably retracts twice.**
The platform's week convention has to be reconciled across `dateDiff`, `firstDayOfWeek`, and
`weekOfYear` regardless, and the only defensible place for that to land is ISO 8601; taking Sunday
now means changing `dateDiff` again when it does. Third, a retraction is easier to accept when it
has a reason a user can look up, and "weeks now follow ISO 8601, like the rest of Pure's date
handling" is the most explicable reason available. Translatability does not break the tie: a fixed
seven-day grid count is trivially emulatable on any target that can produce a day number, and the
adapter split is close to even either way.

**1a remains an acceptable fallback** if the team judges the two retractions too expensive right
now, but only as part of moving `firstDayOfWeek` to Sunday as well. What should not happen is
settling `dateDiff` in isolation and leaving three week conventions standing. `weekOfYear`'s
dependence on the JVM default locale should be fixed whichever way the decision goes.

**Option 2** remains the strongest choice if translatability is weighted above coherence, and is the
only option that makes the whole function a single sentence with an additive, antisymmetric rule.
But note its cost shape: it retracts the same three anomalies as 1a and then changes 47.1% of HOURS
answers with no declaration moving at all. If it is taken, the time-unit assertions should be
rewritten to state the new rule even where they would still pass, so that the change is visible to
anyone reading what the platform says about the function.

**Option 4** is stronger than it first appears and should still not be taken. Its WEEKS story is much
better than the failure signatures alone suggest, and it is the only single-reading option that
leaves nothing a user cannot compute in the platform, which is goal 2. Against that: it abandons the
calendar and time distinction, it retracts the worked example the published documentation leads with,
and `%2015-12-31T23:59:59 -> %2016-01-01T00:00:01 = 0 years` is a worse answer to give a user than
any of the alternatives. Option 6 gets the same goal 2 benefit without any of that, which is the
better trade.

**Option 5** should not be taken standalone. Its properties on the constant-duration units are
attractive, but it needs a convention for months and years, hides a silent precision cliff at about
104 days for nanosecond differences, and above all changes the signature of a shipped
`<<PCT.function>>`. The same semantics is available at a fraction of the cost as a sibling.

**Option 6 should be taken next, and it is smaller than it looks.** Goal 2 says users must be able to
compute what they need in the platform, and there is exactly one thing they currently cannot:
complete years and months. Everything else, on every other unit, is already reachable by truncating
operands or by dividing an elapsed count in a finer unit. So the whole of the gap closes with **one
function, giving complete calendar units**, not a menu. Order it after the default is coherent,
since a companion to an incoherent default compounds the problem, but do not treat it as
speculative: it is the only option here that adds a capability rather than moving one.

If a third reading is ever wanted, make it the fractional one, which is the natural fit for Oracle
and Databricks and the only one that can carry a day-count convention. Prefer an options record over
a family of names at that point, since the week start of axis 3, the rounding mode of axis 2, and
the day-count convention of axis 5 multiply.

## Consequences

**Positive, under Option 1 or 2:**

- `dateDiff` becomes statable without exceptions, and is antisymmetric everywhere. It becomes
  additive on the calendar units under Option 1, and on every unit under Option 2.
- WEEKS gains a chance of ever working under pushdown, and the `//TODO - fix this, still not right`
  in the Oracle generator can be retired.
- The WEEKS special case disappears from the javadoc, the Pure documentation, and the declarations.
- Under 1b, weeks follow ISO 8601, the standard Pure already uses for dates, and `dateDiff` and
  `firstDayOfWeek` stop disagreeing about when a week starts.

**Negative:**

- Any change to `dateDiff` is a change to a `<<PCT.function>>`, so it must be coordinated with
  legend-engine: `dateDiff.pure` assertions, adapter manifests, and a full PCT run per adapter, some
  of which need live database credentials.
- Published assertions are withdrawn: two under 1b, three under 1a. Users read those to find out
  what Legend supports, so the change needs release notes that say what was retracted and why, not
  just a version bump. Under 1b the two are defensible statements rather than anomalies, which makes
  the standards rationale part of the announcement rather than a footnote to it.
- Users relying on the current WEEKS answers would see a change in 12.2% of ordered day pairs.
  In-memory execution is the only place they could be getting those answers today.
- Shipped Pure functions defined in terms of `dateDiff` move with it and must be re-checked:
  `toEpochValue`, `firstMillisecondOfSecond`, `tdsEquivalent`, and the router's timing
  instrumentation.

## Work to do regardless of which option is chosen

1. **Fix the `dateDiff.pure` documentation.** It currently states, without qualification, that the
   function "counts calendar boundaries crossed, not elapsed time," and advises computing in a finer
   unit and converting if elapsed duration is wanted. Both are wrong for HOURS and below, which
   already measure elapsed time, and the advice does not work under boundary counting either. This is
   the documentation users actually read, and it contradicts the implementation for six of the ten
   units.
2. **Declare the calendar and time split explicitly, with unaligned operands.** As measured above,
   nothing in `dateDiff.pure` distinguishes boundary counting from elapsed time for any time unit,
   so the platform has never actually stated the rule it implements, no PCT run can validate a
   decision taken here, and real adapter divergence goes unrecorded. This is worth doing on its own
   terms: additions cost nothing to accept, and they are what makes the rest of this document
   checkable.
3. **Fix the rounding in the Oracle and Databricks generators** so they truncate as Pure does.
4. **Decide what MICROSECONDS and NANOSECONDS mean under pushdown.** They are accepted in memory and
   translatable nowhere; either add them to the generators or exclude them explicitly.
5. **Fix `weekOfYear`.** Depending on the JVM default locale for a query result is a defect
   independent of everything else in this document, as is its use of the Julian cutover calendar
   while `dateDiff` and `adjust` use proleptic ISO.
6. **Document `adjust`'s algebra**, not only its clamping rule: `(d + 1 month) + 1 month` is not
   `d + 2 months`, and `(d + 1 month) - 1 month` is not `d`.

## Verifying the evidence

- Unit semantics and the diagonal invariant:
  `mvn test -pl legend-pure-core/legend-pure-m4 -Dtest=TestDateDifference -DfailIfNoTests=false`
- Cross-engine contract, both engines:
  `mvn test -pl legend-pure-runtime/legend-pure-runtime-java-engine-interpreted -Dtest=Test_Interpreted_EssentialFunctions_PCT -DfailIfNoTests=false`
  and
  `mvn test -pl legend-pure-runtime/legend-pure-runtime-java-engine-compiled -Dtest=Test_Compiled_EssentialFunctions_PCT -DfailIfNoTests=false`
- Per-adapter support: parse the `exclusions` array of each `EssentialFunctions_manifest.json` under
  `legend-engine/**/src/main/resources/pct-manifests/`, filtering for test names matching
  `test(DateDiff|Adjust)`. Cross-check each exclusion against the generator that produces the SQL,
  since the failure signature alone does not identify the cause.
- Generated SQL per dialect: the `dynaFnToSql('dateDiff', ...)` entry and any
  `generateDateDiffExpressionFor*` function in each `*Extension.pure`.

## References

Legend:

- `legend-pure-m4`: `DateFunctions.dateDifference`, `DateDiff`, `TestDateDifference`
- `legend-pure-m3-core`: `platform/pure/essential/date/operation/dateDiff.pure`, `adjust.pure`,
  `_structures.pure`
- legend-engine: `core/pure/corefunctions/dateExtension.pure` for the truncation helpers,
  `firstDayOfWeek`, and `toEpochValue`; `FunctionsHelper.weekOfYear`;
  `relational/sqlQueryToString/dbExtension.pure` and the per-dialect `*Extension.pure` files
- ADR-003 for the house style of these documents

External:

- ANSI SQL year-month and day-time interval types, and the prohibition on mixing them
- ISO 8601-1:2019 for week dates (Monday-based) and durations
- TC39 Temporal: `until`, `since`, `Duration.total({ unit, relativeTo })`, `largestUnit`,
  `smallestUnit`, and `roundingMode`
- NodaTime `Period.Between(start, end, PeriodUnits)`, and its documentation of the
  non-associativity of period arithmetic
- SAS `INTCK` and `INTNX`: the `DISCRETE` and `CONTINUOUS` methods, and shifted intervals
- BigQuery `DATE_DIFF(..., WEEK(MONDAY))`; Snowflake `WEEK_START`; Tableau `DATEDIFF` with
  `start_of_week`; SQL Server's documented decision that `SET DATEFIRST` does not affect `DATEDIFF`
- Oracle `MONTHS_BETWEEN`; PostgreSQL `age()`; MySQL and SingleStore `TIMESTAMPDIFF`; DB2
  `TIMESTAMPDIFF` and its documented approximation
- Excel `DATEDIF`, `DAYS360`, and `YEARFRAC(..., basis)`; the ISDA and ICMA day count fractions
  (30/360, 30E/360, ACT/360, ACT/365F, ACT/ACT)
- Reingold and Dershowitz, *Calendrical Calculations*, for fixed-day (rank) arithmetic, which is the
  formal basis of semantics A
