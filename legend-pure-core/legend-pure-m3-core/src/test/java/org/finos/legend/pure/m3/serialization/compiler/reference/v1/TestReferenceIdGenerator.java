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

package org.finos.legend.pure.m3.serialization.compiler.reference.v1;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.Counter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.tools.PackageableElementIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestReferenceIdGenerator extends AbstractReferenceTest
{
    private static ReferenceIdGenerator idGenerator;

    @BeforeClass
    public static void setUpIdGenerator()
    {
        idGenerator = new ReferenceIdGenerator(processorSupport);
    }

    @Test
    public void testTopLevelAndPackaged()
    {
        PackageableElementIterable.fromProcessorSupport(processorSupport).forEach(this::assertIds);
    }

    @Test
    public void testVirtualPackages()
    {
        String testPath = "test";
        org.finos.legend.pure.m3.coreinstance.Package testPackage = getCoreInstance(testPath);
        Assert.assertNull(testPackage.getSourceInformation());
        assertIds(testPath, Maps.immutable.with(testPath, testPackage));

        String testModelPath = "test::model";
        Package testModelPackage = getCoreInstance(testModelPath);
        Assert.assertNull(testModelPackage.getSourceInformation());
        assertIds(testModelPath, Maps.immutable.with(testModelPath, testModelPackage));
    }

    @Test
    public void testSimpleClass()
    {
        String path = "test::model::SimpleClass";
        Class<?> simpleClass = getCoreInstance(path);
        Property<?, ?> name = findProperty(simpleClass, "name");
        Property<?, ?> id = findProperty(simpleClass, "id");
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, simpleClass)
                .withKeyValue(path + ".classifierGenericType", simpleClass._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", at(simpleClass._generalizations(), 0))
                .withKeyValue(path + ".generalizations[0].general", at(simpleClass._generalizations(), 0)._general())
                .withKeyValue(path + ".properties['name']", name)
                .withKeyValue(path + ".properties['name'].classifierGenericType", name._classifierGenericType())
                .withKeyValue(path + ".properties['name'].classifierGenericType.typeArguments[0]", typeArgument(name._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['name'].classifierGenericType.typeArguments[1]", typeArgument(name._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['name'].genericType", name._genericType())
                .withKeyValue(path + ".properties['id']", id)
                .withKeyValue(path + ".properties['id'].classifierGenericType", id._classifierGenericType())
                .withKeyValue(path + ".properties['id'].classifierGenericType.typeArguments[0]", typeArgument(id._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['id'].classifierGenericType.typeArguments[1]", typeArgument(id._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['id'].genericType", id._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testEnumeration()
    {
        String path = "test::model::SimpleEnumeration";
        Enumeration<? extends Enum> testEnumeration = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, testEnumeration)
                .withKeyValue(path + ".classifierGenericType", testEnumeration._classifierGenericType())
                .withKeyValue(path + ".classifierGenericType.typeArguments[0]", typeArgument(testEnumeration._classifierGenericType(), 0))
                .withKeyValue(path + ".generalizations[0]", at(testEnumeration._generalizations(), 0))
                .withKeyValue(path + ".generalizations[0].general", at(testEnumeration._generalizations(), 0)._general())
                .withKeyValue(path + ".values['VAL1']", at(testEnumeration._values(), 0))
                .withKeyValue(path + ".values['VAL2']", at(testEnumeration._values(), 1));

        assertIds(path, expected);
    }

    @Test
    public void testAssociation()
    {
        String path = "test::model::LeftRight";
        Association leftRight = getCoreInstance(path);

        MutableMap<String, Object> expectedLR = Maps.mutable.<String, Object>with(path, leftRight)
                .withKeyValue(path + ".classifierGenericType", leftRight._classifierGenericType());

        Property<?, ?> toLeft = findProperty(leftRight, "toLeft");
        expectedLR.put(path + ".properties['toLeft']", toLeft);
        expectedLR.put(path + ".properties['toLeft'].classifierGenericType", toLeft._classifierGenericType());
        expectedLR.put(path + ".properties['toLeft'].classifierGenericType.typeArguments[0]", typeArgument(toLeft._classifierGenericType(), 0));
        expectedLR.put(path + ".properties['toLeft'].classifierGenericType.typeArguments[1]", typeArgument(toLeft._classifierGenericType(), 1));
        expectedLR.put(path + ".properties['toLeft'].genericType", toLeft._genericType());

        Property<?, ?> toRight = findProperty(leftRight, "toRight");
        expectedLR.put(path + ".properties['toRight']", toRight);
        expectedLR.put(path + ".properties['toRight'].classifierGenericType", toRight._classifierGenericType());
        expectedLR.put(path + ".properties['toRight'].classifierGenericType.typeArguments[0]", typeArgument(toRight._classifierGenericType(), 0));
        expectedLR.put(path + ".properties['toRight'].classifierGenericType.typeArguments[1]", typeArgument(toRight._classifierGenericType(), 1));
        expectedLR.put(path + ".properties['toRight'].genericType", toRight._genericType());

        QualifiedProperty<?> toLeftByName = findQualifiedProperty(leftRight, "toLeft(String[1])");
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])']", toLeftByName);
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType", toLeftByName._classifierGenericType());
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0]", typeArgument(toLeftByName._classifierGenericType(), 0));
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toLeftByName._classifierGenericType(), 0)._rawType());
//        expectedLR.put(
//                path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this']",
//                findFuncTypeParam(typeArgument(toLeftByName._classifierGenericType(), 0)._rawType(), "this"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toLeftByName._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['name']",
                funcTypeParam(typeArgument(toLeftByName._classifierGenericType(), 0)._rawType(), "name"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['name'].genericType",
                funcTypeParam(typeArgument(toLeftByName._classifierGenericType(), 0)._rawType(), "name")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toLeftByName._classifierGenericType(), 0)._rawType()));
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0]", exprSeq(toLeftByName, 0));
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].genericType", exprSeq(toLeftByName, 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toLeftByName, 0), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toLeftByName, 0), 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) paramValue(exprSeq(toLeftByName, 0), 0))._parametersValues().getOnly());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) paramValue(exprSeq(toLeftByName, 0), 0))._parametersValues().getOnly()._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toLeftByName, 0), 0))._propertyName());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toLeftByName, 0), 1));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toLeftByName, 0), 1)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toLeftByName, 0), 1)._genericType(), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toLeftByName, 0), 1)._genericType(), 0)._rawType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['l']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toLeftByName, 0), 1)._genericType(), 0)._rawType(), "l"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['l'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toLeftByName, 0), 1)._genericType(), 0)._rawType(), "l")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toLeftByName, 0), 1)._genericType(), 0)._rawType()));
        LambdaFunction<?> nameFilterLambda = instanceValueValue(paramValue(exprSeq(toLeftByName, 0), 1), 0);
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0]",
                nameFilterLambda);
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                nameFilterLambda._classifierGenericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(nameFilterLambda._classifierGenericType(), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(nameFilterLambda._classifierGenericType(), 0)._rawType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['l']",
                funcTypeParam(typeArgument(nameFilterLambda._classifierGenericType(), 0)._rawType(), "l"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['l'].genericType",
                funcTypeParam(typeArgument(nameFilterLambda._classifierGenericType(), 0)._rawType(), "l")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(nameFilterLambda._classifierGenericType(), 0)._rawType()));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                nameFilterLambda._expressionSequence().getOnly());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                nameFilterLambda._expressionSequence().getOnly()._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(nameFilterLambda, 0), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(nameFilterLambda, 0), 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) paramValue(exprSeq(nameFilterLambda, 0), 0))._parametersValues().getOnly());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) paramValue(exprSeq(nameFilterLambda, 0), 0))._parametersValues().getOnly()._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(nameFilterLambda, 0), 0))._propertyName());
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(nameFilterLambda, 0), 1));
        expectedLR.put(
                path + ".qualifiedProperties[id='toLeft(String[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(nameFilterLambda, 0), 1)._genericType());
        expectedLR.put(path + ".qualifiedProperties[id='toLeft(String[1])'].genericType", toLeftByName._genericType());

        QualifiedProperty<?> toRightById = findQualifiedProperty(leftRight, "toRight(Integer[1])");
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])']", toRightById);
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType", toRightById._classifierGenericType());
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0]", typeArgument(toRightById._classifierGenericType(), 0));
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toRightById._classifierGenericType(), 0)._rawType());
//        expectedLR.put(
//                path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this']",
//                findFuncTypeParam(typeArgument(toRightById._classifierGenericType(), 0)._rawType(), "this"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toRightById._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.parameters['id']",
                funcTypeParam(typeArgument(toRightById._classifierGenericType(), 0)._rawType(), "id"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.parameters['id'].genericType",
                funcTypeParam(typeArgument(toRightById._classifierGenericType(), 0)._rawType(), "id")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toRightById._classifierGenericType(), 0)._rawType()));
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0]", exprSeq(toRightById, 0));
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].genericType", exprSeq(toRightById, 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toRightById, 0), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toRightById, 0), 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) paramValue(exprSeq(toRightById, 0), 0))._parametersValues().getOnly());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) paramValue(exprSeq(toRightById, 0), 0))._parametersValues().getOnly()._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toRightById, 0), 0))._propertyName());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toRightById, 0), 1));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toRightById, 0), 1)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toRightById, 0), 1)._genericType(), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toRightById, 0), 1)._genericType(), 0)._rawType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['r']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toRightById, 0), 1)._genericType(), 0)._rawType(), "r"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['r'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toRightById, 0), 1)._genericType(), 0)._rawType(), "r")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toRightById, 0), 1)._genericType(), 0)._rawType()));
        LambdaFunction<?> idFilterLambda = (LambdaFunction<?>) ((InstanceValue) paramValue(exprSeq(toRightById, 0), 1))._values().getOnly();
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0]",
                idFilterLambda);
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                idFilterLambda._classifierGenericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(idFilterLambda._classifierGenericType(), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(idFilterLambda._classifierGenericType(), 0)._rawType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['r']",
                funcTypeParam(typeArgument(idFilterLambda._classifierGenericType(), 0)._rawType(), "r"));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['r'].genericType",
                funcTypeParam(typeArgument(idFilterLambda._classifierGenericType(), 0)._rawType(), "r")._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(idFilterLambda._classifierGenericType(), 0)._rawType()));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(idFilterLambda, 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(idFilterLambda, 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(idFilterLambda, 0), 0));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(idFilterLambda, 0), 0)._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) paramValue(exprSeq(idFilterLambda, 0), 0))._parametersValues().getOnly());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) paramValue(exprSeq(idFilterLambda, 0), 0))._parametersValues().getOnly()._genericType());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(idFilterLambda, 0), 0))._propertyName());
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(idFilterLambda, 0), 1));
        expectedLR.put(
                path + ".qualifiedProperties[id='toRight(Integer[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(idFilterLambda, 0), 1)._genericType());
        expectedLR.put(path + ".qualifiedProperties[id='toRight(Integer[1])'].genericType", toRightById._genericType());

        assertIds(path, expectedLR);

        String leftPath = "test::model::Left";
        Class<?> left = getCoreInstance(leftPath);
        Property<?, ?> leftName = findProperty(left, "name");
        MutableMap<String, CoreInstance> expectedL = Maps.mutable.<String, CoreInstance>with(leftPath, left)
                .withKeyValue(leftPath + ".classifierGenericType", left._classifierGenericType())
                .withKeyValue(leftPath + ".generalizations[0]", left._generalizations().getOnly())
                .withKeyValue(leftPath + ".generalizations[0].general", left._generalizations().getOnly()._general())
                .withKeyValue(leftPath + ".properties['name']", leftName)
                .withKeyValue(leftPath + ".properties['name'].classifierGenericType", leftName._classifierGenericType())
                .withKeyValue(leftPath + ".properties['name'].classifierGenericType.typeArguments[0]", typeArgument(leftName._classifierGenericType(), 0))
                .withKeyValue(leftPath + ".properties['name'].classifierGenericType.typeArguments[1]", typeArgument(leftName._classifierGenericType(), 1))
                .withKeyValue(leftPath + ".properties['name'].genericType", leftName._genericType());
        assertIds(leftPath, expectedL);

        String rightPath = "test::model::Right";
        Class<?> right = getCoreInstance(rightPath);
        Property<?, ?> rightId = findProperty(right, "id");
        MutableMap<String, CoreInstance> expectedR = Maps.mutable.<String, CoreInstance>with(rightPath, right)
                .withKeyValue(rightPath + ".classifierGenericType", right._classifierGenericType())
                .withKeyValue(rightPath + ".generalizations[0]", right._generalizations().getOnly())
                .withKeyValue(rightPath + ".generalizations[0].general", right._generalizations().getOnly()._general())
                .withKeyValue(rightPath + ".properties['id']", rightId)
                .withKeyValue(rightPath + ".properties['id'].classifierGenericType", rightId._classifierGenericType())
                .withKeyValue(rightPath + ".properties['id'].classifierGenericType.typeArguments[0]", typeArgument(rightId._classifierGenericType(), 0))
                .withKeyValue(rightPath + ".properties['id'].classifierGenericType.typeArguments[1]", typeArgument(rightId._classifierGenericType(), 1))
                .withKeyValue(rightPath + ".properties['id'].genericType", rightId._genericType());
        assertIds(rightPath, expectedR);
    }

    @Test
    public void testSimpleProfile()
    {
        String path = "test::model::SimpleProfile";
        Profile testProfile = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, testProfile)
                .withKeyValue(path + ".classifierGenericType", testProfile._classifierGenericType())
                .withKeyValue(path + ".p_stereotypes[value='st1']", at(testProfile._p_stereotypes(), 0))
                .withKeyValue(path + ".p_stereotypes[value='st2']", at(testProfile._p_stereotypes(), 1))
                .withKeyValue(path + ".p_tags[value='t1']", at(testProfile._p_tags(), 0))
                .withKeyValue(path + ".p_tags[value='t2']", at(testProfile._p_tags(), 1))
                .withKeyValue(path + ".p_tags[value='t3']", at(testProfile._p_tags(), 2));

        assertIds(path, expected);
    }

    @Test
    public void testClassWithGeneralizations()
    {
        String path = "test::model::BothSides";
        Class<?> bothSides = getCoreInstance(path);
        Property<?, ?> leftCount = findProperty(bothSides, "leftCount");
        Property<?, ?> rightCount = findProperty(bothSides, "rightCount");
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, bothSides)
                .withKeyValue(path + ".classifierGenericType", bothSides._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", at(bothSides._generalizations(), 0))
                .withKeyValue(path + ".generalizations[0].general", at(bothSides._generalizations(), 0)._general())
                .withKeyValue(path + ".generalizations[1]", at(bothSides._generalizations(), 1))
                .withKeyValue(path + ".generalizations[1].general", at(bothSides._generalizations(), 1)._general())
                .withKeyValue(path + ".properties['leftCount']", leftCount)
                .withKeyValue(path + ".properties['leftCount'].classifierGenericType", leftCount._classifierGenericType())
                .withKeyValue(path + ".properties['leftCount'].classifierGenericType.typeArguments[0]", typeArgument(leftCount._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['leftCount'].classifierGenericType.typeArguments[1]", typeArgument(leftCount._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['leftCount'].genericType", leftCount._genericType())
                .withKeyValue(path + ".properties['rightCount']", rightCount)
                .withKeyValue(path + ".properties['rightCount'].classifierGenericType", rightCount._classifierGenericType())
                .withKeyValue(path + ".properties['rightCount'].classifierGenericType.typeArguments[0]", typeArgument(rightCount._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['rightCount'].classifierGenericType.typeArguments[1]", typeArgument(rightCount._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['rightCount'].genericType", rightCount._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testClassWithAnnotations()
    {
        String path = "test::model::ClassWithAnnotations";
        Class<?> classWithAnnotations = getCoreInstance(path);
        Property<?, ?> deprecated = findProperty(classWithAnnotations, "deprecated");
        Property<?, ?> alsoDeprecated = findProperty(classWithAnnotations, "alsoDeprecated");
        Property<?, ?> date = findProperty(classWithAnnotations, "date");
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, classWithAnnotations)
                .withKeyValue(path + ".classifierGenericType", classWithAnnotations._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", at(classWithAnnotations._generalizations(), 0))
                .withKeyValue(path + ".generalizations[0].general", at(classWithAnnotations._generalizations(), 0)._general())
                .withKeyValue(path + ".taggedValues[0]", at(classWithAnnotations._taggedValues(), 0))
                .withKeyValue(path + ".properties['deprecated']", deprecated)
                .withKeyValue(path + ".properties['deprecated'].classifierGenericType", deprecated._classifierGenericType())
                .withKeyValue(path + ".properties['deprecated'].classifierGenericType.typeArguments[0]", typeArgument(deprecated._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['deprecated'].classifierGenericType.typeArguments[1]", typeArgument(deprecated._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['deprecated'].genericType", deprecated._genericType())
                .withKeyValue(path + ".properties['alsoDeprecated']", alsoDeprecated)
                .withKeyValue(path + ".properties['alsoDeprecated'].classifierGenericType", alsoDeprecated._classifierGenericType())
                .withKeyValue(path + ".properties['alsoDeprecated'].classifierGenericType.typeArguments[0]", typeArgument(alsoDeprecated._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['alsoDeprecated'].classifierGenericType.typeArguments[1]", typeArgument(alsoDeprecated._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['alsoDeprecated'].genericType", alsoDeprecated._genericType())
                .withKeyValue(path + ".properties['alsoDeprecated'].taggedValues[0]", alsoDeprecated._taggedValues().getOnly())
                .withKeyValue(path + ".properties['date']", date)
                .withKeyValue(path + ".properties['date'].classifierGenericType", date._classifierGenericType())
                .withKeyValue(path + ".properties['date'].classifierGenericType.typeArguments[0]", typeArgument(date._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['date'].classifierGenericType.typeArguments[1]", typeArgument(date._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['date'].genericType", date._genericType())
                .withKeyValue(path + ".properties['date'].taggedValues[0]", at(date._taggedValues(), 0))
                .withKeyValue(path + ".properties['date'].taggedValues[1]", at(date._taggedValues(), 1));

        assertIds(path, expected);
    }

    @Test
    public void testClassWithTypeAndMultiplicityParameters()
    {
        String path = "test::model::ClassWithTypeAndMultParams";
        Class<?> classWithTypeMultParams = getCoreInstance(path);
        ListIterable<? extends Property<?, ?>> properties = toList(classWithTypeMultParams._properties());
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, classWithTypeMultParams)
                .withKeyValue(path + ".classifierGenericType", classWithTypeMultParams._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", at(classWithTypeMultParams._generalizations(), 0))
                .withKeyValue(path + ".generalizations[0].general", at(classWithTypeMultParams._generalizations(), 0)._general())
//                .withKeyValue(path + ".multiplicityParameters[0]", at(classWithTypeMultParams._multiplicityParameters(), 0))
//                .withKeyValue(path + ".multiplicityParameters[1]", at(classWithTypeMultParams._multiplicityParameters(), 1))
//                .withKeyValue(path + ".typeParameters[0]", at(classWithTypeMultParams._typeParameters(), 0))
//                .withKeyValue(path + ".typeParameters[1]", at(classWithTypeMultParams._typeParameters(), 1))
                .withKeyValue(path + ".properties['propT']", properties.get(0))
                .withKeyValue(path + ".properties['propT'].classifierGenericType", properties.get(0)._classifierGenericType())
                .withKeyValue(path + ".properties['propT'].classifierGenericType.typeArguments[0]", typeArgument(properties.get(0)._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['propT'].classifierGenericType.typeArguments[1]", typeArgument(properties.get(0)._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['propT'].genericType", properties.get(0)._genericType())
                .withKeyValue(path + ".properties['propT'].multiplicity", properties.get(0)._multiplicity())
                .withKeyValue(path + ".properties['propV']", properties.get(1))
                .withKeyValue(path + ".properties['propV'].classifierGenericType", properties.get(1)._classifierGenericType())
                .withKeyValue(path + ".properties['propV'].classifierGenericType.typeArguments[0]", typeArgument(properties.get(1)._classifierGenericType(), 0))
                .withKeyValue(path + ".properties['propV'].classifierGenericType.typeArguments[1]", typeArgument(properties.get(1)._classifierGenericType(), 1))
                .withKeyValue(path + ".properties['propV'].genericType", properties.get(1)._genericType())
                .withKeyValue(path + ".properties['propV'].multiplicity", properties.get(1)._multiplicity());

        assertIds(path, expected);
    }

    @Test
    public void testClassWithQualifiedProperties()
    {
        String path = "test::model::ClassWithQualifiedProperties";
        Class<?> classWithQualifiedProps = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, classWithQualifiedProps)
                .withKeyValue(path + ".classifierGenericType", classWithQualifiedProps._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", classWithQualifiedProps._generalizations().getOnly())
                .withKeyValue(path + ".generalizations[0].general", classWithQualifiedProps._generalizations().getOnly()._general());

        Property<?, ?> names = classWithQualifiedProps._properties().getFirst();
        expected.put(path + ".properties['names']", names);
        expected.put(path + ".properties['names'].classifierGenericType", names._classifierGenericType());
        expected.put(path + ".properties['names'].classifierGenericType.typeArguments[0]", typeArgument(names._classifierGenericType(), 0));
        expected.put(path + ".properties['names'].classifierGenericType.typeArguments[1]", typeArgument(names._classifierGenericType(), 1));
        expected.put(path + ".properties['names'].genericType", names._genericType());

        Property<?, ?> title = classWithQualifiedProps._properties().getLast();
        expected.put(path + ".properties['title']", title);
        expected.put(path + ".properties['title'].classifierGenericType", title._classifierGenericType());
        expected.put(path + ".properties['title'].classifierGenericType.typeArguments[0]", typeArgument(title._classifierGenericType(), 0));
        expected.put(path + ".properties['title'].classifierGenericType.typeArguments[1]", typeArgument(title._classifierGenericType(), 1));
        expected.put(path + ".properties['title'].genericType", title._genericType());


        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(classWithQualifiedProps._qualifiedProperties());

        QualifiedProperty<?> firstName = qualifiedProperties.get(0);
        SimpleFunctionExpression firstNameIfExp = (SimpleFunctionExpression) firstName._expressionSequence().getOnly();
        ListIterable<? extends ValueSpecification> firstNameIfParams = toList(firstNameIfExp._parametersValues());
        SimpleFunctionExpression firstNameIfCond = (SimpleFunctionExpression) firstNameIfParams.get(0);
        InstanceValue firstNameIfTrue = (InstanceValue) firstNameIfParams.get(1);
        LambdaFunction<?> firstNameIfTrueLambda = (LambdaFunction<?>) firstNameIfTrue._values().getOnly();
        InstanceValue firstNameIfFalse = (InstanceValue) firstNameIfParams.get(2);
        LambdaFunction<?> firstNameIfFalseLambda = (LambdaFunction<?>) firstNameIfFalse._values().getOnly();
        expected.put(path + ".qualifiedProperties[id='firstName()']", firstName);
        expected.put(path + ".qualifiedProperties[id='firstName()'].classifierGenericType", firstName._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].classifierGenericType.typeArguments[0]", typeArgument(firstName._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='firstName()'].classifierGenericType.typeArguments[0].rawType", typeArgument(firstName._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='firstName()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(firstName._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='firstName()'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(firstName._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0]", firstName._expressionSequence().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].genericType", firstName._expressionSequence().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0]", firstNameIfParams.get(0));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0].genericType", firstNameIfParams.get(0)._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0].parametersValues[0]", firstNameIfCond._parametersValues().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType", firstNameIfCond._parametersValues().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]", ((SimpleFunctionExpression) firstNameIfCond._parametersValues().getOnly())._parametersValues().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType", ((SimpleFunctionExpression) firstNameIfCond._parametersValues().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName", ((SimpleFunctionExpression) firstNameIfCond._parametersValues().getOnly())._propertyName());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1]", firstNameIfParams.get(1));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].genericType", firstNameIfParams.get(1)._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]", firstNameIfParams.get(1)._genericType()._typeArguments().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType", firstNameIfParams.get(1)._genericType()._typeArguments().getOnly()._rawType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType", funcTypeRetType(firstNameIfParams.get(1)._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0]", firstNameIfTrueLambda);
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType", firstNameIfTrueLambda._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]", typeArgument(firstNameIfTrueLambda._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(firstNameIfTrueLambda._classifierGenericType(), 0)._rawType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(firstNameIfTrueLambda._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]", firstNameIfTrueLambda._expressionSequence().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType", firstNameIfTrueLambda._expressionSequence().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2]", firstNameIfFalse);
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].genericType", firstNameIfFalse._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].genericType.typeArguments[0]", typeArgument(firstNameIfFalse._genericType(), 0));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].genericType.typeArguments[0].rawType", typeArgument(firstNameIfFalse._genericType(), 0)._rawType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].genericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(firstNameIfFalse._genericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0]", firstNameIfFalseLambda);
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].classifierGenericType", firstNameIfFalseLambda._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].classifierGenericType.typeArguments[0]", typeArgument(firstNameIfFalseLambda._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(firstNameIfFalseLambda._classifierGenericType(), 0)._rawType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(firstNameIfFalseLambda._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0]", firstNameIfFalseLambda._expressionSequence().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].genericType", firstNameIfFalseLambda._expressionSequence().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[0]", ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getFirst());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType", ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getFirst()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]", ((SimpleFunctionExpression) ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getFirst())._parametersValues().getOnly());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType", ((SimpleFunctionExpression) ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getFirst())._parametersValues().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].propertyName", ((SimpleFunctionExpression) ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getFirst())._propertyName());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[1]", ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getLast());
        expected.put(path + ".qualifiedProperties[id='firstName()'].expressionSequence[0].parametersValues[2].values[0].expressionSequence[0].parametersValues[1].genericType", ((SimpleFunctionExpression) firstNameIfFalseLambda._expressionSequence().getOnly())._parametersValues().getLast()._genericType());
        expected.put(path + ".qualifiedProperties[id='firstName()'].genericType", firstName._genericType());

        QualifiedProperty<?> fullNameNoTitle = qualifiedProperties.get(1);
        SimpleFunctionExpression fullNameExpression = (SimpleFunctionExpression) fullNameNoTitle._expressionSequence().getOnly();
        expected.put(path + ".qualifiedProperties[id='fullName()']", fullNameNoTitle);
        expected.put(path + ".qualifiedProperties[id='fullName()'].classifierGenericType", fullNameNoTitle._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='fullName()'].classifierGenericType.typeArguments[0]", typeArgument(fullNameNoTitle._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='fullName()'].classifierGenericType.typeArguments[0].rawType", typeArgument(fullNameNoTitle._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='fullName()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(fullNameNoTitle._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName()'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(fullNameNoTitle._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0]", fullNameNoTitle._expressionSequence().getOnly());
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0].genericType", fullNameNoTitle._expressionSequence().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0].parametersValues[0]", fullNameExpression._parametersValues().getFirst());
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0].parametersValues[0].genericType", fullNameExpression._parametersValues().getFirst()._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0].parametersValues[1]", fullNameExpression._parametersValues().getLast());
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0].parametersValues[1].genericType", fullNameExpression._parametersValues().getLast()._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName()'].expressionSequence[0].qualifiedPropertyName", fullNameExpression._qualifiedPropertyName());
        expected.put(path + ".qualifiedProperties[id='fullName()'].genericType", fullNameNoTitle._genericType());

        QualifiedProperty<?> fullNameWithTitle = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])']", fullNameWithTitle);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType", fullNameWithTitle._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0]", typeArgument(fullNameWithTitle._classifierGenericType(), 0));

        FunctionType fullNameWithTitleFunctionType = (FunctionType) typeArgument(fullNameWithTitle._classifierGenericType(), 0)._rawType();
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0].rawType", fullNameWithTitleFunctionType);
//        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this']", findFuncTypeParam(fullNameWithTitleFunctionType, "this"));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType", funcTypeParam(fullNameWithTitleFunctionType, "this")._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0].rawType.parameters['withTitle']", funcTypeParam(fullNameWithTitleFunctionType, "withTitle"));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0].rawType.parameters['withTitle'].genericType", funcTypeParam(fullNameWithTitleFunctionType, "withTitle")._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(fullNameWithTitleFunctionType));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].genericType", fullNameWithTitle._genericType());

        SimpleFunctionExpression fullNameWithTitleLetExp = (SimpleFunctionExpression) fullNameWithTitle._expressionSequence().getFirst();
        InstanceValue fullNameWithTitleLetVarExp = (InstanceValue) fullNameWithTitleLetExp._parametersValues().getFirst();
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0]", fullNameWithTitleLetExp);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].genericType", fullNameWithTitleLetExp._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[0]", fullNameWithTitleLetVarExp);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[0].genericType", fullNameWithTitleLetVarExp._genericType());

        SimpleFunctionExpression fullNameWithTitleLetValExp = (SimpleFunctionExpression) fullNameWithTitleLetExp._parametersValues().getLast();
        ListIterable<? extends ValueSpecification> fullNameWithTitleLetValIfParams = toList(fullNameWithTitleLetValExp._parametersValues());
        SimpleFunctionExpression fullNameWithTitleLetValIfCond = (SimpleFunctionExpression) fullNameWithTitleLetValIfParams.get(0);
        InstanceValue fullNameWithTitleLetValIfTrue = (InstanceValue) fullNameWithTitleLetValIfParams.get(1);
        LambdaFunction<?> fullNameWithTitleLetValIfTrueLambda = (LambdaFunction<?>) fullNameWithTitleLetValIfTrue._values().getOnly();
        InstanceValue fullNameWithTitleLetValIfFalse = (InstanceValue) fullNameWithTitleLetValIfParams.get(2);
        LambdaFunction<?> fullNameWithTitleLetValIfFalseLambda = (LambdaFunction<?>) fullNameWithTitleLetValIfFalse._values().getOnly();
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1]", fullNameWithTitleLetValExp);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].genericType", fullNameWithTitleLetValExp._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0]", fullNameWithTitleLetValIfCond);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].genericType", fullNameWithTitleLetValIfCond._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]", fullNameWithTitleLetValIfCond._parametersValues().getFirst());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType", fullNameWithTitleLetValIfCond._parametersValues().getFirst()._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1]", fullNameWithTitleLetValIfCond._parametersValues().getLast());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].genericType", fullNameWithTitleLetValIfCond._parametersValues().getLast()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0]",
                ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0].genericType",
                ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly())._parametersValues().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly())._parametersValues().getOnly())._parametersValues().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly())._parametersValues().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[1].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((SimpleFunctionExpression) fullNameWithTitleLetValIfCond._parametersValues().getLast())._parametersValues().getOnly())._parametersValues().getOnly())._propertyName());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1]", fullNameWithTitleLetValIfTrue);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].genericType", fullNameWithTitleLetValIfTrue._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0]", typeArgument(fullNameWithTitleLetValIfTrue._genericType(), 0));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType", typeArgument(fullNameWithTitleLetValIfTrue._genericType(), 0)._rawType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(fullNameWithTitleLetValIfTrue._genericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0]", fullNameWithTitleLetValIfTrueLambda);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType", fullNameWithTitleLetValIfTrueLambda._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0]", typeArgument(fullNameWithTitleLetValIfTrueLambda._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(fullNameWithTitleLetValIfTrueLambda._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(fullNameWithTitleLetValIfTrueLambda._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0]", fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType", fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0]",
                (SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0].genericType",
                ((SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst())._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0].parametersValues[0]",
                ((SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst())._parametersValues().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0].parametersValues[0].parametersValues[0]",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst())._parametersValues().getOnly())._parametersValues().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0].parametersValues[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst())._parametersValues().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) ((SimpleFunctionExpression) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getFirst())._parametersValues().getOnly())._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[1]",
                (InstanceValue) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getLast());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].values[1].genericType",
                ((InstanceValue) ((InstanceValue) ((SimpleFunctionExpression) fullNameWithTitleLetValIfTrueLambda._expressionSequence().getOnly())._parametersValues().getOnly())._values().getLast())._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2]", fullNameWithTitleLetValIfFalse);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].genericType", fullNameWithTitleLetValIfFalse._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0]", typeArgument(fullNameWithTitleLetValIfFalse._genericType(), 0));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType", typeArgument(fullNameWithTitleLetValIfFalse._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(fullNameWithTitleLetValIfFalse._genericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0]", fullNameWithTitleLetValIfFalseLambda);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType", fullNameWithTitleLetValIfFalseLambda._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0]", typeArgument(fullNameWithTitleLetValIfFalseLambda._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(fullNameWithTitleLetValIfFalseLambda._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(fullNameWithTitleLetValIfFalseLambda._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0]", fullNameWithTitleLetValIfFalseLambda._expressionSequence().getOnly());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType", fullNameWithTitleLetValIfFalseLambda._expressionSequence().getOnly()._genericType());

        SimpleFunctionExpression fullNameWithTitleJoinStrExp = (SimpleFunctionExpression) exprSeq(fullNameWithTitle, 1);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1]", fullNameWithTitleJoinStrExp);
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].genericType", fullNameWithTitleJoinStrExp._genericType());
        ListIterable<? extends ValueSpecification> fullNameWithTitleJoinStrExpParams = toList(fullNameWithTitleJoinStrExp._parametersValues());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[0]", fullNameWithTitleJoinStrExpParams.get(0));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[0].genericType", fullNameWithTitleJoinStrExpParams.get(0)._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[0].parametersValues[0]", ((SimpleFunctionExpression) fullNameWithTitleJoinStrExpParams.get(0))._parametersValues().getOnly());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[0].parametersValues[0].genericType", ((SimpleFunctionExpression) fullNameWithTitleJoinStrExpParams.get(0))._parametersValues().getOnly()._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[0].propertyName", ((SimpleFunctionExpression) fullNameWithTitleJoinStrExpParams.get(0))._propertyName());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[1]", fullNameWithTitleJoinStrExpParams.get(1));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[1].genericType", fullNameWithTitleJoinStrExpParams.get(1)._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[2]", fullNameWithTitleJoinStrExpParams.get(2));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[2].genericType", fullNameWithTitleJoinStrExpParams.get(2)._genericType());
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[3]", fullNameWithTitleJoinStrExpParams.get(3));
        expected.put(path + ".qualifiedProperties[id='fullName(Boolean[1])'].expressionSequence[1].parametersValues[3].genericType", fullNameWithTitleJoinStrExpParams.get(3)._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testClassProjection()
    {
        String path = "test::model::ProductProjection";
        ClassProjection<?> productProjection = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, productProjection)
                .withKeyValue(path + ".classifierGenericType", productProjection._classifierGenericType())
                .withKeyValue(path + ".classifierGenericType", productProjection._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", productProjection._generalizations().getOnly())
                .withKeyValue(path + ".generalizations[0].general", productProjection._generalizations().getOnly()._general());

        RootRouteNode projectionSpec = productProjection._projectionSpecification();
        expected.put(path + ".projectionSpecification", projectionSpec);
        expected.put(path + ".projectionSpecification.included[0]", at(projectionSpec._included(), 0));
        expected.put(path + ".projectionSpecification.included[1]", at(projectionSpec._included(), 1));
        expected.put(path + ".projectionSpecification.included[2]", at(projectionSpec._included(), 2));
        expected.put(path + ".projectionSpecification.included[2].parameters[0].genericType", at(at(projectionSpec._included(), 2)._parameters(), 0)._genericType());
        expected.put(path + ".projectionSpecification.type", projectionSpec._type());

        Property<?, ?> idProperty = findProperty(productProjection, "id");
        expected.put(path + ".properties['id']", idProperty);
        expected.put(path + ".properties['id'].classifierGenericType", idProperty._classifierGenericType());
        expected.put(path + ".properties['id'].classifierGenericType.typeArguments[0]", typeArgument(idProperty._classifierGenericType(), 0));
        expected.put(path + ".properties['id'].classifierGenericType.typeArguments[1]", typeArgument(idProperty._classifierGenericType(), 1));
        expected.put(path + ".properties['id'].genericType", idProperty._genericType());

        Property<?, ?> synonymsProperty = findProperty(productProjection, "synonyms");
        expected.put(path + ".properties['synonyms']", synonymsProperty);
        expected.put(path + ".properties['synonyms'].classifierGenericType", synonymsProperty._classifierGenericType());
        expected.put(path + ".properties['synonyms'].classifierGenericType.typeArguments[0]", typeArgument(synonymsProperty._classifierGenericType(), 0));
        expected.put(path + ".properties['synonyms'].classifierGenericType.typeArguments[1]", typeArgument(synonymsProperty._classifierGenericType(), 1));
        expected.put(path + ".properties['synonyms'].genericType", synonymsProperty._genericType());

        QualifiedProperty<?> synonymByType = findQualifiedProperty(productProjection, "synonymByType(String[1])");
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])']", synonymByType);
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType", synonymByType._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0]", typeArgument(synonymByType._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(synonymByType._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this']",
                funcTypeParam(typeArgument(synonymByType._classifierGenericType(), 0)._rawType(), "this"));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(synonymByType._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['type']",
                funcTypeParam(typeArgument(synonymByType._classifierGenericType(), 0)._rawType(), "type"));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0].rawType.parameters['type'].genericType",
                funcTypeParam(typeArgument(synonymByType._classifierGenericType(), 0)._rawType(), "type")._genericType());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(synonymByType._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0]", exprSeq(synonymByType, 0));
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].genericType", exprSeq(synonymByType, 0)._genericType());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0]", paramValue(exprSeq(synonymByType, 0), 0));
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].genericType", paramValue(exprSeq(synonymByType, 0), 0)._genericType());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]", paramValue(paramValue(exprSeq(synonymByType, 0), 0), 0));
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType", paramValue(paramValue(exprSeq(synonymByType, 0), 0), 0)._genericType());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]", paramValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 0), 0));
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType", paramValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 0), 0)._genericType());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName", ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(synonymByType, 0), 0), 0))._propertyName());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]", paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['s']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1)._genericType(), 0)._rawType(), "s"));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['s'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1)._genericType(), 0)._rawType(), "s")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['s']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "s"));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['s'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "s")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='synonymByType(String[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(synonymByType, 0), 0), 1), 0), 0), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='synonymByType(String[1])'].genericType", synonymByType._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testClassWithMilestoning1()
    {
        String path = "test::model::ClassWithMilestoning1";
        Class<?> classWithMilestoning1 = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, classWithMilestoning1)
                .withKeyValue(path + ".classifierGenericType", classWithMilestoning1._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", classWithMilestoning1._generalizations().getOnly())
                .withKeyValue(path + ".generalizations[0].general", classWithMilestoning1._generalizations().getOnly()._general());

        ListIterable<? extends Property<?, ?>> originalMilestonedProperties = toList(classWithMilestoning1._originalMilestonedProperties());
        Property<?, ?> toClass2Original = originalMilestonedProperties.get(0);
        expected.put(path + ".originalMilestonedProperties['toClass2']", toClass2Original);
        expected.put(path + ".originalMilestonedProperties['toClass2'].classifierGenericType", toClass2Original._classifierGenericType());
        expected.put(path + ".originalMilestonedProperties['toClass2'].classifierGenericType.typeArguments[0]", typeArgument(toClass2Original._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass2'].classifierGenericType.typeArguments[1]", typeArgument(toClass2Original._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass2'].genericType", toClass2Original._genericType());

        Property<?, ?> toClass3Original = originalMilestonedProperties.get(1);
        expected.put(path + ".originalMilestonedProperties['toClass3']", toClass3Original);
        expected.put(path + ".originalMilestonedProperties['toClass3'].classifierGenericType", toClass3Original._classifierGenericType());
        expected.put(path + ".originalMilestonedProperties['toClass3'].classifierGenericType.typeArguments[0]", typeArgument(toClass3Original._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass3'].classifierGenericType.typeArguments[1]", typeArgument(toClass3Original._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass3'].genericType", toClass3Original._genericType());

        Property<?, ?> businessDate = findProperty(classWithMilestoning1, "businessDate");
        expected.put(path + ".properties['businessDate']", businessDate);
        expected.put(path + ".properties['businessDate'].classifierGenericType", businessDate._classifierGenericType());
        expected.put(path + ".properties['businessDate'].classifierGenericType.typeArguments[0]", typeArgument(businessDate._classifierGenericType(), 0));
        expected.put(path + ".properties['businessDate'].classifierGenericType.typeArguments[1]", typeArgument(businessDate._classifierGenericType(), 1));
        expected.put(path + ".properties['businessDate'].genericType", businessDate._genericType());

        Property<?, ?> milestoning = findProperty(classWithMilestoning1, "milestoning");
        expected.put(path + ".properties['milestoning']", milestoning);
        expected.put(path + ".properties['milestoning'].classifierGenericType", milestoning._classifierGenericType());
        expected.put(path + ".properties['milestoning'].classifierGenericType.typeArguments[0]", typeArgument(milestoning._classifierGenericType(), 0));
        expected.put(path + ".properties['milestoning'].classifierGenericType.typeArguments[1]", typeArgument(milestoning._classifierGenericType(), 1));
        expected.put(path + ".properties['milestoning'].genericType", milestoning._genericType());

        Property<?, ?> toClass2AllVersions = findProperty(classWithMilestoning1, "toClass2AllVersions");
        expected.put(path + ".properties['toClass2AllVersions']", toClass2AllVersions);
        expected.put(path + ".properties['toClass2AllVersions'].classifierGenericType", toClass2AllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass2AllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass2AllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass2AllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass2AllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass2AllVersions'].genericType", toClass2AllVersions._genericType());

        Property<?, ?> toClass3AllVersions = findProperty(classWithMilestoning1, "toClass3AllVersions");
        expected.put(path + ".properties['toClass3AllVersions']", toClass3AllVersions);
        expected.put(path + ".properties['toClass3AllVersions'].classifierGenericType", toClass3AllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass3AllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass3AllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass3AllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass3AllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass3AllVersions'].genericType", toClass3AllVersions._genericType());

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(classWithMilestoning1._qualifiedProperties());
        QualifiedProperty<?> toClass2 = qualifiedProperties.get(0);
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])']", toClass2);
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType", toClass2._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0]", typeArgument(toClass2._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toClass2._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass2._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass2._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0]", exprSeq(toClass2, 0));
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].genericType", exprSeq(toClass2, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass2, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass2, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass2, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].genericType", toClass2._genericType());

        QualifiedProperty<?> toClass2AllVersionsInRange = qualifiedProperties.get(1);
        expected.put(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])']", toClass2AllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass2AllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass2AllVersionsInRange, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass2AllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].genericType", toClass2AllVersionsInRange._genericType());

        QualifiedProperty<?> toClass3_date_date = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])']", toClass3_date_date);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType", toClass3_date_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3_date_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd']",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "bd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd'].genericType",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "bd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd']",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "pd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd'].genericType",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "pd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass3_date_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3_date_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3_date_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3_date_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3_date_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3_date_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3_date_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3_date_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].genericType", toClass3_date_date._genericType());

        QualifiedProperty<?> toClass3_date = qualifiedProperties.get(3);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1])']", toClass3_date);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType", toClass3_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0]",
                exprSeq(toClass3_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1])'].genericType", toClass3_date._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testClassWithMilestoning2()
    {
        String path = "test::model::ClassWithMilestoning2";
        Class<?> classWithMilestoning2 = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, classWithMilestoning2)
                .withKeyValue(path + ".classifierGenericType", classWithMilestoning2._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", classWithMilestoning2._generalizations().getOnly())
                .withKeyValue(path + ".generalizations[0].general", classWithMilestoning2._generalizations().getOnly()._general());

        ListIterable<? extends Property<?, ?>> originalMilestonedProperties = toList(classWithMilestoning2._originalMilestonedProperties());
        Property<?, ?> toClass1Original = originalMilestonedProperties.get(0);
        expected.put(path + ".originalMilestonedProperties['toClass1']", toClass1Original);
        expected.put(path + ".originalMilestonedProperties['toClass1'].classifierGenericType", toClass1Original._classifierGenericType());
        expected.put(path + ".originalMilestonedProperties['toClass1'].classifierGenericType.typeArguments[0]", typeArgument(toClass1Original._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass1'].classifierGenericType.typeArguments[1]", typeArgument(toClass1Original._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass1'].genericType", toClass1Original._genericType());

        Property<?, ?> toClass3Original = originalMilestonedProperties.get(1);
        expected.put(path + ".originalMilestonedProperties['toClass3']", toClass3Original);
        expected.put(path + ".originalMilestonedProperties['toClass3'].classifierGenericType", toClass3Original._classifierGenericType());
        expected.put(path + ".originalMilestonedProperties['toClass3'].classifierGenericType.typeArguments[0]", typeArgument(toClass3Original._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass3'].classifierGenericType.typeArguments[1]", typeArgument(toClass3Original._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass3'].genericType", toClass3Original._genericType());

        ListIterable<? extends Property<?, ?>> properties = toList(classWithMilestoning2._properties());
        Property<?, ?> processingDate = properties.get(0);
        expected.put(path + ".properties['processingDate']", processingDate);
        expected.put(path + ".properties['processingDate'].classifierGenericType", processingDate._classifierGenericType());
        expected.put(path + ".properties['processingDate'].classifierGenericType.typeArguments[0]", typeArgument(processingDate._classifierGenericType(), 0));
        expected.put(path + ".properties['processingDate'].classifierGenericType.typeArguments[1]", typeArgument(processingDate._classifierGenericType(), 1));
        expected.put(path + ".properties['processingDate'].genericType", processingDate._genericType());

        Property<?, ?> milestoning = properties.get(1);
        expected.put(path + ".properties['milestoning']", milestoning);
        expected.put(path + ".properties['milestoning'].classifierGenericType", milestoning._classifierGenericType());
        expected.put(path + ".properties['milestoning'].classifierGenericType.typeArguments[0]", typeArgument(milestoning._classifierGenericType(), 0));
        expected.put(path + ".properties['milestoning'].classifierGenericType.typeArguments[1]", typeArgument(milestoning._classifierGenericType(), 1));
        expected.put(path + ".properties['milestoning'].genericType", milestoning._genericType());

        Property<?, ?> toClass1AllVersions = properties.get(2);
        expected.put(path + ".properties['toClass1AllVersions']", toClass1AllVersions);
        expected.put(path + ".properties['toClass1AllVersions'].classifierGenericType", toClass1AllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass1AllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass1AllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass1AllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass1AllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass1AllVersions'].genericType", toClass1AllVersions._genericType());

        Property<?, ?> toClass3AllVersions = properties.get(3);
        expected.put(path + ".properties['toClass3AllVersions']", toClass3AllVersions);
        expected.put(path + ".properties['toClass3AllVersions'].classifierGenericType", toClass3AllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass3AllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass3AllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass3AllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass3AllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass3AllVersions'].genericType", toClass3AllVersions._genericType());

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(classWithMilestoning2._qualifiedProperties());
        QualifiedProperty<?> toClass1 = qualifiedProperties.get(0);
        expected.put(path + ".qualifiedProperties[id='toClass1(Date[1])']", toClass1);
        expected.put(path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType", toClass1._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass1._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass1._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0]",
                exprSeq(toClass1, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass1, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass1, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass1, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass1, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1(Date[1])'].genericType", toClass1._genericType());

        QualifiedProperty<?> toClass1AllVersionsInRange = qualifiedProperties.get(1);
        expected.put(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])']", toClass1AllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass1AllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass1AllVersionsInRange, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass1AllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].genericType", toClass1AllVersionsInRange._genericType());

        QualifiedProperty<?> toClass3_date_date = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])']", toClass3_date_date);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType", toClass3_date_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3_date_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd']",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "bd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd'].genericType",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "bd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd']",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "pd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd'].genericType",
                funcTypeParam(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType(), "pd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3_date_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass3_date_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3_date_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3_date_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3_date_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3_date_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3_date_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3_date_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3_date_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3_date_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1],Date[1])'].genericType", toClass3_date_date._genericType());

        QualifiedProperty<?> toClass3_date = qualifiedProperties.get(3);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1])']", toClass3_date);
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType", toClass3_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0]",
                exprSeq(toClass3_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 0), 1))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass3(Date[1])'].genericType", toClass3_date._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testClassWithMilestoning3()
    {
        String path = "test::model::ClassWithMilestoning3";
        Class<?> classWithMilestoning3 = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, classWithMilestoning3)
                .withKeyValue(path + ".classifierGenericType", classWithMilestoning3._classifierGenericType())
                .withKeyValue(path + ".generalizations[0]", classWithMilestoning3._generalizations().getOnly())
                .withKeyValue(path + ".generalizations[0].general", classWithMilestoning3._generalizations().getOnly()._general());

        ListIterable<? extends Property<?, ?>> originalMilestonedProperties = toList(classWithMilestoning3._originalMilestonedProperties());
        Property<?, ?> toClass1Original = originalMilestonedProperties.get(0);
        expected.put(path + ".originalMilestonedProperties['toClass1']", toClass1Original);
        expected.put(path + ".originalMilestonedProperties['toClass1'].classifierGenericType", toClass1Original._classifierGenericType());
        expected.put(path + ".originalMilestonedProperties['toClass1'].classifierGenericType.typeArguments[0]", typeArgument(toClass1Original._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass1'].classifierGenericType.typeArguments[1]", typeArgument(toClass1Original._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass1'].genericType", toClass1Original._genericType());

        Property<?, ?> toClass2Original = originalMilestonedProperties.get(1);
        expected.put(path + ".originalMilestonedProperties['toClass2']", toClass2Original);
        expected.put(path + ".originalMilestonedProperties['toClass2'].classifierGenericType", toClass2Original._classifierGenericType());
        expected.put(path + ".originalMilestonedProperties['toClass2'].classifierGenericType.typeArguments[0]", typeArgument(toClass2Original._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass2'].classifierGenericType.typeArguments[1]", typeArgument(toClass2Original._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass2'].genericType", toClass2Original._genericType());

        ListIterable<? extends Property<?, ?>> properties = toList(classWithMilestoning3._properties());
        Property<?, ?> processingDate = properties.get(0);
        expected.put(path + ".properties['processingDate']", processingDate);
        expected.put(path + ".properties['processingDate'].classifierGenericType", processingDate._classifierGenericType());
        expected.put(path + ".properties['processingDate'].classifierGenericType.typeArguments[0]", typeArgument(processingDate._classifierGenericType(), 0));
        expected.put(path + ".properties['processingDate'].classifierGenericType.typeArguments[1]", typeArgument(processingDate._classifierGenericType(), 1));
        expected.put(path + ".properties['processingDate'].genericType", processingDate._genericType());

        Property<?, ?> businessDate = properties.get(1);
        expected.put(path + ".properties['businessDate']", businessDate);
        expected.put(path + ".properties['businessDate'].classifierGenericType", businessDate._classifierGenericType());
        expected.put(path + ".properties['businessDate'].classifierGenericType.typeArguments[0]", typeArgument(businessDate._classifierGenericType(), 0));
        expected.put(path + ".properties['businessDate'].classifierGenericType.typeArguments[1]", typeArgument(businessDate._classifierGenericType(), 1));
        expected.put(path + ".properties['businessDate'].genericType", businessDate._genericType());

        Property<?, ?> milestoning = properties.get(2);
        expected.put(path + ".properties['milestoning']", milestoning);
        expected.put(path + ".properties['milestoning'].classifierGenericType", milestoning._classifierGenericType());
        expected.put(path + ".properties['milestoning'].classifierGenericType.typeArguments[0]", typeArgument(milestoning._classifierGenericType(), 0));
        expected.put(path + ".properties['milestoning'].classifierGenericType.typeArguments[1]", typeArgument(milestoning._classifierGenericType(), 1));
        expected.put(path + ".properties['milestoning'].genericType", milestoning._genericType());

        Property<?, ?> toClass1AllVersions = properties.get(3);
        expected.put(path + ".properties['toClass1AllVersions']", toClass1AllVersions);
        expected.put(path + ".properties['toClass1AllVersions'].classifierGenericType", toClass1AllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass1AllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass1AllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass1AllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass1AllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass1AllVersions'].genericType", toClass1AllVersions._genericType());

        Property<?, ?> toClass2AllVersions = properties.get(4);
        expected.put(path + ".properties['toClass2AllVersions']", toClass2AllVersions);
        expected.put(path + ".properties['toClass2AllVersions'].classifierGenericType", toClass2AllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass2AllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass2AllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass2AllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass2AllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass2AllVersions'].genericType", toClass2AllVersions._genericType());

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(classWithMilestoning3._qualifiedProperties());
        QualifiedProperty<?> toClass1_noDate = qualifiedProperties.get(0);
        expected.put(path + ".qualifiedProperties[id='toClass1()']", toClass1_noDate);
        expected.put(path + ".qualifiedProperties[id='toClass1()'].classifierGenericType", toClass1_noDate._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1_noDate._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1_noDate._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1_noDate._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1_noDate._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0]",
                exprSeq(toClass1_noDate, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].genericType",
                exprSeq(toClass1_noDate, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1_noDate, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1_noDate, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_noDate, 0), 0), 1), 0), 0), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='toClass1()'].genericType", toClass1_noDate._genericType());

        QualifiedProperty<?> toClass1_date = qualifiedProperties.get(1);
        expected.put(path + ".qualifiedProperties[id='toClass1(Date[1])']", toClass1_date);
        expected.put(path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType", toClass1_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass1_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass1_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0]",
                exprSeq(toClass1_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass1_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1_date, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1(Date[1])'].genericType", toClass1_date._genericType());

        QualifiedProperty<?> toClass1AllVersionsInRange = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])']", toClass1AllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass1AllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1AllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass1AllVersionsInRange, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass1AllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass1AllVersionsInRange, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1AllVersionsInRange(Date[1],Date[1])'].genericType", toClass1AllVersionsInRange._genericType());

        QualifiedProperty<?> toClass2_noDate = qualifiedProperties.get(3);
        expected.put(path + ".qualifiedProperties[id='toClass2()']", toClass2_noDate);
        expected.put(path + ".qualifiedProperties[id='toClass2()'].classifierGenericType", toClass2_noDate._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass2_noDate._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass2_noDate._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2_noDate._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2_noDate._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0]",
                exprSeq(toClass2_noDate, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].genericType",
                exprSeq(toClass2_noDate, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2_noDate, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2_noDate, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2()'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_noDate, 0), 0), 1), 0), 0), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='toClass2()'].genericType", toClass2_noDate._genericType());

        QualifiedProperty<?> toClass2_date = qualifiedProperties.get(4);
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])']", toClass2_date);
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType", toClass2_date._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0]", typeArgument(toClass2_date._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toClass2_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass2_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass2_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2_date._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0]", exprSeq(toClass2_date, 0));
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].genericType", exprSeq(toClass2_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2_date, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2(Date[1])'].genericType", toClass2_date._genericType());

        QualifiedProperty<?> toClass2AllVersionsInRange = qualifiedProperties.get(5);
        expected.put(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])']", toClass2AllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass2AllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2AllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass2AllVersionsInRange, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass2AllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(paramValue(exprSeq(toClass2AllVersionsInRange, 0), 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2AllVersionsInRange(Date[1],Date[1])'].genericType", toClass2AllVersionsInRange._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testAssociationWithMilestoning1()
    {
        String path = "test::model::AssociationWithMilestoning1";
        Association association = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, association)
                .withKeyValue(path + ".classifierGenericType", association._classifierGenericType());

        ListIterable<? extends Property<?, ?>> originalMilestonedProperties = toList(association._originalMilestonedProperties());
        Property<?, ?> toClass1AOriginal = originalMilestonedProperties.get(0);
        expected.put(path + ".originalMilestonedProperties['toClass1A']", toClass1AOriginal);
        expected.put(path + ".originalMilestonedProperties['toClass1A'].classifierGenericType", toClass1AOriginal._classifierGenericType());
//        expected.put(path + ".originalMilestonedProperties['toClass1A'].classifierGenericType.typeArguments[0]", typeArgument(toClass1AOriginal._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass1A'].classifierGenericType.typeArguments[1]", typeArgument(toClass1AOriginal._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass1A'].genericType", toClass1AOriginal._genericType());

        Property<?, ?> toClass2AOriginal = originalMilestonedProperties.get(1);
        expected.put(path + ".originalMilestonedProperties['toClass2A']", toClass2AOriginal);
        expected.put(path + ".originalMilestonedProperties['toClass2A'].classifierGenericType", toClass2AOriginal._classifierGenericType());
//        expected.put(path + ".originalMilestonedProperties['toClass2A'].classifierGenericType.typeArguments[0]", typeArgument(toClass2AOriginal._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass2A'].classifierGenericType.typeArguments[1]", typeArgument(toClass2AOriginal._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass2A'].genericType", toClass2AOriginal._genericType());

        ListIterable<? extends Property<?, ?>> properties = toList(association._properties());
        Property<?, ?> toClass1AAllVersions = properties.get(0);
        expected.put(path + ".properties['toClass1AAllVersions']", toClass1AAllVersions);
        expected.put(path + ".properties['toClass1AAllVersions'].classifierGenericType", toClass1AAllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass1AAllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass1AAllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass1AAllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass1AAllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass1AAllVersions'].genericType", toClass1AAllVersions._genericType());

        Property<?, ?> toClass2AAllVersions = properties.get(1);
        expected.put(path + ".properties['toClass2AAllVersions']", toClass2AAllVersions);
        expected.put(path + ".properties['toClass2AAllVersions'].classifierGenericType", toClass2AAllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass2AAllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass2AAllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass2AAllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass2AAllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass2AAllVersions'].genericType", toClass2AAllVersions._genericType());

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(association._qualifiedProperties());
        QualifiedProperty<?> toClass1A = qualifiedProperties.get(0);
        expected.put(path + ".qualifiedProperties[id='toClass1A(Date[1])']", toClass1A);
        expected.put(path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType", toClass1A._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0]", typeArgument(toClass1A._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toClass1A._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1A._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass1A._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass1A._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1A._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0]",
                exprSeq(toClass1A, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass1A, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1A, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1A, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1A, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1A, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass1A, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass1A, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass1A, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass1A, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass1A, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass1A, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass1A, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass1A, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1A, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1A(Date[1])'].genericType", toClass1A._genericType());

        QualifiedProperty<?> toClass1AAllVersionsInRange = qualifiedProperties.get(1);
        expected.put(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])']", toClass1AAllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass1AAllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1AAllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]", exprSeq(toClass1AAllVersionsInRange, 0));
        expected.put(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType", exprSeq(toClass1AAllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1AAllVersionsInRange, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1AAllVersionsInRange(Date[1],Date[1])'].genericType", toClass1AAllVersionsInRange._genericType());

        QualifiedProperty<?> toClass2A = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])']", toClass2A);
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType", toClass2A._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0]", typeArgument(toClass2A._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toClass2A._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2A._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass2A._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass2A._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2A._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0]", exprSeq(toClass2A, 0));
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].genericType", exprSeq(toClass2A, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2A, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2A, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2A, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2A, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass2A, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass2A, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass2A, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass2A, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass2A, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass2A, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass2A, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass2A, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2A(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2A, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2A(Date[1])'].genericType", toClass2A._genericType());

        QualifiedProperty<?> toClass2AAllVersionsInRange = qualifiedProperties.get(3);
        expected.put(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])']", toClass2AAllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass2AAllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2AAllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass2AAllVersionsInRange, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass2AAllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2AAllVersionsInRange, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2AAllVersionsInRange(Date[1],Date[1])'].genericType", toClass2AAllVersionsInRange._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testAssociationWithMilestoning2()
    {
        String path = "test::model::AssociationWithMilestoning2";
        Association association = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, association)
                .withKeyValue(path + ".classifierGenericType", association._classifierGenericType());

        ListIterable<? extends Property<?, ?>> originalMilestonedProperties = toList(association._originalMilestonedProperties());
        Property<?, ?> toClass1BOriginal = originalMilestonedProperties.get(0);
        expected.put(path + ".originalMilestonedProperties['toClass1B']", toClass1BOriginal);
        expected.put(path + ".originalMilestonedProperties['toClass1B'].classifierGenericType", toClass1BOriginal._classifierGenericType());
//        expected.put(path + ".originalMilestonedProperties['toClass1B'].classifierGenericType.typeArguments[0]", typeArgument(toClass1BOriginal._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass1B'].classifierGenericType.typeArguments[1]", typeArgument(toClass1BOriginal._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass1B'].genericType", toClass1BOriginal._genericType());

        Property<?, ?> toClass3BOriginal = originalMilestonedProperties.get(1);
        expected.put(path + ".originalMilestonedProperties['toClass3B']", toClass3BOriginal);
        expected.put(path + ".originalMilestonedProperties['toClass3B'].classifierGenericType", toClass3BOriginal._classifierGenericType());
//        expected.put(path + ".originalMilestonedProperties['toClass3B'].classifierGenericType.typeArguments[0]", typeArgument(toClass3BOriginal._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass3B'].classifierGenericType.typeArguments[1]", typeArgument(toClass3BOriginal._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass3B'].genericType", toClass3BOriginal._genericType());

        ListIterable<? extends Property<?, ?>> properties = toList(association._properties());
        Property<?, ?> toClass1BAllVersions = properties.get(0);
        expected.put(path + ".properties['toClass1BAllVersions']", toClass1BAllVersions);
        expected.put(path + ".properties['toClass1BAllVersions'].classifierGenericType", toClass1BAllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass1BAllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass1BAllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass1BAllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass1BAllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass1BAllVersions'].genericType", toClass1BAllVersions._genericType());

        Property<?, ?> toClass3BAllVersions = properties.get(1);
        expected.put(path + ".properties['toClass3BAllVersions']", toClass3BAllVersions);
        expected.put(path + ".properties['toClass3BAllVersions'].classifierGenericType", toClass3BAllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass3BAllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass3BAllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass3BAllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass3BAllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass3BAllVersions'].genericType", toClass3BAllVersions._genericType());

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(association._qualifiedProperties());
        QualifiedProperty<?> toClass1B_noDate = qualifiedProperties.get(0);
        expected.put(path + ".qualifiedProperties[id='toClass1B()']", toClass1B_noDate);
        expected.put(path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType", toClass1B_noDate._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType.typeArguments[0]", typeArgument(toClass1B_noDate._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1B_noDate._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1B_noDate._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1B_noDate._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0]",
                exprSeq(toClass1B_noDate, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].genericType",
                exprSeq(toClass1B_noDate, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1B_noDate, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1B_noDate, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1B_noDate, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1B_noDate, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass1B_noDate, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass1B_noDate, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass1B_noDate, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass1B_noDate, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass1B_noDate, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass1B_noDate, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass1B_noDate, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass1B_noDate, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_noDate, 0), 1), 0), 0), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='toClass1B()'].genericType", toClass1B_noDate._genericType());

        QualifiedProperty<?> toClass1B_date = qualifiedProperties.get(1);
        expected.put(path + ".qualifiedProperties[id='toClass1B(Date[1])']", toClass1B_date);
        expected.put(path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType", toClass1B_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1B_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1B_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1B_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass1B_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass1B_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1B_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0]",
                exprSeq(toClass1B_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass1B_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1B_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1B_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1B_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1B_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass1B_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass1B_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass1B_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass1B_date, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass1B_date, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass1B_date, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass1B_date, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass1B_date, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1B_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1B(Date[1])'].genericType", toClass1B_date._genericType());

        QualifiedProperty<?> toClass1BAllVersionsInRange = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])']", toClass1BAllVersionsInRange);
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType",
                toClass1BAllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass1BAllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]", exprSeq(toClass1BAllVersionsInRange, 0));
        expected.put(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType", exprSeq(toClass1BAllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1)._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass1BAllVersionsInRange, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass1BAllVersionsInRange(Date[1],Date[1])'].genericType", toClass1BAllVersionsInRange._genericType());

        QualifiedProperty<?> toClass3B_date_date = qualifiedProperties.get(3);
        expected.put(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])']", toClass3B_date_date);
        expected.put(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType", toClass3B_date_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3B_date_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd']",
                funcTypeParam(typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType(), "bd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd'].genericType",
                funcTypeParam(typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType(), "bd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd']",
                funcTypeParam(typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType(), "pd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd'].genericType",
                funcTypeParam(typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType(), "pd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3B_date_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass3B_date_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3B_date_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3B_date_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3B_date_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3B_date_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3B_date_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3B_date_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3B_date_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3B_date_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3B_date_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3B_date_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3B_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3B_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3B_date_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass3B(Date[1],Date[1])'].genericType", toClass3B_date_date._genericType());

        QualifiedProperty<?> toClass3B_date = qualifiedProperties.get(4);
        expected.put(path + ".qualifiedProperties[id='toClass3B(Date[1])']", toClass3B_date);
        expected.put(path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType", toClass3B_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3B_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3B_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3B_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass3B_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass3B_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3B_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0]",
                exprSeq(toClass3B_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3B_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3B_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3B_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3B_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3B_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3B_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3B_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3B_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3B_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3B_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3B_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3B_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3B_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3B(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3B_date, 0), 1), 0), 0), 1), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='toClass3B(Date[1])'].genericType", toClass3B_date._genericType());

        assertIds(path, expected);
    }

    @Test
    public void testAssociationWithMilestoning3()
    {
        String path = "test::model::AssociationWithMilestoning3";
        Association association = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, association)
                .withKeyValue(path + ".classifierGenericType", association._classifierGenericType());

        ListIterable<? extends Property<?, ?>> originalMilestonedProperties = toList(association._originalMilestonedProperties());
        Property<?, ?> toClass2COriginal = originalMilestonedProperties.get(0);
        expected.put(path + ".originalMilestonedProperties['toClass2C']", toClass2COriginal);
        expected.put(path + ".originalMilestonedProperties['toClass2C'].classifierGenericType", toClass2COriginal._classifierGenericType());
//        expected.put(path + ".originalMilestonedProperties['toClass2C'].classifierGenericType.typeArguments[0]", typeArgument(toClass2COriginal._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass2C'].classifierGenericType.typeArguments[1]", typeArgument(toClass2COriginal._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass2C'].genericType", toClass2COriginal._genericType());

        Property<?, ?> toClass3COriginal = originalMilestonedProperties.get(1);
        expected.put(path + ".originalMilestonedProperties['toClass3C']", toClass3COriginal);
        expected.put(path + ".originalMilestonedProperties['toClass3C'].classifierGenericType", toClass3COriginal._classifierGenericType());
//        expected.put(path + ".originalMilestonedProperties['toClass3C'].classifierGenericType.typeArguments[0]", typeArgument(toClass3COriginal._classifierGenericType(), 0));
        expected.put(path + ".originalMilestonedProperties['toClass3C'].classifierGenericType.typeArguments[1]", typeArgument(toClass3COriginal._classifierGenericType(), 1));
        expected.put(path + ".originalMilestonedProperties['toClass3C'].genericType", toClass3COriginal._genericType());

        ListIterable<? extends Property<?, ?>> properties = toList(association._properties());
        Property<?, ?> toClass2CAllVersions = properties.get(0);
        expected.put(path + ".properties['toClass2CAllVersions']", toClass2CAllVersions);
        expected.put(path + ".properties['toClass2CAllVersions'].classifierGenericType", toClass2CAllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass2CAllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass2CAllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass2CAllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass2CAllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass2CAllVersions'].genericType", toClass2CAllVersions._genericType());

        Property<?, ?> toClass3CAllVersions = properties.get(1);
        expected.put(path + ".properties['toClass3CAllVersions']", toClass3CAllVersions);
        expected.put(path + ".properties['toClass3CAllVersions'].classifierGenericType", toClass3CAllVersions._classifierGenericType());
        expected.put(path + ".properties['toClass3CAllVersions'].classifierGenericType.typeArguments[0]", typeArgument(toClass3CAllVersions._classifierGenericType(), 0));
        expected.put(path + ".properties['toClass3CAllVersions'].classifierGenericType.typeArguments[1]", typeArgument(toClass3CAllVersions._classifierGenericType(), 1));
        expected.put(path + ".properties['toClass3CAllVersions'].genericType", toClass3CAllVersions._genericType());

        ListIterable<? extends QualifiedProperty<?>> qualifiedProperties = toList(association._qualifiedProperties());
        QualifiedProperty<?> toClass2C_noDate = qualifiedProperties.get(0);
        expected.put(path + ".qualifiedProperties[id='toClass2C()']", toClass2C_noDate);
        expected.put(path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType", toClass2C_noDate._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass2C_noDate._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass2C_noDate._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2C_noDate._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2C_noDate._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0]",
                exprSeq(toClass2C_noDate, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].genericType",
                exprSeq(toClass2C_noDate, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2C_noDate, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2C_noDate, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2C_noDate, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2C_noDate, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass2C_noDate, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass2C_noDate, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass2C_noDate, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass2C_noDate, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass2C_noDate, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass2C_noDate, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass2C_noDate, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass2C_noDate, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C()'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_noDate, 0), 1), 0), 0), 1))._propertyName());
        expected.put(path + ".qualifiedProperties[id='toClass2C()'].genericType", toClass2C_noDate._genericType());

        QualifiedProperty<?> toClass2C_date = qualifiedProperties.get(1);
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])']", toClass2C_date);
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType", toClass2C_date._classifierGenericType());
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0]", typeArgument(toClass2C_date._classifierGenericType(), 0));
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType", typeArgument(toClass2C_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2C_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass2C_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass2C_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2C_date._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0]", exprSeq(toClass2C_date, 0));
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].genericType", exprSeq(toClass2C_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2C_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2C_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2C_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2C_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass2C_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass2C_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass2C_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass2C_date, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass2C_date, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass2C_date, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass2C_date, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass2C_date, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2C_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2C(Date[1])'].genericType", toClass2C_date._genericType());

        QualifiedProperty<?> toClass2CAllVersionsInRange = qualifiedProperties.get(2);
        expected.put(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])']", toClass2CAllVersionsInRange);
        expected.put(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType", toClass2CAllVersionsInRange._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start']",
                funcTypeParam(typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['start'].genericType",
                funcTypeParam(typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType(), "start")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end']",
                funcTypeParam(typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['end'].genericType",
                funcTypeParam(typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType(), "end")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass2CAllVersionsInRange._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass2CAllVersionsInRange, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass2CAllVersionsInRange, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                at(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                at(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(at(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(at(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(at(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1)._genericType()._typeArguments(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass2CAllVersionsInRange, 0), 1), 0), 0), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass2CAllVersionsInRange(Date[1],Date[1])'].genericType", toClass2CAllVersionsInRange._genericType());

        QualifiedProperty<?> toClass3C_date_date = qualifiedProperties.get(3);
        expected.put(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])']", toClass3C_date_date);
        expected.put(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType", toClass3C_date_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3C_date_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd']",
                funcTypeParam(typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType(), "bd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['bd'].genericType",
                funcTypeParam(typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType(), "bd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd']",
                funcTypeParam(typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType(), "pd"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['pd'].genericType",
                funcTypeParam(typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType(), "pd")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3C_date_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0]",
                exprSeq(toClass3C_date_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3C_date_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3C_date_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3C_date_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3C_date_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3C_date_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3C_date_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3C_date_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3C_date_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3C_date_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3C_date_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3C_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3C_date_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3C_date_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass3C(Date[1],Date[1])'].genericType", toClass3C_date_date._genericType());

        QualifiedProperty<?> toClass3C_date = qualifiedProperties.get(4);
        expected.put(path + ".qualifiedProperties[id='toClass3C(Date[1])']", toClass3C_date);
        expected.put(path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType", toClass3C_date._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0]",
                typeArgument(toClass3C_date._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType",
                typeArgument(toClass3C_date._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['this'].genericType",
                funcTypeParam(typeArgument(toClass3C_date._classifierGenericType(), 0)._rawType(), "this")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td']",
                funcTypeParam(typeArgument(toClass3C_date._classifierGenericType(), 0)._rawType(), "td"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType.parameters['td'].genericType",
                funcTypeParam(typeArgument(toClass3C_date._classifierGenericType(), 0)._rawType(), "td")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(toClass3C_date._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0]",
                exprSeq(toClass3C_date, 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].genericType",
                exprSeq(toClass3C_date, 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(toClass3C_date, 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(toClass3C_date, 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(toClass3C_date, 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(toClass3C_date, 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(exprSeq(toClass3C_date, 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(toClass3C_date, 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(toClass3C_date, 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0]",
                typeArgument(paramValue(exprSeq(toClass3C_date, 0), 1)._genericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType",
                typeArgument(paramValue(exprSeq(toClass3C_date, 0), 1)._genericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3C_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(paramValue(exprSeq(toClass3C_date, 0), 1)._genericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(paramValue(exprSeq(toClass3C_date, 0), 1)._genericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0]",
                instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType",
                instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0)._classifierGenericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0]",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0)._classifierGenericType(), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType",
                typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone']",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone"));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['v_milestone'].genericType",
                funcTypeParam(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType(), "v_milestone")._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0)._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0]",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].genericType",
                exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].parametersValues[1].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 0), 1))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 0), 0));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType",
                paramValue(paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 0), 0)._genericType());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[0].propertyName",
                ((SimpleFunctionExpression) paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 0))._propertyName());
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1]",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 1));
        expected.put(
                path + ".qualifiedProperties[id='toClass3C(Date[1])'].expressionSequence[0].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].parametersValues[1].genericType",
                paramValue(paramValue(exprSeq(instanceValueValue(paramValue(exprSeq(toClass3C_date, 0), 1), 0), 0), 1), 1)._genericType());
        expected.put(path + ".qualifiedProperties[id='toClass3C(Date[1])'].genericType", toClass3C_date._genericType());
        assertIds(path, expected);
    }

    @Test
    public void testNativeFunction()
    {
        String path = "meta::pure::functions::lang::compare_T_1__T_1__Integer_1_";
        NativeFunction<?> compare = getCoreInstance(path);

        MutableMap<String, CoreInstance> expected = Maps.mutable.<String, CoreInstance>with(path, compare)
                .withKeyValue(path + ".classifierGenericType", compare._classifierGenericType())
                .withKeyValue(path + ".classifierGenericType.typeArguments[0]", typeArgument(compare._classifierGenericType(), 0))
                .withKeyValue(path + ".classifierGenericType.typeArguments[0].rawType", typeArgument(compare._classifierGenericType(), 0)._rawType())
                .withKeyValue(path + ".classifierGenericType.typeArguments[0].rawType.parameters['a']", funcTypeParam(typeArgument(compare._classifierGenericType(), 0)._rawType(), "a"))
                .withKeyValue(path + ".classifierGenericType.typeArguments[0].rawType.parameters['a'].genericType", funcTypeParam(typeArgument(compare._classifierGenericType(), 0)._rawType(), "a")._genericType())
                .withKeyValue(path + ".classifierGenericType.typeArguments[0].rawType.parameters['b']", funcTypeParam(typeArgument(compare._classifierGenericType(), 0)._rawType(), "b"))
                .withKeyValue(path + ".classifierGenericType.typeArguments[0].rawType.parameters['b'].genericType", funcTypeParam(typeArgument(compare._classifierGenericType(), 0)._rawType(), "b")._genericType())
                .withKeyValue(path + ".classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(compare._classifierGenericType(), 0)._rawType()))
                .withKeyValue(path + ".taggedValues[0]", compare._taggedValues().getFirst())
                .withKeyValue(path + ".taggedValues[1]", compare._taggedValues().getLast());

        assertIds(path, expected);
    }

    @Test
    public void testFunction()
    {
        String path = "test::model::testFunc_T_m__Function_$0_1$__String_m_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.with(path, testFunction);

        FunctionType functionType = (FunctionType) typeArgument(testFunction._classifierGenericType(), 0)._rawType();
        expected.put(path + ".classifierGenericType", testFunction._classifierGenericType());
        expected.put(path + ".classifierGenericType.typeArguments[0]", typeArgument(testFunction._classifierGenericType(), 0));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType", functionType);
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['col']", funcTypeParam(functionType, "col"));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['col'].genericType", funcTypeParam(functionType, "col")._genericType());
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['col'].multiplicity", funcTypeParam(functionType, "col")._multiplicity());
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['func']", funcTypeParam(functionType, "func"));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType", funcTypeParam(functionType, "func")._genericType());
//        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType.typeArguments[0]", funcTypeParam(functionType, "func")._genericType()._typeArguments().getOnly());
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType.typeArguments[0].rawType", funcTypeParam(functionType, "func")._genericType()._typeArguments().getOnly()._rawType());
//        expected.put(
//                path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType.typeArguments[0].rawType.parameters['']",
//                funcTypeParam(funcTypeParam(functionType, "func")._genericType()._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(funcTypeParam(functionType, "func")._genericType()._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".classifierGenericType.typeArguments[0].rawType.parameters['func'].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(funcTypeParam(functionType, "func")._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.returnMultiplicity", functionType._returnMultiplicity());
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(functionType));

        SimpleFunctionExpression letExp = (SimpleFunctionExpression) testFunction._expressionSequence().getFirst();
        expected.put(path + ".expressionSequence[0]", letExp);
        expected.put(path + ".expressionSequence[0].genericType", letExp._genericType());
        expected.put(path + ".expressionSequence[0].genericType.typeArguments[0]", typeArgument(letExp._genericType(), 0));
        expected.put(path + ".expressionSequence[0].genericType.typeArguments[0].rawType", typeArgument(letExp._genericType(), 0)._rawType());
        expected.put(path + ".expressionSequence[0].genericType.typeArguments[0].rawType.parameters['']", funcTypeParam(typeArgument(letExp._genericType(), 0)._rawType(), ""));
        expected.put(path + ".expressionSequence[0].genericType.typeArguments[0].rawType.parameters[''].genericType", funcTypeParam(typeArgument(letExp._genericType(), 0)._rawType(), "")._genericType());
        expected.put(path + ".expressionSequence[0].genericType.typeArguments[0].rawType.parameters[''].genericType.typeParameter", funcTypeParam(typeArgument(letExp._genericType(), 0)._rawType(), "")._genericType()._typeParameter());
        expected.put(path + ".expressionSequence[0].genericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(letExp._genericType(), 0)._rawType()));

        InstanceValue letVar = (InstanceValue) letExp._parametersValues().getFirst();
        expected.put(path + ".expressionSequence[0].parametersValues[0]", letVar);
        expected.put(path + ".expressionSequence[0].parametersValues[0].genericType", letVar._genericType());

        SimpleFunctionExpression letValIfExp = (SimpleFunctionExpression) letExp._parametersValues().getLast();
        expected.put(path + ".expressionSequence[0].parametersValues[1]", letValIfExp);
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType", letValIfExp._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType.typeArguments[0]", typeArgument(letValIfExp._genericType(), 0));
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType", typeArgument(letValIfExp._genericType(), 0)._rawType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters['']", funcTypeParam(typeArgument(letValIfExp._genericType(), 0)._rawType(), ""));
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters[''].genericType", funcTypeParam(typeArgument(letValIfExp._genericType(), 0)._rawType(), "")._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.parameters[''].genericType.typeParameter", funcTypeParam(typeArgument(letValIfExp._genericType(), 0)._rawType(), "")._genericType()._typeParameter());
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType.typeArguments[0].rawType.returnType", funcTypeRetType(typeArgument(letValIfExp._genericType(), 0)._rawType()));

        ListIterable<? extends ValueSpecification> letValIfParams = toList(letValIfExp._parametersValues());
        SimpleFunctionExpression letValIfCond = (SimpleFunctionExpression) letValIfParams.get(0);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[0]", letValIfCond);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[0].genericType", letValIfCond._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0]", letValIfCond._parametersValues().getOnly());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType", letValIfCond._parametersValues().getOnly()._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType.typeArguments[0]", letValIfCond._parametersValues().getOnly()._genericType()._typeArguments().getOnly());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType.typeArguments[0].rawType", letValIfCond._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters['']",
                funcTypeParam(letValIfCond._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(letValIfCond._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters[''].genericType.typeParameter",
                funcTypeParam(letValIfCond._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[0].parametersValues[0].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(letValIfCond._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType()));

        InstanceValue letValIfTrue = (InstanceValue) letValIfParams.get(1);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1]", letValIfTrue);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType", letValIfTrue._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0]", typeArgument(letValIfTrue._genericType(), 0));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType", typeArgument(letValIfTrue._genericType(), 0)._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType.typeArguments[0]",
                funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType())._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType",
                funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "x"));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['x'].genericType",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "x")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['x'].genericType.typeParameter",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "x")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.returnType",
                funcTypeRetType(funcTypeRetType(typeArgument(letValIfTrue._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType()));

        LambdaFunction<?> letValIfTrueLambda = (LambdaFunction<?>) letValIfTrue._values().getOnly();
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0]", letValIfTrueLambda);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType", letValIfTrueLambda._classifierGenericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0]", typeArgument(letValIfTrueLambda._classifierGenericType(), 0));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0]",
                funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType",
                funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "x"));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['x'].genericType",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "x")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['x'].genericType.typeParameter",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "x")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.returnType",
                funcTypeRetType(funcTypeRetType(typeArgument(letValIfTrueLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType()));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0]", letValIfTrueLambda._expressionSequence().getOnly());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType", letValIfTrueLambda._expressionSequence().getOnly()._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType.typeArguments[0]", letValIfTrueLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType.typeArguments[0].rawType", letValIfTrueLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(letValIfTrueLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "x"));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.parameters['x'].genericType",
                funcTypeParam(letValIfTrueLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "x")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.parameters['x'].genericType.typeParameter",
                funcTypeParam(letValIfTrueLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "x")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(letValIfTrueLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0]",
                (LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].classifierGenericType",
                ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._classifierGenericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].classifierGenericType.typeArguments[0]",
                ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._classifierGenericType()._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].classifierGenericType.typeArguments[0].rawType",
                ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._classifierGenericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].classifierGenericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._classifierGenericType()._typeArguments().getOnly()._rawType(), "x"));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].classifierGenericType.typeArguments[0].rawType.parameters['x'].genericType",
                funcTypeParam(((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._classifierGenericType()._typeArguments().getOnly()._rawType(), "x")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._classifierGenericType()._typeArguments().getOnly()._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].expressionSequence[0]",
                ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._expressionSequence().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].expressionSequence[0].genericType",
                ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._expressionSequence().getOnly()._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].expressionSequence[0].parametersValues[0]",
                ((SimpleFunctionExpression) ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._expressionSequence().getOnly())._parametersValues().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].expressionSequence[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._expressionSequence().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[1].values[0].expressionSequence[0].values[0].expressionSequence[0].parametersValues[0].genericType.typeParameter",
                ((SimpleFunctionExpression) ((LambdaFunction<?>) ((InstanceValue) letValIfTrueLambda._expressionSequence().getOnly())._values().getOnly())._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeParameter());

        InstanceValue letValIfFalse = (InstanceValue) letValIfParams.get(2);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2]", letValIfFalse);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType", letValIfFalse._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0]", typeArgument(letValIfFalse._genericType(), 0));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType", typeArgument(letValIfFalse._genericType(), 0)._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType.typeArguments[0]",
                funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType())._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType",
                funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['']",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters[''].genericType.typeParameter",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].genericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.returnType",
                funcTypeRetType(funcTypeRetType(typeArgument(letValIfFalse._genericType(), 0)._rawType())._typeArguments().getOnly()._rawType()));

        LambdaFunction<?> letValIfFalseLambda = (LambdaFunction<?>) letValIfFalse._values().getOnly();
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0]", letValIfFalseLambda);
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType", letValIfFalseLambda._classifierGenericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0]", typeArgument(letValIfFalseLambda._classifierGenericType(), 0));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0]",
                funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType",
                funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters['']",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.parameters[''].genericType.typeParameter",
                funcTypeParam(funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType(), "")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].classifierGenericType.typeArguments[0].rawType.returnType.typeArguments[0].rawType.returnType",
                funcTypeRetType(funcTypeRetType(typeArgument(letValIfFalseLambda._classifierGenericType(), 0)._rawType())._typeArguments().getOnly()._rawType()));
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0]", letValIfFalseLambda._expressionSequence().getOnly());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType", letValIfFalseLambda._expressionSequence().getOnly()._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType.typeArguments[0]", letValIfFalseLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType.typeArguments[0].rawType",
                letValIfFalseLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.parameters['']",
                funcTypeParam(letValIfFalseLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(letValIfFalseLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.parameters[''].genericType.typeParameter",
                funcTypeParam(letValIfFalseLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(letValIfFalseLambda._expressionSequence().getOnly()._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0]",
                ((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0]",
                ((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType",
                ((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters['']",
                funcTypeParam(((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters[''].genericType.typeParameter",
                funcTypeParam(((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[0].parametersValues[1].parametersValues[2].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(((SimpleFunctionExpression) letValIfFalseLambda._expressionSequence().getOnly())._parametersValues().getOnly()._genericType()._typeArguments().getOnly()._rawType()));

        SimpleFunctionExpression mapExp = (SimpleFunctionExpression) testFunction._expressionSequence().getLast();
        VariableExpression mapColParam = (VariableExpression) mapExp._parametersValues().getFirst();
        InstanceValue mapFuncParam = (InstanceValue) mapExp._parametersValues().getLast();
        LambdaFunction<?> mapFunc = (LambdaFunction<?>) mapFuncParam._values().getOnly();
        expected.put(path + ".expressionSequence[1]", mapExp);
        expected.put(path + ".expressionSequence[1].genericType", mapExp._genericType());
        expected.put(path + ".expressionSequence[1].multiplicity", mapExp._multiplicity());
        expected.put(path + ".expressionSequence[1].parametersValues[0]", mapColParam);
        expected.put(path + ".expressionSequence[1].parametersValues[0].genericType", mapColParam._genericType());
        expected.put(path + ".expressionSequence[1].parametersValues[0].genericType.typeParameter", mapColParam._genericType()._typeParameter());
        expected.put(path + ".expressionSequence[1].parametersValues[0].multiplicity", mapColParam._multiplicity());
        expected.put(path + ".expressionSequence[1].parametersValues[1]", mapFuncParam);
        expected.put(path + ".expressionSequence[1].parametersValues[1].genericType", mapFuncParam._genericType());
        expected.put(path + ".expressionSequence[1].parametersValues[1].genericType.typeArguments[0]", typeArgument(mapFuncParam._genericType(), 0));
        expected.put(path + ".expressionSequence[1].parametersValues[1].genericType.typeArguments[0].rawType", typeArgument(mapFuncParam._genericType(), 0)._rawType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].genericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(typeArgument(mapFuncParam._genericType(), 0)._rawType(), "x"));
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].genericType.typeArguments[0].rawType.parameters['x'].genericType",
                funcTypeParam(typeArgument(mapFuncParam._genericType(), 0)._rawType(), "x")._genericType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].genericType.typeArguments[0].rawType.parameters['x'].genericType.typeParameter",
                funcTypeParam(typeArgument(mapFuncParam._genericType(), 0)._rawType(), "x")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(mapFuncParam._genericType(), 0)._rawType()));
        expected.put(path + ".expressionSequence[1].parametersValues[1].values[0]", mapFunc);
        expected.put(path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType", mapFunc._classifierGenericType());
        expected.put(path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0]", typeArgument(mapFunc._classifierGenericType(), 0));
        expected.put(path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType", typeArgument(mapFunc._classifierGenericType(), 0)._rawType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(typeArgument(mapFunc._classifierGenericType(), 0)._rawType(), "x"));
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['x'].genericType",
                funcTypeParam(typeArgument(mapFunc._classifierGenericType(), 0)._rawType(), "x")._genericType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.parameters['x'].genericType.typeParameter",
                funcTypeParam(typeArgument(mapFunc._classifierGenericType(), 0)._rawType(), "x")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(typeArgument(mapFunc._classifierGenericType(), 0)._rawType()));
        expected.put(path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0]", mapFunc._expressionSequence().getOnly());
        expected.put(path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].genericType", mapFunc._expressionSequence().getOnly()._genericType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0]",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0]",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType()._typeArguments().getOnly());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters['']",
                funcTypeParam(((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType()._typeArguments().getOnly()._rawType(), ""));
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters[''].genericType",
                funcTypeParam(((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.parameters[''].genericType.typeParameter",
                funcTypeParam(((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType()._typeArguments().getOnly()._rawType(), "")._genericType()._typeParameter());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[0].genericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getFirst()._genericType()._typeArguments().getOnly()._rawType()));
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[1]",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getLast());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getLast()._genericType());
        expected.put(
                path + ".expressionSequence[1].parametersValues[1].values[0].expressionSequence[0].parametersValues[1].genericType.typeParameter",
                ((SimpleFunctionExpression) mapFunc._expressionSequence().getOnly())._parametersValues().getLast()._genericType()._typeParameter());

        assertIds(path, expected);
    }

    @Test
    public void testFunction2()
    {
        String path = "test::model::testFunc2__String_1_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        MutableMap<String, Object> expected = Maps.mutable.with(path, testFunction);

        FunctionType functionType = (FunctionType) typeArgument(testFunction._classifierGenericType(), 0)._rawType();
        expected.put(path + ".classifierGenericType", testFunction._classifierGenericType());
        expected.put(path + ".classifierGenericType.typeArguments[0]", typeArgument(testFunction._classifierGenericType(), 0));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType", functionType);
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(functionType));

        ListIterable<? extends ValueSpecification> expressionSequence = toList(testFunction._expressionSequence());
        SimpleFunctionExpression letPkg = (SimpleFunctionExpression) expressionSequence.get(0);
        expected.put(path + ".expressionSequence[0]", letPkg);
        expected.put(path + ".expressionSequence[0].genericType", letPkg._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[0]", letPkg._parametersValues().getFirst());
        expected.put(path + ".expressionSequence[0].parametersValues[0].genericType", letPkg._parametersValues().getFirst()._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1]", letPkg._parametersValues().getLast());
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType", letPkg._parametersValues().getLast()._genericType());

        SimpleFunctionExpression letUnit = (SimpleFunctionExpression) expressionSequence.get(1);
        expected.put(path + ".expressionSequence[1]", letUnit);
        expected.put(path + ".expressionSequence[1].genericType", letUnit._genericType());
        expected.put(path + ".expressionSequence[1].parametersValues[0]", letUnit._parametersValues().getFirst());
        expected.put(path + ".expressionSequence[1].parametersValues[0].genericType", letUnit._parametersValues().getFirst()._genericType());
        expected.put(path + ".expressionSequence[1].parametersValues[1]", letUnit._parametersValues().getLast());
        expected.put(path + ".expressionSequence[1].parametersValues[1].genericType", letUnit._parametersValues().getLast()._genericType());

        SimpleFunctionExpression joinStrs = (SimpleFunctionExpression) expressionSequence.get(2);
        expected.put(path + ".expressionSequence[2]", joinStrs);
        expected.put(path + ".expressionSequence[2].genericType", joinStrs._genericType());
//        expected.put(path + ".expressionSequence[2].parametersValues[0]", joinStrs._parametersValues().getOnly());
        expected.put(path + ".expressionSequence[2].parametersValues[0].genericType", joinStrs._parametersValues().getOnly()._genericType());

        ListIterable<?> stringValSpecs = toList(((InstanceValue) joinStrs._parametersValues().getOnly())._values());

        SimpleFunctionExpression eltToPath = (SimpleFunctionExpression) stringValSpecs.get(0);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[0]", eltToPath);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[0].genericType", eltToPath._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[0].parametersValues[0]", eltToPath._parametersValues().getOnly());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[0].parametersValues[0].genericType", eltToPath._parametersValues().getOnly()._genericType());

        expected.put(path + ".expressionSequence[2].parametersValues[0].values[1]", stringValSpecs.get(1));
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[1].genericType", ((ValueSpecification) stringValSpecs.get(1))._genericType());

        SimpleFunctionExpression measureNameToOne = (SimpleFunctionExpression) stringValSpecs.get(2);
        SimpleFunctionExpression measureName = (SimpleFunctionExpression) measureNameToOne._parametersValues().getOnly();
        SimpleFunctionExpression unitMeasure = (SimpleFunctionExpression) measureName._parametersValues().getOnly();
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2]", measureNameToOne);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].genericType", measureNameToOne._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0]", measureName);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].genericType", measureName._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].parametersValues[0]", unitMeasure);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].parametersValues[0].genericType", unitMeasure._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].parametersValues[0].propertyName", unitMeasure._propertyName());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].parametersValues[0].parametersValues[0]", unitMeasure._parametersValues().getOnly());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].parametersValues[0].parametersValues[0].genericType", unitMeasure._parametersValues().getOnly()._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[2].parametersValues[0].propertyName", measureName._propertyName());

        expected.put(path + ".expressionSequence[2].parametersValues[0].values[3]", stringValSpecs.get(3));
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[3].genericType", ((ValueSpecification) stringValSpecs.get(3))._genericType());

        SimpleFunctionExpression unitNameToOne = (SimpleFunctionExpression) stringValSpecs.get(4);
        SimpleFunctionExpression unitName = (SimpleFunctionExpression) unitNameToOne._parametersValues().getOnly();
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4]", unitNameToOne);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4].genericType", unitNameToOne._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4].parametersValues[0]", unitName);
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4].parametersValues[0].genericType", unitName._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4].parametersValues[0].parametersValues[0]", unitName._parametersValues().getOnly());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4].parametersValues[0].parametersValues[0].genericType", unitName._parametersValues().getOnly()._genericType());
        expected.put(path + ".expressionSequence[2].parametersValues[0].values[4].parametersValues[0].propertyName", unitName._propertyName());

        assertIds(path, expected);
    }

    @Test
    public void testFunction3()
    {
        String path = "test::model::testFunc3__Any_MANY_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        MutableMap<String, Object> expected = Maps.mutable.with(path, testFunction);

        Assert.assertNull(getCoreInstance("test::model").getSourceInformation());

        FunctionType functionType = (FunctionType) typeArgument(testFunction._classifierGenericType(), 0)._rawType();
        expected.put(path + ".classifierGenericType", testFunction._classifierGenericType());
        expected.put(path + ".classifierGenericType.typeArguments[0]", typeArgument(testFunction._classifierGenericType(), 0));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType", functionType);
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(functionType));

        InstanceValue listExpr = (InstanceValue) testFunction._expressionSequence().getOnly();
        expected.put(path + ".expressionSequence[0]", listExpr);
        expected.put(path + ".expressionSequence[0].genericType", listExpr._genericType());

        ListIterable<?> listValues = toList(listExpr._values());

        InstanceValue pkgWrapper = (InstanceValue) listValues.get(0);
        expected.put(path + ".expressionSequence[0].values[0]", pkgWrapper);
        expected.put(path + ".expressionSequence[0].values[0].genericType", pkgWrapper._genericType());

        LambdaFunction<?> lambdaFunc = (LambdaFunction<?>) listValues.get(4);
        expected.put(path + ".expressionSequence[0].values[4]", lambdaFunc);
        expected.put(path + ".expressionSequence[0].values[4].classifierGenericType", lambdaFunc._classifierGenericType());
        expected.put(path + ".expressionSequence[0].values[4].classifierGenericType.typeArguments[0]", typeArgument(lambdaFunc._classifierGenericType(), 0));

        FunctionType lambdaFuncType = (FunctionType) typeArgument(lambdaFunc._classifierGenericType(), 0)._rawType();
        expected.put(path + ".expressionSequence[0].values[4].classifierGenericType.typeArguments[0].rawType", lambdaFuncType);
        expected.put(path + ".expressionSequence[0].values[4].classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(lambdaFuncType));

        SimpleFunctionExpression childrenExpr = (SimpleFunctionExpression) lambdaFunc._expressionSequence().getOnly();
        expected.put(path + ".expressionSequence[0].values[4].expressionSequence[0]", childrenExpr);
        expected.put(path + ".expressionSequence[0].values[4].expressionSequence[0].genericType", childrenExpr._genericType());
        expected.put(path + ".expressionSequence[0].values[4].expressionSequence[0].parametersValues[0]", childrenExpr._parametersValues().getOnly());
        expected.put(path + ".expressionSequence[0].values[4].expressionSequence[0].parametersValues[0].genericType", childrenExpr._parametersValues().getOnly()._genericType());
        expected.put(path + ".expressionSequence[0].values[4].expressionSequence[0].propertyName", childrenExpr._propertyName());

        assertIds(path, expected);
    }

    @Test
    public void testFunction4()
    {
        String path = "test::model::testFunc4_ClassWithMilestoning1_1__ClassWithMilestoning3_MANY_";
        ConcreteFunctionDefinition<?> testFunction = getCoreInstance(path);
        MutableMap<String, Object> expected = Maps.mutable.with(path, testFunction);

        Assert.assertNull(getCoreInstance("test::model").getSourceInformation());

        FunctionType functionType = (FunctionType) typeArgument(testFunction._classifierGenericType(), 0)._rawType();
        expected.put(path + ".classifierGenericType", testFunction._classifierGenericType());
        expected.put(path + ".classifierGenericType.typeArguments[0]", typeArgument(testFunction._classifierGenericType(), 0));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType", functionType);
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['input']", funcTypeParam(typeArgument(testFunction._classifierGenericType(), 0)._rawType(), "input"));
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.parameters['input'].genericType", funcTypeParam(typeArgument(testFunction._classifierGenericType(), 0)._rawType(), "input")._genericType());
        expected.put(path + ".classifierGenericType.typeArguments[0].rawType.returnType", funcTypeRetType(functionType));

        SimpleFunctionExpression expr = (SimpleFunctionExpression) testFunction._expressionSequence().getOnly();
        expected.put(path + ".expressionSequence[0]", expr);
        expected.put(path + ".expressionSequence[0].genericType", expr._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[0]", paramValue(expr, 0));
        expected.put(path + ".expressionSequence[0].parametersValues[0].genericType", paramValue(expr, 0)._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[1]", paramValue(expr, 1));
        expected.put(path + ".expressionSequence[0].parametersValues[1].genericType", paramValue(expr, 1)._genericType());
        expected.put(path + ".expressionSequence[0].parametersValues[2]", paramValue(expr, 2));
        expected.put(path + ".expressionSequence[0].parametersValues[2].genericType", paramValue(expr, 2)._genericType());
        expected.put(path + ".expressionSequence[0].qualifiedPropertyName", expr._qualifiedPropertyName());

        assertIds(path, expected);
    }

    @Test
    public void testMeasureWithNonconvertibleUnits()
    {
        String path = "test::model::Currency";
        Measure currency = getCoreInstance(path);
        MutableMap<String, CoreInstance> expected = Maps.mutable.with(path, currency);

        expected.put(path + ".classifierGenericType", currency._classifierGenericType());
        expected.put(path + ".generalizations[0]", currency._generalizations().getOnly());
        expected.put(path + ".generalizations[0].general", currency._generalizations().getOnly()._general());

        expected.put(path + ".canonicalUnit", currency._canonicalUnit());
        expected.put(path + ".canonicalUnit.classifierGenericType", currency._canonicalUnit()._classifierGenericType());
        expected.put(path + ".canonicalUnit.generalizations[0]", currency._canonicalUnit()._generalizations().getOnly());
        expected.put(path + ".canonicalUnit.generalizations[0].general", currency._canonicalUnit()._generalizations().getOnly()._general());

        ListIterable<? extends Unit> units = toList(currency._nonCanonicalUnits());
        expected.put(path + ".nonCanonicalUnits['GBP']", units.get(0));
        expected.put(path + ".nonCanonicalUnits['GBP'].classifierGenericType", units.get(0)._classifierGenericType());
        expected.put(path + ".nonCanonicalUnits['GBP'].generalizations[0]", units.get(0)._generalizations().getOnly());
        expected.put(path + ".nonCanonicalUnits['GBP'].generalizations[0].general", units.get(0)._generalizations().getOnly()._general());

        expected.put(path + ".nonCanonicalUnits['EUR']", units.get(1));
        expected.put(path + ".nonCanonicalUnits['EUR'].classifierGenericType", units.get(1)._classifierGenericType());
        expected.put(path + ".nonCanonicalUnits['EUR'].generalizations[0]", units.get(1)._generalizations().getOnly());
        expected.put(path + ".nonCanonicalUnits['EUR'].generalizations[0].general", units.get(1)._generalizations().getOnly()._general());

        assertIds(path, expected);
    }

    @Test
    public void testMeasureWithConveritbleUnits()
    {
        String path = "test::model::Mass";
        Measure mass = getCoreInstance(path);
        MutableMap<String, Object> expected = Maps.mutable.with(path, mass);

        expected.put(path + ".classifierGenericType", mass._classifierGenericType());
        expected.put(path + ".generalizations[0]", mass._generalizations().getOnly());
        expected.put(path + ".generalizations[0].general", mass._generalizations().getOnly()._general());

        expected.put(path + ".canonicalUnit", mass._canonicalUnit());
        expected.put(path + ".canonicalUnit.classifierGenericType", mass._canonicalUnit()._classifierGenericType());
        expected.put(path + ".canonicalUnit.conversionFunction", mass._canonicalUnit()._conversionFunction());
        expected.put(path + ".canonicalUnit.conversionFunction.classifierGenericType", mass._canonicalUnit()._conversionFunction()._classifierGenericType());
        expected.put(path + ".canonicalUnit.conversionFunction.classifierGenericType.typeArguments[0]", mass._canonicalUnit()._conversionFunction()._classifierGenericType()._typeArguments().getOnly());
        expected.put(path + ".canonicalUnit.conversionFunction.classifierGenericType.typeArguments[0].rawType", mass._canonicalUnit()._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".canonicalUnit.conversionFunction.classifierGenericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(mass._canonicalUnit()._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType(), "x"));
        expected.put(
                path + ".canonicalUnit.conversionFunction.classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(mass._canonicalUnit()._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType()));
        expected.put(path + ".canonicalUnit.conversionFunction.expressionSequence[0]", mass._canonicalUnit()._conversionFunction()._expressionSequence().getOnly());
        expected.put(path + ".canonicalUnit.conversionFunction.expressionSequence[0].genericType", mass._canonicalUnit()._conversionFunction()._expressionSequence().getOnly()._genericType());
        expected.put(path + ".canonicalUnit.generalizations[0]", mass._canonicalUnit()._generalizations().getOnly());
        expected.put(path + ".canonicalUnit.generalizations[0].general", mass._canonicalUnit()._generalizations().getOnly()._general());

        ListIterable<? extends Unit> units = toList(mass._nonCanonicalUnits());
        expected.put(path + ".nonCanonicalUnits['Kilogram']", units.get(0));
        expected.put(path + ".nonCanonicalUnits['Kilogram'].classifierGenericType", units.get(0)._classifierGenericType());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].conversionFunction", units.get(0)._conversionFunction());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].conversionFunction.classifierGenericType", units.get(0)._conversionFunction()._classifierGenericType());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].conversionFunction.classifierGenericType.typeArguments[0]", units.get(0)._conversionFunction()._classifierGenericType()._typeArguments().getOnly());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].conversionFunction.classifierGenericType.typeArguments[0].rawType", units.get(0)._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".nonCanonicalUnits['Kilogram'].conversionFunction.classifierGenericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(units.get(0)._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType(), "x"));
        expected.put(
                path + ".nonCanonicalUnits['Kilogram'].conversionFunction.classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(units.get(0)._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType()));
        expected.put(path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0]", units.get(0)._conversionFunction()._expressionSequence().getOnly());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0].genericType", units.get(0)._conversionFunction()._expressionSequence().getOnly()._genericType());
        expected.put(
                path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0].parametersValues[0].values[0]",
                ((InstanceValue) ((SimpleFunctionExpression) (units.get(0)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getFirst());
        expected.put(
                path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0].parametersValues[0].values[0].genericType",
                ((VariableExpression) ((InstanceValue) ((SimpleFunctionExpression) (units.get(0)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getFirst())._genericType());
        expected.put(
                path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0].parametersValues[0].values[1]",
                ((InstanceValue) ((SimpleFunctionExpression) (units.get(0)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getLast());
        expected.put(
                path + ".nonCanonicalUnits['Kilogram'].conversionFunction.expressionSequence[0].parametersValues[0].values[1].genericType",
                ((InstanceValue) ((InstanceValue) ((SimpleFunctionExpression) (units.get(0)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getLast())._genericType());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].generalizations[0]", units.get(0)._generalizations().getOnly());
        expected.put(path + ".nonCanonicalUnits['Kilogram'].generalizations[0].general", units.get(0)._generalizations().getOnly()._general());

        expected.put(path + ".nonCanonicalUnits['Pound']", units.get(1));
        expected.put(path + ".nonCanonicalUnits['Pound'].classifierGenericType", units.get(1)._classifierGenericType());
        expected.put(path + ".nonCanonicalUnits['Pound'].conversionFunction", units.get(1)._conversionFunction());
        expected.put(path + ".nonCanonicalUnits['Pound'].conversionFunction.classifierGenericType", units.get(1)._conversionFunction()._classifierGenericType());
        expected.put(path + ".nonCanonicalUnits['Pound'].conversionFunction.classifierGenericType.typeArguments[0]", units.get(1)._conversionFunction()._classifierGenericType()._typeArguments().getOnly());
        expected.put(path + ".nonCanonicalUnits['Pound'].conversionFunction.classifierGenericType.typeArguments[0].rawType", units.get(1)._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType());
        expected.put(
                path + ".nonCanonicalUnits['Pound'].conversionFunction.classifierGenericType.typeArguments[0].rawType.parameters['x']",
                funcTypeParam(units.get(1)._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType(), "x"));
        expected.put(
                path + ".nonCanonicalUnits['Pound'].conversionFunction.classifierGenericType.typeArguments[0].rawType.returnType",
                funcTypeRetType(units.get(1)._conversionFunction()._classifierGenericType()._typeArguments().getOnly()._rawType()));
        expected.put(path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0]", units.get(1)._conversionFunction()._expressionSequence().getOnly());
        expected.put(path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0].genericType", units.get(1)._conversionFunction()._expressionSequence().getOnly()._genericType());
        expected.put(
                path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0].parametersValues[0].values[0]",
                ((InstanceValue) ((SimpleFunctionExpression) (units.get(1)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getFirst());
        expected.put(
                path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0].parametersValues[0].values[0].genericType",
                ((VariableExpression) ((InstanceValue) ((SimpleFunctionExpression) (units.get(1)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getFirst())._genericType());
        expected.put(
                path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0].parametersValues[0].values[1]",
                ((InstanceValue) ((SimpleFunctionExpression) (units.get(1)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getLast());
        expected.put(
                path + ".nonCanonicalUnits['Pound'].conversionFunction.expressionSequence[0].parametersValues[0].values[1].genericType",
                ((InstanceValue) ((InstanceValue) ((SimpleFunctionExpression) (units.get(1)._conversionFunction()._expressionSequence().getOnly()))._parametersValues().getOnly())._values().getLast())._genericType());
        expected.put(path + ".nonCanonicalUnits['Pound'].generalizations[0]", units.get(1)._generalizations().getOnly());
        expected.put(path + ".nonCanonicalUnits['Pound'].generalizations[0].general", units.get(1)._generalizations().getOnly()._general());

        assertIds(path, expected);
    }

    private void assertIds(CoreInstance element)
    {
        String path = PackageableElement.getUserPathForPackageableElement(element);
        if (_Package.isPackage(element, processorSupport))
        {
            // For packages, we have specific expectations
            assertIds(path, Maps.immutable.with(path, element));
        }
        else
        {
            // For everything else, we check that the element itself has the expected id
            MapIterable<CoreInstance, String> idsByInstance = idGenerator.generateIdsForElement(element);
            Assert.assertEquals(path, idsByInstance.get(element));
            Assert.assertSame(element, reverseIdMap(idsByInstance, path).get(path));
        }
    }

    private void assertIds(String path, MapIterable<String, ?> expected)
    {
        validateExpectedIds(path, expected);
        MapIterable<CoreInstance, String> idsByInstance = idGenerator.generateIdsForElement(path);
        MutableMap<String, CoreInstance> instancesById = reverseIdMap(idsByInstance, path);
        if (!expected.equals(instancesById))
        {
            MutableList<Pair<String, ?>> expectedMismatches = Lists.mutable.empty();
            Counter expectedMissing = new Counter();
            Counter mismatches = new Counter();
            Counter unexpected = new Counter();
            expected.forEachKeyValue((id, instance) ->
            {
                CoreInstance actualInstance = instancesById.get(id);
                if (!instance.equals(actualInstance))
                {
                    expectedMismatches.add(Tuples.pair(id, instance));
                    ((actualInstance == null) ? expectedMissing : mismatches).increment();
                }
            });
            MutableList<Pair<String, ?>> actualMismatches = Lists.mutable.empty();
            instancesById.forEachKeyValue((id, instance) ->
            {
                Object expectedInstance = expected.get(id);
                if (!instance.equals(expectedInstance))
                {
                    actualMismatches.add(Tuples.pair(id, instance));
                    if (expectedInstance == null)
                    {
                        unexpected.increment();
                    }
                }
            });
            Assert.assertEquals(
                    "Ids for " + path + " not as expected (" + expectedMissing.getCount() + " expected missing, " + mismatches.getCount() + " mismatches, " + unexpected.getCount() + " unexpected found)",
                    expectedMismatches.sortThis().makeString(System.lineSeparator()),
                    actualMismatches.sortThis().makeString(System.lineSeparator()));
        }
    }

    private void validateExpectedIds(String path, MapIterable<String, ?> expected)
    {
        MutableList<String> nullInstances = Lists.mutable.empty();
        expected.forEachKeyValue((id, instance) ->
        {
            if (instance == null)
            {
                nullInstances.add(id);
            }
        });
        if (nullInstances.notEmpty())
        {
            StringBuilder builder = new StringBuilder("Null instances for ").append(nullInstances.size()).append(" expected ids for \"").append(path).append("\":");
            nullInstances.sortThis().appendString(builder, "\n\t", "\n\t", "");
            Assert.fail(builder.toString());
        }
    }

    private MutableMap<String, CoreInstance> reverseIdMap(MapIterable<CoreInstance, String> idsByInstance, String path)
    {
        MutableMap<String, CoreInstance> instancesById = Maps.mutable.ofInitialCapacity(idsByInstance.size());
        MutableSet<String> duplicateIds = Sets.mutable.empty();
        idsByInstance.forEachKeyValue((instance, id) ->
        {
            if (instancesById.put(id, instance) != null)
            {
                duplicateIds.add(id);
            }
        });
        if (duplicateIds.notEmpty())
        {
            Assert.fail(duplicateIds.toSortedList().makeString("Duplicate ids for " + path + ": \"", "\", \"", "\""));
        }
        return instancesById;
    }
}
