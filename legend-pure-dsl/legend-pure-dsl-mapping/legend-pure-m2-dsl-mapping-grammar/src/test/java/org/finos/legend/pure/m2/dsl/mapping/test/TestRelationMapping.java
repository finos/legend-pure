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

package org.finos.legend.pure.m2.dsl.mapping.test;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.modelJoin.ModelJoinPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_ENUMERATION_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_CLASS_SOURCE;
import static org.finos.legend.pure.m2.dsl.mapping.test.RelationMappingShared.RELATION_MAPPING_FUNCTION_SOURCE;

public class TestRelationMapping extends AbstractPureTestWithCoreCompiled
{
    @Before
    public void _setUp()
    {
        setUpRuntime();
    }

    @After
    public void _tearDown()
    {
        tearDownRuntime();
    }

    @Test
    public void testRelationMapping()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +age: Integer[0..1]: AGE\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );

        RichIterable<? extends SetImplementation> setImpls = ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings();
        Assert.assertEquals(1, setImpls.size());

        Assert.assertTrue(setImpls.getOnly() instanceof RelationFunctionInstanceSetImplementation);
        RelationFunctionInstanceSetImplementation relationSetImpl = (RelationFunctionInstanceSetImplementation) setImpls.getOnly();
        Assert.assertEquals("person", relationSetImpl._id());
        Assert.assertTrue(relationSetImpl._root());

        FunctionDefinition<?> relationFunction = relationSetImpl._relationFunction();
        Assert.assertEquals("personFunction", relationFunction._functionName());
        Type lastExpressionType = relationFunction._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType();
        Assert.assertTrue(lastExpressionType instanceof RelationType);

        RichIterable<? extends PropertyMapping> propertyMappings = relationSetImpl._propertyMappings();
        Assert.assertEquals(2, propertyMappings.size());
        propertyMappings.each(r -> Assert.assertTrue(r instanceof RelationFunctionPropertyMapping));

        RelationFunctionPropertyMapping propertyMapping1 = (RelationFunctionPropertyMapping) propertyMappings.toList().get(0);
        Assert.assertFalse(propertyMapping1._localMappingProperty());
        Assert.assertEquals("person", propertyMapping1._sourceSetImplementationId());
        Assert.assertEquals("String", propertyMapping1._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());

        RelationFunctionPropertyMapping propertyMapping2 = (RelationFunctionPropertyMapping) propertyMappings.toList().get(1);
        Assert.assertTrue(propertyMapping2._localMappingProperty());
        Assert.assertEquals("person", propertyMapping2._sourceSetImplementationId());
        Assert.assertEquals("Integer", propertyMapping2._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingWithEmbeddedPropertyMapping()
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

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        Mapping mapping = (Mapping) runtime.getCoreInstance("my::testMapping");
        RelationFunctionInstanceSetImplementation relationSetImpl = (RelationFunctionInstanceSetImplementation) mapping._classMappings().detect(s -> "person".equals(s._id()));
        RichIterable<? extends PropertyMapping> propertyMappings = relationSetImpl._propertyMappings();
        Assert.assertEquals(2, propertyMappings.size());

        PropertyMapping addressMapping = propertyMappings.toList().get(1);
        Assert.assertTrue(addressMapping instanceof EmbeddedRelationFunctionSetImplementation);
        EmbeddedRelationFunctionSetImplementation embedded = (EmbeddedRelationFunctionSetImplementation) addressMapping;
        Assert.assertEquals("person_address", embedded._id());
        Assert.assertEquals("person", embedded._sourceSetImplementationId());
        Assert.assertEquals("person_address", embedded._targetSetImplementationId());
        Assert.assertFalse(embedded._root());
        Assert.assertEquals("Address", embedded._class()._name());

        RichIterable<? extends PropertyMapping> subPropertyMappings = embedded._propertyMappings();
        Assert.assertEquals(1, subPropertyMappings.size());
        RelationFunctionPropertyMapping cityMapping = (RelationFunctionPropertyMapping) subPropertyMappings.getOnly();
        Assert.assertEquals("person_address", cityMapping._sourceSetImplementationId());
        Assert.assertEquals("String", cityMapping._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingWithInlineEmbeddedPropertyMapping()
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

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        Mapping mapping = (Mapping) runtime.getCoreInstance("my::testMapping");
        RelationFunctionInstanceSetImplementation personSetImpl = (RelationFunctionInstanceSetImplementation) mapping._classMappings().detect(s -> "person".equals(s._id()));
        PropertyMapping addressMapping = personSetImpl._propertyMappings().toList().get(1);
        Assert.assertTrue(addressMapping instanceof EmbeddedRelationFunctionSetImplementation);
        EmbeddedRelationFunctionSetImplementation embedded = (EmbeddedRelationFunctionSetImplementation) addressMapping;
        Assert.assertEquals("person_address", embedded._id());
        Assert.assertEquals("person", embedded._sourceSetImplementationId());
        Assert.assertEquals("addr", embedded._targetSetImplementationId());
        Assert.assertTrue(embedded._propertyMappings().isEmpty());
    }

    @Test
    public void testRelationMappingWithEnumerationTransformer()
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

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_ENUMERATION_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relationSetImpl = (RelationFunctionInstanceSetImplementation) ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(s -> "person".equals(s._id()));
        RichIterable<? extends PropertyMapping> propertyMappings = relationSetImpl._propertyMappings();
        Assert.assertEquals(2, propertyMappings.size());

        RelationFunctionPropertyMapping genderMapping = (RelationFunctionPropertyMapping) propertyMappings.toList().get(1);
        Assert.assertTrue(genderMapping._transformer() instanceof EnumerationMapping);
        Assert.assertEquals("GenderMapping", ((EnumerationMapping<?>) genderMapping._transformer())._name());
    }


    @Test
    public void testRelationMappingExpressionRhsArithmetic()
    {
        // Use the local-mapping-property form so we can introduce an arbitrary
        // String-typed property without modifying the shared Person fixture.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +concatenated: String[1]: $row.FIRSTNAME + ' ' + $row.FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping concat = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", concat._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingRejectsLegacySrcRowAccess()
    {
        // no `$src.<col>` syntax for relation mappings
        // Authored sources must migrate to `$row.<col>`
        //
        // `$src` is bound again (typed to the whole `Relation<T>`, not a row) so that
        // navigationJoin/filterJoin/modelJoin can take it as their source — see
        // RelationFunctionInstanceSetImplementationProcessor.preResolveNavigationFilterJoinUsage. A bare
        // `$src.<col>` is still rejected, just with a different (structurally accurate) error:
        // `Relation<T>` has no columns of its own to access directly.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: $src.FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception (direct column access on $src, the whole relation, is not valid)");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(), e.getMessage().contains("Can't find the property 'FIRSTNAME' in the class meta::pure::metamodel::relation::Relation"));
        }
    }

    private static final String NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE = "###Pure\n" +
            "import meta::pure::metamodel::relation::*;\n" +
            "function my::firmFunctionTyped(): Relation<(ID:Integer[1], LEGALNAME:String[1])>[1]\n" +
            "{\n" +
            "  1->cast(@Relation<(ID:Integer[1], LEGALNAME:String[1])>);\n" +
            "}\n";

    @Test
    public void testNavigationJoinCompiles()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping firmName = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", firmName._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testNavigationJoinWithExplicitJoinKindCompiles()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.INNER, {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping firmName = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", firmName._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testFilterJoinCompiles()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +gatedFirstName: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping gatedFirstName = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", gatedFirstName._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testChainedFilterJoinThenNavigationJoinCompiles()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +chained: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping chained = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", chained._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testChainedNavigationJoinThenFilterJoinCompiles()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +chained: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->filterJoin(my::firmFunctionTyped(), {f, f2 | $f.ID == $f2.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping chained = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", chained._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testNavigationJoinRejectsSourceThatIsNotSrcOrAChain()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmName: String[1]: my::personFunctionTyped()->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception (navigationJoin's source must be $src or another navigationJoin/filterJoin call)");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(), e.getMessage().contains("The source of 'navigationJoin' must be $src or another navigationJoin/filterJoin call."));
        }
    }

    @Test
    public void testFilterJoinRejectsSourceThatIsNotSrcOrAChain()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +gatedFirstName: String[1]: my::personFunctionTyped()->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception (filterJoin's source must be $src or another navigationJoin/filterJoin call)");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(), e.getMessage().contains("The source of 'filterJoin' must be $src or another navigationJoin/filterJoin call."));
        }
    }

    @Test
    public void testInlineModelJoinResolvesTargetFromIncludedMapping()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::includedFirmMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionTyped__Relation_1_\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  include my::includedFirmMapping\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firmId: FIRMID,\n" +
                "    +firmRef: my::Firm[1]: modelJoin({person, firm | $person.firmId == $firm.id})\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        ModelJoinPropertyMapping modelJoinMapping = inlineModelJoin("my::testMapping", "person", "firmRef");
        Assert.assertEquals("person", modelJoinMapping._sourceSetImplementationId());
        Assert.assertEquals("firm", modelJoinMapping._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinTargetCanBeSoleUnstarredClassMapping()
    {
        // MappingValidator.validateStar auto-promotes a class's sole class mapping to root when it has
        // no explicit '*', but that runs later, in the Validator phase, after this inline modelJoin
        // target resolution (a Processor-phase step) -- so an unstarred sole mapping for the target
        // class must still resolve correctly here, not be rejected as "found 0".
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n",
                "  my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionTyped__Relation_1_\n" +
                "    id: ID,\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n");

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testModelJoinBareNameCollisionWithUserFunctionIsNotMisidentified()
    {
        // A user's own same-named "modelJoin" function, visible via import, must not be mistaken for
        // the platform construct just because the bare name matches: real overload resolution picks the
        // user's function here, since the platform modelJoin(Function[1]):Any[1] doesn't match an
        // Integer argument. Before the fix, the structural bare-name sniff misidentified this
        // regardless, failing with a parser-level "modelJoin() expects exactly one argument: a
        // 2-parameter join condition lambda" error instead of ever reaching real overload resolution.
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("userFunc.pure", "###Pure\n" +
                "function my::test::relationNameCollision::modelJoin(x:Integer[1]):String[1]\n" +
                "{\n" +
                "  $x->toString();\n" +
                "}\n");
        compileTestSource("mapping.pure", "###Mapping\n" +
                "import my::test::relationNameCollision::*;\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +collisionTest: String[1]: modelJoin($row.FIRMID)\n" +
                "  }\n" +
                ")\n");

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping collisionTest = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", collisionTest._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testInlineModelJoinIsModelLevel()
    {
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({person, firm | $person.firmId == $firm.id})\n", FIRM_RELATION_CLASS_MAPPING);

        ModelJoinPropertyMapping modelJoinMapping = inlineModelJoin("my::testMapping", "person", "firmRef");
        Assert.assertEquals("person", modelJoinMapping._sourceSetImplementationId());
        Assert.assertEquals("firm", modelJoinMapping._targetSetImplementationId());

        // Same shape as an association ModelJoin's condition: positional params renamed to _mj_src/_mj_tgt and typed to
        // the class mappings' (Mapping)Classes -- not to the relation functions' row types.
        LambdaFunction<?> condition = modelJoinMapping._joinCondition();
        ListIterable<? extends VariableExpression> params = ((FunctionType) condition._classifierGenericType()._typeArguments().getOnly()._rawType())._parameters().toList();
        Assert.assertEquals(Lists.mutable.with("_mj_src", "_mj_tgt"), params.collect(VariableExpression::_name));
        Type sourceType = params.get(0)._genericType()._rawType();
        Assert.assertTrue(sourceType instanceof MappingClass);
        Assert.assertEquals("Person", ((MappingClass<?>) sourceType)._generalizations().getOnly()._general()._rawType()._name());
        Assert.assertEquals("Firm", params.get(1)._genericType()._rawType()._name());
        Assert.assertEquals("Boolean", condition._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testInlineModelJoinAllowsInferredAndDeclaredParamTypes()
    {
        // The params' classes may be left to inference (positional: owning class, then property's class) or declared
        // explicitly as in an association ModelJoin; both compile to the same model join.
        compileInlineModelJoinMapping(
                "    +firmRef: my::Firm[1]: modelJoin({person, firm | $person.firmId == $firm.id}),\n" +
                "    +typedFirmRef: my::Firm[1]: modelJoin({person: my::Person[1], firm: my::Firm[1] | $person.firmId == $firm.id})\n",
                FIRM_RELATION_CLASS_MAPPING);

        ModelJoinPropertyMapping inferred = inlineModelJoin("my::testMapping", "person", "firmRef");
        ModelJoinPropertyMapping declared = inlineModelJoin("my::testMapping", "person", "typedFirmRef");
        for (ModelJoinPropertyMapping modelJoinMapping : Lists.mutable.with(inferred, declared))
        {
            Assert.assertEquals("person", modelJoinMapping._sourceSetImplementationId());
            Assert.assertEquals("firm", modelJoinMapping._targetSetImplementationId());
            Assert.assertEquals("Boolean", modelJoinMapping._joinCondition()._expressionSequence().getOnly()._genericType()._rawType()._name());
        }
        Assert.assertEquals(joinConditionParams(inferred), joinConditionParams(declared));
        Assert.assertEquals(Lists.mutable.with("_mj_src", "_mj_tgt"), joinConditionParams(inferred).collect(p -> p.substring(0, p.indexOf(':'))));
    }

    @Test
    public void testInlineModelJoinUsesLocalMappingPropertiesOfBothClassMappings()
    {
        // The target is declared after the owner, so its MappingClass (with +firmKey) is built on demand.
        compileInlineModelJoinMapping(
                "    +personFirmKey: Integer[1]: FIRMID,\n" +
                "    +firmRef: my::Firm[1]: modelJoin({p, f | $p.personFirmKey == $f.firmKey})\n",
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionTyped__Relation_1_\n" +
                "    legalName: LEGALNAME,\n" +
                "    +firmKey: Integer[1]: ID\n" +
                "  }\n");

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinTargetCanBeANonRelationClassMapping()
    {
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n",
                "  *my::Firm[firm]: Pure\n" +
                "  {\n" +
                "    ~src my::Firm\n" +
                "    id: $src.id,\n" +
                "    legalName: $src.legalName\n" +
                "  }\n");

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinInBothDirections()
    {
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n",
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionTyped__Relation_1_\n" +
                "    id: ID,\n" +
                "    +employeeRefs: my::Person[*]: modelJoin({f, p | $f.id == $p.firmId})\n" +
                "  }\n");

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
        Assert.assertEquals("person", inlineModelJoin("my::testMapping", "firm", "employeeRefs")._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinIsStableAcrossRecompilation()
    {
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({person, firm | $person.firmId == $firm.id})\n", FIRM_RELATION_CLASS_MAPPING);
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE),
                        Tuples.pair("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );
        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinRejectsAmbiguousTargetAcrossLocalAndIncludedMapping()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::includedFirmMapping\n" +
                "(\n" +
                "  *my::Firm[firm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionTyped__Relation_1_\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  include my::includedFirmMapping\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                "  *my::Firm[localFirm]: Relation\n" +
                "  {\n" +
                "    ~func my::firmFunctionTyped__Relation_1_\n" +
                "    legalName: LEGALNAME\n" +
                "  }\n" +
                ")\n";

        assertMappingFailsToCompile(mappingSource, "modelJoin: expected exactly one root class mapping for type 'my::Firm' in this mapping or its includes, found 2: [");
    }

    // ------------------------------------------------------------------
    // Negative compilation coverage for modelJoin / navigationJoin / filterJoin
    // ------------------------------------------------------------------

    @Test
    public void testInlineModelJoinRejectsRelationColumns()
    {
        // The condition is model-level: FIRMID/ID are columns of the relation functions, not properties of Person/Firm.
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.FIRMID == $f.ID})\n", FIRM_RELATION_CLASS_MAPPING,
                "Can't find the property 'FIRMID' in the class");
    }

    @Test
    public void testInlineModelJoinRejectsUnknownTargetProperty()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.unknownProperty})\n", FIRM_RELATION_CLASS_MAPPING,
                "Can't find the property 'unknownProperty' in the class my::Firm");
    }

    @Test
    public void testInlineModelJoinRejectsSrcArgument()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: $src->modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin() takes only a join condition between the owning class and the property's class, e.g. modelJoin({p, f | $p.firmId == $f.id}); it does not take $src.");
    }

    @Test
    public void testInlineModelJoinRejectsMissingCondition()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin()\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin() expects exactly one argument: a 2-parameter join condition lambda");
    }

    @Test
    public void testInlineModelJoinRejectsNestedUsage()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})->toOne()\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin() must be the entire value of a property mapping; it cannot be used inside another expression.");
    }

    @Test
    public void testInlineModelJoinRejectsRowVariable()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $row.FIRMID == $f.id})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin()'s join condition is over the two classes, not the relation's rows: $row is not available.");
    }

    @Test
    public void testInlineModelJoinAllowsNestedLambdaParamNamedRowOrSrc()
    {
        // A nested lambda's own parameter legitimately named 'row' or 'src' shadows those names -- it
        // must not be mistaken for the relation-level $row/$src, which modelJoin's condition (compiled
        // in its own variable scope) can't see at all regardless.
        compileInlineModelJoinMapping(
                "    +firmRef: my::Firm[1]: modelJoin({p, f | $f.clientNames->exists({row | $row == $p.firstName})})\n",
                FIRM_RELATION_CLASS_MAPPING);

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinRejectsReferenceToTheMappedProperty()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmRef.id == $f.id})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin()'s join condition cannot use the property 'firmRef' that it maps.");
    }

    @Test
    public void testInlineModelJoinRejectsWrongConditionArity()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p | $p.firmId == 1})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin()'s join condition must take exactly 2 parameters (a Person and a Firm), found 1.");
    }

    @Test
    public void testInlineModelJoinRejectsSameParamNames()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, p | $p.firmId == $p.firmId})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin()'s join condition parameters share the same name 'p'");
    }

    @Test
    public void testInlineModelJoinRejectsNonBooleanCondition()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId})\n", FIRM_RELATION_CLASS_MAPPING,
                "ModelJoin join condition must return Boolean[1], found: Integer");
    }

    @Test
    public void testInlineModelJoinRejectsMismatchedDeclaredParamType()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p: my::Firm[1], f: my::Firm[1] | $p.id == $f.id})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin()'s join condition parameter 'p' is declared as 'my::Firm', but the owning class is 'my::Person'.");
    }

    @Test
    public void testInlineModelJoinRejectsPrimitivePropertyType()
    {
        assertPersonMappingFails("    +firmRef: String[1]: modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin() can only map a property whose type is a class; property 'firmRef' is of type 'String'.");
    }

    @Test
    public void testInlineModelJoinRejectsUnmappedTargetClass()
    {
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n", "",
                "modelJoin: expected exactly one root class mapping for type 'my::Firm' in this mapping or its includes, found 0.");
    }

    @Test
    public void testNavigationJoinRejectsUnknownSourceColumnInCondition()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.UNKNOWN_COL == $f.ID}).LEGALNAME\n", "",
                "The system can't find the column UNKNOWN_COL in the Relation (FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])");
    }

    @Test
    public void testNavigationJoinRejectsUnknownTargetColumnInCondition()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.UNKNOWN_COL}).LEGALNAME\n", "",
                "The system can't find the column UNKNOWN_COL in the Relation (ID:Integer[1], LEGALNAME:String[1])");
    }

    @Test
    public void testNavigationJoinRejectsUnknownProjectedColumn()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).UNKNOWN_COL\n", "",
                "The system can't find the column UNKNOWN_COL in the Relation (ID:Integer[1], LEGALNAME:String[1])");
    }

    @Test
    public void testFilterJoinRejectsUnknownColumnInCondition()
    {
        assertPersonMappingFails("    +gatedFirstName: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.UNKNOWN_COL}).FIRSTNAME\n", "",
                "The system can't find the column UNKNOWN_COL in the Relation (ID:Integer[1], LEGALNAME:String[1])");
    }

    @Test
    public void testFilterJoinRejectsProjectingATargetColumn()
    {
        // filterJoin keeps the source's columns, so the target's LEGALNAME is not projectable.
        assertPersonMappingFails("    +gatedName: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "",
                "The system can't find the column LEGALNAME in the Relation (FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])");
    }

    @Test
    public void testNavigationJoinRejectsWrongConditionArity()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p | $p.FIRMID == 1}).LEGALNAME\n", "",
                "The system can't find a match for the function: navigationJoin(_:Relation<(FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])>[1],_:Relation<(ID:Integer[1], LEGALNAME:String[1])>[1],_:LambdaFunction<{NULL[NULL]->NULL[NULL]}>[1])");
    }

    @Test
    public void testNavigationJoinRejectsNonBooleanCondition()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID}).LEGALNAME\n", "",
                "The system can't find a match for the function: navigationJoin(_:Relation<(FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])>[1],_:Relation<(ID:Integer[1], LEGALNAME:String[1])>[1],_:LambdaFunction<{(FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])[1], (ID:Integer[1], LEGALNAME:String[1])[1]->Integer[1]}>[1])");
    }

    @Test
    public void testNavigationJoinRejectsNonRelationTarget()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::integerFunction(), {p, f | $p.FIRMID == $f}).LEGALNAME\n", "",
                "The system can't find a match for the function: navigationJoin(_:Relation<(FIRSTNAME:String[1], AGE:Integer[1], FIRMID:Integer[1], CITY:String[1])>[1],_:Integer[1],");
    }

    @Test
    public void testNavigationJoinRejectsRightJoinKind()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.RIGHT, {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "",
                "JoinKind.RIGHT is not supported by navigationJoin(); use JoinKind.LEFT (the default) or JoinKind.INNER.");
    }

    @Test
    public void testFilterJoinRejectsFullJoinKind()
    {
        assertPersonMappingFails("    +gatedFirstName: String[1]: $src->filterJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.FULL, {p, f | $p.FIRMID == $f.ID}).FIRSTNAME\n", "",
                "JoinKind.FULL is not supported by filterJoin(); use JoinKind.INNER (the default) or JoinKind.LEFT.");
    }

    @Test
    public void testNavigationJoinRejectsNonLiteralJoinKind()
    {
        // joinKindLiteral only recognizes a literal JoinKind.XXX enum access; any other JoinKind-typed
        // expression (e.g. a function call) must be rejected rather than silently skipping the
        // RIGHT/FULL gate below it.
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("kindFunc.pure", "###Pure\n" +
                "function my::rightJoinKind(): meta::pure::functions::relation::JoinKind[1]\n" +
                "{\n" +
                "  meta::pure::functions::relation::JoinKind.RIGHT;\n" +
                "}\n");
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), my::rightJoinKind(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n" +
                "  }\n" +
                ")\n";
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected a compilation failure requiring a literal JoinKind");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(), e.getMessage().contains(
                    "navigationJoin()'s JoinKind argument must be a literal value (e.g. JoinKind.LEFT), not a computed expression."));
        }
    }

    @Test
    public void testNavigationJoinRejectsUsageNestedInAnotherExpression()
    {
        // Only "the property's entire value" or "a single column access directly over it" are
        // structurally validated; navigationJoin/filterJoin nested any deeper (e.g. inside ->toOne())
        // must be rejected with a clear error instead of silently reaching the generic matcher.
        assertPersonMappingFails(
                "    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME->toOne()\n", "",
                "'navigationJoin' may only be used as the property's entire value expression, or immediately followed by a single column access");
    }

    // The key/value pairs of a ^Class(key=value) new-instance expression are KeyExpressions, which the
    // usage checks must look inside as well.

    @Test
    public void testRelationMappingRejectsSrcInsideNewInstanceKeyExpression()
    {
        assertPersonMappingFails("    +cityName: String[1]: ^my::Address(city=$src->size()->toString()).city\n", "",
                "$src is the whole source relation and may only be used as the source of navigationJoin/filterJoin");
    }

    @Test
    public void testNavigationJoinRejectsUsageInsideNewInstanceKeyExpression()
    {
        assertPersonMappingFails(
                "    +firmName: String[1]: ^my::Address(city=$src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME->toOne()).city\n", "",
                "'navigationJoin' may only be used as the property's entire value expression, or immediately followed by a single column access");
    }

    @Test
    public void testInlineModelJoinRejectsUsageInsideNewInstanceKeyExpression()
    {
        assertPersonMappingFails("    +cityName: String[1]: ^my::Address(city=modelJoin({p, f | $p.firmId == $f.id})->toString()).city\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin() must be the entire value of a property mapping; it cannot be used inside another expression.");
    }

    @Test
    public void testInlineModelJoinRenamesParamsInsideNewInstanceKeyExpression()
    {
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({p, f | ^my::Firm(id=$p.firmId, legalName='x').id == $f.id})\n", FIRM_RELATION_CLASS_MAPPING);
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE),
                        Tuples.pair("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );

        ModelJoinPropertyMapping modelJoinMapping = inlineModelJoin("my::testMapping", "person", "firmRef");
        Assert.assertEquals("firm", modelJoinMapping._targetSetImplementationId());
        Assert.assertEquals("Boolean", modelJoinMapping._joinCondition()._expressionSequence().getOnly()._genericType()._rawType()._name());
        Assert.assertEquals(Lists.mutable.empty(), modelJoinMapping._joinCondition()._openVariables().toList());
    }

    @Test
    public void testJoinChainValueFnHasNoOpenVariables()
    {
        // $src only types the source of a join chain; it is not a free variable of the processed valueFn.
        compileInlineModelJoinMapping(
                "    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME,\n" +
                "    +chained: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n",
                "");
        assertNoOpenVariables("firmName", "chained");

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE),
                        Tuples.pair("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );
        assertNoOpenVariables("firmName", "chained");
    }

    private void assertNoOpenVariables(String... propertyNames)
    {
        RelationFunctionInstanceSetImplementation set = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(s -> "person".equals(s._id()));
        for (String propertyName : propertyNames)
        {
            RelationFunctionPropertyMapping propertyMapping = (RelationFunctionPropertyMapping) set._propertyMappings().detect(pm -> propertyName.equals(pm._property()._name()));
            Assert.assertEquals(propertyName, "String", propertyMapping._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
            Assert.assertEquals(propertyName, Lists.mutable.empty(), propertyMapping._valueFn()._openVariables().toList());
        }
    }

    // $row is the mapped row, while a join is over the whole source relation: $row is never available as
    // the source of a join, nor in its arguments (its join condition included), whether the chain is the
    // property's entire value (typed Any here, as a join returns a Relation) or followed by a column access.

    private static final String ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE = "navigationJoin()'s join condition is over the source and target rows: $row is not available. Use the condition's parameters, e.g. {p, f | $p.FIRMID == $f.ID}.";
    private static final String ROW_IN_FILTER_JOIN_CONDITION_MESSAGE = "filterJoin()'s join condition is over the source and target rows: $row is not available. Use the condition's parameters, e.g. {p, f | $p.FIRMID == $f.ID}.";

    @Test
    public void testNavigationJoinRejectsRowAsSourceWithColumnAccess()
    {
        assertPersonMappingFails("    +firmName: String[1]: $row->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "",
                "The source of 'navigationJoin' must be $src or another navigationJoin/filterJoin call.");
    }

    @Test
    public void testNavigationJoinRejectsRowAsSourceAsWholeValue()
    {
        assertPersonMappingFails("    +firms: Any[1]: $row->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})\n", "",
                "The source of 'navigationJoin' must be $src or another navigationJoin/filterJoin call.");
    }

    @Test
    public void testNavigationJoinRejectsRowAsSourceNestedInAnotherExpression()
    {
        assertPersonMappingFails("    +firmName: String[1]: $row->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME->toOne()\n", "",
                "'navigationJoin' may only be used as the property's entire value expression, or immediately followed by a single column access");
    }

    @Test
    public void testNavigationJoinRejectsRowInConditionWithColumnAccess()
    {
        // Previously failed only as "The variable 'row' is unknown!".
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID}).LEGALNAME\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testNavigationJoinRejectsRowInConditionAsWholeValue()
    {
        // Previously compiled: the valueFn's own `row` parameter was in scope.
        assertPersonMappingFails("    +firms: Any[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID})\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testNavigationJoinRejectsRowInConditionWithExplicitJoinKind()
    {
        assertPersonMappingFails("    +firms: Any[1]: $src->navigationJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.INNER, {p, f | $p.FIRMID == $row.FIRMID})\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testNavigationJoinRejectsRowInsideNestedLambdaOfCondition()
    {
        // The nested lambda binds only `i`, so this $row is still the mapped row.
        assertPersonMappingFails("    +firms: Any[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | [$f.ID]->exists({i | $i == $row.FIRMID})})\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testQualifiedNavigationJoinRejectsRowInCondition()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->meta::pure::functions::relation::navigationJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID}).LEGALNAME\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testNavigationJoinRejectsRowAsTarget()
    {
        // Previously failed only as "The system can't find a match for the function: navigationJoin(...)".
        assertPersonMappingFails("    +firms: Any[1]: $src->navigationJoin($row, {p, f | $p.FIRMID == $f.FIRMID})\n", "",
                "$row is not available in the arguments of navigationJoin(): the join is over the whole source relation, not the mapped row.");
    }

    @Test
    public void testFilterJoinRejectsRowAsSourceWithColumnAccess()
    {
        assertPersonMappingFails("    +gatedFirstName: String[1]: $row->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).FIRSTNAME\n", "",
                "The source of 'filterJoin' must be $src or another navigationJoin/filterJoin call.");
    }

    @Test
    public void testFilterJoinRejectsRowAsSourceAsWholeValue()
    {
        assertPersonMappingFails("    +gatedRows: Any[1]: $row->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})\n", "",
                "The source of 'filterJoin' must be $src or another navigationJoin/filterJoin call.");
    }

    @Test
    public void testFilterJoinRejectsRowInConditionWithColumnAccess()
    {
        assertPersonMappingFails("    +gatedFirstName: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID}).FIRSTNAME\n", "",
                ROW_IN_FILTER_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testFilterJoinRejectsRowInConditionAsWholeValue()
    {
        assertPersonMappingFails("    +gatedRows: Any[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID})\n", "",
                ROW_IN_FILTER_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testChainedJoinRejectsRowAsChainRoot()
    {
        assertPersonMappingFails("    +chained: String[1]: $row->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "",
                "The source of 'filterJoin' must be $src or another navigationJoin/filterJoin call.");
    }

    @Test
    public void testChainedJoinRejectsRowInLaterConditionWithColumnAccess()
    {
        assertPersonMappingFails("    +chained: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID}).LEGALNAME\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testChainedJoinRejectsRowInLaterConditionAsWholeValue()
    {
        assertPersonMappingFails("    +chained: Any[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID})\n", "",
                ROW_IN_NAVIGATION_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testChainedJoinRejectsRowInEarlierCondition()
    {
        assertPersonMappingFails("    +chained: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $row.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "",
                ROW_IN_FILTER_JOIN_CONDITION_MESSAGE);
    }

    @Test
    public void testInlineModelJoinRejectsRowArgument()
    {
        // Previously failed only as "modelJoin() expects exactly one argument: ...".
        assertPersonMappingFails("    +firmRef: my::Firm[1]: $row->modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_CLASS_MAPPING,
                "modelJoin() takes only a join condition between the owning class and the property's class, e.g. modelJoin({p, f | $p.firmId == $f.id}); it does not take $row.");
    }

    @Test
    public void testJoinConditionAllowsNestedLambdaParamNamedRow()
    {
        // A nested lambda's own `row` parameter shadows the mapped row, so it is not the forbidden $row.
        compileInlineModelJoinMapping(
                "    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | [$p.FIRMID]->exists({row | $row == $f.ID})}).LEGALNAME,\n" +
                "    +gatedFirstName: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | [$f.ID]->exists(row | $row == $p.FIRMID)}).FIRSTNAME,\n" +
                "    +firms: Any[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | [$p.FIRMID]->exists({row | $row == $f.ID})})\n",
                "");

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        Assert.assertEquals("String", ((RelationFunctionPropertyMapping) relSet._propertyMappings().detect(pm -> "firmName".equals(pm._property()._name())))._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
        Assert.assertEquals("String", ((RelationFunctionPropertyMapping) relSet._propertyMappings().detect(pm -> "gatedFirstName".equals(pm._property()._name())))._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
        Assert.assertEquals("Relation", ((RelationFunctionPropertyMapping) relSet._propertyMappings().detect(pm -> "firms".equals(pm._property()._name())))._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testInlineModelJoinInsideEmbeddedPropertyMapping()
    {
        // As in a Relational mapping, a join may be used inside an embedded block: the embedded set is the
        // modelJoin's source, typed to the embedded class itself (it has no MappingClass of its own).
        compileInlineModelJoinMapping(
                "    address\n" +
                "    (\n" +
                "      city: CITY,\n" +
                "      +firm: my::Firm[1]: modelJoin({a, f | $a.city == $f.legalName})\n" +
                "    )\n",
                FIRM_RELATION_CLASS_MAPPING);
        // The unbind cleans an embedded set twice (MappingUnbind, then the parent's unbind recursion).
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE),
                        Tuples.pair("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );

        RelationFunctionInstanceSetImplementation personSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(s -> "person".equals(s._id()));
        EmbeddedRelationFunctionSetImplementation embedded = (EmbeddedRelationFunctionSetImplementation) personSet._propertyMappings().detect(pm -> "address".equals(pm._property()._name()));
        Assert.assertEquals("person_address", embedded._id());
        PropertyMapping firmMapping = embedded._propertyMappings().detect(pm -> "firm".equals(pm._property()._name()));
        Assert.assertTrue("Expected a ModelJoinPropertyMapping, got: " + firmMapping, firmMapping instanceof ModelJoinPropertyMapping);
        ModelJoinPropertyMapping modelJoinMapping = (ModelJoinPropertyMapping) firmMapping;
        Assert.assertEquals("person_address", modelJoinMapping._sourceSetImplementationId());
        Assert.assertEquals("firm", modelJoinMapping._targetSetImplementationId());
        Assert.assertEquals("Boolean", modelJoinMapping._joinCondition()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    private static final String FIRM_RELATION_CLASS_MAPPING =
            "  *my::Firm[firm]: Relation\n" +
            "  {\n" +
            "    ~func my::firmFunctionTyped__Relation_1_\n" +
            "    id: ID,\n" +
            "    legalName: LEGALNAME\n" +
            "  }\n";

    // my::testMapping: a `person` Relation class mapping with the given extra property mappings, then otherClassMappings.
    private static String inlineJoinMappingSource(String extraPersonPropertyMappings, String otherClassMappings)
    {
        return "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firmId: FIRMID,\n" +
                extraPersonPropertyMappings +
                "  }\n" +
                otherClassMappings +
                ")\n";
    }

    private void compileInlineModelJoinMapping(String extraPersonPropertyMappings, String otherClassMappings)
    {
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", inlineJoinMappingSource(extraPersonPropertyMappings, otherClassMappings));
    }

    private void assertPersonMappingFails(String extraPersonPropertyMappings, String otherClassMappings, String expectedMessage)
    {
        assertMappingFailsToCompile(inlineJoinMappingSource(extraPersonPropertyMappings, otherClassMappings), expectedMessage);
    }

    private void assertMappingFailsToCompile(String mappingSource, String expectedMessage)
    {
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected a compilation failure containing: " + expectedMessage);
        }
        catch (PureCompilationException | PureParserException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(), e.getMessage().contains(expectedMessage));
        }
    }

    // "<name>:<raw type>" for each of the join condition's params.
    private static ListIterable<String> joinConditionParams(ModelJoinPropertyMapping modelJoinMapping)
    {
        FunctionType functionType = (FunctionType) modelJoinMapping._joinCondition()._classifierGenericType()._typeArguments().getOnly()._rawType();
        return functionType._parameters().toList().collect(p -> p._name() + ":" + p._genericType()._rawType()._name());
    }

    private ModelJoinPropertyMapping inlineModelJoin(String mappingPath, String classMappingId, String propertyName)
    {
        RelationFunctionInstanceSetImplementation set = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance(mappingPath))._classMappings().detect(s -> classMappingId.equals(s._id()));
        PropertyMapping propertyMapping = set._propertyMappings().detect(pm -> propertyName.equals(pm._property()._name()));
        Assert.assertTrue("Expected a ModelJoinPropertyMapping, got: " + propertyMapping, propertyMapping instanceof ModelJoinPropertyMapping);
        return (ModelJoinPropertyMapping) propertyMapping;
    }

    @Test
    public void testRelationMappingInlineSrc()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        FunctionDefinition<?> rf = relSet._relationFunction();
        Assert.assertTrue("~src should produce a LambdaFunction in relationFunction", rf instanceof LambdaFunction);
        Type lastType = rf._expressionSequence().getLast()._genericType()._typeArguments().getOnly()._rawType();
        Assert.assertTrue(lastType instanceof RelationType);

        RelationFunctionPropertyMapping pm = (RelationFunctionPropertyMapping) relSet._propertyMappings().getOnly();
        Assert.assertEquals("String", pm._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    // ------------------------------------------------------------------
    // §9.1 — additional coverage for Pure-expression-based property RHS
    // ------------------------------------------------------------------

    @Test
    public void testRelationMappingExpressionRhsConditional()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +ageBucket: String[1]: if($row.AGE > 65, |'senior', |'other')\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping bucket = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", bucket._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingExpressionRhsTypeError()
    {
        // age:Integer mapped from FIRSTNAME (String) -> compile error.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    age: $row.FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(), e.getMessage().contains("Mismatching property and relation expression types. Property 'age' is of type 'Integer', but the expression mapped to it is of type 'String'."));
        }
    }

    @Test
    public void testRelationMappingExpressionRhsMissingColumn()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: $row.MISSING\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(),
                    e.getMessage().contains("MISSING"));
        }
    }

    @Test
    public void testRelationMappingExpressionRhsMultiplicityViolation()
    {
        // The relation function's row type has no [*] columns so we can't
        // construct a many-valued column accessor directly — but we can use
        // an explicit list literal as the lambda body to exercise the
        // multiplicity check on the result.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: [$row.FIRSTNAME, $row.FIRSTNAME]\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(),
                    e.getMessage().contains("multiplicity") ||
                            e.getMessage().contains("Multiplicity"));
        }
    }

    @Test
    public void testRelationMappingWithEnumerationTransformerOverExpression()
    {
        // Enumeration transformer applied on top of a (trivial) expression RHS,
        // exercising the transformer + valueFn co-existence.
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
                "    firstName: $row.FIRSTNAME,\n" +
                "    gender: EnumerationMapping GenderMapping: $row.GENDER\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_ENUMERATION_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_CLASS_ENUMERATION_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(s -> "person".equals(s._id()));
        RelationFunctionPropertyMapping genderMapping = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertTrue(genderMapping._transformer() instanceof EnumerationMapping);
        Assert.assertEquals("GenderMapping", ((EnumerationMapping<?>) genderMapping._transformer())._name());
    }

    @Test
    public void testRelationMappingEmbeddedWithExpressionSubProperty()
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
                "      city: $row.CITY\n" +
                "    )\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        Mapping mapping = (Mapping) runtime.getCoreInstance("my::testMapping");
        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                mapping._classMappings().detect(s -> "person".equals(s._id()));
        EmbeddedRelationFunctionSetImplementation embedded =
                (EmbeddedRelationFunctionSetImplementation) relSet._propertyMappings().toList().get(1);
        RelationFunctionPropertyMapping cityMapping = (RelationFunctionPropertyMapping) embedded._propertyMappings().getOnly();
        Assert.assertEquals("String", cityMapping._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    // ------------------------------------------------------------------
    // §9.1 — coverage for the inline `~src` source form
    // ------------------------------------------------------------------

    @Test
    public void testRelationMappingInlineSrcStructurallyMatchesFunc()
    {
        // Two mappings, one using ~func and one using ~src that bodies the
        // same expression: both must yield a relationFunction whose last
        // expression resolves to a Relation type with the same row columns.
        String funcMapping = "###Mapping\n" +
                "Mapping my::funcMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";
        String srcMapping = "###Mapping\n" +
                "Mapping my::srcMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("funcMapping.pure", funcMapping);
        compileTestSource("srcMapping.pure", srcMapping);

        RelationFunctionInstanceSetImplementation funcSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::funcMapping"))._classMappings().getOnly();
        RelationFunctionInstanceSetImplementation srcSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::srcMapping"))._classMappings().getOnly();

        FunctionDefinition<?> funcRf = funcSet._relationFunction();
        FunctionDefinition<?> srcRf = srcSet._relationFunction();
        Assert.assertTrue("~src form must produce a LambdaFunction", srcRf instanceof LambdaFunction);

        // Both functions take zero parameters and return a Relation<...>.
        Assert.assertTrue(funcRf._expressionSequence().getLast()._genericType()._typeArguments().getOnly()._rawType() instanceof RelationType);
        Assert.assertTrue(srcRf._expressionSequence().getLast()._genericType()._typeArguments().getOnly()._rawType() instanceof RelationType);

        // Property mapping under ~src resolves $row.FIRSTNAME the same way.
        RelationFunctionPropertyMapping pm = (RelationFunctionPropertyMapping) srcSet._propertyMappings().getOnly();
        Assert.assertEquals("String", pm._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingInlineSrcNonRelationReturnError()
    {
        // ~src expression evaluates to Integer rather than Relation -> validateRelationFunction must reject.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src 1\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected compilation exception");
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue("Got: " + e.getMessage(),
                    e.getMessage().contains("Relation mapping function should return a Relation"));
        }
    }

    @Test
    public void testRelationMappingInlineSrcRejectsLambdaBody()
    {
        // The new ~src form takes a single zero-arg Pure expression, not a lambda
        // literal.  Writing `~src { | ... }` (the legacy form) is still
        // syntactically valid — a lambda literal is an atomic expression — but
        // the synthesized wrapper `{| { | ... } }` produces a function whose last
        // expression is a LambdaFunction, so `validateRelationFunction` must
        // reject it.  This protects existing call-sites from silently accepting
        // the old form when they meant `~src <expr>`.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src { | my::personFunctionTyped() }\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected parser or compilation exception");
        }
        catch (PureParserException | PureCompilationException expected)
        {
            // ok
        }
    }

    @Test
    public void testRelationMappingFuncAndSrcMutuallyExclusive()
    {
        // Grammar must reject both ~func and ~src in the same class mapping.
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

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        try
        {
            compileTestSource("mapping.pure", mappingSource);
            Assert.fail("Expected parser exception (~func and ~src are mutually exclusive)");
        }
        catch (PureParserException | PureCompilationException expected)
        {
            // ok
        }
    }

    @Test
    public void testRelationMappingInlineSrcWithEmbeddedSubMapping()
    {
        // Embedded mapping inherits relationFunction from the ~src parent;
        // sub-property $row.<col> must resolve against the same row type.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    firstName: FIRSTNAME,\n" +
                "    address\n" +
                "    (\n" +
                "      city: $row.CITY\n" +
                "    )\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(s -> "person".equals(s._id()));
        Assert.assertTrue(relSet._relationFunction() instanceof LambdaFunction);

        EmbeddedRelationFunctionSetImplementation embedded =
                (EmbeddedRelationFunctionSetImplementation) relSet._propertyMappings().toList().get(1);
        // Embedded mapping inherits the parent's relationFunction.
        Assert.assertSame(relSet._relationFunction(), embedded._relationFunction());
        RelationFunctionPropertyMapping cityMapping = (RelationFunctionPropertyMapping) embedded._propertyMappings().getOnly();
        Assert.assertEquals("String", cityMapping._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    // ------------------------------------------------------------------
    // §9.1 — incremental integrity: delete-and-reload-stable across forms
    // ------------------------------------------------------------------

    @Test
    public void testIncrementalIntegrityForExpressionRhsAndInlineSrc()
    {
        // Mixes bare-column, $row expression, ~src inline source, embedded
        // mapping, and an enumeration transformer in a single mapping. Each
        // dependency source is deleted, recompiled, and reloaded; the
        // resulting graph must remain structurally identical.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    firstName: FIRSTNAME,\n" +
                "    +concatenated: String[1]: $row.FIRSTNAME + ' ' + $row.FIRSTNAME,\n" +
                "    address\n" +
                "    (\n" +
                "      city: $row.CITY\n" +
                "    )\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );

        // Sanity: after the verifier loop the mapping is still in the model
        // and the lambda forms are intact.
        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(s -> "person".equals(s._id()));
        Assert.assertTrue(relSet._relationFunction() instanceof LambdaFunction);
        RelationFunctionPropertyMapping fn = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(0);
        Assert.assertEquals("String", fn._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
        RelationFunctionPropertyMapping concat = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("String", concat._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    // ------------------------------------------------------------------
    // Quoted column names (spaces in identifiers) — both bare-column and
    // explicit `$row.'…'` expression forms must lower to a property
    // accessor whose name preserves the space.
    // ------------------------------------------------------------------

    /**
     * Pure source for a typed relation function whose row has a column
     * named {@code 'FIRST NAME'} (with a space).  Used by the tests below
     * to exercise quoted-column lowering on both source forms.
     */
    private static final String RELATION_FUNCTION_QUOTED_COLUMN_SOURCE = "###Pure\n" +
            "import meta::pure::metamodel::relation::*;\n" +
            "function my::personFunctionQuoted(): Relation<('FIRST NAME':String[1], AGE:Integer[1])>[1]\n" +
            "{\n" +
            "  []->cast(@Relation<('FIRST NAME':String[1], AGE:Integer[1])>)->toOne();\n" +
            "}\n";

    @Test
    public void testRelationMappingWithQuotedColumnBareForm()
    {
        // Bare-column form: `firstName: 'FIRST NAME'` must lower to
        // `{| $row.'FIRST NAME'}` and resolve to the column whose name
        // contains a space.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionQuoted__Relation_1_\n" +
                "    firstName: 'FIRST NAME',\n" +
                "    +age: Integer[0..1]: AGE\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_FUNCTION_QUOTED_COLUMN_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping firstNamePm = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(0);
        // Value-type resolves through the quoted `'FIRST NAME'` accessor
        // against the row type — proves the bare-column lowering preserved
        // the quoted column identifier verbatim.
        Assert.assertEquals("String", firstNamePm._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());

        // Sibling unquoted column on the same relation still works.
        RelationFunctionPropertyMapping agePm = (RelationFunctionPropertyMapping) relSet._propertyMappings().toList().get(1);
        Assert.assertEquals("Integer", agePm._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingWithQuotedColumnExplicitRowForm()
    {
        // Explicit `$row.'FIRST NAME'` expression form must resolve the
        // quoted column against the row type and produce a String-typed
        // value, exactly like the bare-column form.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionQuoted__Relation_1_\n" +
                "    firstName: $row.'FIRST NAME'\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_FUNCTION_QUOTED_COLUMN_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping pm = (RelationFunctionPropertyMapping) relSet._propertyMappings().getOnly();
        Assert.assertEquals("String", pm._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingWithQuotedColumnInArithmeticExpression()
    {
        // `$row.'FIRST NAME'` participating in a multi-step expression: the
        // quoted column accessor inside the body must still resolve and the
        // overall expression's result type must match the property type.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionQuoted__Relation_1_\n" +
                "    +greeted: String[1]: 'Hi ' + $row.'FIRST NAME'\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_FUNCTION_QUOTED_COLUMN_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping pm = (RelationFunctionPropertyMapping) relSet._propertyMappings().getOnly();
        Assert.assertEquals("String", pm._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testRelationMappingWithCompositePrimaryKey()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    ~primaryKey: [FIRSTNAME, AGE]\n" +
                "    firstName: FIRSTNAME,\n" +
                "    age: AGE\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        Assert.assertEquals(Lists.fixedSize.of("FIRSTNAME", "AGE"), relSet._primaryKey().collect(Column::_name).toList());
        Assert.assertEquals(Lists.fixedSize.of("String", "Integer"), relSet._primaryKey().collect(c -> _Column.getColumnType(c)._rawType()._name()).toList());
    }

    @Test
    public void testRelationMappingWithSinglePrimaryKey()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    ~primaryKey: FIRMID\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        Assert.assertEquals(Lists.fixedSize.of("FIRMID"), relSet._primaryKey().collect(Column::_name).toList());
    }

    @Test
    public void testRelationMappingWithoutPrimaryKeyLeavesItEmpty()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        Assert.assertTrue(relSet._primaryKey().isEmpty());
    }

    @Test
    public void testRelationMappingWithQuotedPrimaryKeyColumn()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionQuoted__Relation_1_\n" +
                "    ~primaryKey: ['FIRST NAME', AGE]\n" +
                "    firstName: 'FIRST NAME'\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_FUNCTION_QUOTED_COLUMN_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        Assert.assertEquals(Lists.fixedSize.of("FIRST NAME", "AGE"), relSet._primaryKey().collect(Column::_name).toList());
    }

    @Test
    public void testRelationMappingInlineSrcWithPrimaryKey()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~src my::personFunctionTyped()\n" +
                "    ~primaryKey: [FIRSTNAME]\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        Assert.assertEquals(Lists.fixedSize.of("FIRSTNAME"), relSet._primaryKey().collect(Column::_name).toList());
    }

    @Test
    public void testRelationMappingWithUnknownPrimaryKeyColumn()
    {
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunction__Relation_1_\n" +
                "    ~primaryKey: [FOO]\n" +
                "    firstName: FIRSTNAME\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileTestSource("mapping.pure", mappingSource));
        Assert.assertEquals("Primary key column 'FOO' declared in class mapping 'person' (mapping 'my::testMapping') is not part of the columns returned by the relation function."
                + " Available columns: [FIRSTNAME, AGE, FIRMID, CITY]."
                + " Use `~primaryKey: [col1, col2, ...]` referencing only columns from the relation function's output.", e.getInfo());
        Assert.assertEquals(7, e.getSourceInformation().getLine());
    }

    private static final String SRC_OUTSIDE_JOIN_MESSAGE = "$src is the whole source relation and may only be used as the source of navigationJoin/filterJoin";

    @Test
    public void testSrcRejectedAsArgumentOfOtherFunction()
    {
        assertPersonMappingFails("    +cnt: Integer[1]: $src->size()\n", "", SRC_OUTSIDE_JOIN_MESSAGE);
    }

    @Test
    public void testSrcRejectedAsSourceOfFilter()
    {
        assertPersonMappingFails("    +cnt: Integer[1]: $src->filter(r | $r.FIRMID == 1)->size()\n", "", SRC_OUTSIDE_JOIN_MESSAGE);
    }

    @Test
    public void testSrcRejectedInsideNestedLambda()
    {
        assertPersonMappingFails("    +flag: Boolean[1]: [1, 2]->exists(i | $src->size() == $i)\n", "", SRC_OUTSIDE_JOIN_MESSAGE);
    }

    @Test
    public void testSrcRejectedInsideJoinCondition()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin(my::firmFunctionTyped(), {p, f | $src->size() == $f.ID}).LEGALNAME\n", "", SRC_OUTSIDE_JOIN_MESSAGE);
    }

    @Test
    public void testSrcRejectedAsJoinTarget()
    {
        assertPersonMappingFails("    +firmName: String[1]: $src->navigationJoin($src, {p, f | $p.FIRMID == $f.FIRMID}).FIRSTNAME\n", "", SRC_OUTSIDE_JOIN_MESSAGE);
    }

    @Test
    public void testChainedJoinRejectsRightJoinKindOnInnerCall()
    {
        assertPersonMappingFails("    +chained: String[1]: $src->navigationJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.RIGHT, {p, f | $p.FIRMID == $f.ID})->filterJoin(my::firmFunctionTyped(), {f, f2 | $f.ID == $f2.ID}).LEGALNAME\n", "",
                "JoinKind.RIGHT is not supported by navigationJoin(); use JoinKind.LEFT (the default) or JoinKind.INNER.");
    }

    @Test
    public void testChainedJoinKeepsSrcOnlyAtChainRoot()
    {
        compileInlineModelJoinMapping("    +chained: String[1]: $src->filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID})->navigationJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.INNER, {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "");

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping chained = (RelationFunctionPropertyMapping) relSet._propertyMappings().detect(pm -> "chained".equals(pm._property()._name()));
        Assert.assertEquals("String", chained._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
        // .LEGALNAME -> navigationJoin -> filterJoin -> $src: the chain root is the only free `src` reference.
        SimpleFunctionExpression navigationJoin = (SimpleFunctionExpression) ((SimpleFunctionExpression) chained._valueFn()._expressionSequence().getOnly())._parametersValues().getOnly();
        SimpleFunctionExpression filterJoin = (SimpleFunctionExpression) navigationJoin._parametersValues().getFirst();
        VariableExpression chainRoot = (VariableExpression) filterJoin._parametersValues().getFirst();
        Assert.assertEquals("src", chainRoot._name());
    }

    private static final String FIRM_OPERATION_SETS_FUNCTION_SOURCE = "###Pure\n" +
            "function my::firmOperationSets(): meta::pure::mapping::SetImplementation[*]\n" +
            "{\n" +
            "  []\n" +
            "}\n";

    private static final String UNSTARRED_FIRM_RELATION_CLASS_MAPPING =
            "  my::Firm[firm]: Relation\n" +
            "  {\n" +
            "    ~func my::firmFunctionTyped__Relation_1_\n" +
            "    id: ID,\n" +
            "    legalName: LEGALNAME\n" +
            "  }\n";

    private static String firmOperationClassMapping(boolean root)
    {
        return "  " + (root ? "*" : "") + "my::Firm[firmOp]: Operation\n" +
                "  {\n" +
                "    my::firmOperationSets__SetImplementation_MANY_(firm)\n" +
                "  }\n";
    }

    @Test
    public void testInlineModelJoinRejectsStarredOperationMappingAsTarget()
    {
        // The starred Operation is Firm's root, so it is the target -- the unstarred Relation mapping beside it is
        // not picked instead.
        // TODO: support Operation mappings as the target of an inline modelJoin
        compileTestSource("opFunc.pure", FIRM_OPERATION_SETS_FUNCTION_SOURCE);
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n",
                UNSTARRED_FIRM_RELATION_CLASS_MAPPING + firmOperationClassMapping(true),
                "modelJoin: Operation mappings are not supported as the target of modelJoin; the root class mapping for type 'my::Firm' ('firmOp') is an Operation.");
    }

    @Test
    public void testInlineModelJoinRejectsUnstarredRelationMappingAlongsideUnstarredOperation()
    {
        // Two unstarred class mappings for the class, Operation included: neither is promoted, so there is no root.
        compileTestSource("opFunc.pure", FIRM_OPERATION_SETS_FUNCTION_SOURCE);
        assertPersonMappingFails("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n",
                UNSTARRED_FIRM_RELATION_CLASS_MAPPING + firmOperationClassMapping(false),
                "modelJoin: expected exactly one root class mapping for type 'my::Firm' in this mapping or its includes, found 0.");
    }

    @Test
    public void testInlineModelJoinTargetsStarredRelationMappingAlongsideNonRootOperation()
    {
        compileTestSource("opFunc.pure", FIRM_OPERATION_SETS_FUNCTION_SOURCE);
        compileInlineModelJoinMapping("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n",
                FIRM_RELATION_CLASS_MAPPING + firmOperationClassMapping(false));

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinTargetCanBeSoleUnstarredClassMappingOfIncludedMapping()
    {
        // The sole-mapping promotion applies per declaring mapping, so it also holds in an included mapping.
        String mappingSource = "###Mapping\n" +
                "Mapping my::includedFirmMapping\n" +
                "(\n" +
                UNSTARRED_FIRM_RELATION_CLASS_MAPPING +
                ")\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  include my::includedFirmMapping\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionTyped__Relation_1_\n" +
                "    firstName: FIRSTNAME,\n" +
                "    firmId: FIRMID,\n" +
                "    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n" +
                "  }\n" +
                ")\n";

        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", mappingSource);

        Assert.assertEquals("firm", inlineModelJoin("my::testMapping", "person", "firmRef")._targetSetImplementationId());
    }

    @Test
    public void testQualifiedNavigationJoinCompiles()
    {
        compileInlineModelJoinMapping("    +firmName: String[1]: $src->meta::pure::functions::relation::navigationJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "");

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping firmName = (RelationFunctionPropertyMapping) relSet._propertyMappings().detect(pm -> "firmName".equals(pm._property()._name()));
        Assert.assertEquals("String", firmName._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testQualifiedNavigationJoinRejectsRightJoinKind()
    {
        // Qualifying the call must not bypass the structural checks applied to the bare form.
        assertPersonMappingFails("    +firmName: String[1]: $src->meta::pure::functions::relation::navigationJoin(my::firmFunctionTyped(), meta::pure::functions::relation::JoinKind.RIGHT, {p, f | $p.FIRMID == $f.ID}).LEGALNAME\n", "",
                "JoinKind.RIGHT is not supported by navigationJoin(); use JoinKind.LEFT (the default) or JoinKind.INNER.");
    }

    @Test
    public void testQualifiedFilterJoinRejectsInvalidSource()
    {
        assertPersonMappingFails("    +firmIdCheck: Integer[1]: my::firmFunctionTyped()->meta::pure::functions::relation::filterJoin(my::firmFunctionTyped(), {p, f | $p.ID == $f.ID}).ID\n", "",
                "The source of 'filterJoin' must be $src or another navigationJoin/filterJoin call.");
    }

    @Test
    public void testQualifiedFilterJoinRejectsUsageNestedInAnotherExpression()
    {
        assertPersonMappingFails("    +gatedFirstName: String[1]: $src->meta::pure::functions::relation::filterJoin(my::firmFunctionTyped(), {p, f | $p.FIRMID == $f.ID}).FIRSTNAME->toOne()\n", "",
                "'filterJoin' may only be used as the property's entire value expression, or immediately followed by a single column access");
    }

    @Test
    public void testRootPackageUserFunctionsAreNotMistakenForJoins()
    {
        // Bare names also resolve to functions in Root, so a user's own root-level modelJoin/navigationJoin must win
        // over the platform construct exactly as it would in ordinary function resolution.
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("userFunc.pure", "###Pure\n" +
                "function modelJoin(x:Integer[1]):String[1]\n" +
                "{\n" +
                "  $x->toString();\n" +
                "}\n" +
                "function navigationJoin(x:Integer[1]):String[1]\n" +
                "{\n" +
                "  $x->toString();\n" +
                "}\n");
        compileTestSource("mapping.pure", inlineJoinMappingSource(
                "    +modelJoinCollision: String[1]: modelJoin($row.FIRMID),\n" +
                "    +navigationJoinCollision: String[1]: navigationJoin($row.FIRMID)\n", ""));

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        for (String propertyName : Lists.mutable.with("modelJoinCollision", "navigationJoinCollision"))
        {
            PropertyMapping propertyMapping = relSet._propertyMappings().detect(pm -> propertyName.equals(pm._property()._name()));
            Assert.assertTrue(propertyName + ": " + propertyMapping, propertyMapping instanceof RelationFunctionPropertyMapping);
            Assert.assertEquals("String", ((RelationFunctionPropertyMapping) propertyMapping)._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
        }
    }

    @Test
    public void testSameBatchUserNavigationJoinIsNotMistakenForJoin()
    {
        // navigationJoin/filterJoin are recognised during post-processing, once every function of the batch is
        // registered, so a same-named user function defined alongside the mapping wins.
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", "###Pure\n" +
                "function my::test::sameBatch::navigationJoin(x:Integer[1]):String[1]\n" +
                "{\n" +
                "  $x->toString();\n" +
                "}\n" +
                inlineJoinMappingSource("    +collision: String[1]: navigationJoin($row.FIRMID)\n", "")
                        .replace("###Mapping\n", "###Mapping\nimport my::test::sameBatch::*;\n"));

        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().getOnly();
        RelationFunctionPropertyMapping collision = (RelationFunctionPropertyMapping) relSet._propertyMappings().detect(pm -> "collision".equals(pm._property()._name()));
        Assert.assertEquals("String", collision._valueFn()._expressionSequence().getOnly()._genericType()._rawType()._name());
    }

    @Test
    public void testSameBatchUserModelJoinUsesNormalFunctionResolution()
    {
        // Resolve after the same-batch function is registered; preserve the user call as an ordinary valueFn.
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        String source = "###Pure\n" +
                "function my::test::sameBatch::modelJoin(condition:Function<{my::Person[1], my::Firm[1]->Boolean[1]}>[1]):my::Firm[1]\n" +
                "{\n" +
                "  ^my::Firm(id = 1, legalName = 'x');\n" +
                "}\n" +
                inlineJoinMappingSource("    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_CLASS_MAPPING)
                        .replace("###Mapping\n", "###Mapping\nimport my::test::sameBatch::*;\n");
        compileTestSource("mapping.pure", source);
        RelationFunctionInstanceSetImplementation relSet = (RelationFunctionInstanceSetImplementation)
                ((Mapping) runtime.getCoreInstance("my::testMapping"))._classMappings().detect(cm -> "person".equals(cm._id()));
        Assert.assertTrue(relSet._propertyMappings().detect(pm -> "firmRef".equals(pm._property()._name())) instanceof RelationFunctionPropertyMapping);
    }

    @Test
    public void testSameBatchUserModelJoinDoesNotShadowQualifiedModelJoin()
    {
        // A fully qualified call names the platform modelJoin, so a visible same-named user function can't make it ambiguous.
        compileTestSource("class.pure", RELATION_MAPPING_CLASS_SOURCE);
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("mapping.pure", "###Pure\n" +
                "function my::test::sameBatch::modelJoin(condition:Function<{my::Person[1], my::Firm[1]->Boolean[1]}>[1]):my::Firm[1]\n" +
                "{\n" +
                "  ^my::Firm(id = 1, legalName = 'x');\n" +
                "}\n" +
                inlineJoinMappingSource("    +firmRef: my::Firm[1]: meta::pure::functions::relation::modelJoin({p, f | $p.firmId == $f.id})\n", FIRM_RELATION_CLASS_MAPPING)
                        .replace("###Mapping\n", "###Mapping\nimport my::test::sameBatch::*;\n"));
        // The resolved join must survive unbind and rebind without storing the call spelling.
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("class.pure", RELATION_MAPPING_CLASS_SOURCE),
                        Tuples.pair("func.pure", RELATION_MAPPING_FUNCTION_SOURCE),
                        Tuples.pair("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE)
                ), this.getAdditionalVerifiers()
        );

        ModelJoinPropertyMapping firmRef = inlineModelJoin("my::testMapping", "person", "firmRef");
        Assert.assertEquals("firm", firmRef._targetSetImplementationId());
    }

    @Test
    public void testInlineModelJoinRenameRespectsNestedLambdaShadowingAParam()
    {
        // The nested lambda's own 'p' (a client name) shadows the condition's 'p' (the Person): only the outer
        // references become _mj_src. Renaming the inner one too would turn startsWith over a String into one
        // over a Person, which doesn't compile.
        compileInlineModelJoinMapping(
                "    +firmRef: my::Firm[1]: modelJoin({p, f | $p.firmId == $f.id && $f.clientNames->exists(p | $p->startsWith('A'))})\n",
                FIRM_RELATION_CLASS_MAPPING);

        LambdaFunction<?> condition = inlineModelJoin("my::testMapping", "person", "firmRef")._joinCondition();
        SimpleFunctionExpression and = (SimpleFunctionExpression) condition._expressionSequence().getOnly();
        SimpleFunctionExpression equal = (SimpleFunctionExpression) and._parametersValues().toList().get(0);
        SimpleFunctionExpression firmId = (SimpleFunctionExpression) equal._parametersValues().toList().get(0);
        Assert.assertEquals("_mj_src", ((VariableExpression) firmId._parametersValues().getOnly())._name());
        SimpleFunctionExpression exists = (SimpleFunctionExpression) and._parametersValues().toList().get(1);
        LambdaFunction<?> nested = (LambdaFunction<?>) ((InstanceValue) exists._parametersValues().toList().get(1))._values().getOnly();
        SimpleFunctionExpression startsWith = (SimpleFunctionExpression) nested._expressionSequence().getOnly();
        Assert.assertEquals("p", ((VariableExpression) startsWith._parametersValues().toList().get(0))._name());
    }

    @Test
    public void testInlineModelJoinDeclaredParamTypesSurviveRecompile()
    {
        compileRetypeableModelJoinMapping("my::retype::Firm");
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("retypeClasses.pure", RETYPE_CLASSES_SOURCE),
                        Tuples.pair("retypePerson.pure", retypePersonSource("my::retype::Firm"))
                ), this.getAdditionalVerifiers()
        );

        ModelJoinPropertyMapping modelJoin = inlineModelJoin("my::retype::testMapping", "person", "employer");
        Assert.assertEquals("firm", modelJoin._targetSetImplementationId());
        Assert.assertEquals(Lists.mutable.with("_mj_src:Person", "_mj_tgt:Firm"), joinConditionParams(modelJoin));
    }

    @Test
    public void testInlineModelJoinDeclaredParamTypeIsRevalidatedOnRecompile()
    {
        // Retyping the mapped property elsewhere must fail an incremental recompile of the mapping just as it fails
        // a full compile (see testInlineModelJoinRejectsDeclaredParamTypeOfRetypedProperty): the declared type
        // survives unbinding the mapping.
        compileRetypeableModelJoinMapping("my::retype::Firm");
        runtime.modify("retypePerson.pure", retypePersonSource("my::retype::Company"));
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        Assert.assertEquals(RETYPED_PROPERTY_RECOMPILE_MESSAGE, e.getInfo());
    }

    @Test
    public void testInlineModelJoinRejectsDeclaredParamTypeOfRetypedProperty()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, () -> compileRetypeableModelJoinMapping("my::retype::Company"));
        Assert.assertEquals(RETYPED_PROPERTY_MESSAGE, e.getInfo());
    }

    private static final String RETYPED_PROPERTY_MESSAGE = "modelJoin()'s join condition parameter 'f' is declared as 'my::retype::Firm', but the class of property 'employer' is 'my::retype::Company'.";

    // An unbind leaves the param with its positional _mj_tgt name, so the recompile names it by position.
    private static final String RETYPED_PROPERTY_RECOMPILE_MESSAGE = "modelJoin()'s join condition's second parameter is declared as 'my::retype::Firm', but the class of property 'employer' is 'my::retype::Company'.";

    @Test
    public void testInlineModelJoinDeclaredSupertypeParamsAreTypedToClassMappings()
    {
        // A declared supertype is only checked: the params are still typed to the class mappings, so the condition
        // sees the classes' own properties. That holds after an incremental recompile too, which (as a full compile
        // would) accepts retyping the property to another subtype of the declared type.
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("retypeClasses.pure", RETYPE_CLASSES_SOURCE);
        compileTestSource("retypePerson.pure", retypePersonSource("my::retype::Firm"));
        compileTestSource("retypeMapping.pure", RETYPE_MAPPING_SOURCE.replace("p: my::retype::Person[1], f: my::retype::Firm[1]",
                "p: meta::pure::metamodel::type::Any[1], f: meta::pure::metamodel::type::Any[1]"));
        Assert.assertEquals(Lists.mutable.with("_mj_src:Person", "_mj_tgt:Firm"), joinConditionParams(inlineModelJoin("my::retype::testMapping", "person", "employer")));

        runtime.modify("retypePerson.pure", retypePersonSource("my::retype::Company"));
        runtime.compile();
        ModelJoinPropertyMapping modelJoin = inlineModelJoin("my::retype::testMapping", "person", "employer");
        Assert.assertEquals("company", modelJoin._targetSetImplementationId());
        Assert.assertEquals(Lists.mutable.with("_mj_src:Person", "_mj_tgt:Company"), joinConditionParams(modelJoin));
    }

    @Test
    @Ignore("Preserving declared supertypes across incremental recompilation is deferred")
    public void testInlineModelJoinDeclaredSupertypeIsRevalidatedOnRecompile()
    {
        compileSupertypeModelJoinMapping("my::retype::Firm", SUPERTYPE_CLASSES_SOURCE);
        RuntimeVerifier.deleteCompileAndReloadMultipleTimesIsStable(
                runtime, functionExecution, Lists.fixedSize.of(
                        Tuples.pair("retypeClasses.pure", SUPERTYPE_CLASSES_SOURCE),
                        Tuples.pair("retypePerson.pure", retypePersonSource("my::retype::Firm"))
                ), this.getAdditionalVerifiers()
        );

        for (int i = 0; i < 2; i++)
        {
            runtime.modify("retypePerson.pure", retypePersonSource("my::retype::Company"));
            PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
            Assert.assertEquals("modelJoin()'s join condition's second parameter is declared as 'my::retype::Organization', but the class of property 'employer' is 'my::retype::Company'.", e.getInfo());
            runtime.modify("retypePerson.pure", retypePersonSource("my::retype::Firm"));
            runtime.compile();
        }
    }

    @Test
    public void testInlineModelJoinDeclaredSupertypeRejectsUnrelatedClassOnCleanCompile()
    {
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class,
                () -> compileSupertypeModelJoinMapping("my::retype::Company", SUPERTYPE_CLASSES_SOURCE));
        Assert.assertEquals("modelJoin()'s join condition parameter 'f' is declared as 'my::retype::Organization', but the class of property 'employer' is 'my::retype::Company'.", e.getInfo());
    }

    @Test
    public void testInlineModelJoinDeclaredSupertypeAllowsAnotherSubtypeOnRecompile()
    {
        compileSupertypeModelJoinMapping("my::retype::Firm",
                SUPERTYPE_CLASSES_SOURCE.replace("Class my::retype::Company", "Class my::retype::Company extends my::retype::Organization"));
        runtime.modify("retypePerson.pure", retypePersonSource("my::retype::Company"));
        runtime.compile();
        ModelJoinPropertyMapping modelJoin = inlineModelJoin("my::retype::testMapping", "person", "employer");
        Assert.assertEquals("company", modelJoin._targetSetImplementationId());
        Assert.assertEquals(Lists.mutable.with("_mj_src:Person", "_mj_tgt:Company"), joinConditionParams(modelJoin));
    }

    private void compileSupertypeModelJoinMapping(String employerType, String classesSource)
    {
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("retypeClasses.pure", classesSource);
        compileTestSource("retypePerson.pure", retypePersonSource(employerType));
        compileTestSource("retypeMapping.pure", RETYPE_MAPPING_SOURCE.replace("f: my::retype::Firm[1]", "f: my::retype::Organization[1]"));
    }

    private static final String RETYPE_CLASSES_SOURCE = "###Pure\n" +
            "Class my::retype::Firm\n" +
            "{\n" +
            "  id: Integer[1];\n" +
            "}\n" +
            "\n" +
            "Class my::retype::Company\n" +
            "{\n" +
            "  id: Integer[1];\n" +
            "}\n";

    private static final String SUPERTYPE_CLASSES_SOURCE = "Class my::retype::Organization {}\n" +
            RETYPE_CLASSES_SOURCE.replace("Class my::retype::Firm", "Class my::retype::Firm extends my::retype::Organization");

    private static final String RETYPE_MAPPING_SOURCE = "###Mapping\n" +
            "Mapping my::retype::testMapping\n" +
            "(\n" +
            "  *my::retype::Person[person]: Relation\n" +
            "  {\n" +
            "    ~func my::personFunctionTyped__Relation_1_\n" +
            "    firmId: FIRMID,\n" +
            "    employer: modelJoin({p: my::retype::Person[1], f: my::retype::Firm[1] | $p.firmId == $f.id})\n" +
            "  }\n" +
            "  *my::retype::Firm[firm]: Relation\n" +
            "  {\n" +
            "    ~func my::firmFunctionTyped__Relation_1_\n" +
            "    id: ID\n" +
            "  }\n" +
            "  *my::retype::Company[company]: Relation\n" +
            "  {\n" +
            "    ~func my::firmFunctionTyped__Relation_1_\n" +
            "    id: ID\n" +
            "  }\n" +
            ")\n";

    private static String retypePersonSource(String employerType)
    {
        return "###Pure\n" +
                "Class my::retype::Person\n" +
                "{\n" +
                "  firmId: Integer[1];\n" +
                "  employer: " + employerType + "[0..1];\n" +
                "}\n";
    }

    private void compileRetypeableModelJoinMapping(String employerType)
    {
        compileTestSource("func.pure", RELATION_MAPPING_FUNCTION_SOURCE);
        compileTestSource("joinFunc.pure", NAVIGATION_FILTER_JOIN_FUNCTION_SOURCE);
        compileTestSource("retypeClasses.pure", RETYPE_CLASSES_SOURCE);
        compileTestSource("retypePerson.pure", retypePersonSource(employerType));
        compileTestSource("retypeMapping.pure", RETYPE_MAPPING_SOURCE);
    }
}
