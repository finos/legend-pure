# Benchmarks, 2026-10-11: five ways of running the same Pure

The same Pure under five modes, on one machine:

1. **Rust native**: the evaluator alone, no host (`crates/evaluator/examples/expressions.rs`,
   `examples/hot_path.rs`).
2. **Rust through JNI**: `legend-pure-runtime-rust-evaluator-jni`, `PureRustEvaluator`.
3. **Rust through the C ABI and FFM**: `legend-pure-runtime-rust-evaluator-ffm`,
   `PureRustFfmEvaluator`.
4. **The Java interpreted engine.**
5. **The Java compiled engine**, through its `start` entry and through the generated method
   called directly.

Tooling at commit `7da9cde` (on `8b3a42c3042`, branch `legend-pure-rust`).

## Machine and build

| | |
|---|---|
| CPU | Apple M4 Max, 16 cores (12 performance, 4 efficiency) |
| Memory | 64 GB |
| OS | macOS 26.6.2 (25G83) |
| JDK | Eclipse Adoptium OpenJDK 64-Bit Server VM 25.0.4.1+1-LTS, aarch64 (as each benchmark JVM printed it), default flags plus `--enable-native-access=ALL-UNNAMED`; one JDK for every Java mode |
| Maven | 3.9.12 |
| Rust | rustc 1.94.0 (4a4ef493e 2026-03-02), LLVM 21.1.8, aarch64-apple-darwin; Cargo's default release profile |
| Libraries | `cargo build --release --workspace`; `-Dlegend.rust.libdir=<worktree>/legend-pure-rust/target/release` |
| Load | not idle: an IDE was running, load average 4.2 to 7.3 over the runs |
| Clock | `System.nanoTime` and `Instant` tick every 41.7 ns |

## Method

**The twelve expressions** (`legend-pure-runtime-rust-evaluator-api/src/test/resources/bench/expressions.pure`,
one zero-argument function each). The timed unit is one call, with the result returned as a value:

| Mode | The call | Tool |
|---|---|---|
| Rust native | `apply_callable` on a zero-argument lambda built once with the builders, inside `push_scope`/`pop_scope` | `examples/expressions.rs` |
| Rust JNI | `PureRustEvaluator.evaluate(function, [])`, the results as raw `CoreInstance[]` | `-ffm` `BenchmarkTransportExpressions` |
| Rust FFM | `PureRustFfmEvaluator.evaluate(function, [])` | same |
| Interpreted | `FunctionExecutionInterpreted.start(function, [])` | same |
| Compiled `start` | `FunctionExecutionCompiled.start(function, [])` | same |
| Compiled generated method | the generated static method, `Method.invoke(null, executionSupport)` | same |

- The node that crosses to Rust is the expression's `ConcreteFunctionDefinition`, the node the
  Java modes run. It is lowered on the first call. Every later call resolves it by path in the
  grown Rust model, so no call in the expression tables re-lowers. The PCT corpus crosses
  lambdas, and on the compiled host those are lowered again on every call (see the corpus table).
- JNI and FFM run over the interpreted engine's graph (`InterpretedHost`). Over the compiled
  engine's graph (`CompiledHost`) they have a separate table.
- Every Java and transport mode runs in one JVM (one surefire fork per run), in this order: the
  interpreted runtime (interpreted, JNI, FFM), then the compiled runtime (compiled `start`, the
  generated method, JNI and FFM over the compiled host).
- Each result is checked once against the expected text, outside the timed region.
- Java modes: 200 warm-up calls per expression and mode, then 1,000 samples. Rust native:
  200 warm-up calls, then 2,000 samples. A sample times a batch of 32 calls and divides by 32,
  so the 41.7 ns tick does not show.
- Reported: the median, p90 and minimum of the samples. Each benchmark ran three times, as three
  JVMs or three processes. The tables give the median of the three medians, and the spread table
  the range of the three medians.
- **Rust split**: the transport's `lastTimings()`, read after each batch outside the timed region.
  It is the last call's lowering and run, timed by Rust in nanoseconds, plus the host reads made
  in each phase. It times one call, so it carries the 41.7 ns tick (its values are multiples of
  about 41.7). The median is over the 1,000 batches. The column "rest" is the call's median minus
  the lowering and the run: the crossing in and out, the host's materialisation and the end of
  execution.

**The PCT corpus**: `-ffm` `BenchmarkPctExpressions` (`PctExpressionsBenchmark`), every
`<<PCT.test>>` of the essential and grammar scopes under six modes. Each engine runs with its own
adapter, with `Rust_JNI` and with `Rust_FFM`.

- A test's time is the whole test function: the adapter call, the lambda under it and its
  assertions.
- Per test, the first call is reported apart; then come the warm-up and the timed samples, with
  the median, p90 and minimum.
- Per mode, the table gives the median over the 472 tests every mode completed.
- Three settings:
  - the tool's defaults (warm-up 5, 20 samples of 1 call: 26 calls per test and mode), three runs;
  - warm-up 20, 50 samples of 4 (221 calls), three runs;
  - warm-up 50, 100 samples of 4 (451 calls), one run.

**Rust native has no corpus number.** `cargo run --release -p legend-pure-evaluator --example pct_native`
prints, for both scopes, `Execution error "no loaded definition of function
meta::pure::test::surveyor::runPCTTestsFromPath_String_1__String_1__String_1__TestReport_1_: the
model holds the bootstrap m3.pure (105 elements) and no metadata for it"`. The surveyor needs the
platform, which Step 3 loads.

## The twelve expressions

### Five modes, nanoseconds per call

Median of three runs. In parentheses: the ratio to the compiled generated method.

| Expression | Rust native | Rust JNI | Rust FFM | Interpreted | Compiled `start` | Compiled generated method |
|---|---:|---:|---:|---:|---:|---:|
| e01 `1 + 2` | 56 (0.72x) | 4,335 (56x) | 2,432 (31x) | 2,800 (36x) | 3,326 (43x) | 78 |
| e02 `(3 * 4) + (10 - 2)` | 154 (31x)† | 4,222 (844x)† | 1,937 (387x)† | 5,320 (1,064x)† | 2,565 (513x)† | 5† |
| e03 `let x = 5; $x * $x` | 127 (42x)† | 4,101 (1,367x)† | 1,847 (616x)† | 3,674 (1,225x)† | 2,304 (768x)† | 3† |
| e04 `if(1 < 2, …)` | 122 (41x)† | 4,213 (1,404x)† | 1,976 (659x)† | 4,690 (1,563x)† | 3,182 (1,061x)† | 3† |
| e05 `map` then `fold` over five | 1,092 (3.2x) | 5,046 (15x) | 2,852 (8.4x) | 23,644 (69x) | 2,598 (7.6x) | 341 |
| e06 `range` of 100, `filter`, `size` | 10,860 (6.4x) | 13,005 (7.7x) | 10,549 (6.2x) | 343,164 (203x) | 4,462 (2.6x) | 1,694 |
| e07 `sort` then `at` | 256 (6.9x) | 4,266 (115x) | 1,957 (53x) | 5,066 (137x) | 2,485 (67x) | 37 |
| e08 `format` | 166 (2.3x) | 4,308 (60x) | 1,934 (27x) | 4,395 (61x) | 2,480 (34x) | 72 |
| e09 `adjust` a date | 85 (1.3x) | 4,260 (64x) | 1,910 (29x) | 3,365 (50x) | 3,182 (48x) | 67 |
| e10 `match` | 248 (83x)† | 4,197 (1,399x)† | 2,015 (672x)† | 2,942 (981x)† | 2,391 (797x)† | 3† |
| e11 `removeDuplicates` then `size` | 365 (17x) | 4,368 (199x) | 2,166 (98x) | 3,234 (147x) | 2,518 (114x) | 22 |
| e12 `joinStrings` | 192 (14x) | 4,394 (314x) | 2,071 (148x) | 2,493 (178x) | 2,734 (195x) | 14 |

† **The generated method of e02, e03, e04 and e10 takes 3 to 5 ns per call.** Its bytecode
builds lists of boxed `Long`s and calls `CompiledSupport.plus`, `times`, `minus` and `lessThan`,
or instantiates the `match` closure. With C2 disabled (`-XX:TieredStopAtLevel=1`, one run, Java
modes only), the same methods take 190, 139, 98 and 134 ns. In those four rows the ratio measures
C2 removing the work of a constant expression, not a cost of evaluation.

### The generated method under C1 only, one run, ns per call

Rust native's ratio to it is in parentheses.

| e01 | e02 | e03 | e04 | e05 | e06 | e07 | e08 | e09 | e10 | e11 | e12 |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 138 (0.41x) | 190 (0.81x) | 139 (0.91x) | 98 (1.24x) | 578 (1.89x) | 5,565 (1.95x) | 230 (1.11x) | 195 (0.85x) | 408 (0.21x) | 134 (1.85x) | 234 (1.56x) | 190 (1.01x) |

### Spread: range of the three medians, ns

| Expression | Rust native | Rust JNI | Rust FFM | Interpreted | Compiled `start` | Generated method |
|---|---|---|---|---|---|---|
| e01 | 56–61 | 4,213–4,414 | 2,242–2,444 | 2,636–2,932 | 3,220–3,563 | 74–108 |
| e02 | 153–162 | 4,175–4,487 | 1,925–1,968 | 4,649–5,717 | 2,468–2,681 | 5–5 |
| e03 | 127–135 | 4,027–4,216 | 1,834–1,877 | 3,631–3,692 | 2,302–2,670 | 3–3 |
| e04 | 122–128 | 4,167–4,414 | 1,971–2,005 | 4,522–4,757 | 2,851–3,825 | 3–3 |
| e05 | 1,089–1,117 | 4,877–5,411 | 2,847–2,908 | 23,548–23,813 | 2,386–2,704 | 335–371 |
| e06 | 10,832–10,867 | 12,925–13,239 | 10,522–10,631 | 331,664–346,243 | 4,272–4,526 | 1,662–1,739 |
| e07 | 250–261 | 4,145–4,515 | 1,925–2,039 | 5,054–5,135 | 2,365–2,708 | 37–37 |
| e08 | 166–167 | 4,208–4,648 | 1,859–2,015 | 4,238–4,651 | 2,412–3,363 | 69–83 |
| e09 | 84–85 | 4,186–4,446 | 1,878–1,957 | 3,356–3,376 | 3,114–3,561 | 66–71 |
| e10 | 244–250 | 4,154–4,377 | 1,981–2,057 | 2,861–3,005 | 2,351–2,703 | 3–3 |
| e11 | 360–373 | 4,335–4,436 | 2,130–2,194 | 3,175–3,287 | 2,447–2,723 | 22–22 |
| e12 | 179–195 | 4,300–4,575 | 2,035–2,121 | 2,476–2,572 | 2,526–2,809 | 14–15 |

Highest over lowest of the three medians, the median across expressions for each mode:

| Mode | Median | Worst expression |
|---|---:|---|
| Rust native | 1.04 | 1.09, e12 |
| JNI | 1.06 | 1.11 |
| FFM | 1.03 | 1.09 |
| Interpreted | 1.04 | 1.23 |
| Compiled `start` | 1.14 | 1.39, e08 |
| Generated method | 1.02 | 1.46, e01 |

### p90 / minimum, ns: median of three runs

| Expression | Rust native | Rust JNI | Rust FFM | Interpreted | Compiled `start` | Generated method |
|---|---|---|---|---|---|---|
| e01 | 70 / 53 | 4,687 / 3,877 | 4,824 / 1,632 | 4,730 / 2,083 | 5,266 / 2,539 | 92 / 65 |
| e02 | 204 / 141 | 4,585 / 3,973 | 2,072 / 1,713 | 9,434 / 3,845 | 3,140 / 2,190 | 74 / 2 |
| e03 | 171 / 119 | 4,546 / 3,856 | 1,937 / 1,647 | 5,386 / 1,846 | 2,565 / 2,180 | 3 / 2 |
| e04 | 147 / 114 | 4,578 / 4,010 | 3,186 / 1,664 | 5,647 / 2,600 | 3,890 / 2,253 | 3 / 2 |
| e05 | 1,295 / 955 | 5,570 / 4,812 | 3,080 / 2,536 | 36,135 / 20,958 | 2,864 / 2,299 | 398 / 325 |
| e06 | 11,287 / 9,759 | 13,459 / 11,901 | 10,966 / 9,486 | 351,859 / 324,053 | 7,147 / 3,753 | 1,796 / 1,492 |
| e07 | 272 / 221 | 4,579 / 4,046 | 2,114 / 1,766 | 5,920 / 4,505 | 2,746 / 2,235 | 39 / 32 |
| e08 | 178 / 140 | 4,634 / 4,096 | 2,067 / 1,751 | 6,966 / 3,950 | 2,729 / 2,222 | 75 / 63 |
| e09 | 88 / 74 | 4,638 / 4,032 | 2,675 / 1,661 | 4,110 / 2,945 | 3,533 / 2,675 | 67 / 58 |
| e10 | 264 / 216 | 4,640 / 4,044 | 2,092 / 1,777 | 3,205 / 2,492 | 2,647 / 2,253 | 5 / 3 |
| e11 | 385 / 317 | 4,750 / 4,148 | 2,320 / 1,924 | 3,558 / 2,941 | 2,897 / 2,231 | 24 / 18 |
| e12 | 220 / 166 | 4,733 / 4,170 | 2,177 / 1,790 | 2,688 / 2,183 | 3,190 / 2,240 | 15 / 13 |

### The Rust split per call, interpreted host, ns: median of three runs

Host reads are 0 in both phases for all twelve, under both transports and on both hosts.

| Expression | Rust native, whole call | JNI lowering / run / rest | FFM lowering / run / rest |
|---|---:|---|---|
| e01 | 56 | 1,083 / 125 / 3,127 | 1,083 / 125 / 1,224 |
| e02 | 154 | 1,042 / 208 / 2,972 | 1,042 / 208 / 687 |
| e03 | 127 | 959 / 167 / 2,975 | 1,042 / 167 / 638 |
| e04 | 122 | 959 / 166 / 3,088 | 1,041 / 167 / 768 |
| e05 | 1,092 | 958 / 1,000 / 3,088 | 1,042 / 1,083 / 727 |
| e06 | 10,860 | 1,042 / 8,833 / 3,130 | 1,042 / 8,708 / 799 |
| e07 | 256 | 958 / 292 / 3,016 | 1,000 / 292 / 665 |
| e08 | 166 | 958 / 208 / 3,142 | 1,000 / 209 / 725 |
| e09 | 85 | 959 / 167 / 3,134 | 1,042 / 167 / 701 |
| e10 | 248 | 959 / 292 / 2,946 | 1,042 / 292 / 681 |
| e11 | 365 | 959 / 375 / 3,034 | 1,000 / 416 / 750 |
| e12 | 192 | 1,000 / 250 / 3,144 | 1,042 / 250 / 779 |

The same over the compiled engine's graph has the same lowering (959 to 1,042) and run. The rest is
3,072 to 3,382 ns for JNI and 736 to 1,052 ns for FFM.

### The transports over the compiled engine's graph, ns per call

Median of three runs, [range], (ratio to the generated method).

| Expression | JNI, compiled host | FFM, compiled host |
|---|---|---|
| e01 | 4,238 [4,190–4,619] (54x) | 1,902 [1,868–1,962] (24x) |
| e02 | 4,368 [4,136–4,763] (874x)† | 2,020 [2,003–2,079] (404x)† |
| e03 | 4,278 [4,156–4,684] (1,426x)† | 1,932 [1,878–2,033] (644x)† |
| e04 | 4,375 [4,279–4,893] (1,458x)† | 2,026 [2,010–2,118] (675x)† |
| e05 | 5,341 [5,113–5,696] (16x) | 3,014 [2,960–3,050] (8.8x) |
| e06 | 13,173 [13,056–13,743] (7.8x) | 10,927 [10,712–11,147] (6.5x) |
| e07 | 4,412 [4,326–4,884] (119x) | 2,160 [2,104–2,197] (58x) |
| e08 | 4,468 [4,277–4,856] (62x) | 2,065 [2,014–2,153] (29x) |
| e09 | 4,464 [4,311–4,867] (67x) | 2,019 [1,977–2,029] (30x) |
| e10 | 4,416 [4,252–4,890] (1,472x)† | 2,162 [2,057–2,231] (721x)† |
| e11 | 4,544 [4,304–4,985] (207x) | 2,321 [2,261–2,345] (106x) |
| e12 | 4,464 [4,424–4,985] (319x) | 2,169 [2,138–2,257] (155x) |

## Rust native, the hot path (`examples/hot_path.rs`)

Release build, three runs. Each line takes 300 warm-up calls, then 1,000 samples of 32 calls.
Medians are the median of three [range]; p90 and minimum are the median of three. The last four
lines are the average of a tight loop.

| Operation | Median ns | p90 | Minimum |
|---|---|---:|---:|
| literal `1` | 13 [13–13] | 26 | 10 |
| `1 + 2` | 56 [56–57] | 119 | 53 |
| `range(0, 100, 1)->size()` | 779 [737–785] | 1,132 | 683 |
| `range->filter(x \| true)->size()` | 2,972 [2,955–3,360] | 3,441 | 2,613 |
| `range->filter(x \| $x->mod(2) == 0)->size()` | 10,756 [10,712–10,850] | 11,173 | 9,779 |
| `range->map(x \| $x)->size()` | 3,283 [3,253–3,285] | 3,437 | 2,911 |
| `range->map(x \| $x * 2)->size()` | 8,091 [7,852–8,093] | 8,470 | 7,261 |
| `[100 literals]->size()` | 1,010 [992–1,033] | 1,049 | 880 |
| `Value::Integer` clone | 2 [2–2] | | |
| `Value::Collection(100)` clone | 12 [11–18] | | |
| `PVector` of 2 from an iterator | 63 [60–65] | | |
| `push_scope` + `set` + `pop_scope` | 7 [7–8] | | |

## The PCT corpus

### Defaults: warm-up 5, 20 samples of 1 call

Median over the 472 tests every mode completed; median of three runs, [range of the three].
Times are µs per test unless marked ns.

| Mode | Tests ok | Median | p90 | Minimum | First run | Geomean ratio to interpreted, own adapter | Rust lowering | Host reads in lowering | Rust run (ns) | Host reads in run |
|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---|
| Rust native | — | no number until Step 3 | | | | | | | | |
| interpreted, own adapter | 483 | 48.1 [46.9–48.3] | 53.8 | 45.2 | 92.3 | 1.00x | | | | |
| interpreted, `Rust_JNI` | 472 | 59.7 [58.8–60.2] | 68.5 | 58.2 | 371.0 | 1.27x | 1.33 | 0 (p90 1, max 4) | 250 | 0 in 451 tests, max 6 |
| interpreted, `Rust_FFM` | 472 | 52.4 [52.1–53.4] | 60.3 | 51.1 | 223.5 | 1.10x | 1.29 | 0 (p90 1, max 4) | 250 | 0 in 451 tests, max 6 |
| compiled, own adapter | 480 | 1.5 [1.4–1.5] | 1.8 | 1.4 | 68.2 | 0.03x | | | | |
| compiled, `Rust_JNI` | 473 | 191.9 [189.7–199.1] | 208.0 | 187.8 | 366.0 | 4.04x | 77.7 | 31 (p90 90, max 249) | 209 | 0 in 451 tests, max 6 |
| compiled, `Rust_FFM` | 473 | 87.8 [86.7–90.2] | 96.9 | 85.5 | 173.0 | 1.86x | 33.4 | 31 (p90 90, max 249) | 208 | 0 in 451 tests, max 6 |

**Overhead per test, median over the tests.**

- Interpreted host, Rust mode minus the engine's own adapter: JNI 10.5 to 10.9 µs, FFM 4.7 to
  4.8 µs.
- Compiled host, test time minus the Rust lowering and run: JNI 76.0 to 79.2 µs, FFM 36.8 to
  38.1 µs.

### Per-test time by the number of calls per test

Median per test, µs. The Rust split stays the same in every row: lowering 1.29 to 1.33 µs on the
interpreted host and 74 to 78 µs (JNI) or 31 to 33 µs (FFM) on the compiled host; run 208 to 250 ns.

| Calls per test and mode | Interpreted, own | Interpreted, `Rust_JNI` | Interpreted, `Rust_FFM` | Compiled, own | Compiled, `Rust_JNI` | Compiled, `Rust_FFM` |
|---|---:|---:|---:|---:|---:|---:|
| 26 (warm-up 5, 20 × 1; 3 runs) | 48.1 | 59.7 | 52.4 | 1.5 | 191.9 | 87.8 |
| 221 (warm-up 20, 50 × 4; 3 runs) | 41.6 [41.4–42.3] | 79.9 [79.1–81.5] | 72.8 [71.7–72.9] | 1.1 | 181.3 | 82.7 |
| 451 (warm-up 50, 100 × 4; 1 run) | 42.5 | 108.8 | 101.8 | 1.1 | 188.8 | 84.3 |

On the interpreted host, both transports add the same amount from 26 to 451 calls: JNI
+49.1 µs, FFM +49.4 µs.

The corpus split into thirds in run order shows the same rise. The table gives the overhead of
the interpreted-host Rust mode over the engine's own adapter, median per third, µs, from the first
run of each setting (the other runs agree within 2.5 µs):

| Calls per test | JNI | FFM |
|---|---|---|
| 26 | 9.0, 11.2, 12.0 | 4.6, 4.9, 5.2 |
| 221 | 22.6, 43.1, 41.7 | 17.6, 31.1, 34.4 |
| 451 | 39.9, 76.1, 77.9 | 36.3, 67.9, 71.2 |

The compiled-host modes stay flat. So does the expression benchmark on the interpreted host
(function nodes, 33,000 calls per expression and mode).

## Reading

**The evaluator's own cost: Rust native against the engines.**

- Against the interpreted engine's `start`, the evaluator is 9x to 50x faster: 8.9x on e11,
  12x on e10, 50x on e01, and 32x on e06 (10.9 µs against 343 µs).
- Against the compiled engine's `start` entry, it is faster on eleven of twelve. The exception
  is e06, at 10.9 µs against 4.5 µs; the entry itself costs 2.3 µs or more per call.
- Against the generated method under C2:
  - e01 runs at 0.72x and e09 at 1.3x;
  - e05, e06, e07 and e08 run at 2.3x to 6.9x;
  - e11 and e12 run at 14x to 17x;
  - for the four expressions C2 reduces to 3 to 5 ns, the ratio is 31x to 83x.
- Against the generated method under C1 only, the evaluator is within 0.21x to 1.95x on all
  twelve.
- In the hot path, one lambda application over `range(0, 100)` costs about 22 ns (`filter(x | true)`
  less the range and size, divided by 100). With `$x->mod(2) == 0` on top it costs about 100 ns
  per element.

**The transport's cost: JNI and FFM against Rust native.**

- With the function node already lowered, each call through a transport costs a fixed amount
  plus the evaluator's run:
  - JNI: about 1.0 µs of lowering and 2.9 to 3.1 µs of rest, 3.9 to 4.3 µs over Rust native;
  - FFM: the same 1.0 µs of lowering and 0.64 to 0.80 µs of rest (1.2 µs on e01, the first
    expression measured), 1.7 to 1.9 µs over Rust native (2.4 µs on e01).
  - e06 is the exception to both "over Rust native" figures: there the run through a transport
    is faster than Rust native (see the run point below).
- FFM is therefore 1.9 to 2.5 µs cheaper per call than JNI. Both transports pay the same
  lowering.
- The lowering of an element already in the model takes about 1.0 µs with no host read. It
  includes the setup of a `Lowering`, which resolves its M3 ids by path on every call
  (`crates/evaluator/src/lower/mod.rs`, `Lowering::new` → `M3Ids::resolve`).
- The run through a transport is the evaluator over the lowered body. That body names natives by
  element where the builders name them by key (Step 2b item 4).
  - e06: the run through a transport is 8.7 to 8.8 µs, against 10.9 µs for Rust native.
  - The sub-microsecond runs read up to one clock tick high, because the split times a single call.
- Through FFM, every expression is faster than the interpreted engine's `start`. Ten of twelve
  are faster than the compiled engine's `start` (all but e05 and e06).
- Through JNI, six of twelve are slower than the interpreted engine's `start` (e01, e03, e09, e10,
  e11, e12).
- In the corpus on the interpreted host, where the closure cache holds, a test through the Rust
  transports costs this much more than through the engine's own adapter (defaults setting):
  - `Rust_JNI`: 10.5 to 10.9 µs more (1.27x);
  - `Rust_FFM`: 4.7 to 4.8 µs more (1.10x).
- The Rust run inside those tests is 250 ns, with no host read in 451 of 472 tests.

**What Step 3 will change: the per-call lowering on the compiled host, and the host reads.**

- On the compiled host, every PCT call lowers the test's lambda again, because `CompiledHost`
  names no lambda definition. That costs:
  - 77.7 µs through JNI, 40% of the test's 191.9 µs;
  - 33.4 µs through FFM, 38% of 87.8 µs;
  - in both cases a median of 31 host reads (p90 90, maximum 249) during the lowering.
- On the interpreted host the lowering reads nothing (median 0, p90 1). During the run, all
  modes make 0 host reads in 451 of 472 tests.
- The rest of a compiled-host corpus test, outside the lowering and the run, is 76 to 79 µs
  (JNI) and 37 to 38 µs (FFM). An expression's function node costs 3.1 to 3.4 µs and 0.7 to
  1.1 µs on the same host. That rest is the crossing of a compiled lambda and its results.
- Step 3 loads the platform's metadata on the Rust side. Then the static lowering leaves the
  per-call path, and a test crosses for its arguments and results only (plan, Step 2c).
- The fixed 1.0 to 1.3 µs of lowering setup per call, with no host read, is not a host read.
  It stays until the lowering's per-call setup changes.

**Not explained by these measurements.** The interpreted-host Rust modes slow down as calls
accumulate in a JVM: +49 µs per test from 26 to 451 calls per test, the same in both transports.

- What stays flat: the Rust split, the compiled-host modes, and the expression benchmark's
  interpreted-host modes (function nodes, not lambdas).
- So the rise is outside lowering and run, in what both transports share for a lambda on the
  interpreted host.
- `HostShared::end_execution` (`crates/evaluator/src/bridge/host.rs`) walks every live host node
  on every call. The live node count was not measured.

## Reproduce

From the repository root, with JDK 25 for every Java run and no Maven run during a cargo build.

```bash
# the machine
sysctl -n machdep.cpu.brand_string; sysctl -n hw.memsize; sw_vers; rustc -Vv; uptime

# the libraries (debug as well: the -jni/-ffm skip profiles key on target/debug)
cd legend-pure-rust && cargo build --workspace && cargo build --release --workspace
mkdir -p /tmp/bench

# Rust native: the twelve expressions and the hot path, three runs each
for i in 1 2 3; do
  cargo run -q --release -p legend-pure-evaluator --example expressions > /tmp/bench/expressions-rust-$i.txt
  cp target/benchmarks/expressions-rust.csv /tmp/bench/expressions-rust-$i.csv
  cargo run -q --release -p legend-pure-evaluator --example hot_path > /tmp/bench/hot_path-$i.txt
done
# Rust native on the corpus (stops at the surveyor until Step 3)
cargo run -q --release -p legend-pure-evaluator --example pct_native
cd ..

# the twelve expressions under every Java mode and both transports, one JVM per run, three runs
M=legend-pure-runtime/legend-pure-runtime-rust-evaluator-api,legend-pure-runtime/legend-pure-runtime-rust-evaluator-jni,legend-pure-runtime/legend-pure-runtime-rust-evaluator-ffm
for i in 1 2 3; do
  JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -pl $M test -Dtest=BenchmarkTransportExpressions \
    -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false \
    -Dlegend.rust.libdir=$PWD/legend-pure-rust/target/release
  cp legend-pure-runtime/legend-pure-runtime-rust-evaluator-ffm/target/benchmarks/expressions-java.csv /tmp/bench/expressions-java-$i.csv
done

# median of the three runs and the range, per expression and mode; c=3 median_ns, 4 p90_ns, 5 min_ns,
# 6 rust_lower_ns, 7 rust_run_ns, 8 rust_reads_in_run, 9 rust_reads_in_lowering
cat /tmp/bench/expressions-rust-[123].csv /tmp/bench/expressions-java-[123].csv | awk -F, -v c=3 '
  $1 != "expression" { k = $1 "," $2; n[k]++; v[k, n[k]] = $c + 0 }
  END { for (k in n) { m = n[k]; for (i = 1; i <= m; i++) a[i] = v[k, i];
        for (i = 1; i <= m; i++) for (j = i + 1; j <= m; j++) if (a[j] < a[i]) { t = a[i]; a[i] = a[j]; a[j] = t }
        print k "," a[int((m + 1) / 2)] "," a[1] "," a[m] } }' | sort

# the generated method under C1 only (Java modes only, one run)
JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -pl legend-pure-runtime/legend-pure-runtime-rust-evaluator-api \
  test -Dtest=BenchmarkNativeExpressions -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false \
  "-DargLine=-XX:TieredStopAtLevel=1"

# the PCT corpus, six modes; the summary is target/benchmarks/pct-expressions.md, per test the .csv
for i in 1 2 3; do
  JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q -pl $M test \
    -Dtest=org.finos.legend.pure.rust.ffm.pct.BenchmarkPctExpressions \
    -Dsurefire.failIfNoSpecifiedTests=false -DfailIfNoTests=false \
    -Dlegend.rust.libdir=$PWD/legend-pure-rust/target/release
  cp legend-pure-runtime/legend-pure-runtime-rust-evaluator-ffm/target/benchmarks/pct-expressions.md /tmp/bench/pct-defaults-$i.md
  cp legend-pure-runtime/legend-pure-runtime-rust-evaluator-ffm/target/benchmarks/pct-expressions.csv /tmp/bench/pct-defaults-$i.csv
done
# 221 calls per test: add -Dlegend.bench.warmup=20 -Dlegend.bench.runs=50 -Dlegend.bench.batch=4
# 451 calls per test: add -Dlegend.bench.warmup=50 -Dlegend.bench.runs=100 -Dlegend.bench.batch=4
```

Where each table's numbers come from:

- **The expression tables** are the awk output for `c=3`, 4 and 5.
- **The split** is `c=6` and 7, and the "rest" is `c=3` minus `c=6` minus `c=7`. Each run also
  prints its table and split to the console.
- **The corpus table** is the "median of medians", "median of p90s", "median of minimums",
  "median first run", "geomean ratio", "Rust lowering" and "Rust run" columns of the three
  `pct-expressions.md` files.
- **The corpus read counts and overheads** are medians over the 472 rows every mode completed in
  `pct-expressions.csv`:
  - `rust_reads_in_lowering` and `rust_reads_in_run`;
  - a Rust mode's `median_ns` less the same engine's own adapter, on the interpreted host;
  - `median_ns` less `rust_lower_ns` less `rust_run_ns`, on the compiled host.
- **The thirds** take those 472 rows in the CSV's order and split them into three equal parts.
