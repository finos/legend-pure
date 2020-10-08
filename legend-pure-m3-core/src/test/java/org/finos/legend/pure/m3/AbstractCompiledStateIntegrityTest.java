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

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
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
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.AfterClass;
import org.junit.Assert;
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

    protected static void initialize(MutableCodeStorage codeStorage, RichIterable<? extends Parser> parsers, RichIterable<? extends InlineDSL> inlineDSLs)
    {
        runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(new PrintPureRuntimeStatus(System.out)).setTransactionalByDefault(false)
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
        MutableSet<String> topLevelNames = repository.getTopLevels().collect(CoreInstance.GET_NAME).toSet();
        MutableSet<String> expectedTopLevelNames = _Package.SPECIAL_TYPES.toSet().with(M3Paths.Root);

        Assert.assertEquals(repository.getTopLevels().size(), topLevelNames.size());
        Verify.assertSetsEqual(expectedTopLevelNames, topLevelNames);

        for (CoreInstance topLevel : repository.getTopLevels())
        {
            SourceInformation sourceInfo = topLevel.getSourceInformation();
            Assert.assertNotNull("Null source information for " + topLevel.getName(), sourceInfo);

            Assert.assertEquals("Source information for " + topLevel.getName() + " not in m3.pure", "/platform/pure/m3.pure", sourceInfo.getSourceId());
        }
    }

    @Test
    public void testNullClassifiers()
    {
        MutableList<CoreInstance> nullClassifiers = selectNodes(Predicates.attributeIsNull(CoreInstance.GET_CLASSIFIER));
        if (nullClassifiers.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following ");
            message.append(nullClassifiers.size());
            message.append(" elements have null classifiers:");
            for (CoreInstance instance : nullClassifiers)
            {
                message.append("\n\t");
                message.append(instance.getName());
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    message.append(": ");
                    sourceInfo.writeMessage(message);
                }
            }
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
        final CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(functionTypeClass);
        Assert.assertNotNull(importGroupClass);
        Assert.assertNotNull(packageClass);
        Assert.assertNotNull(packageableElementClass);

        final ImmutableSet<CoreInstance> exceptionClasses = Sets.immutable.with(importGroupClass, packageClass, functionTypeClass);

        MutableList<CoreInstance> noSourceInfo = selectNodes(new Predicate<CoreInstance>()
        {
            @Override
            public boolean accept(CoreInstance instance)
            {
                if (instance.getSourceInformation() != null)
                {
                    return false;
                }

                CoreInstance classifier = instance.getClassifier();
                return Type.subTypeOf(classifier, packageableElementClass, processorSupport) && !exceptionClasses.contains(classifier);
            }
        });
        if (noSourceInfo.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following packageable elements have no source information:");
            for (CoreInstance instance : noSourceInfo)
            {
                message.append("\n\t");
                PackageableElement.writeUserPathForPackageableElement(message, instance);
                message.append(" (");
                PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                message.append(')');
            }
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testAllSourceNewInstancesArePackageableElements()
    {
        MutableListMultimap<Source, CoreInstance> nonPackageableElementsBySource = Multimaps.mutable.list.empty();
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            for (CoreInstance instance : source.getNewInstances())
            {
                if (!Instance.instanceOf(instance, M3Paths.PackageableElement, processorSupport))
                {
                    nonPackageableElementsBySource.put(source, instance);
                }
            }
        }
        if (nonPackageableElementsBySource.notEmpty())
        {
            StringBuilder message = new StringBuilder();
            for (Pair<Source, RichIterable<CoreInstance>> pair : nonPackageableElementsBySource.keyMultiValuePairsView())
            {
                message.append(pair.getOne().getId());
                pair.getTwo().appendString(message, ":\n", "\n\t", "\n");
            }
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testAllPackageChildrenNonNull()
    {
        MutableSet<String> badPackages = Sets.mutable.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            if (pkg._children().contains(null))
            {
                badPackages.add(PackageableElement.getUserPathForPackageableElement(pkg));
            }
        }
        if (badPackages.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following packages have null children: ");
            badPackages.toSortedList().appendString(message, ", ");
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testAllPackageChildrenArePackageable()
    {
        CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(packageableElementClass);

        MutableListMultimap<CoreInstance, CoreInstance> nonPackageableElements = Multimaps.mutable.list.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance child : pkg._children())
            {
                if (!Instance.instanceOf(child, packageableElementClass, processorSupport))
                {
                    nonPackageableElements.put(pkg, child);
                }
            }
        }
        if (nonPackageableElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children that are not instances of ");
            builder.append(M3Paths.PackageableElement);
            for (Pair<CoreInstance, RichIterable<CoreInstance>> pair : nonPackageableElements.keyMultiValuePairsView())
            {
                RichIterable<CoreInstance> children = pair.getTwo();
                if (children.notEmpty())
                {
                    builder.append("\n\t");
                    PackageableElement.writeUserPathForPackageableElement(builder, pair.getOne());
                    for (CoreInstance child : pair.getTwo())
                    {
                        builder.append("\n\t\t");
                        builder.append(child);
                    }
                }
            }
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllPackageChildrenHaveMatchingPackage()
    {
        CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(packageableElementClass);

        MutableListMultimap<CoreInstance, CoreInstance> badElements = Multimaps.mutable.list.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance child : pkg._children())
            {
                CoreInstance childPackage = child.getValueForMetaPropertyToOne(M3Properties._package);
                if (pkg != childPackage)
                {
                    badElements.put(pkg, child);
                }
            }
        }
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with a different package");
            for (Pair<CoreInstance, RichIterable<CoreInstance>> pair : badElements.keyMultiValuePairsView())
            {
                RichIterable<CoreInstance> children = pair.getTwo();
                if (children.notEmpty())
                {
                    builder.append("\n\t");
                    PackageableElement.writeUserPathForPackageableElement(builder, pair.getOne());
                    for (CoreInstance child : pair.getTwo())
                    {
                        builder.append("\n\t\t");
                        builder.append(child);
                        builder.append(": ");
                        CoreInstance childPackage = child.getValueForMetaPropertyToOne(M3Properties._package);
                        if (childPackage == null)
                        {
                            builder.append(childPackage);
                        }
                        else
                        {
                            PackageableElement.writeUserPathForPackageableElement(builder, childPackage);
                        }
                    }
                }
            }
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllElementsWithPackagesArePackageChildren()
    {
        MutableList<CoreInstance> badElements = Lists.mutable.empty();
        for (CoreInstance instance : GraphNodeIterable.fromModelRepository(repository))
        {
            CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties._package, processorSupport);
            if (pkg != null)
            {
                if (!pkg.getValueForMetaPropertyToMany(M3Properties.children).contains(instance))
                {
                    badElements.add(instance);
                }
            }
        }
        if (badElements.notEmpty())
        {
            Assert.fail(badElements.collect(PackageableElement.GET_USER_PATH).sortThis().makeString("The following elements are not children of their packages:\n\t", "\n\t", ""));
        }
    }

    @Test
    public void testAllPackageChildrenHaveNames()
    {
        MutableListMultimap<CoreInstance, CoreInstance> badElements = Multimaps.mutable.list.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance child : pkg._children())
            {
                if (Instance.getValueForMetaPropertyToOneResolved(child, M3Properties.name, processorSupport) == null)
                {
                    badElements.put(pkg, child);
                }
            }
        }
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with no name:");
            for (Pair<CoreInstance, RichIterable<CoreInstance>> pair : badElements.keyMultiValuePairsView())
            {
                RichIterable<CoreInstance> children = pair.getTwo();
                if (children.notEmpty())
                {
                    builder.append("\n\t");
                    PackageableElement.writeUserPathForPackageableElement(builder, pair.getOne());
                    children.appendString(builder, "\n\t\t", "\n\t\t", "");
                }
            }
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllPackageChildrenHaveNamesMatchingNameProperty()
    {
        MutableListMultimap<CoreInstance, Pair<String, String>> badElements = Multimaps.mutable.list.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance child : pkg._children())
            {
                CoreInstance nameInstance = child.getValueForMetaPropertyToOne(M3Properties.name);
                if ((nameInstance == null) || !child.getName().equals(nameInstance.getName()))
                {
                    badElements.put(pkg, Tuples.pair(child.getName(), (nameInstance == null) ? null : nameInstance.getName()));
                }
            }
        }
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with instance names that do not match the name property:");
            for (Pair<CoreInstance, RichIterable<Pair<String, String>>> pair : badElements.keyMultiValuePairsView())
            {
                RichIterable<Pair<String, String>> children = pair.getTwo();
                if (children.notEmpty())
                {
                    builder.append("\n\t");
                    PackageableElement.writeUserPathForPackageableElement(builder, pair.getOne());
                    for (Pair<String, String> nameName : children)
                    {
                        builder.append("\n\t\t'");
                        builder.append(nameName.getOne());
                        builder.append("' / '");
                        builder.append(nameName.getTwo());
                        builder.append('\'');
                    }
                }
            }
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testNonAnonymousPackageChildren()
    {
        MutableListMultimap<CoreInstance, CoreInstance> badElements = Multimaps.mutable.list.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance child : pkg._children())
            {
                if (ModelRepository.isAnonymousInstanceName(child.getName()))
                {
                    badElements.put(pkg, child);
                }
            }
        }
        if (badElements.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have anonymous children:");
            for (Pair<CoreInstance, RichIterable<CoreInstance>> pair : badElements.keyMultiValuePairsView())
            {
                RichIterable<CoreInstance> children = pair.getTwo();
                if (children.notEmpty())
                {
                    builder.append("\n\t");
                    PackageableElement.writeUserPathForPackageableElement(builder, pair.getOne());
                    children.appendString(builder, "\n\t\t", "\n\t\t", "");
                }
            }
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testPackageHasChildrenWithDuplicateNames()
    {
        MutableListMultimap<CoreInstance, ObjectIntPair<String>> badPackageNames = Multimaps.mutable.list.empty();
        for (Package pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            Multimap<String, ? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> childrenByName = pkg._children().groupBy(CoreInstance.GET_NAME);
            for (Pair<String, ? extends RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>> pair : childrenByName.keyMultiValuePairsView())
            {
                int size = pair.getTwo().size();
                if (size > 1)
                {
                    badPackageNames.put(pkg, PrimitiveTuples.pair(pair.getOne(), size));
                }
            }
        }
        if (badPackageNames.notEmpty())
        {
            StringBuilder builder = new StringBuilder("The following packages have children with duplicate names:");
            for (Pair<CoreInstance, RichIterable<ObjectIntPair<String>>> pair : badPackageNames.keyMultiValuePairsView())
            {
                RichIterable<ObjectIntPair<String>> children = pair.getTwo();
                if (children.notEmpty())
                {
                    builder.append("\n\t");
                    PackageableElement.writeUserPathForPackageableElement(builder, pair.getOne());
                    builder.append(" (");
                    builder.append(children.size());
                    builder.append(')');
                    for (ObjectIntPair<String> nameCount : pair.getTwo().toSortedList())
                    {
                        builder.append("\n\t\t'");
                        builder.append(nameCount.getOne());
                        builder.append("': ");
                        builder.append(nameCount.getTwo());
                    }
                }
            }
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testAllImportGroupsHaveSourceInfo()
    {
        CoreInstance imports = processorSupport.package_getByUserPath("system::imports");
        Assert.assertNotNull("Could not find system::imports", imports);
        ListIterable<? extends CoreInstance> importGroupsWithoutSourceInfo = imports.getValueForMetaPropertyToMany(M3Properties.children).select(Predicates.attributeIsNull(CoreInstance.GET_SOURCE_INFO));
        if (importGroupsWithoutSourceInfo.notEmpty())
        {
            Assert.fail(importGroupsWithoutSourceInfo.collect(PackageableElement.GET_USER_PATH, FastList.<String>newList(importGroupsWithoutSourceInfo.size())).sortThis().makeString("The following ImportGroups have no source information:\n\t", "\n\t", ""));
        }
    }

    @Test
    public void testModelElementsForAnnotations()
    {
        MutableSetMultimap<CoreInstance, CoreInstance> expectedModelElements = Multimaps.mutable.set.empty();
        MutableMap<CoreInstance, ListIterable<? extends CoreInstance>> actualModelElements = Maps.mutable.empty();

        CoreInstance stereotypeClass = runtime.getCoreInstance(M3Paths.Stereotype);
        CoreInstance tagClass = runtime.getCoreInstance(M3Paths.Tag);
        for (CoreInstance node : GraphNodeIterable.fromModelRepository(repository))
        {
            if ((node.getClassifier() == stereotypeClass) || (node.getClassifier() == tagClass))
            {
                ListIterable<? extends CoreInstance> modelElements = node.getValueForMetaPropertyToMany(M3Properties.modelElements);
                actualModelElements.put(node, modelElements);
            }
            for (CoreInstance stereotype : getValueForMetaPropertyToManyResolved(node, M3Properties.stereotypes))
            {
                expectedModelElements.put(stereotype, node);
            }
            for (CoreInstance taggedValue : getValueForMetaPropertyToManyResolved(node, M3Properties.taggedValues))
            {
                CoreInstance tag = getValueForMetaPropertyToOneResolved(taggedValue, M3Properties.tag);
                expectedModelElements.put(tag, node);
            }
        }

        // Check for annotations that aren't really annotations
        MutableList<CoreInstance> badAnnotations = Lists.mutable.empty();
        for (CoreInstance annotation : expectedModelElements.keysView())
        {
            if (!actualModelElements.containsKey(annotation))
            {
                badAnnotations.add(annotation);
            }
        }
        Verify.assertEmpty(badAnnotations);

        // Check for missing and extra elements
        MutableMap<CoreInstance, SetIterable<CoreInstance>> annotationsWithMissingModelElements = Maps.mutable.empty();
        MutableMap<CoreInstance, SetIterable<CoreInstance>> annotationsWithExtraModelElements = Maps.mutable.empty();
        for (CoreInstance annotation : actualModelElements.keysView())
        {
            MutableSet<CoreInstance> expected = expectedModelElements.get(annotation);
            MutableSet<CoreInstance> actual = (MutableSet<CoreInstance>) actualModelElements.get(annotation).toSet();
            if (!expected.equals(actual))
            {
                MutableSet<CoreInstance> missing = expected.difference(actual);
                if (missing.notEmpty())
                {
                    annotationsWithMissingModelElements.put(annotation, missing);
                }
                MutableSet<CoreInstance> extra = actual.difference(expected);
                if (extra.notEmpty())
                {
                    annotationsWithExtraModelElements.put(annotation, extra);
                }
            }
        }

        if (annotationsWithMissingModelElements.notEmpty() || annotationsWithExtraModelElements.notEmpty())
        {
            StringBuilder message = new StringBuilder();
            if (annotationsWithMissingModelElements.notEmpty())
            {
                message.append("The following annotations are missing model elements:");
                for (Pair<CoreInstance, SetIterable<CoreInstance>> pair : annotationsWithMissingModelElements.keyValuesView())
                {
                    CoreInstance annotation = pair.getOne();
                    message.append("\n\t");
                    message.append(annotation.getClassifier().getName());
                    message.append(": ");
                    PackageableElement.writeUserPathForPackageableElement(message, annotation.getValueForMetaPropertyToOne(M3Properties.profile));
                    message.append('.');
                    message.append(annotation.getName());
                    SourceInformation sourceInfo = annotation.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        message.append(" (source information: ");
                        sourceInfo.writeMessage(message);
                        message.append(")");
                    }
                    for (CoreInstance missing : pair.getTwo())
                    {
                        message.append("\n\t\t");
                        message.append(missing);
                        SourceInformation missingSourceInfo = missing.getSourceInformation();
                        if (missingSourceInfo != null)
                        {
                            message.append(" (source information: ");
                            missingSourceInfo.writeMessage(message);
                            message.append(")");
                        }
                    }
                }
            }
            if (annotationsWithExtraModelElements.notEmpty())
            {
                message.append("The following annotations have extra model elements:");
                for (Pair<CoreInstance, SetIterable<CoreInstance>> pair : annotationsWithExtraModelElements.keyValuesView())
                {
                    CoreInstance annotation = pair.getOne();
                    message.append("\n\t");
                    message.append(annotation.getClassifier().getName());
                    message.append(": ");
                    PackageableElement.writeUserPathForPackageableElement(message, annotation.getValueForMetaPropertyToOne(M3Properties.profile));
                    message.append('.');
                    message.append(annotation.getName());
                    SourceInformation sourceInfo = annotation.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        message.append(" (source information: ");
                        message.append(sourceInfo.getMessage());
                        message.append(")");
                    }
                    for (CoreInstance extra : pair.getTwo())
                    {
                        message.append("\n\t\t");
                        message.append(extra);
                        SourceInformation missingSourceInfo = extra.getSourceInformation();
                        if (missingSourceInfo != null)
                        {
                            message.append(" (source information: ");
                            missingSourceInfo.writeMessage(message);
                            message.append(")");
                        }
                    }
                }
            }
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testPropertyClassifierGenericTypes()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        CoreInstance classClass = processorSupport.package_getByUserPath(M3Paths.Class);
        for (CoreInstance pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance node : pkg.getValueForMetaPropertyToMany(M3Properties.children))
            {
                if (Instance.instanceOf(node, classClass, processorSupport))
                {
                    ListIterable<? extends CoreInstance> typeParams = node.getValueForMetaPropertyToMany(M3Properties.typeParameters);
                    MutableList<CoreInstance> expectedSourceTypeArgs = null;
                    if (typeParams.notEmpty())
                    {
                        expectedSourceTypeArgs = FastList.newList(typeParams.size());
                        for (CoreInstance typeParam : typeParams)
                        {
                            CoreInstance typeArg = processorSupport.newGenericType(null, typeParam, false);
                            Instance.addValueToProperty(typeArg, M3Properties.typeParameter, typeParam, processorSupport);
                            expectedSourceTypeArgs.add(typeArg);
                        }
                    }

                    ListIterable<? extends CoreInstance> multParams = node.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);
                    MutableList<CoreInstance> expectedSourceMultArgs = null;
                    if (multParams.notEmpty())
                    {
                        expectedSourceMultArgs = FastList.newList(multParams.size());
                        for (CoreInstance multParamInstanceValue : multParams)
                        {
                            expectedSourceMultArgs.add(Multiplicity.newMultiplicity(PrimitiveUtilities.getStringValue(multParamInstanceValue.getValueForMetaPropertyToOne(M3Properties.values)), processorSupport));
                        }
                    }

                    SourceInformation classSourceInfo = node.getSourceInformation();

                    for (Pair<String, ? extends RichIterable<? extends CoreInstance>> pair : Lists.immutable.with(Tuples.pair("property", node.getValueForMetaPropertyToMany(M3Properties.properties)), Tuples.pair("property from association", node.getValueForMetaPropertyToMany(M3Properties.propertiesFromAssociations))))
                    {
                        String propertyType = pair.getOne();
                        for (CoreInstance property : pair.getTwo())
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
                                    message.append(" (");
                                    classSourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; ").append(propertyType).append(": ");
                                message.append(property.getName());
                                SourceInformation propertySourceInfo = property.getSourceInformation();
                                if (propertySourceInfo != null)
                                {
                                    message.append(" (");
                                    propertySourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; property classifierGenericType: ");
                                GenericType.print(message, classifierGenericType, true, processorSupport);
                                message.append("; expected 2 type arguments (got ").append(typeArgs.size()).append(") and 1 multiplicity argument (got ").append(multArgs.size()).append(")");
                                errorMessages.add(message.toString());
                                continue;
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
                                    message.append(" (");
                                    classSourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; ").append(propertyType).append(": ");
                                message.append(property.getName());
                                SourceInformation propertySourceInfo = property.getSourceInformation();
                                if (propertySourceInfo != null)
                                {
                                    message.append(" (");
                                    propertySourceInfo.writeMessage(message);
                                    message.append(')');
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
                                    message.append(" (");
                                    sourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; ").append(propertyType).append(": ");
                                message.append(property.getName());
                                SourceInformation propertySourceInfo = property.getSourceInformation();
                                if (propertySourceInfo != null)
                                {
                                    message.append(" (");
                                    propertySourceInfo.writeMessage(message);
                                    message.append(')');
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
                                    message.append(" (");
                                    sourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; ").append(propertyType).append(": ");
                                message.append(property.getName());
                                SourceInformation propertySourceInfo = property.getSourceInformation();
                                if (propertySourceInfo != null)
                                {
                                    message.append(" (");
                                    propertySourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; property classifierGenericType: ");
                                GenericType.print(message, classifierGenericType, true, processorSupport);
                                message.append("; property multiplicity: ");
                                Multiplicity.print(message, expectedMultiplicity, false);
                                message.append("; multiplicity argument of classifierGenericType should be the same as property multiplicity");
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
            message.append("There are ");
            message.append(errorCount);
            message.append(" property classifierGenericType errors:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testQualifiedPropertyNames()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        CoreInstance propertyOwnerClass = processorSupport.package_getByUserPath(M3Paths.PropertyOwner);
        for (CoreInstance pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            for (CoreInstance node : pkg.getValueForMetaPropertyToMany(M3Properties.children))
            {
                if (Instance.instanceOf(node, propertyOwnerClass, processorSupport))
                {
                    ListIterable<CoreInstance> qualifiedProperties = (ListIterable<CoreInstance>) node.getValueForMetaPropertyToMany(M3Properties.qualifiedProperties);
                    if (qualifiedProperties.notEmpty())
                    {
                        ListMultimap<String, CoreInstance> qualifiedPropertiesByName = qualifiedProperties.groupBy(CoreInstance.GET_NAME);
                        for (Pair<String, RichIterable<CoreInstance>> pair : qualifiedPropertiesByName.keyMultiValuePairsView())
                        {
                            String name = pair.getOne();
                            RichIterable<CoreInstance> qualifiedPropertiesForName = pair.getTwo();
                            if (qualifiedPropertiesForName.size() > 1)
                            {
                                StringBuilder message = new StringBuilder("Property owner: ");
                                PackageableElement.writeUserPathForPackageableElement(message, node);
                                SourceInformation sourceInfo = node.getSourceInformation();
                                if (sourceInfo != null)
                                {
                                    message.append(" (");
                                    sourceInfo.writeMessage(message);
                                    message.append(')');
                                }
                                message.append("; multiple qualified properties with name '");
                                message.append(name);
                                message.append("' (");
                                message.append(qualifiedPropertiesForName.size());
                                message.append(')');
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
            message.append("There are ");
            message.append(errorCount);
            message.append(" qualified property name errors:\n\t");
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

        MutableSetMultimap<CoreInstance, CoreInstance> expected = Multimaps.mutable.set.empty();
        MutableMap<CoreInstance, MutableSet<? extends CoreInstance>> actual = Maps.mutable.empty();

        for (CoreInstance instance : GraphNodeIterable.fromModelRepository(repository))
        {
            if (Instance.instanceOf(instance, functionClass, processorSupport))
            {
                ListIterable<? extends CoreInstance> applications = instance.getValueForMetaPropertyToMany(M3Properties.applications);
                if (applications.notEmpty())
                {
                    actual.put(instance, applications.toSet());
                }
            }
            else if (Instance.instanceOf(instance, functionExpressionClass, processorSupport))
            {
                CoreInstance function = instance.getValueForMetaPropertyToOne(M3Properties.func);
                expected.put(function, instance);
            }
        }

        Verify.assertMapsEqual(expected.toMap(), actual);
    }

    @Test
    public void testFunctionTypes()
    {
        CoreInstance functionClass = runtime.getCoreInstance(M3Paths.Function);

        MutableList<String> errorMessages = Lists.mutable.empty();

        for (CoreInstance instance : GraphNodeIterable.fromModelRepository(repository))
        {
            if (Instance.instanceOf(instance, functionClass, processorSupport))
            {
                try
                {
                    CoreInstance functionType = processorSupport.function_getFunctionType(instance);
                    if (functionType == null)
                    {
                        StringBuilder message = new StringBuilder("Instance: ");
                        message.append(instance);
                        SourceInformation sourceInfo = instance.getSourceInformation();
                        if (sourceInfo != null)
                        {
                            message.append(" (");
                            sourceInfo.writeMessage(message);
                            message.append(")");
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
                        message.append(" (");
                        sourceInfo.writeMessage(message);
                        message.append(")");
                    }
                    message.append("; classifier: ");
                    PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                    message.append("; problem: exception occurred while computing function type; exception: ");
                    message.append(e);
                    errorMessages.add(message.toString());
                }
            }
        }

        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            message.append("There are ");
            message.append(errorCount);
            message.append(" function type computation errors:\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testSpecializations()
    {
        CoreInstance typeClass = runtime.getCoreInstance(M3Paths.Type);
        CoreInstance topType = processorSupport.type_TopType();

        MutableSetMultimap<CoreInstance, CoreInstance> expected = Multimaps.mutable.set.empty();
        MutableMap<CoreInstance, MutableSet<? extends CoreInstance>> actual = Maps.mutable.empty();

        for (CoreInstance instance : GraphNodeIterable.fromModelRepository(repository))
        {
            if (Instance.instanceOf(instance, typeClass, processorSupport))
            {
                for (CoreInstance generalization : instance.getValueForMetaPropertyToMany(M3Properties.generalizations))
                {
                    CoreInstance general = Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, M3Properties.rawType, processorSupport);
                    if (general != topType)
                    {
                        expected.put(general, generalization);
                    }
                }

                ListIterable<? extends CoreInstance> specializations = instance.getValueForMetaPropertyToMany(M3Properties.specializations);
                if (specializations.notEmpty())
                {
                    actual.put(instance, specializations.toSet());
                }
            }
        }

        Verify.assertMapsEqual(expected.toMap(), actual);
    }

    @Test
    public void testSourceSerialization()
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        for (Source source : runtime.getSourceRegistry().getSources())
        {
            stream.reset();
            BinaryModelSourceSerializer.serialize(BinaryWriters.newBinaryWriter(stream), source, runtime);
            BinaryModelSourceDeserializer.deserialize(BinaryReaders.newBinaryReader(stream.toByteArray()), ExternalReferenceSerializerLibrary.newLibrary(runtime));
        }
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
                Number offset = PrimitiveUtilities.getIntegerValue(referenceUsage.getValueForMetaPropertyToOne(M3Properties.offset), (Integer)null);
                if ((owner == null) || (propertyName == null) || (offset == null))
                {
                    MutableList<String> details = FastList.newList(3);
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
                                message.append("property value on owner does not match the node that holds the ReferenceUsage: ");
                                message.append(resolvedValue);
                                SourceInformation sourceInfo = resolvedValue.getSourceInformation();
                                if (sourceInfo != null)
                                {
                                    message.append(" (");
                                    sourceInfo.writeMessage(message);
                                    message.append(")");
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
            message.append("There are ");
            message.append(errorCount);
            message.append(" ReferenceUsage issues:\n\t");
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
            message.append(" (");
            nodeSourceInfo.writeMessage(message);
            message.append(')');
        }

        message.append("; ReferenceUsage: ");
        ReferenceUsage.writeReferenceUsage(message, referenceUsage, true, true);
        SourceInformation refUsageSourceInfo = referenceUsage.getSourceInformation();
        if (refUsageSourceInfo != null)
        {
            message.append(" (");
            refUsageSourceInfo.writeMessage(message);
            message.append(')');
        }
        message.append("; Detail: ");
        return message;
    }

    protected MutableList<CoreInstance> selectNodes(Predicate<? super CoreInstance> predicate)
    {
        return selectNodes(predicate, Lists.mutable.<CoreInstance>empty());
    }

    protected <T extends Collection<CoreInstance>> T selectNodes(Predicate<? super CoreInstance> predicate, T targetCollection)
    {
        return Iterate.select(GraphNodeIterable.fromModelRepository(repository), predicate, targetCollection);
    }

    protected CoreInstance getValueForMetaPropertyToOneResolved(CoreInstance instance, String property)
    {
        CoreInstance value = instance.getValueForMetaPropertyToOne(property);
        return (value == null) ? null : resolveValue(value);
    }

    protected ListIterable<CoreInstance> getValueForMetaPropertyToManyResolved(CoreInstance instance, String property)
    {
        ListIterable<CoreInstance> values = (ListIterable<CoreInstance>) instance.getValueForMetaPropertyToMany(property);
        MutableList<CoreInstance> resolvedValues = null;
        for (int i = 0; i < values.size(); i++)
        {
            CoreInstance value = values.get(i);
            CoreInstance resolvedValue = resolveValue(value);
            if (resolvedValue != value)
            {
                if (resolvedValues == null)
                {
                    resolvedValues = values.toList();
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
            return resolveImportStub((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub)value);
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
