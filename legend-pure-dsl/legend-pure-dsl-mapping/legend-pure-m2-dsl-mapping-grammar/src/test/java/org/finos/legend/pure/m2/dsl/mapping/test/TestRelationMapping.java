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
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
                "    +concatenated: String[1]: $src.FIRSTNAME + ' ' + $src.FIRSTNAME\n" +
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
                "    +ageBucket: String[1]: if($src.AGE > 65, |'senior', |'other')\n" +
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
                "    age: $src.FIRSTNAME\n" +
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
                "    firstName: $src.MISSING\n" +
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
                "    firstName: [$src.FIRSTNAME, $src.FIRSTNAME]\n" +
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
                "    firstName: $src.FIRSTNAME,\n" +
                "    gender: EnumerationMapping GenderMapping: $src.GENDER\n" +
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
                "      city: $src.CITY\n" +
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

        // Property mapping under ~src resolves $src.FIRSTNAME the same way.
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
        // sub-property $src.<col> must resolve against the same row type.
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
        // Mixes bare-column, $src expression, ~src inline source, embedded
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
                "    +concatenated: String[1]: $src.FIRSTNAME + ' ' + $src.FIRSTNAME,\n" +
                "    address\n" +
                "    (\n" +
                "      city: $src.CITY\n" +
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
    // explicit `$src.'…'` expression forms must lower to a property
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
            "  1->cast(@Relation<('FIRST NAME':String[1], AGE:Integer[1])>);\n" +
            "}\n";

    @Test
    public void testRelationMappingWithQuotedColumnBareForm()
    {
        // Bare-column form: `firstName: 'FIRST NAME'` must lower to
        // `{| $src.'FIRST NAME'}` and resolve to the column whose name
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
    public void testRelationMappingWithQuotedColumnExplicitSrcForm()
    {
        // Explicit `$src.'FIRST NAME'` expression form must resolve the
        // quoted column against the row type and produce a String-typed
        // value, exactly like the bare-column form.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionQuoted__Relation_1_\n" +
                "    firstName: $src.'FIRST NAME'\n" +
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
        // `$src.'FIRST NAME'` participating in a multi-step expression: the
        // quoted column accessor inside the body must still resolve and the
        // overall expression's result type must match the property type.
        String mappingSource = "###Mapping\n" +
                "Mapping my::testMapping\n" +
                "(\n" +
                "  *my::Person[person]: Relation\n" +
                "  {\n" +
                "    ~func my::personFunctionQuoted__Relation_1_\n" +
                "    +greeted: String[1]: 'Hi ' + $src.'FIRST NAME'\n" +
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
}
