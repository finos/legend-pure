// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.compiler.reference.v1;

import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.InstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingClass;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregateSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.aggregationAware.AggregationAwareSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.serialization.compiler.reference.v1.TestReferenceIdExtensionV1;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReferenceIdExtensionV1Mapping extends TestReferenceIdExtensionV1
{
    @BeforeClass
    public static void extraSource()
    {
        compileTestSource(
                "/ref_test/mapping/test_mapping.pure",
                "import test::model::*;\n" +
                        "\n" +
                        "Class test::model::SourceSimpleClass\n" +
                        "{\n" +
                        "  names : String[*];\n" +
                        "  id : Integer[1];\n" +
                        "  titles : String[*];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::SourceLeft\n" +
                        "{\n" +
                        "  names : String[*];\n" +
                        "  id : Integer[1];\n" +
                        "  titles : String[*];\n" +
                        "  family : String[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::SourceRight\n" +
                        "{\n" +
                        "  id : Integer[0..1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::Sales\n" +
                        "{\n" +
                        "   id: Integer[1];\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   revenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::FiscalCalendar\n" +
                        "{\n" +
                        "   date: Date[1];\n" +
                        "   fiscalYear: Integer[1];\n" +
                        "   fiscalMonth: Integer[1];\n" +
                        "   fiscalQtr: Integer[1];\n" +
                        "}\n" +
                        "\n" +
                        "Class test::model::Sales_By_Date\n" +
                        "{\n" +
                        "   salesDate: FiscalCalendar[1];\n" +
                        "   netRevenue: Float[1];\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "\n" +
                        "import test::model::*;\n" +
                        "\n" +
                        "Mapping test::mapping::TestMapping\n" +
                        "(\n" +
                        "   SimpleClass : Pure\n" +
                        "   {\n" +
                        "      ~src SourceSimpleClass\n" +
                        "      ~filter !$src.names->isEmpty()\n" +
                        "      name : $src.names->joinStrings(' '),\n" +
                        "      id : $src.id,\n" +
                        "      +title : String[0..1] : $src.titles->first()\n" +
                        "   }\n" +
                        "\n" +
                        "   Left : Pure\n" +
                        "   {\n" +
                        "      ~src SourceLeft\n" +
                        "      name : $src.names->joinStrings(' '),\n" +
                        "      +titles : String[*] : $src.titles,\n" +
                        "      +family : String[0..1] : $src.family\n" +
                        "   }\n" +
                        "\n" +
                        "   Right : Pure\n" +
                        "   {\n" +
                        "      ~src SourceRight\n" +
                        "      id : if($src.id->isEmpty(), |0, |$src.id->toOne())\n" +
                        "   }\n" +
                        "\n" +
                        "   SimpleEnumeration: EnumerationMapping simpleEnum\n" +
                        "   {\n" +
                        "      VAL1: 1,\n" +
                        "      VAL2: 2\n" +
                        "   }\n" +
                        "\n" +
                        "   AggregationKind: EnumerationMapping aggKind\n" +
                        "   {\n" +
                        "      None: 'none',\n" +
                        "      Shared: 'shared',\n" +
                        "      Composite: ['composite', 'comp']\n" +
                        "   }\n" +
                        "\n" +
                        "   FiscalCalendar [b]: Pure {\n" +
                        "      ~src FiscalCalendar\n" +
                        "      date : $src.date,\n" +
                        "      fiscalYear : $src.fiscalYear,\n" +
                        "      fiscalMonth : $src.fiscalMonth,\n" +
                        "      fiscalQtr : $src.fiscalQtr\n" +
                        "   }\n" +
                        "   \n" +
                        "   Sales [a]: AggregationAware {\n" +
                        "      Views : [\n" +
                        "         (\n" +
                        "            ~modelOperation : {\n" +
                        "               ~canAggregate true,\n" +
                        "               ~groupByFunctions (\n" +
                        "                  $this.salesDate\n" +
                        "               ),\n" +
                        "               ~aggregateValues (\n" +
                        "                  ( ~mapFn: $this.revenue, ~aggregateFn: $mapped->plus() )\n" +
                        "               )\n" +
                        "            },\n" +
                        "            ~aggregateMapping : Pure {\n" +
                        "               ~src Sales_By_Date\n" +
                        "               salesDate [b] : $src.salesDate,\n" +
                        "               revenue : $src.netRevenue\n" +
                        "            }\n" +
                        "         )\n" +
                        "      ],\n" +
                        "      ~mainMapping : Pure {\n" +
                        "         ~src Sales\n" +
                        "         salesDate [b] : $src.salesDate,\n" +
                        "         revenue : $src.revenue\n" +
                        "      }\n" +
                        "   }\n" +
                        ")\n"
        );
    }

    @Test
    public void testMapping()
    {
        String path = "test::mapping::TestMapping";
        Mapping mapping = getCoreInstance(path);

        assertRefId(path, mapping);

        mapping._classMappings().forEach(cm -> testSetImplementation(cm, path + ".classMappings[id='" + cm._id() + "']"));

        mapping._associationMappings().forEach(assocMapping ->
        {
            String assocMappingId = assertRefId(path + ".associationMappings[id='" + assocMapping._id() + "']", assocMapping);

            int[] counter = {0};
            assocMapping._propertyMappings().forEach(propertyMapping -> assertRefId(assocMappingId + ".propertyMappings[" + counter[0]++ + "]", propertyMapping));
        });

        mapping._enumerationMappings().forEach(enumMapping ->
        {
            String enumMappingId = assertRefId(path + ".enumerationMappings['" + enumMapping._name() + "']", enumMapping);

            int[] counter = {0};
            enumMapping._enumValueMappings().forEach(enumValueMapping -> assertRefId(enumMappingId + ".enumValueMappings[" + counter[0]++ + "]", enumValueMapping));
        });
    }

    private void testSetImplementation(SetImplementation classMapping, String expectedId)
    {
        String setImplId = assertRefId(expectedId, classMapping);
        if (classMapping instanceof InstanceSetImplementation)
        {
            testInstanceSetImplementation((InstanceSetImplementation) classMapping, setImplId);
        }
        else
        {
            Assert.fail("Unhandled set implementation (" + setImplId + "): " + classMapping);
        }
    }

    private void testInstanceSetImplementation(InstanceSetImplementation classMapping, String classMappingId)
    {
        MappingClass<?> mappingClass = classMapping._mappingClass();
        if (mappingClass != null)
        {
            String mappingClassId = assertRefId(classMappingId + ".mappingClass", mappingClass);
            mappingClass._properties().forEach(property -> assertRefId(mappingClassId + ".properties['" + property._name() + "']", property));
        }

        AggregateSpecification aggregateSpec = classMapping._aggregateSpecification();
        if (aggregateSpec != null)
        {
            testAggregateSpecification(aggregateSpec, classMappingId);
        }

        if (classMapping instanceof PureInstanceSetImplementation)
        {
            testPureInstanceSetImplementation((PureInstanceSetImplementation) classMapping, classMappingId);
        }
        else if (classMapping instanceof AggregationAwareSetImplementation)
        {
            testAggregationAwareSetImplementation((AggregationAwareSetImplementation) classMapping, classMappingId);
        }
        else
        {
            Assert.fail("Unhandled instance set implementation (" + classMappingId + "): " + classMapping);
        }
    }

    private void testPureInstanceSetImplementation(PureInstanceSetImplementation classMapping, String classMappingId)
    {
        LambdaFunction<?> filter = classMapping._filter();
        if (filter != null)
        {
            testLambdaFunction(filter, classMappingId + ".filter");
        }

        int[] counter = {0};
        classMapping._propertyMappings().forEach(propertyMapping -> assertRefId(classMappingId + ".propertyMappings[" + counter[0]++ + "]", propertyMapping));
    }

    private void testAggregationAwareSetImplementation(AggregationAwareSetImplementation classMapping, String classMappingId)
    {
        InstanceSetImplementation mainImpl = classMapping._mainSetImplementation();
        if (mainImpl != null)
        {
            String mainImplId = assertRefId(classMappingId + ".mainSetImplementation", mainImpl);
            testSetImplementation(mainImpl, mainImplId);
        }

        int[] counter = {0};
        classMapping._aggregateSetImplementations().forEach(aggImpl ->
        {
            String aggImplId = assertRefId(classMappingId + ".aggregateSetImplementations[" + counter[0]++ + "]", aggImpl);
            testAggregateSpecification(aggImpl._aggregateSpecification(), aggImplId);
        });
    }

    private void testAggregateSpecification(AggregateSpecification aggSpec, String classMappingId)
    {
        String aggSpecId = assertRefId(classMappingId + ".aggregateSpecification", aggSpec);

        int[] counter = {0};
        aggSpec._groupByFunctions().forEach(groupBySpec ->
        {
            String groupBySpecId = assertRefId(aggSpecId + ".groupByFunctions[" + counter[0]++ + "]", groupBySpec);
            LambdaFunction<?> groupBy = groupBySpec._groupByFn();
            if (groupBy != null)
            {
                testLambdaFunction(groupBy, groupBySpecId + ".groupByFn");
            }
        });

        counter[0] = 0;
        aggSpec._aggregateValues().forEach(aggValue ->
        {
            String aggValueId = assertRefId(aggSpecId + ".aggregateValues[" + counter[0]++ + "]", aggValue);

            LambdaFunction<?> aggFn = aggValue._aggregateFn();
            if (aggFn != null)
            {
                testLambdaFunction(aggFn, aggValueId + ".aggregateFn");
            }

            LambdaFunction<?> mapFn = aggValue._mapFn();
            if (mapFn != null)
            {
                testLambdaFunction(mapFn, aggValueId + ".mapFn");
            }
        });
    }

    private void testLambdaFunction(LambdaFunction<?> lambda, String expectedId)
    {
        String lambdaId = assertRefId(expectedId, lambda);

        testGenericType(lambda._classifierGenericType(), lambdaId + ".classifierGenericType");

        int[] counter = {0};
        lambda._expressionSequence().forEach(expr -> testValueSpecification(expr, lambdaId + ".expressionSequence[" + counter[0]++ + "]"));
    }

    private void testValueSpecification(ValueSpecification valueSpec, String expectedId)
    {
        String valueSpecId = assertRefId(expectedId, valueSpec);
        testGenericType(valueSpec._genericType(), valueSpecId + ".genericType");
        if (valueSpec instanceof FunctionExpression)
        {
            int[] counter = {0};
            ((FunctionExpression) valueSpec)._parametersValues().forEach(v -> testValueSpecification(v, valueSpecId + ".parametersValues[" + counter[0]++ + "]"));
        }
    }

    private void testGenericType(GenericType genericType, String expectedId)
    {
        String genericTypeId = assertRefId(expectedId, genericType);
        Type rawType = genericType._rawType();
        if (rawType instanceof FunctionType)
        {
            testFunctionType((FunctionType) rawType, genericTypeId + ".rawType");
        }
        int[] counter = {0};
        genericType._typeArguments().forEach(ta -> testGenericType(ta, genericTypeId + ".typeArguments[" + counter[0]++ + "]"));
    }

    private void testFunctionType(FunctionType functionType, String expectedId)
    {
        String funcTypeId = assertRefId(expectedId, functionType);
        functionType._parameters().forEach(param ->
        {
            String paramId = assertRefId(funcTypeId + ".parameters['" + param._name() + "']", param);
            testGenericType(param._genericType(), paramId + ".genericType");
        });

        testGenericType(functionType._returnType(), funcTypeId + ".returnType");
    }
}
