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

package org.finos.legend.pure.m3;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.PrintPureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelSourceDeserializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelSourceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializerLibrary;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collection;

public abstract class AbstractCompiledStateIntegrityTest
{
    protected static PureRuntime runtime;
    protected static ModelRepository repository;
    protected static Context context;
    protected static ProcessorSupport processorSupport;
    protected static CoreInstance importStubClass;
    protected static CoreInstance propertyStubClass;
    protected static CoreInstance enumStubClass;

    @Deprecated
    protected static void initialize(MutableCodeStorage codeStorage, RichIterable<? extends Parser> parsers, RichIterable<? extends InlineDSL> inlineDSLs)
    {
        initialize(codeStorage);
    }

    protected static void initialize(MutableCodeStorage codeStorage)
    {
        runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(new PrintPureRuntimeStatus(System.out))
                .setTransactionalByDefault(false)
                .buildAndInitialize();

        repository = runtime.getModelRepository();
        context = runtime.getContext();
        processorSupport = runtime.getProcessorSupport();

        importStubClass = runtime.getCoreInstance(M3Paths.ImportStub);
        propertyStubClass = runtime.getCoreInstance(M3Paths.PropertyStub);
        enumStubClass = runtime.getCoreInstance(M3Paths.EnumStub);
    }

    @AfterClass
    public static void cleanUp()
    {
        if (runtime != null)
        {
            runtime.reset();
        }
        if (repository != null)
        {
            repository.clear();
        }
        if (context != null)
        {
            context.clear();
        }
        runtime = null;
        repository = null;
        context = null;
        processorSupport = null;
        importStubClass = null;
        propertyStubClass = null;
        enumStubClass = null;
    }


    @Test
    public void testTopLevels()
    {
        MutableSet<String> topLevelNames = repository.getTopLevels().collect(CoreInstance::getName, Sets.mutable.empty());
        MutableSet<String> expectedTopLevelNames = _Package.SPECIAL_TYPES.toSet().with(M3Paths.Root);

        Assert.assertEquals(repository.getTopLevels().size(), topLevelNames.size());
        Verify.assertSetsEqual(expectedTopLevelNames, topLevelNames);

        repository.getTopLevels().forEach(topLevel ->
        {
            SourceInformation sourceInfo = topLevel.getSourceInformation();
            Assert.assertNotNull("Null source information for " + topLevel.getName(), sourceInfo);

            Assert.assertEquals("Source information for " + topLevel.getName() + " not in m3.pure", "/platform/pure/m3.pure", sourceInfo.getSourceId());
        });
    }

    @Test
    public void testNullClassifiers()
    {
        MutableList<CoreInstance> nullClassifiers = selectNodes(n -> n.getClassifier() == null);
        if (nullClassifiers.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following ").append(nullClassifiers.size()).append(" elements have null classifiers:");
            nullClassifiers.forEach(instance ->
            {
                message.append("\n\t").append(instance.getName());
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    sourceInfo.appendMessage(message.append(": "));
                }
            });
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testClassifierGenericType()
    {
        CompiledStateIntegrityTestTools.testInstanceClassifierGenericType(GraphNodeIterable.fromModelRepository(repository), processorSupport);
    }

    @Test
    public void testPackageableElementsHaveSourceInfo()
    {
        CoreInstance functionTypeClass = runtime.getCoreInstance(M3Paths.FunctionType);
        CoreInstance importGroupClass = runtime.getCoreInstance(M3Paths.ImportGroup);
        CoreInstance packageClass = runtime.getCoreInstance(M3Paths.Package);
        CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(functionTypeClass);
        Assert.assertNotNull(importGroupClass);
        Assert.assertNotNull(packageClass);
        Assert.assertNotNull(packageableElementClass);

        ImmutableSet<CoreInstance> exceptionClasses = Sets.immutable.with(importGroupClass, packageClass, functionTypeClass);

        MutableList<CoreInstance> noSourceInfo = selectNodes(instance -> (instance.getSourceInformation() == null) &&
                !exceptionClasses.contains(instance.getClassifier()) &&
                Type.subTypeOf(instance.getClassifier(), packageableElementClass, processorSupport));
        if (noSourceInfo.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following packageable elements have no source information:");
            noSourceInfo.forEach(instance ->
            {
                message.append("\n\t");
                PackageableElement.writeUserPathForPackageableElement(message, instance);
                message.append(" (");
                PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                message.append(')');
            });
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testAllSourceNewInstancesArePackageableElements()
    {
        MutableMap<Source, MutableList<CoreInstance>> nonPackageableElementsBySource = Maps.mutable.empty();
        runtime.getSourceRegistry().getSources().forEach(source -> source.getNewInstances().forEach(instance ->
        {
            if (!Instance.instanceOf(instance, M3Paths.PackageableElement, processorSupport))
            {
                nonPackageableElementsBySource.getIfAbsentPut(source, Lists.mutable::empty).add(instance);
            }
        }));
        if (nonPackageableElementsBySource.notEmpty())
        {
            StringBuilder message = new StringBuilder();
            nonPackageableElementsBySource.forEachKeyValue((source, instances) -> instances.appendString(message.append(source.getId()), ":\n", "\n\t", "\n"));
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testAllPackageChildrenNonNull()
    {
        MutableList<Package> badPackages = PackageTreeIterable.newRootPackageTreeIterable(repository).select(pkg -> pkg._children().contains(null), Lists.mutable.empty());
        if (badPackages.notEmpty())
        {
            Assert.fail(badPackages.collect(PackageableElement::getUserPathForPackageableElement).sortThis().makeString("The following packages have null children: ", ", ", ""));
        }
    }

    @Test
    public void testAllPackageChildrenArePackageable()
    {
        CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(packageableElementClass);

        MutableMap<CoreInstance, MutableList<CoreInstance>> nonPackageableElements = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg -> pkg._children().forEach(child ->
        {
            if (!Instance.instanceOf(child, packageableElementClass, processorSupport))
            {
                nonPackageableElements.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
            }
        }));
        if (nonPackageableElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children that are not instances of ");
            builder.append(M3Paths.PackageableElement);
            nonPackageableElements.forEachKeyValue((pkg, children) ->
            {
                if (children.notEmpty())
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.forEach(child -> builder.append("\n\t\t").append(child));
                }
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllPackageChildrenHaveMatchingPackage()
    {
        CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(packageableElementClass);

        MutableMap<CoreInstance, MutableList<CoreInstance>> badElements = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg -> pkg._children().forEach(child ->
        {
            if (pkg != child.getValueForMetaPropertyToOne(M3Properties._package))
            {
                badElements.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
            }
        }));
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with a different package");
            badElements.forEachKeyValue((pkg, children) ->
            {
                if (children.notEmpty())
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.forEach(child ->
                    {
                        builder.append("\n\t\t").append(child).append(": ");
                        CoreInstance childPackage = child.getValueForMetaPropertyToOne(M3Properties._package);
                        if (childPackage == null)
                        {
                            builder.append(childPackage);
                        }
                        else
                        {
                            PackageableElement.writeUserPathForPackageableElement(builder, childPackage);
                        }
                    });
                }
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllElementsWithPackagesArePackageChildren()
    {
        MutableList<CoreInstance> badElements = Lists.mutable.empty();
        GraphNodeIterable.fromModelRepository(repository).forEach(instance ->
        {
            CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties._package, processorSupport);
            if (pkg != null)
            {
                if (!pkg.getValueForMetaPropertyToMany(M3Properties.children).contains(instance))
                {
                    badElements.add(instance);
                }
            }
        });
        if (badElements.notEmpty())
        {
            Assert.fail(badElements.collect(PackageableElement::getUserPathForPackageableElement).sortThis().makeString("The following elements are not children of their packages:\n\t", "\n\t", ""));
        }
    }

    @Test
    public void testAllPackageChildrenHaveNames()
    {
        MutableMap<CoreInstance, MutableList<CoreInstance>> badElements = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg -> pkg._children().forEach(child ->
        {
            if (child.getValueForMetaPropertyToOne(M3Properties.name) == null)
            {
                badElements.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
            }
        }));
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with no name:");
            badElements.forEachKeyValue((pkg, children) ->
            {
                if (children.notEmpty())
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.appendString(builder, "\n\t\t", "\n\t\t", "");
                }
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllPackageChildrenHaveNamesMatchingNameProperty()
    {
        MutableMap<CoreInstance, MutableList<Pair<String, String>>> badElements = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg -> pkg._children().forEach(child ->
        {
            CoreInstance nameInstance = child.getValueForMetaPropertyToOne(M3Properties.name);
            if ((nameInstance == null) || !child.getName().equals(nameInstance.getName()))
            {
                badElements.getIfAbsentPut(pkg, Lists.mutable::empty).add(Tuples.pair(child.getName(), (nameInstance == null) ? null : nameInstance.getName()));
            }
        }));
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with instance names that do not match the name property:");
            badElements.forEachKeyValue((pkg, children) ->
            {
                if (children.notEmpty())
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.forEach(nameName -> builder.append("\n\t\t'").append(nameName.getOne()).append("' / '").append(nameName.getTwo()).append('\''));
                }
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testNonAnonymousPackageChildren()
    {
        MutableMap<CoreInstance, MutableList<CoreInstance>> badElements = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg -> pkg._children().forEach(child ->
        {
            if (ModelRepository.isAnonymousInstanceName(child.getName()))
            {
                badElements.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
            }
        }));
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have anonymous children:");
            badElements.forEachKeyValue((pkg, children) ->
            {
                if (children.notEmpty())
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.appendString(builder, "\n\t\t", "\n\t\t", "");
                }
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testPackageHasChildrenWithDuplicateNames()
    {
        MutableMap<CoreInstance, MutableList<ObjectIntPair<String>>> badPackageNames = Maps.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg ->
        {
            Multimap<String, ? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> childrenByName = pkg._children().groupBy(CoreInstance::getName);
            childrenByName.forEachKeyMultiValues((name, children) ->
            {
                int size = Iterate.sizeOf(children);
                if (size > 1)
                {
                    badPackageNames.getIfAbsentPut(pkg, Lists.mutable::empty).add(PrimitiveTuples.pair(name, size));
                }
            });
        });
        if (badPackageNames.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with duplicate names:");
            badPackageNames.forEachKeyValue((pkg, children) ->
            {
                if (children.notEmpty())
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    builder.append(" (").append(children.size()).append(')');
                    children.toSortedList().forEach(nameCount -> builder.append("\n\t\t'").append(nameCount.getOne()).append("': ").append(nameCount.getTwo()));
                }
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllImportGroupsHaveSourceInfo()
    {
        CoreInstance imports = processorSupport.package_getByUserPath("system::imports");
        Assert.assertNotNull("Could not find system::imports", imports);
        ListIterable<? extends CoreInstance> importGroupsWithoutSourceInfo = imports.getValueForMetaPropertyToMany(M3Properties.children).select(child -> child.getSourceInformation() == null);
        if (importGroupsWithoutSourceInfo.notEmpty())
        {
            Assert.fail(importGroupsWithoutSourceInfo.collect(PackageableElement::getUserPathForPackageableElement, Lists.mutable.withInitialCapacity(importGroupsWithoutSourceInfo.size()))
                    .sortThis()
                    .makeString("The following ImportGroups have no source information:\n\t", "\n\t", ""));
        }
    }

    @Test
    public void testModelElementsForAnnotations()
    {
        MutableMap<CoreInstance, MutableSet<CoreInstance>> expectedModelElements = Maps.mutable.empty();
        MutableMap<CoreInstance, ListIterable<? extends CoreInstance>> actualModelElements = Maps.mutable.empty();

        CoreInstance stereotypeClass = runtime.getCoreInstance(M3Paths.Stereotype);
        CoreInstance tagClass = runtime.getCoreInstance(M3Paths.Tag);
        GraphNodeIterable.fromModelRepository(repository).forEach(node ->
        {
            if ((node.getClassifier() == stereotypeClass) || (node.getClassifier() == tagClass))
            {
                ListIterable<? extends CoreInstance> modelElements = node.getValueForMetaPropertyToMany(M3Properties.modelElements);
                actualModelElements.put(node, modelElements);
            }
            getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes).forEach(stereotype -> expectedModelElements.getIfAbsentPut(stereotype, Sets.mutable::empty).add(node));
            getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues).forEach(taggedValue ->
            {
                CoreInstance tag = getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.tag);
                expectedModelElements.getIfAbsentPut(tag, Sets.mutable::empty).add(node);
            });
        });

        // Check for annotations that aren't really annotations
        Verify.assertEmpty(expectedModelElements.keysView().reject(actualModelElements::containsKey, Lists.mutable.empty()));

        // Check for missing and extra elements
        MutableMap<CoreInstance, SetIterable<CoreInstance>> annotationsWithMissingModelElements = Maps.mutable.empty();
        MutableMap<CoreInstance, SetIterable<CoreInstance>> annotationsWithExtraModelElements = Maps.mutable.empty();
        actualModelElements.forEachKeyValue((annotation, actualList) ->
        {
            MutableSet<CoreInstance> expected = expectedModelElements.get(annotation);
            if (actualList.isEmpty())
            {
                if ((expected != null) && expected.notEmpty())
                {
                    annotationsWithMissingModelElements.put(annotation, expected);
                }
            }
            else if ((expected == null) || expected.isEmpty())
            {
                annotationsWithExtraModelElements.put(annotation, Sets.mutable.withAll(actualList));
            }
            else
            {
                MutableSet<CoreInstance> actualSet = Sets.mutable.withAll(actualList);
                if (!expected.equals(actualSet))
                {
                    MutableSet<CoreInstance> missing = expected.difference(actualSet);
                    if (missing.notEmpty())
                    {
                        annotationsWithMissingModelElements.put(annotation, missing);
                    }
                    MutableSet<CoreInstance> extra = actualSet.difference(expected);
                    if (extra.notEmpty())
                    {
                        annotationsWithExtraModelElements.put(annotation, extra);
                    }
                }
            }
        });

        if (annotationsWithMissingModelElements.notEmpty() || annotationsWithExtraModelElements.notEmpty())
        {
            StringBuilder message = new StringBuilder();
            if (annotationsWithMissingModelElements.notEmpty())
            {
                message.append("The following annotations are missing model elements:");
                annotationsWithMissingModelElements.forEachKeyValue((annotation, missingValues) ->
                {
                    message.append("\n\t").append(annotation.getClassifier().getName()).append(": ");
                    PackageableElement.writeUserPathForPackageableElement(message, annotation.getValueForMetaPropertyToOne(M3Properties.profile));
                    message.append('.').append(annotation.getName());
                    SourceInformation sourceInfo = annotation.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        sourceInfo.appendMessage(message.append(" (source information: ")).append(')');
                    }
                    missingValues.forEach(missing ->
                    {
                        message.append("\n\t\t").append(missing);
                        SourceInformation missingSourceInfo = missing.getSourceInformation();
                        if (missingSourceInfo != null)
                        {
                            missingSourceInfo.appendMessage(message.append(" (source information: ")).append(')');
                        }
                    });
                });
            }
            if (annotationsWithExtraModelElements.notEmpty())
            {
                message.append("The following annotations have extra model elements:");
                annotationsWithExtraModelElements.forEachKeyValue((annotation, extraValues) ->
                {
                    message.append("\n\t").append(annotation.getClassifier().getName()).append(": ");
                    PackageableElement.writeUserPathForPackageableElement(message, annotation.getValueForMetaPropertyToOne(M3Properties.profile));
                    message.append('.').append(annotation.getName());
                    SourceInformation sourceInfo = annotation.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        sourceInfo.appendMessage(message.append(" (source information: ")).append(')');
                    }
                    extraValues.forEach(extra ->
                    {
                        message.append("\n\t\t").append(extra);
                        SourceInformation missingSourceInfo = extra.getSourceInformation();
                        if (missingSourceInfo != null)
                        {
                            missingSourceInfo.appendMessage(message.append(" (source information: ")).append(')');
                        }
                    });
                });
            }
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testPropertyClassifierGenericTypes()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        CoreInstance classClass = processorSupport.package_getByUserPath(M3Paths.Class);
        PackageTreeIterable.newRootPackageTreeIterable(repository)
                .flatCollect(pkg -> pkg.getValueForMetaPropertyToMany(M3Properties.children))
                .select(node -> Instance.instanceOf(node, classClass, processorSupport))
                .forEach(node ->
                {
                    ListIterable<? extends CoreInstance> typeParams = node.getValueForMetaPropertyToMany(M3Properties.typeParameters);
                    ListIterable<CoreInstance> expectedSourceTypeArgs = typeParams.isEmpty() ?
                            null :
                            typeParams.collect(typeParam ->
                            {
                                CoreInstance typeArg = processorSupport.newGenericType(null, typeParam, false);
                                Instance.addValueToProperty(typeArg, M3Properties.typeParameter, typeParam, processorSupport);
                                return typeArg;
                            });

                    ListIterable<? extends CoreInstance> multParams = node.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
                    ListIterable<CoreInstance> expectedSourceMultArgs = multParams.isEmpty() ?
                            null :
                            multParams.collect(multParamInstanceValue -> Multiplicity.newMultiplicity(PrimitiveUtilities.getStringValue(multParamInstanceValue.getValueForMetaPropertyToOne(M3Properties.values)), processorSupport));

                    SourceInformation classSourceInfo = node.getSourceInformation();
                    Maps.fixedSize.with(
                            "property", node.getValueForMetaPropertyToMany(M3Properties.properties),
                            "property from association", node.getValueForMetaPropertyToMany(M3Properties.propertiesFromAssociations)
                    ).forEachKeyValue((propertyType, properties) -> properties.forEach(property ->
                    {
                        CoreInstance classifierGenericType = property.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
                        ListIterable<? extends CoreInstance> typeArgs = classifierGenericType.getValueForMetaPropertyToMany(M3Properties.typeArguments);
                        ListIterable<? extends CoreInstance> multArgs = Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, M3Properties.multiplicityArguments, processorSupport);
                        if ((typeArgs.size() != 2) || (multArgs.size() != 1))
                        {
                            StringBuilder message = new StringBuilder("Class: ");
                            _Class.print(message, node, true);
                            if (classSourceInfo != null)
                            {
                                classSourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; ").append(propertyType).append(": ");
                            message.append(property.getName());
                            SourceInformation propertySourceInfo = property.getSourceInformation();
                            if (propertySourceInfo != null)
                            {
                                propertySourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; property classifierGenericType: ");
                            GenericType.print(message, classifierGenericType, true, processorSupport);
                            message.append("; expected 2 type arguments (got ").append(typeArgs.size()).append(") and 1 multiplicity argument (got ").append(multArgs.size()).append(")");
                            errorMessages.add(message.toString());
                            return;
                        }

                        CoreInstance sourceGenericType = typeArgs.get(0);
                        CoreInstance expectedSourceGenericType = Type.wrapGenericType(node, processorSupport);
                        if (expectedSourceTypeArgs != null)
                        {
                            Instance.addValueToProperty(expectedSourceGenericType, M3Properties.typeArguments, expectedSourceTypeArgs, processorSupport);
                        }
                        if (expectedSourceMultArgs != null)
                        {
                            Instance.addValueToProperty(expectedSourceGenericType, M3Properties.multiplicityArguments, expectedSourceMultArgs, processorSupport);
                        }
                        if (!GenericType.genericTypesEqual(expectedSourceGenericType, sourceGenericType, processorSupport))
                        {
                            StringBuilder message = new StringBuilder("Class: ");
                            _Class.print(message, node, true);
                            if (classSourceInfo != null)
                            {
                                classSourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; ").append(propertyType).append(": ");
                            message.append(property.getName());
                            SourceInformation propertySourceInfo = property.getSourceInformation();
                            if (propertySourceInfo != null)
                            {
                                propertySourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; property classifierGenericType: ");
                            GenericType.print(message, classifierGenericType, true, processorSupport);
                            message.append("; first type argument should be ");
                            GenericType.print(message, expectedSourceGenericType, true, processorSupport);
                            errorMessages.add(message.toString());
                        }

                        CoreInstance targetGenericType = typeArgs.get(1);
                        CoreInstance expectedTargetGenericType = property.getValueForMetaPropertyToOne(M3Properties.genericType);
                        if (!GenericType.genericTypesEqual(expectedTargetGenericType, targetGenericType, processorSupport))
                        {
                            StringBuilder message = new StringBuilder("Class: ");
                            _Class.print(message, node, true);
                            SourceInformation sourceInfo = node.getSourceInformation();
                            if (sourceInfo != null)
                            {
                                sourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; ").append(propertyType).append(": ");
                            message.append(property.getName());
                            SourceInformation propertySourceInfo = property.getSourceInformation();
                            if (propertySourceInfo != null)
                            {
                                propertySourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; property classifierGenericType: ");
                            GenericType.print(message, classifierGenericType, true, processorSupport);
                            message.append("; property genericType: ");
                            GenericType.print(message, expectedTargetGenericType, true, processorSupport);
                            message.append("; second type argument of classifierGenericType should be the same as property genericType");
                            errorMessages.add(message.toString());
                        }

                        CoreInstance multiplicity = multArgs.get(0);
                        CoreInstance expectedMultiplicity = property.getValueForMetaPropertyToOne(M3Properties.multiplicity);
                        if (!Multiplicity.multiplicitiesEqual(expectedMultiplicity, multiplicity))
                        {
                            StringBuilder message = new StringBuilder("Class: ");
                            _Class.print(message, node, true);
                            SourceInformation sourceInfo = node.getSourceInformation();
                            if (sourceInfo != null)
                            {
                                sourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; ").append(propertyType).append(": ");
                            message.append(property.getName());
                            SourceInformation propertySourceInfo = property.getSourceInformation();
                            if (propertySourceInfo != null)
                            {
                                propertySourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; property classifierGenericType: ");
                            GenericType.print(message, classifierGenericType, true, processorSupport);
                            message.append("; property multiplicity: ");
                            Multiplicity.print(message, expectedMultiplicity, false);
                            message.append("; multiplicity argument of classifierGenericType should be the same as property multiplicity");
                            errorMessages.add(message.toString());
                        }
                    }));
                });
        int errorCount = errorMessages.size();
        if (errorCount > 0)

        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            message.append("There are ").append(errorCount).append(" property classifierGenericType errors:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }

    }

    @Test
    public void testQualifiedPropertyNames()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        CoreInstance propertyOwnerClass = processorSupport.package_getByUserPath(M3Paths.PropertyOwner);
        PackageTreeIterable.newRootPackageTreeIterable(repository)
                .flatCollect(pkg -> pkg.getValueForMetaPropertyToMany(M3Properties.children))
                .select(node -> Instance.instanceOf(node, propertyOwnerClass, processorSupport))
                .forEach(node ->
                {
                    ListIterable<? extends CoreInstance> qualifiedProperties = node.getValueForMetaPropertyToMany(M3Properties.qualifiedProperties);
                    if (qualifiedProperties.notEmpty())
                    {
                        qualifiedProperties.groupBy(CoreInstance::getName).forEachKeyMultiValues((name, qualifiedPropertiesForName) ->
                        {
                            int size = Iterate.sizeOf(qualifiedPropertiesForName);
                            if (size > 1)
                            {
                                StringBuilder message = new StringBuilder("Property owner: ");
                                PackageableElement.writeUserPathForPackageableElement(message, node);
                                SourceInformation sourceInfo = node.getSourceInformation();
                                if (sourceInfo != null)
                                {
                                    sourceInfo.appendMessage(message.append(" (")).append(')');
                                }
                                message.append("; multiple qualified properties with name '").append(name).append("' (").append(size).append(')');
                                errorMessages.add(message.toString());
                            }
                        });
                    }
                });
        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            message.append("There are ").append(errorCount).append(" qualified property name errors:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testPropertyIntegrity()
    {
        CompiledStateIntegrityTestTools.testClassifierProperties(GraphNodeIterable.fromModelRepository(repository), processorSupport);
    }

    @Test
    public void testPropertyValueMultiplicities()
    {
        CompiledStateIntegrityTestTools.testPropertyValueMultiplicities(GraphNodeIterable.fromModelRepository(repository), processorSupport);
    }

    @Test
    public void testPropertyValueTypes()
    {
        CompiledStateIntegrityTestTools.testPropertyValueTypes(GraphNodeIterable.fromModelRepository(repository), processorSupport);
    }

    @Test
    public void testFunctionApplications()
    {
        CoreInstance functionClass = runtime.getCoreInstance(M3Paths.Function);
        CoreInstance functionExpressionClass = runtime.getCoreInstance(M3Paths.FunctionExpression);

        MutableMap<CoreInstance, MutableSet<CoreInstance>> expected = Maps.mutable.empty();
        MutableMap<CoreInstance, MutableSet<CoreInstance>> actual = Maps.mutable.empty();

        GraphNodeIterable.fromModelRepository(repository).forEach(instance ->
        {
            if (Instance.instanceOf(instance, functionClass, processorSupport))
            {
                ListIterable<? extends CoreInstance> applications = instance.getValueForMetaPropertyToMany(M3Properties.applications);
                if (applications.notEmpty())
                {
                    actual.put(instance, Sets.mutable.withAll(applications));
                }
            }
            else if (Instance.instanceOf(instance, functionExpressionClass, processorSupport))
            {
                CoreInstance function = instance.getValueForMetaPropertyToOne(M3Properties.func);
                expected.getIfAbsentPut(function, Sets.mutable::empty).add(instance);
            }
        });

        Verify.assertMapsEqual(expected, actual);
    }

    @Test
    public void testFunctionsHaveFunctionTypes()
    {
        CoreInstance functionClass = runtime.getCoreInstance(M3Paths.Function);
        MutableList<String> errorMessages = Lists.mutable.empty();
        GraphNodeIterable.fromModelRepository(repository)
                .select(instance -> Instance.instanceOf(instance, functionClass, processorSupport))
                .forEach(instance ->
                {
                    try
                    {
                        CoreInstance functionType = processorSupport.function_getFunctionType(instance);
                        if (functionType == null)
                        {
                            StringBuilder message = new StringBuilder("Instance: ").append(instance);
                            SourceInformation sourceInfo = instance.getSourceInformation();
                            if (sourceInfo != null)
                            {
                                sourceInfo.appendMessage(message.append(" (")).append(')');
                            }
                            message.append("; classifier: ");
                            PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                            message.append("; problem: null function type");
                            errorMessages.add(message.toString());
                        }
                    }
                    catch (Exception e)
                    {
                        StringBuilder message = new StringBuilder("Instance: ");
                        message.append(instance);
                        SourceInformation sourceInfo = instance.getSourceInformation();
                        if (sourceInfo != null)
                        {
                            sourceInfo.appendMessage(message.append(" (")).append(')');
                        }
                        message.append("; classifier: ");
                        PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                        message.append("; problem: exception occurred while computing function type; exception: ").append(e);
                        errorMessages.add(message.toString());
                    }
                });

        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            message.append("There are ").append(errorCount).append(" function type computation errors:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    @Test
    @Ignore
    public void testFunctionTypesHaveSourceInformation()
    {
        testFunctionTypesHaveSourceInformation(true);
    }

    protected void testFunctionTypesHaveSourceInformation(boolean findPaths)
    {
        CoreInstance functionTypeClass = runtime.getCoreInstance(M3Paths.FunctionType);
        CompiledStateIntegrityTestTools.testHasSourceInformation(
                GraphNodeIterable.fromModelRepository(repository).select(n -> functionTypeClass == n.getClassifier()),
                "FunctionType",
                (sb, ft) -> org.finos.legend.pure.m3.navigation.function.FunctionType.print(sb, ft, true, processorSupport),
                findPaths,
                processorSupport);
    }

    @Test
    public void testFunctionTypesDoNotShareFunctions()
    {
        MutableMap<CoreInstance, MutableList<CoreInstance>> functionTypesByFunction = Maps.mutable.empty();
        CoreInstance functionTypeClass = runtime.getCoreInstance(M3Paths.FunctionType);
        GraphNodeIterable.fromModelRepository(repository)
                .select(n -> functionTypeClass == n.getClassifier())
                .forEach(ft -> ft.getValueForMetaPropertyToMany(M3Properties.function).forEach(f -> functionTypesByFunction.getIfAbsentPut(f, Lists.mutable::empty).add(ft)));

        MutableList<String> errorMessages = Lists.mutable.empty();
        functionTypesByFunction.forEachKeyValue((function, functionTypes) ->
        {
            if (functionTypes.size() > 1)
            {
                StringBuilder builder = new StringBuilder();
                if (function.getValueForMetaPropertyToOne(M3Properties._package) != null)
                {
                    PackageableElement.writeUserPathForPackageableElement(builder, function);
                }
                else
                {
                    // TODO improve this for other types of functions
                    builder.append(function);
                }
                SourceInformation functionSourceInfo = function.getSourceInformation();
                if (functionSourceInfo != null)
                {
                    functionSourceInfo.appendMessage(builder.append(" (")).append(')');
                }
                builder.append(" has ").append(functionTypes.size()).append(" function types associated with it: ");
                functionTypes.forEachWithIndex((ft, i) ->
                {
                    if (i > 0)
                    {
                        builder.append(", ");
                    }
                    FunctionType.print(builder, ft, true, processorSupport);
                    SourceInformation sourceInfo = ft.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        sourceInfo.appendMessage(builder.append(" (")).append(')');
                    }
                });
                errorMessages.add(builder.toString());
            }
        });
        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            message.append("There are ").append(errorCount).append(" function type function conflicts:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testSpecializations()
    {
        CoreInstance typeClass = runtime.getCoreInstance(M3Paths.Type);
        CoreInstance topType = processorSupport.type_TopType();

        MutableMap<CoreInstance, MutableSet<CoreInstance>> expected = Maps.mutable.empty();
        MutableMap<CoreInstance, MutableSet<CoreInstance>> actual = Maps.mutable.empty();

        GraphNodeIterable.fromModelRepository(repository)
                .select(instance -> Instance.instanceOf(instance, typeClass, processorSupport))
                .forEach(instance ->
                {
                    instance.getValueForMetaPropertyToMany(M3Properties.generalizations).forEach(generalization ->
                    {
                        CoreInstance general = Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, M3Properties.rawType, processorSupport);
                        if (general != topType)
                        {
                            expected.getIfAbsentPut(general, Sets.mutable::empty).add(generalization);
                        }
                    });

                    ListIterable<? extends CoreInstance> specializations = instance.getValueForMetaPropertyToMany(M3Properties.specializations);
                    if (specializations.notEmpty())
                    {
                        actual.put(instance, Sets.mutable.withAll(specializations));
                    }
                });

        Verify.assertMapsEqual(expected, actual);
    }

    @Test
    public void testSourceSerialization()
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            stream.reset();
            BinaryModelSourceSerializer.serialize(BinaryWriters.newBinaryWriter(stream), source, runtime);
            BinaryModelSourceDeserializer.deserialize(BinaryReaders.newBinaryReader(stream.toByteArray()), ExternalReferenceSerializerLibrary.newLibrary(runtime));
        });
    }

    @Test
    public void testReferenceUsages()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        for (CoreInstance node : GraphNodeIterable.fromModelRepository(repository))
        {
            ListIterable<? extends CoreInstance> referenceUsages = node.getValueForMetaPropertyToMany(M3Properties.referenceUsages);
            for (CoreInstance referenceUsage : referenceUsages)
            {
                CoreInstance owner = referenceUsage.getValueForMetaPropertyToOne(M3Properties.owner);
                String propertyName = PrimitiveUtilities.getStringValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.propertyName), null);
                Number offset = PrimitiveUtilities.getIntegerValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.offset), (Integer) null);
                if ((owner == null) || (propertyName == null) || (offset == null))
                {
                    MutableList<String> details = Lists.mutable.withInitialCapacity(3);
                    if (owner == null)
                    {
                        details.add("missing owner");
                    }
                    if (propertyName == null)
                    {
                        details.add("missing propertyName");
                    }
                    if (offset == null)
                    {
                        details.add("missing offset");
                    }
                    StringBuilder message = buildInvalidReferenceUsageMessage(node, referenceUsage);
                    details.appendString(message, ", ");
                    errorMessages.add(message.toString());
                }
                else if (!owner.isValueDefinedForKey(propertyName))
                {
                    errorMessages.add(buildInvalidReferenceUsageMessage(node, referenceUsage).append("property not defined for owner").toString());
                }
                else
                {
                    ListIterable<? extends CoreInstance> ownerPropertyValues = owner.getValueForMetaPropertyToMany(propertyName);
                    int index = offset.intValue();
                    if (ownerPropertyValues.size() <= index)
                    {
                        errorMessages.add(buildInvalidReferenceUsageMessage(node, referenceUsage).append("invalid offset for property on owner (").append(ownerPropertyValues.size()).append(" values)").toString());
                    }
                    else
                    {
                        CoreInstance rawValue = ownerPropertyValues.get(index);
                        if (rawValue != node)
                        {
                            CoreInstance resolvedValue = resolveValue(rawValue);
                            if (resolvedValue != node)
                            {
                                StringBuilder message = buildInvalidReferenceUsageMessage(node, referenceUsage);
                                message.append("property value on owner does not match the node that holds the ReferenceUsage: ").append(resolvedValue);
                                SourceInformation sourceInfo = resolvedValue.getSourceInformation();
                                if (sourceInfo != null)
                                {
                                    sourceInfo.appendMessage(message.append(" (")).append(')');
                                }
                                errorMessages.add(message.toString());
                            }
                        }
                    }
                }
            }
        }
        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            message.append("There are ").append(errorCount).append(" ReferenceUsage issues:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    private StringBuilder buildInvalidReferenceUsageMessage(CoreInstance node, CoreInstance referenceUsage)
    {
        StringBuilder message = new StringBuilder("Node: ");
        message.append(node);
        SourceInformation nodeSourceInfo = node.getSourceInformation();
        if (nodeSourceInfo != null)
        {
            nodeSourceInfo.appendMessage(message.append(" (")).append(')');
        }

        message.append("; ReferenceUsage: ");
        ReferenceUsage.writeReferenceUsage(message, referenceUsage, true, true);
        SourceInformation refUsageSourceInfo = referenceUsage.getSourceInformation();
        if (refUsageSourceInfo != null)
        {
            refUsageSourceInfo.appendMessage(message.append(" (")).append(')');
        }
        message.append("; Detail: ");
        return message;
    }

    protected MutableList<CoreInstance> selectNodes(Predicate<? super CoreInstance> predicate)
    {
        return selectNodes(predicate, Lists.mutable.empty());
    }

    protected <T extends Collection<CoreInstance>> T selectNodes(Predicate<? super CoreInstance> predicate, T targetCollection)
    {
        return GraphNodeIterable.fromModelRepository(repository).select(predicate, targetCollection);
    }

    protected CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance instance, String property)
    {
        CoreInstance value = instance.getValueForMetaPropertyToOne(property);
        return (value == null) ? null : resolveValue(value);
    }

    protected ListIterable<? extends CoreInstance> getValueForMetaPropertyToManyResolved(CoreInstance instance, String property)
    {
        ListIterable<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(property);
        MutableList<CoreInstance> resolvedValues = null;
        for (int i = 0; i < values.size(); i++)
        {
            CoreInstance value = values.get(i);
            CoreInstance resolvedValue = resolveValue(value);
            if (resolvedValue != value)
            {
                if (resolvedValues == null)
                {
                    resolvedValues = Lists.mutable.withAll(values);
                }
                resolvedValues.set(i, resolvedValue);
            }
        }
        return (resolvedValues == null) ? values : resolvedValues;
    }

    private CoreInstance resolveValue(CoreInstance value)
    {
        CoreInstance classifier = value.getClassifier();
        if (classifier == importStubClass)
        {
            return resolveImportStub((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub) value);
        }
        if (classifier == propertyStubClass)
        {
            return resolvePropertyStub(value);
        }
        if (classifier == enumStubClass)
        {
            return resolveEnumStub(value);
        }
        return value;
    }

    private CoreInstance resolveImportStub(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub importStub)
    {
        CoreInstance resolved = importStub.getValueForMetaPropertyToOne(M3Properties.resolvedNode);
        return (resolved == null) ? ImportStub.resolveImportStub(importStub, repository, processorSupport) : resolved;
    }

    private CoreInstance resolvePropertyStub(CoreInstance propertyStub)
    {
        CoreInstance resolved = propertyStub.getValueForMetaPropertyToOne(M3Properties.resolvedProperty);
        if (resolved != null)
        {
            return resolved;
        }

        CoreInstance cls = getValueForMetaPropertyToOneResolved(propertyStub, M3Properties.owner);
        String propertyName = PrimitiveUtilities.getStringValue(propertyStub.getValueForMetaPropertyToOne(M3Properties.propertyName));
        // TODO it would be better if we didn't do this
        return _Class.computePropertiesByName(cls, _Class.SIMPLE_PROPERTIES_PROPERTIES, processorSupport).get(propertyName);
    }

    private CoreInstance resolveEnumStub(CoreInstance enumStub)
    {
        CoreInstance resolved = enumStub.getValueForMetaPropertyToOne(M3Properties.resolvedEnum);
        if (resolved != null)
        {
            return resolved;
        }

        CoreInstance enumeration = getValueForMetaPropertyToOneResolved(enumStub, M3Properties.enumeration);
        String enumName = PrimitiveUtilities.getStringValue(enumStub.getValueForMetaPropertyToOne(M3Properties.enumName));
        return enumeration.getValueInValueForMetaPropertyToMany(M3Properties.values, enumName);
    }
}
