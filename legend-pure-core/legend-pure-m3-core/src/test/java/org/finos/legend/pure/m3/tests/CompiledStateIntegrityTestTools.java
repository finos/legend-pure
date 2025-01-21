// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.tests;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.PackageAccessor;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.graph.GraphPathIterable;
import org.finos.legend.pure.m3.navigation.graph.ResolvedGraphPath;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.junit.Assert;

import java.util.Formatter;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class CompiledStateIntegrityTestTools
{
    /**
     * Test the given instance for all available integrity violations.
     *
     * @param instance         instance to test
     * @param processorSupport processor support
     */
    public static void testInstanceIntegrity(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testInstanceIntegrity(Lists.immutable.with(instance), processorSupport);
    }

    /**
     * Test the given instance for all available integrity violations.
     *
     * @param instances        instances to test
     * @param processorSupport processor support
     */
    public static void testInstanceIntegrity(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, "instance integrity",
                (instance, violationConsumer) ->
                {
                    forEachClassifierGenericTypeViolation(instance, violationConsumer, processorSupport);
                    forEachPropertyClassifierViolation(instance, violationConsumer, processorSupport);
                    forEachPropertyValueTypeViolation(instance, violationConsumer, processorSupport);
                    forEachPropertyValueMultiplicityViolation(instance, violationConsumer, processorSupport);
                });
    }

    /**
     * Test the classifierGenericType of an instance. This checks that the
     * rawType of the classifierGenericType matches the instance's classifier
     * and that the number of type and multplicity arguments are appropriate.
     *
     * @param instance         instance to test
     * @param processorSupport processor support
     */
    public static void testInstanceClassifierGenericType(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testInstanceClassifierGenericType(Lists.immutable.with(instance), processorSupport);
    }

    /**
     * Test the classifierGenericType of the given instances. For each instance,
     * this checks that the rawType of the classifierGenericType matches the
     * instance's classifier and that the number of type and multplicity arguments
     * are appropriate.
     *
     * @param instances        instances to test
     * @param processorSupport processor support
     */
    public static void testInstanceClassifierGenericType(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, "instance classifierGenericType integrity",
                (instance, violationConsumer) -> forEachClassifierGenericTypeViolation(instance, violationConsumer, processorSupport));
    }

    /**
     * Test the property integrity of the given instance. This checks that all
     * properties are appropriate for the instance's classifier and that all
     * property values satisfy the properties' types and multiplicities.
     *
     * @param instance         instance to test
     * @param processorSupport processor support
     */
    public static void testPropertyIntegrity(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testPropertyIntegrity(Lists.immutable.with(instance), processorSupport);
    }

    /**
     * Test the property integrity of the given instances. For each instance,
     * this checks that all properties are appropriate for the instance's
     * classifier and that all property values satisfy the properties' types
     * and multiplicities.
     *
     * @param instances        instances to test
     * @param processorSupport processor support
     */
    public static void testPropertyIntegrity(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, "property integrity",
                (instance, violationConsumer) ->
                {
                    forEachPropertyClassifierViolation(instance, violationConsumer, processorSupport);
                    forEachPropertyValueTypeViolation(instance, violationConsumer, processorSupport);
                    forEachPropertyValueMultiplicityViolation(instance, violationConsumer, processorSupport);
                });
    }

    public static void testClassifierProperties(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testClassifierProperties(Lists.immutable.with(instance), processorSupport);
    }

    public static void testClassifierProperties(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, "property classifier",
                (instance, violationConsumer) -> forEachPropertyClassifierViolation(instance, violationConsumer, processorSupport));
    }

    public static void testPropertyValueMultiplicities(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testPropertyValueMultiplicities(Lists.immutable.with(instance), processorSupport);
    }

    public static void testPropertyValueMultiplicities(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, "property value multiplicity",
                (instance, violationConsumer) -> forEachPropertyValueMultiplicityViolation(instance, violationConsumer, processorSupport));
    }

    public static void testPropertyValueTypes(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testPropertyValueTypes(Lists.immutable.with(instance), processorSupport);
    }

    public static void testPropertyValueTypes(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, "property value type",
                (instance, violationConsumer) -> forEachPropertyValueTypeViolation(instance, violationConsumer, processorSupport));
    }

    public static <T extends CoreInstance> void testHasSourceInformation(Iterable<T> instances, String instanceDescription, Function<? super T, ? extends String> instancePrinter, boolean findPaths, ProcessorSupport processorSupport)
    {
        testHasSourceInformation(instances, instanceDescription, (sb, t) -> sb.append(instancePrinter.apply(t)), findPaths, processorSupport);
    }

    public static <T extends CoreInstance> void testHasSourceInformation(Iterable<T> instances, String instanceDescription, BiConsumer<? super StringBuilder, ? super T> instancePrinter, boolean findPaths, ProcessorSupport processorSupport)
    {
        MutableList<T> noSourceInfo = Iterate.select(instances, i -> i.getSourceInformation() == null, Lists.mutable.empty());
        if (noSourceInfo.notEmpty())
        {
            MutableList<String> errorMessages = Lists.mutable.empty();
            BiConsumer<? super StringBuilder, ? super T> resolvedInstancePrinter = (instancePrinter == null) ? StringBuilder::append : instancePrinter;
            if (findPaths)
            {
                MutableSet<T> remaining = forEachInstancePath(noSourceInfo, processorSupport, (instance, path) ->
                {
                    StringBuilder builder = new StringBuilder((instanceDescription == null) ? "Instance" : instanceDescription).append(": ");
                    resolvedInstancePrinter.accept(builder, instance);
                    path.getGraphPath().writeDescription(builder.append("\n\t\tpath: "));
                    errorMessages.add(builder.toString());
                });
                remaining.forEach(instance ->
                {
                    StringBuilder builder = new StringBuilder((instanceDescription == null) ? "Instance" : instanceDescription).append(": ");
                    resolvedInstancePrinter.accept(builder, instance);
                    errorMessages.add(builder.toString());
                });
            }
            else
            {
                noSourceInfo.forEach(instance ->
                {
                    StringBuilder builder = new StringBuilder((instanceDescription == null) ? "Instance" : instanceDescription).append(": ");
                    resolvedInstancePrinter.accept(builder, instance);
                    errorMessages.add(builder.toString());
                });
            }
            int errorCount = errorMessages.size();
            StringBuilder message = new StringBuilder(errorCount * 128).append("There ").append((errorCount == 1) ? "is" : "are");
            new Formatter(message).format(" %,d", errorCount);
            if (instanceDescription != null)
            {
                message.append(' ').append(instanceDescription);
            }
            message.append(" instance");
            if (errorCount != 1)
            {
                message.append('s');
            }
            message.append(" with no source information:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    static <T extends CoreInstance> MutableSet<T> forEachInstancePath(Iterable<T> instances, ProcessorSupport processorSupport, BiConsumer<? super T, ? super ResolvedGraphPath> consumer)
    {
        return forEachInstancePath(instances, null, processorSupport, consumer);
    }

    @SuppressWarnings("unchecked")
    static <T extends CoreInstance> MutableSet<T> forEachInstancePath(Iterable<T> instances, Iterable<? extends CoreInstance> startNodes, ProcessorSupport processorSupport, BiConsumer<? super T, ? super ResolvedGraphPath> consumer)
    {
        MutableSet<T> remaining = Sets.mutable.withAll(instances);
        for (CoreInstance startNode : (startNodes == null) ? getTopLevelAndPackagedIterable(processorSupport).select(n -> n.getSourceInformation() != null) : startNodes)
        {
            for (ResolvedGraphPath rgp : internalGraphPaths(startNode, processorSupport))
            {
                CoreInstance node = rgp.getLastResolvedNode();
                if (remaining.remove(node))
                {
                    consumer.accept((T) node, rgp);
                    if (remaining.isEmpty())
                    {
                        return remaining;
                    }
                }
            }
        }
        return remaining;
    }

    static LazyIterable<CoreInstance> getTopLevelAndPackagedIterable(ModelRepository repository)
    {
        return repository.getTopLevels()
                .asLazy()
                .concatenate(PackageTreeIterable.newRootPackageTreeIterable(repository)
                        .flatCollect(PackageAccessor::_children)
                        .collect(e -> (CoreInstance) e));
    }

    static LazyIterable<CoreInstance> getTopLevelAndPackagedIterable(ProcessorSupport processorSupport)
    {
        return PrimitiveUtilities.getPrimitiveTypes(processorSupport, Lists.mutable.ofInitialCapacity(PrimitiveUtilities.getPrimitiveTypeNames().size() + 2))
                .with(processorSupport.repository_getTopLevel(M3Paths.Package))
                .with(processorSupport.repository_getTopLevel(M3Paths.Root))
                .asLazy()
                .concatenate(PackageTreeIterable.newRootPackageTreeIterable(processorSupport)
                        .flatCollect(PackageAccessor::_children)
                        .collect(e -> (CoreInstance) e));
    }

    static LazyIterable<CoreInstance> componentInstances(CoreInstance element)
    {
        SourceInformation sourceInfo = Objects.requireNonNull(element.getSourceInformation(), "element source information may not be null");
        return GraphNodeIterable.builder()
                .withStartingNode(element)
                .withKeyFilter((node, key) ->
                {
                    switch (key)
                    {
                        case M3Properties._package:
                        {
                            return !M3PropertyPaths._package.equals(node.getRealKeyByName(key));
                        }
                        case M3Properties.children:
                        {
                            return !M3PropertyPaths.children.equals(node.getRealKeyByName(key));
                        }
                        default:
                        {
                            return true;
                        }
                    }
                })
                .withNodeFilter(node ->
                {
                    if (node == element)
                    {
                        return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
                    }
                    SourceInformation nodeSourceInfo = node.getSourceInformation();
                    boolean internal = (nodeSourceInfo == null) ? !(node instanceof Package) : sourceInfo.subsumes(nodeSourceInfo);
                    return internal ? GraphWalkFilterResult.ACCEPT_AND_CONTINUE : GraphWalkFilterResult.REJECT_AND_STOP;
                })
                .build();
    }

    static GraphPathIterable internalGraphPaths(CoreInstance element, ProcessorSupport processorSupport)
    {
        SourceInformation sourceInfo = Objects.requireNonNull(element.getSourceInformation(), "element source information may not be null");
        return GraphPathIterable.builder(processorSupport)
                .withStartNode(element)
                .withPropertyFilter((rgp, property) ->
                {
                    switch (property)
                    {
                        case M3Properties._package:
                        {
                            return !M3PropertyPaths._package.equals(rgp.getLastResolvedNode().getRealKeyByName(property));
                        }
                        case M3Properties.children:
                        {
                            return !M3PropertyPaths.children.equals(rgp.getLastResolvedNode().getRealKeyByName(property));
                        }
                        default:
                        {
                            return true;
                        }
                    }
                })
                .withPathFilter(rgp ->
                {
                    CoreInstance node = rgp.getLastResolvedNode();
                    if (node == element)
                    {
                        // all graph paths start from element and cannot contain loops: so this must be the starting path
                        return GraphWalkFilterResult.ACCEPT_AND_CONTINUE;
                    }
                    SourceInformation nodeSourceInfo = node.getSourceInformation();
                    boolean internal = (nodeSourceInfo == null) ? !(node instanceof Package) : sourceInfo.subsumes(nodeSourceInfo);
                    return internal ? GraphWalkFilterResult.ACCEPT_AND_CONTINUE : GraphWalkFilterResult.REJECT_AND_STOP;
                })
                .build();
    }

    static ResolvedGraphPath findPathToInstance(CoreInstance instance, ProcessorSupport processorSupport)
    {
        return getTopLevelAndPackagedIterable(processorSupport)
                .flatCollect(e -> Lists.immutable.with(findPathToInstanceWithinElement(e, instance, processorSupport)))
                .detect(Objects::nonNull);
    }

    static ResolvedGraphPath findPathToInstanceWithinElement(CoreInstance element, CoreInstance instance, ProcessorSupport processorSupport)
    {
        SourceInformation sourceInfo = element.getSourceInformation();
        if (sourceInfo == null)
        {
            return null;
        }
        SourceInformation instanceSourceInfo = instance.getSourceInformation();
        if ((instanceSourceInfo != null) && !sourceInfo.subsumes(instanceSourceInfo))
        {
            return null;
        }
        MutableSet<CoreInstance> visited = Sets.mutable.empty();
        return GraphPathIterable.builder(processorSupport)
                .withStartNode(element)
                .withPropertyFilter((rgp, property) ->
                {
                    switch (property)
                    {
                        case M3Properties._package:
                        {
                            return !M3PropertyPaths._package.equals(rgp.getLastResolvedNode().getRealKeyByName(property));
                        }
                        case M3Properties.children:
                        {
                            return !M3PropertyPaths.children.equals(rgp.getLastResolvedNode().getRealKeyByName(property));
                        }
                        default:
                        {
                            return true;
                        }
                    }
                })
                .withPathFilter(rgp ->
                {
                    CoreInstance node = rgp.getLastResolvedNode();
                    if (node == instance)
                    {
                        return GraphWalkFilterResult.ACCEPT_AND_STOP;
                    }
                    if (node == element)
                    {
                        // all graph paths start from element and cannot contain loops: so this must be the starting path
                        return GraphWalkFilterResult.REJECT_AND_CONTINUE;
                    }
                    if (visited.add(node))
                    {
                        SourceInformation nodeSourceInfo = node.getSourceInformation();
                        if ((nodeSourceInfo == null) ? !(node instanceof Package) : sourceInfo.subsumes(nodeSourceInfo))
                        {
                            return GraphWalkFilterResult.REJECT_AND_CONTINUE;
                        }
                    }
                    return GraphWalkFilterResult.REJECT_AND_STOP;
                })
                .build()
                .getAny();
    }

    private static void runIntegrityTest(Iterable<? extends CoreInstance> instances, String violationDescription, BiConsumer<CoreInstance, Consumer<? super String>> test)
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        instances.forEach(i -> test.accept(i, errorMessages::add));
        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            new Formatter(message).format("There %s %,d %s violation", (errorCount == 1) ? "is" : "are", errorCount, violationDescription);
            if (errorCount != 1)
            {
                message.append('s');
            }
            errorMessages.appendString(message, ":\n\t", "\n\t", "");
            Assert.fail(message.toString());
        }
    }

    /**
     * Apply the given consumer to each error message for a violation related the instance's
     * classifierGenericType. A violation occurs if the rawType of the classifierGenericType
     * does not match the instance's classifier or if the number of type and multiplicity
     * arguments differs from the required number.
     *
     * @param instance          instance to test
     * @param violationConsumer violation consumer
     * @param processorSupport  processor support
     */
    public static void forEachClassifierGenericTypeViolation(CoreInstance instance, Consumer<? super String> violationConsumer, ProcessorSupport processorSupport)
    {
        CoreInstance classifier = instance.getClassifier();
        ListIterable<? extends CoreInstance> typeParams = classifier.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        ListIterable<? extends CoreInstance> multParams = classifier.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);

        CoreInstance classifierGenericType = instance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        if (classifierGenericType == null)
        {
            if (typeParams.notEmpty() || multParams.notEmpty())
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(" (")).append(')');
                }
                message.append("; classifierGenericType: null; problem: classifierGenericType required when there are type or multiplicity parameters: ");
                _Class.print(message, classifier, true);
                violationConsumer.accept(message.toString());
            }
        }
        else
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(classifierGenericType, M3Properties.rawType, processorSupport);
            ListIterable<? extends CoreInstance> typeArgs = Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, M3Properties.typeArguments, processorSupport);
            ListIterable<? extends CoreInstance> multArgs = Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, M3Properties.multiplicityArguments, processorSupport);

            if (rawType != classifier)
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(" (")).append(')');
                }
                GenericType.print(message.append("; classifierGenericType: "), classifierGenericType, processorSupport);
                message.append("; problem: mismatch between rawType (");
                if (rawType == null)
                {
                    message.append("null");
                }
                else
                {
                    PackageableElement.writeUserPathForPackageableElement(message, rawType);
                }
                message.append(") and instance classifier (");
                PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                message.append(")");
                violationConsumer.accept(message.toString());
            }

            if (typeParams.size() != typeArgs.size())
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(" (")).append(')');
                }
                GenericType.print(message.append("; classifierGenericType: "), classifierGenericType, processorSupport);
                _Class.print(message.append("; classifier: "), classifier, true);
                message.append("; problem: mismatch between the number of type parameters (").append(typeParams.size()).append(") and the number of type arguments (").append(typeArgs.size()).append(")");
                violationConsumer.accept(message.toString());
            }

            if (multParams.size() != multArgs.size())
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(" (")).append(")");
                }
                GenericType.print(message.append("; classifierGenericType: "), classifierGenericType, processorSupport);
                _Class.print(message.append("; classifier: "), classifier, true);
                message.append("; problem: mismatch between the number of multiplicity parameters (").append(multParams.size()).append(") and the number of multiplicity arguments (").append(multArgs.size()).append(")");
                violationConsumer.accept(message.toString());
            }
        }
    }

    /**
     * Apply the given consumer to each error message related to a property classifier
     * violation. A property classifier violation is a case where the instance has a
     * property not defined for its classifier.
     *
     * @param instance          instance to test
     * @param violationConsumer violation consumer
     * @param processorSupport  processor support
     */
    public static void forEachPropertyClassifierViolation(CoreInstance instance, Consumer<? super String> violationConsumer, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(instance.getClassifier());
        MutableList<String> invalidProperties = instance.getKeys().reject(propertiesByName::containsKey, Lists.mutable.empty());
        if (invalidProperties.notEmpty())
        {
            StringBuilder message = new StringBuilder("Instance: ");
            message.append(instance);
            SourceInformation sourceInfo = instance.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(message.append(" (")).append(')');
            }
            message.append("; invalid ").append((invalidProperties.size() == 1) ? "property" : "properties");
            invalidProperties.sortThis().appendString(message, ": ", ", ", "");
            violationConsumer.accept(message.toString());
        }
    }

    /**
     * Apply the given consumer to each error message related to a property multiplicity
     * violation. A property multiplicity violation is a case where the number of values
     * for a property is inconsistent with its multiplicity.
     *
     * @param instance          instance to test
     * @param violationConsumer violation consumer
     * @param processorSupport  processor support
     */
    public static void forEachPropertyValueMultiplicityViolation(CoreInstance instance, Consumer<? super String> violationConsumer, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(instance.getClassifier());
        propertiesByName.forEachKeyValue((propertyName, property) ->
        {
            ListIterable<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(propertyName);
            CoreInstance multiplicity = Property.resolveInstancePropertyReturnMultiplicity(instance, property, processorSupport);
            if (Multiplicity.isMultiplicityConcrete(multiplicity))
            {
                int count = values.size();
                if (!Multiplicity.isValid(multiplicity, count))
                {
                    StringBuilder message = new StringBuilder("Instance: ");
                    message.append(instance);
                    SourceInformation sourceInfo = instance.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        sourceInfo.appendMessage(message.append(" (")).append(')');
                    }
                    message.append("; property: ").append(propertyName);
                    Multiplicity.print(message.append("; multiplicity: "), multiplicity, false);
                    message.append("; count: ").append(count);
                    violationConsumer.accept(message.toString());
                }
            }
            else
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(" (")).append(')');
                }
                message.append("; property: ").append(propertyName);
                Multiplicity.print(message.append("; multiplicity: "), multiplicity, false);
                message.append("; problem: non-concrete");
                violationConsumer.accept(message.toString());
            }
        });
    }

    /**
     * Apply the given consumer to each error message related to a property type
     * violation. A property type violation is a case where the type of the values
     * for a property is inconsistent with the property's type.
     *
     * @param instance          instance to test
     * @param violationConsumer violation consumer
     * @param processorSupport  processor support
     */
    public static void forEachPropertyValueTypeViolation(CoreInstance instance, Consumer<? super String> violationConsumer, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(instance.getClassifier());
        propertiesByName.forEachKeyValue((propertyName, property) ->
        {
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, propertyName, processorSupport);
            CoreInstance genericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(instance, processorSupport), property, processorSupport);
            if (GenericType.isGenericTypeFullyConcrete(genericType, processorSupport))
            {
                values.forEachWithIndex((value, i) ->
                {
                    CoreInstance valueGenericType = Instance.extractGenericTypeFromInstance(value, processorSupport);
                    if (!GenericType.isGenericTypeFullyConcrete(valueGenericType, processorSupport))
                    {
                        // TODO what should we do here?
//                            StringBuilder message = new StringBuilder("Instance: ");
//                            message.append(node);
//                            SourceInformation sourceInfo = node.getSourceInformation();
//                            if (sourceInfo != null)
//                            {
//                                message.append(" (");
//                                sourceInfo.appendMessage(message);
//                                message.append(")");
//                            }
//                            message.append("; property: ").append(propertyName);
//                            message.append("; value: ").append(value);
//                            message.append("; index: ").append(i);
//                            GenericType.print(message.append("; value generic type: "), valueGenericType, context, processorSupport);
//                            message.append("; problem: non-concrete value generic type");
//                            violationConsumer.accept(message.toString());
                    }
                    else if (!GenericType.isGenericCompatibleWith(valueGenericType, genericType, processorSupport))
                    {
                        StringBuilder message = new StringBuilder("Instance: ");
                        message.append(instance);
                        SourceInformation sourceInfo = instance.getSourceInformation();
                        if (sourceInfo != null)
                        {
                            sourceInfo.appendMessage(message.append(" (")).append(')');
                        }
                        ResolvedGraphPath path = findPathToInstance(instance, processorSupport);
                        if (path != null)
                        {
                            if (sourceInfo == null)
                            {
                                message.append(" (");
                            }
                            else
                            {
                                message.setLength(message.length() - 1);
                                message.append(", ");
                            }
                            path.getGraphPath().writeDescription(message).append(')');
                        }
                        message.append("; property: ").append(propertyName);
                        message.append("; value: ").append(value);
                        message.append("; index: ").append(i);
                        GenericType.print(message.append("; value generic type: "), valueGenericType, processorSupport);
                        GenericType.print(message.append("; property generic type: "), genericType, processorSupport);
                        violationConsumer.accept(message.toString());
                    }
                });
            }
            else
            {
                // TODO what should we do here?
//                    StringBuilder message = new StringBuilder("Instance: ");
//                    message.append(node);
//                    SourceInformation sourceInfo = node.getSourceInformation();
//                    if (sourceInfo != null)
//                    {
//                        message.append(" (");
//                        sourceInfo.appendMessage(message);
//                        message.append(")");
//                    }
//                    message.append("; property: ").append(propertyName);
//                    GenericType.print(message.append("; property generic type: "), genericType, context, processorSupport);
//                    message.append("; problem: non-concrete property generic type");
//                    violationConsumer.accept(message.toString());
            }
        });
    }
}
