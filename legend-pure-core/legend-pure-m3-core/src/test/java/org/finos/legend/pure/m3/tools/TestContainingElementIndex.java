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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestContainingElementIndex extends AbstractReferenceTest
{
    private static ContainingElementIndex index;

    @BeforeClass
    public static void setUpIndex()
    {
        index = ContainingElementIndex.builder(processorSupport).withAllElements().build();
    }

    @Test
    public void testVirtualPackage()
    {
        Package testModel = getCoreInstance("test::model");
        assertContainingInstance(testModel, testModel);
    }

    @Test
    public void testConcretePackage()
    {
        Package root = getCoreInstance("Root");
        assertContainingInstance(root, root);
    }

    @Test
    public void testSimpleClass()
    {
        testInstance("test::model::SimpleClass");
    }

    @Test
    public void testLeftClass()
    {
        Class<?> leftClass = testInstance("test::model::Left");
        assertContainingInstance(getCoreInstance("test::model::LeftRight"), leftClass._propertiesFromAssociations().getOnly());
    }

    @Test
    public void testRightClass()
    {
        Class<?> rightClass = testInstance("test::model::Right");
        assertContainingInstance(getCoreInstance("test::model::LeftRight"), rightClass._propertiesFromAssociations().getOnly());
    }

    @Test
    public void testLeftRightAssociation()
    {
        Association leftRight = testInstance("test::model::LeftRight");
        assertContainingInstance(leftRight, leftRight._properties().getFirst());
        assertContainingInstance(leftRight, leftRight._properties().getLast());
    }

    @Test
    public void testSimpleProfile()
    {
        testInstance("test::model::SimpleProfile");
    }

    @Test
    public void testSimpleEnumeration()
    {
        testInstance("test::model::SimpleEnumeration");
    }

    @Test
    public void testBothSidesClass()
    {
        testInstance("test::model::BothSides");
    }

    @Test
    public void testClassWithAnnotations()
    {
        testInstance("test::model::ClassWithAnnotations");
    }

    @Test
    public void testClassWithTypeAndMultParams()
    {
        testInstance("test::model::ClassWithTypeAndMultParams");
    }

    @Test
    public void testClassWithQualifiedProperties()
    {
        testInstance("test::model::ClassWithQualifiedProperties");
    }

    @Test
    public void testClassWithMilestoning1()
    {
        testInstance("test::model::ClassWithMilestoning1");
    }

    @Test
    public void testClassWithMilestoning2()
    {
        testInstance("test::model::ClassWithMilestoning2");
    }

    @Test
    public void testClassWithMilestoning3()
    {
        testInstance("test::model::ClassWithMilestoning2");
    }

    @Test
    public void testAssociationWithMilestoning1()
    {
        testInstance("test::model::AssociationWithMilestoning1");
    }

    @Test
    public void testAssociationWithMilestoning2()
    {
        testInstance("test::model::AssociationWithMilestoning2");
    }

    @Test
    public void testAssociationWithMilestoning3()
    {
        testInstance("test::model::AssociationWithMilestoning3");
    }

    @Test
    public void testTestFunc()
    {
        testInstance("test::model::testFunc_T_m__Function_$0_1$__String_m_");
    }

    @Test
    public void testTopLevelsAndPackaged()
    {
        PackageableElementIterable.fromProcessorSupport(processorSupport).forEach(this::testInstance);
    }

    @Test
    public void testAllInstances()
    {
        MutableList<Pair<CoreInstance, CoreInstance>> shouldBeNull = Lists.mutable.empty();
        MutableList<CoreInstance> shouldBeNonNull = Lists.mutable.empty();
        GraphNodeIterable.fromModelRepository(repository).forEach(instance ->
        {
            CoreInstance containing = index.findContainingElement(instance);
            SourceInformation sourceInfo = instance.getSourceInformation();
            if ((sourceInfo != null) || _Package.isPackage(instance, processorSupport))
            {
                if (containing == null)
                {
                    shouldBeNonNull.add(instance);
                }
            }
            else if (containing != null)
            {
                shouldBeNull.add(Tuples.pair(instance, containing));
            }
        });
        if (shouldBeNull.notEmpty() || shouldBeNonNull.notEmpty())
        {
            StringBuilder builder = new StringBuilder();
            if (shouldBeNull.notEmpty())
            {
                builder.append("There were ").append(shouldBeNull.size()).append(" instances that should not have a containing element but did:");
                shouldBeNull.forEach(pair ->
                {
                    appendInstanceDescriptionForFailureMessage(builder.append("\n\tinstance: "), pair.getOne());
                    appendInstanceDescriptionForFailureMessage(builder.append("\n\t\tfound containing element: "), pair.getTwo());
                });
            }
            if (shouldBeNonNull.notEmpty())
            {
                MutableMap<String, MutableList<CoreInstance>> bySource = Maps.mutable.empty();
                shouldBeNonNull.forEach(i -> bySource.getIfAbsentPut(i.getSourceInformation().getSourceId(), Lists.mutable::empty).add(i));
                builder.append("There were ").append(shouldBeNonNull.size()).append(" instances in ").append(bySource.size()).append(" sources that should have a containing element but did not:");
                bySource.keyValuesView().toSortedListBy(Pair::getOne).forEach(bySourcePair ->
                {
                    MutableList<CoreInstance> instances = bySourcePair.getTwo();
                    builder.append("\n\t").append(bySourcePair.getOne()).append(" (").append(instances.size()).append(')');
                    if (instances.size() <= 10)
                    {
                        instances.sortThisBy(CoreInstance::getSourceInformation)
                                .forEach(i ->
                                {
                                    appendInstanceForFailureMessage(builder.append("\n\t\t"), i);
                                    appendClassifierForFailureMessage(builder.append(" ("), i);
                                    i.getSourceInformation().appendIntervalMessage(builder.append(", ")).append(')');
                                });
                    }
                    else
                    {
                        MutableMap<CoreInstance, MutableList<CoreInstance>> byClassifier = Maps.mutable.empty();
                        instances.forEach(i -> byClassifier.getIfAbsentPut(i.getClassifier(), Lists.mutable::empty).add(i));
                        byClassifier.keyValuesView()
                                .collect(p -> Tuples.pair(PackageableElement.getUserPathForPackageableElement(p.getOne()), p.getTwo()), Lists.mutable.ofInitialCapacity(byClassifier.size()))
                                .sortThisBy(Pair::getOne)
                                .forEach(byClassifierPair ->
                                {
                                    MutableList<CoreInstance> classifierInstances = byClassifierPair.getTwo();
                                    builder.append("\n\t\tclassifier: ").append(byClassifierPair.getOne()).append(" (").append(classifierInstances.size()).append(')');
                                    classifierInstances.sortThisBy(CoreInstance::getSourceInformation)
                                            .forEach(i ->
                                            {
                                                appendInstanceForFailureMessage(builder.append("\n\t\t\t"), i);
                                                i.getSourceInformation().appendIntervalMessage(builder.append(" (")).append(')');
                                            });
                                });
                    }
                });
            }
            Assert.fail(builder.toString());
        }
    }

    private <T extends CoreInstance> T testInstance(String path)
    {
        T instance = getCoreInstance(path);
        testInstance(instance);
        return instance;
    }

    private void testInstance(CoreInstance instance)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        GraphNodeIterable.builder()
                .withStartingNode(instance)
                .withNodeFilter(node ->
                {
                    SourceInformation nodeSourceInfo = node.getSourceInformation();
                    if (nodeSourceInfo == null)
                    {
                        return GraphWalkFilterResult.get(false, !_Package.isPackage(node, processorSupport));
                    }
                    if (sourceInfo.subsumes(nodeSourceInfo))
                    {
                        return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
                    }
                    return GraphWalkFilterResult.REJECT_AND_STOP;
                })
                .build()
                .forEach(node -> assertContainingInstance(instance, node));
    }

    private void assertContainingInstance(CoreInstance expected, CoreInstance instance)
    {
        CoreInstance actual = index.findContainingElement(instance);
        if (expected != actual)
        {
            Assert.assertSame(getFailureMessage(instance, expected, actual), expected, actual);
        }
    }

    private String getFailureMessage(CoreInstance instance, CoreInstance expected, CoreInstance actual)
    {
        StringBuilder builder = new StringBuilder("input instance: ");
        appendInstanceDescriptionForFailureMessage(builder, instance);
        appendInstanceDescriptionForFailureMessage(builder.append(", expected: "), expected);
        appendInstanceDescriptionForFailureMessage(builder.append(", actual: "), actual);
        return builder.toString();
    }

    private StringBuilder appendInstanceDescriptionForFailureMessage(StringBuilder builder, CoreInstance instance)
    {
        appendInstanceForFailureMessage(builder, instance);
        if (instance != null)
        {
            appendClassifierForFailureMessage(builder.append(" ("), instance);

            SourceInformation sourceInfo = instance.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(builder.append(", "));
            }
            builder.append(')');
        }
        return builder;
    }

    private StringBuilder appendInstanceForFailureMessage(StringBuilder builder, CoreInstance instance)
    {
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            return PackageableElement.writeUserPathForPackageableElement(builder, instance);
        }
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType)
        {
            return GenericType.print(builder, instance, true, processorSupport);
        }
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity)
        {
            return Multiplicity.print(builder, instance, true);
        }
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)
        {
            return FunctionType.print(builder, instance, true, processorSupport);
        }
        if (instance instanceof Unit)
        {
            return Measure.printUnit(builder, instance, true);
        }
        if (instance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub)
        {
            return ImportStub.writeImportStubInfo(builder, instance);
        }
        if (instance instanceof VariableExpression)
        {
            VariableExpression var = (VariableExpression) instance;
            builder.append('$').append(var._name()).append(':');
            GenericType.print(builder, var._genericType(), true, processorSupport);
            Multiplicity.print(builder, var._multiplicity(), true);
            return builder;
        }
        if (instance instanceof ValueSpecification)
        {
            ValueSpecification valueSpec = (ValueSpecification) instance;
            builder.append('<').append(instance).append(">:");
            GenericType.print(builder, valueSpec._genericType(), true, processorSupport);
            Multiplicity.print(builder, valueSpec._multiplicity(), true);
            return builder;
        }
        return builder.append(instance);
    }

    private StringBuilder appendClassifierForFailureMessage(StringBuilder builder, CoreInstance instance)
    {
        CoreInstance classifier = instance.getClassifier();
        return (classifier == null) ?
               builder.append("<null classifier>") :
               PackageableElement.writeUserPathForPackageableElement(builder, classifier);
    }

}
