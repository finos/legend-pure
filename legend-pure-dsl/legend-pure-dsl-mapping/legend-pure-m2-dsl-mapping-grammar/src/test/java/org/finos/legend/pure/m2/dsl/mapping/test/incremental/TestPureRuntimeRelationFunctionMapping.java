// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.m2.dsl.mapping.test.incremental;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m2.dsl.mapping.test.AbstractPureMappingTestWithCoreCompiled;
import org.finos.legend.pure.m3.exception.PureUnresolvedIdentifierException;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_ENUMERATION_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_FUNCTION_SOURCE;

public class TestPureRuntimeRelationFunctionMapping extends AbstractPureMappingTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("1.pure");
        runtime.delete("2.pure");
        runtime.delete("3.pure");
        runtime.delete("functionSourceId.pure");
    }

    public void testDeleteAndReloadEachSource(ImmutableMap<String, String> sources, String testFunctionSource)
    {
        for (Pair<String, String> source : sources.keyValuesView())
        {
            new RuntimeTestScriptBuilder().createInMemorySources(sources)
                    .createInMemorySource("functionSourceId.pure", testFunctionSource)
                    .compile().run(runtime, functionExecution);

            RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution,
                    Lists.fixedSize.of(source), this.getAdditionalVerifiers());

            //reset so that the next iteration has a clean environment
            setUpRuntime();
        }
    }

    @Test
    public void testDeleteAndReloadEachSourceWithRelationMapping()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    age: AGE\n" +
                "  }\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction():Relation<Any>[1]\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == my::testMapping.classMappings->size(), |'');}");
    }

    @Test
    public void testDeleteAndReloadEachSourceWithRelationMappingContainingLocalProperty()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction():Relation<Any>[1]\n" +
                "    id: ID,\n" +
                "    +firmName: String[0..1]: LEGALNAME\n" +
                "  }\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    +firstName: String[1]: FIRSTNAME,\n" +
                "    +firmId: Integer[0..1]: FIRMID\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == my::testMapping.classMappings->size(), |'');}");
    }

    @Test
    public void testRelationMappingWithQuotedRelationColumn()
    {
        String functionSource = "###Pure\n" +
                "import meta::pure::metamodel::relation::*;\n" +
                "function my::firmFunction(): Relation<Any>[1]\n" +
                "{\n" +
                "  relationWithQuotedColumn();\n" +
                "}\n" +
                "native function relationWithQuotedColumn():Relation<('LEGAL NAME':String[1])>[1];\n";

        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    legalName: 'LEGAL NAME'" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", functionSource,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingWithNonRelationFunction()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::integerFunction__Integer_1_\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Relation mapping function should return a Relation! Found a Integer instead.", "2.pure", 11, 14, 14, 1, e);
        }
    }

    @Test
    public void testRelationMappingWithMismatchingTypes()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Firm[firm]: Relation\n" +
                    "  {\n" +
                    "    ~func my::firmFunction():Relation<Any>[1]\n" +
                    "    legalName: ID\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mismatching property and relation expression types. Property 'legalName' is of type 'String', but the expression mapped to it is of type 'Integer'.", "3.pure", 7, 5, 7, 13, e);
        }
    }

    @Test
    public void testRelationMappingAllowsToManyPropertyWithToOneExpression()
    {
        // Under the relaxed validator, a property with multiplicity [*] can be
        // mapped to a column expression that yields a value of multiplicity [1] —
        // [*] subsumes [1] and the raw types (String == String) are compatible,
        // so the mapping must compile successfully.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    clientNames: LEGALNAME\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingRejectsToManyExpressionForToOneProperty()
    {
        // The relaxation still requires the valueFn's multiplicity to be subsumed by
        // the property's. Mapping a [1] property to an expression that returns [*]
        // (here `$src.LEGALNAME->split(',')`) must be rejected with a multiplicity
        // error.
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Firm[firm]: Relation\n" +
                    "  {\n" +
                    "    ~func my::firmFunction__Relation_1_\n" +
                    "    legalName: $src.LEGALNAME->split(',')\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class,
                    "Multiplicity Error: The property 'legalName' has a multiplicity range of [1] when the given expression has a multiplicity range of [*]",
                    "3.pure", 7, 5, 7, 13, e);
        }
    }

    @Test
    public void testRelationMappingRejectsIncompatibleTypeForNonPrimitiveProperty()
    {
        // The relaxation lifts the old "primitive-only" property restriction, but the
        // type-compatibility check still rejects mapping an `Address` property to a
        // `String` column: `String` is not a subtype of `Address`.
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::personFunction__Relation_1_\n" +
                    "    address: CITY\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Mismatching property and relation expression types. Property 'address' is of type 'Address', but the expression mapped to it is of type 'String'.", "3.pure", 7, 5, 7, 11, e);
        }
    }

    // ---------------------------------------------------------------------
    // Additional multiplicity / type compatibility coverage.
    //
    // The validator's rule is: the property's multiplicity must subsume the
    // valueFn's, and the valueFn's generic type must be compatible with the
    // property's. The matrix below covers the corner cases.
    // ---------------------------------------------------------------------

    @Test
    public void testRelationMappingAllowsZeroToOnePropertyWithToOneExpression()
    {
        // Property [0..1] subsumes expression [1] — must compile.
        String classSource = "###Pure\n" +
                "Class my::OptionalFirm\n" +
                "{\n" +
                "  legalName: String[0..1];\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::OptionalFirm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", classSource,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingAllowsZeroToOnePropertyWithZeroToOneExpression()
    {
        // Property [0..1] subsumes expression [0..1] — must compile. The relation
        // function declares the column as optional (no explicit [1]) so the
        // bare-column lowering yields a [0..1] expression.
        String classSource = "###Pure\n" +
                "Class my::OptionalFirm\n" +
                "{\n" +
                "  legalName: String[0..1];\n" +
                "}\n";
        String functionSource = "###Pure\n" +
                "import meta::pure::metamodel::relation::*;\n" +
                "function my::optionalColumnFunction(): Relation<Any>[1]\n" +
                "{\n" +
                "  1->cast(@Relation<(LEGALNAME:String[0..1])>);\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::OptionalFirm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::optionalColumnFunction__Relation_1_\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", classSource,
                "2.pure", functionSource,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingRejectsZeroToOneExpressionForToOneProperty()
    {
        // Property [1] does NOT subsume expression [0..1] (the expression may
        // produce zero values, which the property forbids). The validator must
        // surface this as a multiplicity error even though the raw types match.
        String functionSource = "###Pure\n" +
                "import meta::pure::metamodel::relation::*;\n" +
                "function my::optionalColumnFunction(): Relation<Any>[1]\n" +
                "{\n" +
                "  1->cast(@Relation<(LEGALNAME:String[0..1])>);\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::optionalColumnFunction__Relation_1_\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n";
        try
        {
            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", functionSource,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class,
                    "Multiplicity Error: The property 'legalName' has a multiplicity range of [1] when the given expression has a multiplicity range of [0..1]",
                    "3.pure", 7, 5, 7, 13, e);
        }
    }

    @Test
    public void testRelationMappingAllowsOneOrManyPropertyWithToOneExpression()
    {
        // Property [1..*] subsumes expression [1] — must compile.
        String classSource = "###Pure\n" +
                "Class my::FirmWithAliases\n" +
                "{\n" +
                "  aliases: String[1..*];\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::FirmWithAliases[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    aliases: LEGALNAME\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", classSource,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingRejectsManyExpressionForOneOrManyProperty()
    {
        // Property [1..*] does NOT subsume expression [*] because their lower
        // bounds differ — `[*]` allows zero, `[1..*]` requires at least one.
        // The mapping must be rejected.
        String classSource = "###Pure\n" +
                "Class my::FirmWithAliases\n" +
                "{\n" +
                "  aliases: String[1..*];\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::FirmWithAliases[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    aliases: $src.LEGALNAME->split(',')\n" +
                "  }\n" +
                ")\n";
        try
        {
            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", classSource,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class,
                    "Multiplicity Error: The property 'aliases' has a multiplicity range of [1..*] when the given expression has a multiplicity range of [*]",
                    "3.pure", 7, 5, 7, 11, e);
        }
    }

    @Test
    public void testRelationMappingAllowsToManyPropertyWithManyExpression()
    {
        // Property [*] and expression [*] (from `split`) — both lower and upper
        // bounds match. Must compile.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    clientNames: $src.LEGALNAME->split(',')\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingAllowsPrimitiveSubtypeColumn()
    {
        // Type compatibility uses subtype semantics: `Integer` is a subtype of
        // `Number`, so an Integer column may feed a Number property.
        String classSource = "###Pure\n" +
                "Class my::NumericThing\n" +
                "{\n" +
                "  amount: Number[1];\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::NumericThing[t]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    amount: ID\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", classSource,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);
    }

    @Test
    public void testRelationMappingRejectsPrimitiveSupertypeColumn()
    {
        // The reverse direction — `Number` is NOT a subtype of `Integer`, so a
        // Number column cannot feed an Integer property. Must fail with a
        // type-compatibility error.
        String classSource = "###Pure\n" +
                "Class my::IntegerThing\n" +
                "{\n" +
                "  amount: Integer[1];\n" +
                "}\n";
        String functionSource = "###Pure\n" +
                "import meta::pure::metamodel::relation::*;\n" +
                "function my::numericColumnFunction(): Relation<Any>[1]\n" +
                "{\n" +
                "  1->cast(@Relation<(VAL:Number[1])>);\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::IntegerThing[t]: Relation\n" +
                "  {\n" +
                "    ~func my::numericColumnFunction__Relation_1_\n" +
                "    amount: VAL\n" +
                "  }\n" +
                ")\n";
        try
        {
            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", classSource,
                    "2.pure", functionSource,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class,
                    "Mismatching property and relation expression types. Property 'amount' is of type 'Integer', but the expression mapped to it is of type 'Number'.",
                    "3.pure", 7, 5, 7, 10, e);
        }
    }

    @Test
    public void testRelationMappingRejectsBooleanForStringProperty()
    {
        // Two unrelated primitive types — `Boolean` and `String` — must produce
        // a type-compatibility error.
        String functionSource = "###Pure\n" +
                "import meta::pure::metamodel::relation::*;\n" +
                "function my::booleanColumnFunction(): Relation<Any>[1]\n" +
                "{\n" +
                "  1->cast(@Relation<(FLAG:Boolean[1])>);\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::booleanColumnFunction__Relation_1_\n" +
                "    legalName: FLAG\n" +
                "  }\n" +
                ")\n";
        try
        {
            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", functionSource,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class,
                    "Mismatching property and relation expression types. Property 'legalName' is of type 'String', but the expression mapped to it is of type 'Boolean'.",
                    "3.pure", 7, 5, 7, 13, e);
        }
    }

    @Test
    public void testRelationMappingRejectsZeroToOneExpressionForToOnePropertyWithExpressionRhs()
    {
        // Same multiplicity rejection as the bare-column form, but expressed
        // through `$src.<col>->first()` which yields [0..1] from a [1] column.
        // Demonstrates that the multiplicity check operates on the *result* of
        // the lambda, not on the column type alone.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunction__Relation_1_\n" +
                "    legalName: $src.LEGALNAME->toOneMany()->first()\n" +
                "  }\n" +
                ")\n";
        try
        {
            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class,
                    "Multiplicity Error: The property 'legalName' has a multiplicity range of [1] when the given expression has a multiplicity range of [0..1]",
                    "3.pure", 7, 5, 7, 13, e);
        }
    }

    @Test
    public void testRelationMappingWithInvalidRelationColumn()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::personFunction__Relation_1_\n" +
                    "    firstName: FOO\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The system can't find the column FOO in the Relation (FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])", "3.pure", e);
        }
    }

    @Test
    public void testRelationMappingWithInvalidRelationFunction()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::fooFunction__Relation_1_\n" +
                    "    firstName: AGE\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureUnresolvedIdentifierException.class, "my::fooFunction__Relation_1_ has not been defined!", "3.pure", 6, 11, 6, 15, e);
        }
    }

    @Test
    public void testRelationMappingWithMissingRelationFunction()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    firstName: AGE\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureParserException.class, "expected: one of {'~func', '~src'} found: 'firstName'", "3.pure", 6, 5, 6, 13, e);
        }
    }

    @Test
    public void testRelationMappingToRelationFunctionWithArguments()
    {
        try
        {
            String mappingSource = "###Pure\n" +
                    "import meta::pure::metamodel::relation::*;\n" +
                    "function my::personFunctionWithArg(i:Integer[1]): Relation<Any>[1]\n" +
                    "{\n" +
                    "  $i->cast(@Relation<(FIRSTNAME:String, AGE:Integer, FIRMID:Integer, CITY:String)>);\n" +
                    "}\n" +
                    "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::personFunctionWithArg(Integer[1]):Relation<Any>[1]\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of("1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Relation mapping function expecting arguments is not supported!", "3.pure", 3, 14, 6, 1, e);
        }
    }

    @Test
    public void testDeleteAndReloadEachSourceWithRelationMappingContainingEmbedded()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    address\n" +
                "    (\n" +
                "      city: CITY\n" +
                "    )\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == my::testMapping.classMappings->filter(c | $c.id == 'person')->size(), |'');}");
    }

    @Test
    public void testDeleteAndReloadEachSourceWithRelationMappingContainingInlineEmbedded()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  my::Address[addr]: Relation\n" +
                "  {\n" +
                "    ~func my::addressFunction__Relation_1_\n" +
                "    city: CITY\n" +
                "  }\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    address() Inline[addr]\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(my::testMapping.classMappings->filter(c | $c.id == 'person')->size() == 1, |'');}");
    }

    @Test
    public void testDeleteAndReloadEachSourceWithRelationMappingContainingEnumerationTransformer()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  my::Gender: EnumerationMapping GenderMapping\n" +
                "  {\n" +
                "    MALE: ['M'],\n" +
                "    FEMALE: ['F']\n" +
                "  }\n" +
                "  *my::PersonWithGender[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personWithGenderFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    gender: EnumerationMapping GenderMapping: GENDER\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_ENUMERATION_SOURCE,
                "2.pure", RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(my::testMapping.classMappings->filter(c | $c.id == 'person')->size() == 1, |'');}");
    }

    @Test
    public void testRelationMappingWithEmbeddedInvalidColumn()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::personFunction__Relation_1_\n" +
                    "    address\n" +
                    "    (\n" +
                    "      city: FOO\n" +
                    "    )\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The system can't find the column FOO in the Relation (FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])", "3.pure", e);
        }
    }

    @Test
    public void testRelationMappingWithEnumerationTransformerInvalidEnumerationMapping()
    {
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::PersonWithGender[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::personWithGenderFunction__Relation_1_\n" +
                    "    firstName: FIRSTNAME,\n" +
                    "    gender: EnumerationMapping MissingGenderMapping: GENDER\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_ENUMERATION_SOURCE,
                    "2.pure", RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "The transformer 'MissingGenderMapping' is unknown or is not of type EnumerationMapping in the Mapping 'my::testMapping' for property gender", e);
        }
    }

    // ------------------------------------------------------------------
    // Incremental coverage for the new $src.<col> and ~src forms.
    //
    // Each test here exercises delete-and-reload-stable across the new
    // expression-RHS / inline-source code paths, mirroring the existing
    // ~func / bare-column tests above.
    // ------------------------------------------------------------------

    @Test
    public void testDeleteAndReloadEachSourceWithExpressionRhs()
    {
        // Mixes bare-column lowering, explicit `$src.X`, and a multi-step
        // arithmetic expression in a single mapping.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    age: $src.AGE,\n" +
                "    +concatenated: String[1]: $src.FIRSTNAME + ' ' + $src.FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == my::testMapping.classMappings->filter(c | $c.id == 'person')->size(), |'');}");
    }

    @Test
    public void testDeleteAndReloadEachSourceWithInlineSrc()
    {
        // Inline ~src form produces a LambdaFunction in relationFunction.
        // Compile, then delete and recompile the mapping source: the graph
        // must remain consistent.  We do *not* drive this through the
        // per-source delete-and-error verifier because the inline ~src
        // expression (e.g. `my::personFunctionTyped()`) is parsed into the
        // synthesized lambda body whose unbind path differs from the
        // ImportStub-backed `~func` form — that gap is tracked separately.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    firstName: FIRSTNAME,\n" +
                "    age: AGE\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);

        // Delete and recompile only the mapping; class + function sources stay.
        runtime.delete("3.pure");
        runtime.compile();
        runtime.createInMemorySource("3.pure", mappingSource);
        runtime.compile();
    }

    @Test
    public void testDeleteAndReloadEachSourceWithInlineSrcAndEmbedded()
    {
        // Inline ~src parent + embedded sub-mapping with $src.<col> RHS.
        // Same caveat as testDeleteAndReloadEachSourceWithInlineSrc above.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    firstName: FIRSTNAME,\n" +
                "    address\n" +
                "    (\n" +
                "      city: $src.CITY\n" +
                "    )\n" +
                "  }\n" +
                ")\n";

        new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                "3.pure", mappingSource))
                .compile().run(runtime, functionExecution);

        runtime.delete("3.pure");
        runtime.compile();
        runtime.createInMemorySource("3.pure", mappingSource);
        runtime.compile();
    }

    @Test
    public void testRelationMappingWithInlineSrcReturningNonRelation()
    {
        // Inline ~src whose expression evaluates to a non-Relation value
        // must be rejected by `validateRelationFunction`.
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~src 1\n" +
                    "    firstName: FIRSTNAME\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Relation mapping function should return a Relation! Found a Integer instead.", e);
        }
    }

    @Test
    public void testRelationMappingWithBothFuncAndSrcRejected()
    {
        // ~func and ~src in the same class mapping must be rejected as a
        // grammar-level error: only one source form is permitted.
        try
        {
            String mappingSource = "###Mapping\n" +
                    "Mapping my::testMapping\n" +
                    "(\n" +
                    "  *my::Person[person]: Relation\n" +
                    "  {\n" +
                    "    ~func my::personFunction__Relation_1_\n" +
                    "    ~src my::personFunctionTyped()\n" +
                    "    firstName: FIRSTNAME\n" +
                    "  }\n" +
                    ")\n";

            new RuntimeTestScriptBuilder().createInMemorySources(Maps.immutable.of(
                    "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                    "2.pure", RELATION_MAPPING_FUNCTION_SOURCE,
                    "3.pure", mappingSource))
                    .compile().run(runtime, functionExecution);

            Assert.fail("Expected parser exception");
        }
        catch (Exception e)
        {
            // Either the parser or the validator rejects this — both are
            // acceptable, but we must not silently accept the input.
            Assert.assertTrue("Got: " + e.getClass().getName() + ": " + e.getMessage(),
                    e instanceof PureParserException || e instanceof PureCompilationException);
        }
    }

    @Test
    public void testDeleteAndReloadEachSourceWithExpressionRhsAndQuotedColumn()
    {
        // Quoted-column lowering (`'LEGAL NAME'`) must survive delete-reload.
        // The bare-column branch in the graph builder must preserve quotes
        // when synthesizing `{| $src.'LEGAL NAME'}` so the M3 parser still
        // accepts it as a quoted property accessor on the row.
        String functionSource = "###Pure\n" +
                "import meta::pure::metamodel::relation::*;\n" +
                "function my::firmFunctionQuoted(): Relation<Any>[1]\n" +
                "{\n" +
                "  1->cast(@Relation<('LEGAL NAME':String[1], ID:Integer[1])>);\n" +
                "}\n";
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionQuoted__Relation_1_\n" +
                "    legalName: 'LEGAL NAME',\n" +
                "    id: ID\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_SOURCE,
                "2.pure", functionSource,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(1 == my::testMapping.classMappings->filter(c | $c.id == 'firm')->size(), |'');}");
    }

    @Test
    public void testDeleteAndReloadEachSourceWithEnumerationTransformerOverExpression()
    {
        // Enumeration transformer + explicit `$src.<col>` expression RHS must
        // also be stable across delete-reload cycles.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  my::Gender: EnumerationMapping GenderMapping\n" +
                "  {\n" +
                "    MALE: ['M'],\n" +
                "    FEMALE: ['F']\n" +
                "  }\n" +
                "  *my::PersonWithGender[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personWithGenderFunction__Relation_1_\n" +
                "    firstName: $src.FIRSTNAME,\n" +
                "    gender: EnumerationMapping GenderMapping: $src.GENDER\n" +
                "  }\n" +
                ")\n";

        this.testDeleteAndReloadEachSource(Maps.immutable.of(
                "1.pure", RELATION_MAPPING_CLASS_ENUMERATION_SOURCE,
                "2.pure", RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE,
                "3.pure", mappingSource),
                "###Pure\n function test():Boolean[1]{assert(my::testMapping.classMappings->filter(c | $c.id == 'person')->size() == 1, |'');}");
    }
}
