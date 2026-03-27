# Maven Plugins — Real-world Usage Patterns

This document describes how the Legend Pure Maven plugins and their underlying
generator classes are actually invoked across the legend-pure and legend-engine
codebases.

*For plugin parameter reference and configuration details see
[Maven Plugins Reference](maven-plugins-reference.md).*

---

## Usage in legend-pure

Within legend-pure itself the generators are invoked in two distinct ways:
as Maven plugin goals (bound to the standard lifecycle) and as direct JVM
executions via `exec-maven-plugin` (`<goal>java</goal>`).

### Direct exec:java invocations

Several core modules call the generator `main()` methods directly.  This
pattern bypasses the Maven plugin wrapper and calls the underlying generator
with positional command-line arguments:

**`legend-pure-m3-core`** uses all four core generators in a single POM:

```xml
<!-- 1. Generate Java CoreInstance accessors from Pure source -->
<execution>
    <id>Generate Other Support Classes</id>
    <phase>compile</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.finos.legend.pure.m3.generator.bootstrap.M3CoreInstanceGenerator</mainClass>
        <arguments>
            <argument>${project.build.directory}/generated-sources/</argument>
            <argument>M3Platform</argument>
            <argument>/platform/pure/grammar/milestoning.pure, /platform/pure/routing.pure</argument>
        </arguments>
    </configuration>
</execution>

<!-- 2. Compile Pure to binary (compile phase) -->
<execution>
    <id>Compile Pure</id>
    <phase>compile</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.finos.legend.pure.m3.generator.compiler.PureCompilerBinaryGenerator</mainClass>
        <arguments>
            <argument>${project.build.outputDirectory}</argument>  <!-- outputDir -->
            <argument>platform</argument>                          <!-- module name(s) -->
        </arguments>
        <classpathScope>provided</classpathScope>
    </configuration>
</execution>

<!-- 3. Package compiled Pure into a PAR archive -->
<execution>
    <id>Generate PAR</id>
    <phase>compile</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.finos.legend.pure.m3.generator.par.PureJarGenerator</mainClass>
        <arguments>
            <argument>${project.version}</argument>              <!-- purePlatformVersion -->
            <argument>platform</argument>                        <!-- repository name -->
            <argument>${project.build.outputDirectory}</argument><!-- outputDirectory -->
        </arguments>
    </configuration>
</execution>

<!-- 4. Generate PCT function index -->
<execution>
    <id>Generate PCT Essential</id>
    <phase>process-classes</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.finos.legend.pure.m3.pct.functions.generation.FunctionsGeneration</mainClass>
        <arguments>
            <argument>${project.build.directory}/classes/pct-reports/</argument>
            <argument>org.finos.legend.pure.m3.PlatformCodeRepositoryProvider.essentialFunctions</argument>
        </arguments>
    </configuration>
</execution>

<!-- 5. Compile Pure test repositories (test-compile phase) -->
<execution>
    <id>Compile Pure Test Repos</id>
    <phase>test-compile</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>org.finos.legend.pure.m3.generator.compiler.PureCompilerBinaryGenerator</mainClass>
        <arguments>
            <argument>${project.build.testOutputDirectory}</argument>
            <argument>test_generic_repository</argument>
            <argument>other_test_generic_repository</argument>
        </arguments>
        <classpathScope>test</classpathScope>
    </configuration>
</execution>
```

**`legend-pure-runtime-java-engine-compiled`** and the DSL modules
(`legend-pure-dsl-diagram`, `legend-pure-dsl-graph`, etc.) all invoke
`JavaCodeGeneration` directly with three positional arguments:

```xml
<execution>
    <id>GenerateJavaCode</id>
    <phase>generate-test-sources</phase>
    <goals><goal>java</goal></goals>
    <configuration>
        <mainClass>
            org.finos.legend.pure.runtime.java.compiled.generation.orchestrator.JavaCodeGeneration
        </mainClass>
        <arguments>
            <argument>platform</argument>                                           <!-- repository name -->
            <argument>${project.build.directory}/generated-test-resources</argument> <!-- classesDir -->
            <argument>${project.build.directory}</argument>                         <!-- targetDir -->
        </arguments>
        <includeProjectDependencies>true</includeProjectDependencies>
        <includePluginDependencies>true</includePluginDependencies>
    </configuration>
</execution>
```

The optional fourth argument `args[3]` sets `externalAPIPackage` but is not
used in any current exec:java invocation.

### Plugin goal usage in legend-pure

The `compile-pure` goal is declared in root `pluginManagement` and picked up
automatically by most modules.  The only goal-level invocation is in the root
POM itself:

```xml
<goal>compile-pure</goal>  <!-- bound to compile phase via pluginManagement -->
```

---

## Usage in legend-engine (152 modules)

legend-engine uses the Maven plugin goals exclusively — the exec:java pattern
is not used.  The three goals in active use are:

### `build-pure-compiled-jar` — 139 modules

This is by far the most common usage.  Every module that participates in the
compiled execution runtime uses **exactly** this parameter combination:

```xml
<plugin>
    <groupId>org.finos.legend.pure</groupId>
    <artifactId>legend-pure-maven-generation-java</artifactId>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals>
                <goal>build-pure-compiled-jar</goal>
            </goals>
            <configuration>
                <generationType>modular</generationType>
                <useSingleDir>true</useSingleDir>
                <generateSources>true</generateSources>
                <preventJavaCompilation>true</preventJavaCompilation>
                <repositories>
                    <repository>core</repository>   <!-- varies per module -->
                </repositories>
            </configuration>
        </execution>
    </executions>
    <dependencies>
        <!-- module-specific runtime extension jars -->
    </dependencies>
</plugin>
```

The only exception is one module
(`legend-engine-xt-relationalStore-store-entitlement-pure`) which sets
`<preventJavaCompilation>false</preventJavaCompilation>` to actually compile
the generated Java sources to `.class` files rather than just retaining the
`.java` sources.

The practical effect of the standard combination is:
- All generated Java sources and all metadata are written directly into the
  module's `target/classes/` directory (`useSingleDir=true`)
- No separate `metadata-distributed/` directory is created
- The metadata is written at `metadata/classifiers/<repoName>/` and
  `metadata/bin/<repoName>/` within `target/classes/`
- The `.java` sources are retained for debugging but not compiled by this plugin

### `generate-pct-report` — 25 modules

PCT report generation uses both execution modes.  Most modules bind a single
`Compiled` execution, but some bind both `Compiled` and `Interpreted` in the
same POM to produce separate reports:

```xml
<!-- Compiled mode (most common) -->
<execution>
    <id>PCT-Generation</id>
    <phase>process-test-classes</phase>
    <goals>
        <goal>generate-pct-report</goal>
    </goals>
    <!-- No explicit <configuration> — mode and PCTTestSuites are inherited
         from pluginManagement or default to the module's declared suites -->
</execution>

<!-- Where both modes are needed (e.g. scenario/quant modules): -->
<execution>
    <id>PCT-Generation-Compiled</id>
    <phase>process-test-classes</phase>
    <goals><goal>generate-pct-report</goal></goals>
    <configuration>
        <mode>Compiled</mode>
        <targetDir>${project.build.directory}/classes/pct-reports/</targetDir>
        <PCTTestSuites>
            <PCTTestSuite>org.example.Test_Compiled_MyFunctions_PCT</PCTTestSuite>
        </PCTTestSuites>
    </configuration>
</execution>

<execution>
    <id>PCT-Generation-Interpreted</id>
    <phase>process-test-classes</phase>
    <goals><goal>generate-pct-report</goal></goals>
    <configuration>
        <mode>Interpreted</mode>
        <targetDir>${project.build.directory}/classes/pct-reports/</targetDir>
        <PCTTestSuites>
            <PCTTestSuite>org.example.Test_Interpreted_MyFunctions_PCT</PCTTestSuite>
        </PCTTestSuites>
    </configuration>
</execution>
```

### `generate-pct-functions` — 5 modules

Used in modules that define new Pure functions that should be PCT-tracked:

```xml
<execution>
    <id>PCT-Generation</id>
    <phase>process-test-classes</phase>
    <goals>
        <goal>generate-pct-functions</goal>
    </goals>
    <!-- scopeProviderMethod and targetDir resolved from pluginManagement -->
</execution>
```

---

## Argument-position summary for exec:java callers

The positional argument contracts for the generators invoked via exec:java:

| Generator main class | args[0] | args[1] | args[2] | args[3] |
|---|---|---|---|---|
| `JavaCodeGeneration` | repository name | classesDirectory | targetDirectory | externalAPIPackage *(optional)* |
| `PureCompilerBinaryGenerator` | outputDirectory | module name | additional module names... | — |
| `PureJarGenerator` | purePlatformVersion | repository name | outputDirectory | — |
| `M3CoreInstanceGenerator` | outputDirectory | factoryNamePrefix | comma-separated .pure file paths | fileNameStartsWith *(optional)* |
| `FunctionsGeneration` | targetDirectory | scopeProviderMethod | — | — |

These positional contracts are tested in the unit test suite.  Any refactoring
that changes argument order must update both the `main()` implementation and the
corresponding `pom.xml` invocations.

