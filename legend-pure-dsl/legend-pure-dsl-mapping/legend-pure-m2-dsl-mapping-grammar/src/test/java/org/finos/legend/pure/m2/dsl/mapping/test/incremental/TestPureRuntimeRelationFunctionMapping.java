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
                "    ~func my::firmFunction__Relation_1_\n" +
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
                "    ~func my::firmFunction__Relation_1_\n" +
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
                "native function relationWithQuotedColumn():Relation<('LEGAL NAME':String)>[1];\n";

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
                    "    ~func my::firmFunction__Relation_1_\n" +
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
            assertPureException(PureCompilationException.class, "Mismatching property and relation column types. Property type is String, but relation column it is mapped to has type Integer.", "3.pure", 7, 5, 7, 13, e);
        }
    }

    @Test
    public void testRelationMappingWithInvalidMultiplicityProperty()
    {
        try
        {
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
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Properties in relation mappings can only have multiplicity 1 or 0..1, but the property 'clientNames' has multiplicity [*].", "3.pure", 7, 5, 7, 15, e);
        }
    }

    @Test
    public void testRelationMappingWithNonPrimitiveProperty()
    {
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
            assertPureException(PureCompilationException.class, "Relation mapping is only supported for primitive properties, but the property 'address' has type Address.", "3.pure", 7, 5, 7, 11, e);
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
            assertPureException(PureCompilationException.class, "The system can't find the column FOO in the Relation (FIRSTNAME:String, AGE:Integer, FIRMID:Integer, CITY:String)", "3.pure", 7, 5, 7, 13, e);
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
            assertPureException(PureParserException.class, "expected: '~func' found: 'firstName'", "3.pure", 6, 5, 6, 13, e);
        }
    }

}
