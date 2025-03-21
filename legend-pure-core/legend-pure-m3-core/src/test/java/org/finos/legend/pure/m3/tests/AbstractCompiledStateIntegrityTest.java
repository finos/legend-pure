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

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.eclipse.collections.api.LazyIterable;
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
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ModelElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.Referenceable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.function.FunctionType;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.graph.ResolvedGraphPath;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Lexer;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.runtime.GraphLoader;
import org.finos.legend.pure.m3.serialization.runtime.PrintPureRuntimeStatus;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelSourceDeserializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelSourceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJar;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializerLibrary;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m3.tools.PackageableElementIterable;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m4.tools.GraphWalkFilterResult;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Objects;

public abstract class AbstractCompiledStateIntegrityTest
{
    protected static PureRuntime runtime;
    protected static ModelRepository repository;
    protected static Context context;
    protected static ProcessorSupport processorSupport;
    protected static CoreInstance importStubClass;
    protected static CoreInstance propertyStubClass;
    protected static CoreInstance enumStubClass;
    protected static ImmutableSet<String> baseRepositories;

    @Deprecated
    protected static void initialize(MutableRepositoryCodeStorage codeStorage, RichIterable<? extends Parser> parsers, RichIterable<? extends InlineDSL> inlineDSLs)
    {
        initialize(codeStorage);
    }

    protected static void initialize(String... repositories)
    {
        CodeRepositorySet repos = CodeRepositorySet.newBuilder()
                .withCodeRepositories(CodeRepositoryProviderHelper.findCodeRepositories())
                .subset(repositories)
                .build();
        initialize(new CompositeCodeStorage(new ClassLoaderCodeStorage(repos.getRepositories())));
        baseRepositories = Sets.immutable.with(repositories);
    }

    protected static void initialize(MutableRepositoryCodeStorage codeStorage)
    {
        MutableList<String> repoNames = codeStorage.getAllRepositories().collect(CodeRepository::getName, Lists.mutable.empty());
        System.out.println(repoNames.sortThis().makeString("Repositories: ", ", ", ""));

        runtime = new PureRuntimeBuilder(codeStorage)
                .withRuntimeStatus(new PrintPureRuntimeStatus(System.out))
                .setTransactionalByDefault(false)
                .build();
        repository = runtime.getModelRepository();
        context = runtime.getContext();
        processorSupport = runtime.getProcessorSupport();

        MutableList<PureRepositoryJar> repoJars = GraphLoader.findJars(repoNames, Thread.currentThread().getContextClassLoader(), null, false);
        if (repoJars.isEmpty())
        {
            runtime.initialize();
        }
        else
        {
            try
            {
                GraphLoader loader = new GraphLoader(repository, context, runtime.getIncrementalCompiler().getParserLibrary(), runtime.getIncrementalCompiler().getDslLibrary(), runtime.getSourceRegistry(), null, SimplePureRepositoryJarLibrary.newLibrary(repoJars));
                loader.loadAll();
                if (repoJars.size() < repoNames.size())
                {
                    MutableSet<String> found = repoJars.collect(j -> j.getMetadata().getRepositoryName(), Sets.mutable.ofInitialCapacity(repoJars.size()));
                    System.out.println(repoNames.reject(found::contains).sortThis().makeString("Missing caches for: ", ", ", ""));
                    runtime.compile();
                }
                System.out.println("Initialized runtime from caches");
            }
            catch (Exception e)
            {
                System.out.println("Failed to initialize runtime from caches, will try to initialize from source");
                e.printStackTrace();

                runtime = new PureRuntimeBuilder(codeStorage)
                        .withRuntimeStatus(new PrintPureRuntimeStatus(System.out))
                        .setTransactionalByDefault(false)
                        .buildAndInitialize();
                repository = runtime.getModelRepository();
                context = runtime.getContext();
                processorSupport = runtime.getProcessorSupport();
            }
        }

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

        Assert.assertEquals(expectedTopLevelNames.toSortedList(), topLevelNames.toSortedList());

        repository.getTopLevels().forEach(topLevel ->
        {
            SourceInformation sourceInfo = topLevel.getSourceInformation();
            Assert.assertNotNull("Null source information for " + topLevel.getName(), sourceInfo);
            Assert.assertEquals("Source information for " + topLevel.getName() + " not in m3.pure", "/platform/pure/grammar/m3.pure", sourceInfo.getSourceId());
        });
    }

    @Test
    public void testCorePackages()
    {
        // Root
        CoreInstance root = repository.getTopLevel(M3Paths.Root);
        Assert.assertNotNull(root);
        Assert.assertTrue(processorSupport.instance_instanceOf(root, M3Paths.Package));
        Assert.assertEquals(M3Paths.Root, root.getName());
        Assert.assertEquals(M3Paths.Root, PrimitiveUtilities.getStringValue(root.getValueForMetaPropertyToOne(M3Properties.name)));
        Assert.assertEquals(M3Paths.Root, PackageableElement.getUserPathForPackageableElement(root));
        Assert.assertNull(root.getValueForMetaPropertyToOne(M3Properties._package));

        CoreInstance meta = assertCorePackage("meta", root);
        CoreInstance metaPure = assertCorePackage("meta::pure", meta);
        CoreInstance metaPureMetamodel = assertCorePackage("meta::pure::metamodel", metaPure);
        CoreInstance metaPureFunctions = assertCorePackage("meta::pure::functions", metaPure);
        assertCorePackage("meta::pure::tools", metaPure);
        assertCorePackage("meta::pure::router", metaPure);
        assertCorePackage("meta::pure::test", metaPure);

        CoreInstance metaPureMetamodelType = assertCorePackage("meta::pure::metamodel::type", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::treepath", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::constraint", metaPureMetamodel);
        CoreInstance metaPureMetamodelFunction = assertCorePackage("meta::pure::metamodel::function", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::relation", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::relationship", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::valuespecification", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::multiplicity", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::extension", metaPureMetamodel);
        assertCorePackage("meta::pure::metamodel::import", metaPureMetamodel);

        assertCorePackage("meta::pure::functions::lang", metaPureFunctions);
        assertCorePackage("meta::pure::metamodel::type::generics", metaPureMetamodelType);
        assertCorePackage("meta::pure::metamodel::function::property", metaPureMetamodelFunction);

        CoreInstance system = assertCorePackage("system", root);
        assertCorePackage("system::imports", system);
    }

    private CoreInstance assertCorePackage(String path, CoreInstance parent)
    {
        int lastColon = path.lastIndexOf(':');
        String name = (lastColon == -1) ? path : path.substring(lastColon + 1);
        CoreInstance pkg = parent.getValueInValueForMetaPropertyToManyWithKey(M3Properties.children, M3Properties.name, name);
        Assert.assertNotNull(path, pkg);
        Assert.assertTrue(path, processorSupport.instance_instanceOf(pkg, M3Paths.Package));
        Assert.assertSame(path, parent, pkg.getValueForMetaPropertyToOne(M3Properties._package));
        Assert.assertEquals(path, name, pkg.getName());
        Assert.assertEquals(path, name, PrimitiveUtilities.getStringValue(pkg.getValueForMetaPropertyToOne(M3Properties.name)));
        Assert.assertEquals(path, PackageableElement.getUserPathForPackageableElement(pkg));
        Assert.assertNotEquals(path, Lists.fixedSize.empty(), pkg.getValueForMetaPropertyToMany(M3Properties.children));
        return pkg;
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
        CoreInstance packageClass = runtime.getCoreInstance(M3Paths.Package);
        CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
        Assert.assertNotNull(packageClass);
        Assert.assertNotNull(packageableElementClass);

        MutableList<CoreInstance> noSourceInfo = selectNodes(instance -> (instance.getSourceInformation() == null) &&
                (instance.getClassifier() != packageClass) &&
                Type.subTypeOf(instance.getClassifier(), packageableElementClass, processorSupport));
        if (noSourceInfo.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following packageable elements have no source information:");
            noSourceInfo.forEach(instance ->
            {
                PackageableElement.writeUserPathForPackageableElement(message.append("\n\t"), instance);
                PackageableElement.writeUserPathForPackageableElement(message.append(" ("), instance.getClassifier()).append(')');
            });
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testAllSourceInformationIsValid()
    {
        MutableList<CoreInstance> invalidSourceInfo = selectNodes(instance -> (instance.getSourceInformation() != null) && !instance.getSourceInformation().isValid());
        if (invalidSourceInfo.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following elements have invalid source information:");
            CoreInstance packageableElementClass = runtime.getCoreInstance(M3Paths.PackageableElement);
            invalidSourceInfo.forEach(instance ->
            {
                message.append("\n\t");
                if (Type.subTypeOf(instance.getClassifier(), packageableElementClass, processorSupport))
                {
                    PackageableElement.writeUserPathForPackageableElement(message, instance);
                }
                else
                {
                    message.append(instance);
                }
                PackageableElement.writeUserPathForPackageableElement(message.append(" ("), instance.getClassifier()).append("): ");
                instance.getSourceInformation().appendMessage(message);
            });
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testPackagedElementsHaveNonOverlappingSourceInfo()
    {
        MutableMap<String, MutableList<CoreInstance>> elementsBySource = Maps.mutable.empty();
        PackageableElementIterable.fromProcessorSupport(processorSupport)
                .select(e -> e.getSourceInformation() != null)
                .forEach(e -> elementsBySource.getIfAbsentPut(e.getSourceInformation().getSourceId(), Lists.mutable::empty).add(e));
        MutableList<Pair<CoreInstance, CoreInstance>> overlappingSourceInfo = Lists.mutable.empty();
        elementsBySource.forEachValue(elements ->
        {
            elements.sortThis((e1, e2) -> SourceInformation.compareByStartPosition(e1.getSourceInformation(), e2.getSourceInformation()));
            elements.forEachWithIndex((instance, i) ->
            {
                SourceInformation sourceInfo = instance.getSourceInformation();
                for (int j = i + 1; j < elements.size(); j++)
                {
                    CoreInstance other = elements.get(j);
                    if (sourceInfo.intersects(other.getSourceInformation()))
                    {
                        overlappingSourceInfo.add(Tuples.pair(instance, other));
                    }
                    else
                    {
                        // since these are ordered by start line, we have passed the last possible intersecting element
                        break;
                    }
                }
            });
        });
        if (overlappingSourceInfo.notEmpty())
        {
            StringBuilder message = new StringBuilder("The following pairs of packaged elements have overlapping source information:");
            overlappingSourceInfo.forEach(pair ->
            {
                CoreInstance first = pair.getOne();
                PackageableElement.writeUserPathForPackageableElement(message.append("\n\t"), first);
                PackageableElement.writeUserPathForPackageableElement(message.append(" ("), first.getClassifier()).append(')');
                first.getSourceInformation().appendMessage(message.append(" at "));

                CoreInstance second = pair.getTwo();
                PackageableElement.writeUserPathForPackageableElement(message.append(" vs "), second);
                PackageableElement.writeUserPathForPackageableElement(message.append(" ("), second.getClassifier()).append(')');
                second.getSourceInformation().appendMessage(message.append(" at "));
            });
            Assert.fail(message.toString());
        }
    }

    @Test
    public void testPackagedElementsContainAllOthers()
    {
        MutableMap<String, MutableList<Pair<CoreInstance, MutableSet<CoreInstance>>>> elementsBySource = Maps.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(repository).forEach(e ->
        {
            SourceInformation sourceInfo = e.getSourceInformation();
            if (sourceInfo != null)
            {
                MutableSet<CoreInstance> componentInstances = GraphTools.getComponentInstances(e, processorSupport).reject(n -> n.getSourceInformation() == null).toSet();
                elementsBySource.getIfAbsentPut(sourceInfo.getSourceId(), Lists.mutable::empty).add(Tuples.pair(e, componentInstances));
            }
        });

        MutableMap<String, MutableList<CoreInstance>> uncontainedBySource = Maps.mutable.empty();
        MutableMap<String, MutableList<Pair<CoreInstance, CoreInstance>>> miscontainedBySource = Maps.mutable.empty();
        GraphNodeIterable.builder()
                .withStartingNodes(repository.getTopLevels())
                .withKeyFilter((instance, key) -> !M3Properties.referenceUsages.equals(key) || !M3PropertyPaths.referenceUsages.equals(instance.getRealKeyByName(key)))
                .withNodeFilter(n -> GraphWalkFilterResult.cont((n.getSourceInformation() != null) && !ReferenceUsage.isReferenceUsage(n, processorSupport)))
                .build()
                .forEach(node ->
                {
                    SourceInformation sourceInfo = node.getSourceInformation();
                    MutableList<Pair<CoreInstance, MutableSet<CoreInstance>>> sourceElements = elementsBySource.get(sourceInfo.getSourceId());
                    Pair<CoreInstance, MutableSet<CoreInstance>> found = (sourceElements == null) ? null : sourceElements.detect(pair -> (node == pair.getOne()) || pair.getOne().getSourceInformation().subsumes(sourceInfo));
                    if (found == null)
                    {
                        uncontainedBySource.getIfAbsentPut(sourceInfo.getSourceId(), Lists.mutable::empty).add(node);
                    }
                    else if (!found.getTwo().contains(node))
                    {
                        miscontainedBySource.getIfAbsentPut(sourceInfo.getSourceId(), Lists.mutable::empty).add(Tuples.pair(node, found.getOne()));
                    }
                });
        if (uncontainedBySource.notEmpty() || miscontainedBySource.notEmpty())
        {
            StringBuilder builder = new StringBuilder();
            if (uncontainedBySource.notEmpty())
            {
                builder.append("There are ").append(uncontainedBySource.valuesView().collectInt(Collection::size).sum())
                        .append(" uncontained instances in ").append(uncontainedBySource.size()).append(" sources:");
                uncontainedBySource.keyValuesView()
                        .toSortedListBy(Pair::getOne)
                        .forEach(sourceElementsPair ->
                        {
                            MutableList<CoreInstance> sourceUncontained = sourceElementsPair.getTwo();
                            builder.append("\n\t").append(sourceElementsPair.getOne()).append(" (").append(sourceUncontained.size()).append(')');
                            if (sourceUncontained.size() <= 10)
                            {
                                sourceUncontained.sortThisBy(CoreInstance::getSourceInformation).forEach(e ->
                                {
                                    builder.append("\n\t\t").append(e);
                                    e.getSourceInformation().appendIntervalMessage(builder.append(" (")).append(')');
                                });
                            }
                            else
                            {
                                MutableMap<CoreInstance, MutableList<CoreInstance>> byClassifier = Maps.mutable.empty();
                                sourceUncontained.forEach(e -> byClassifier.getIfAbsentPut(e.getClassifier(), Lists.mutable::empty).add(e));
                                byClassifier.keyValuesView()
                                        .collect(p -> Tuples.pair(PackageableElement.getUserPathForPackageableElement(p.getOne()), p.getTwo()), Lists.mutable.ofInitialCapacity(byClassifier.size()))
                                        .sortThisBy(Pair::getOne)
                                        .forEach(classifierElementsPair ->
                                        {
                                            MutableList<CoreInstance> classifierElements = classifierElementsPair.getTwo();
                                            builder.append("\n\t\t").append(classifierElementsPair.getOne()).append(" (").append(classifierElements.size()).append(')');
                                            classifierElements.sortThisBy(CoreInstance::getSourceInformation).forEach(e ->
                                            {
                                                builder.append("\n\t\t\t").append(e);
                                                e.getSourceInformation().appendIntervalMessage(builder.append(" (")).append(')');
                                            });
                                        });
                            }
                        });
            }
            if (miscontainedBySource.notEmpty())
            {
                if (uncontainedBySource.notEmpty())
                {
                    builder.append("\n");
                }
                builder.append("There are ").append(miscontainedBySource.valuesView().collectInt(Collection::size).sum())
                        .append(" mis-contained instances in ").append(miscontainedBySource.size()).append(" sources:");
                miscontainedBySource.keyValuesView()
                        .toSortedListBy(Pair::getOne)
                        .forEach(sourceElementsPair ->
                        {
                            MutableList<Pair<CoreInstance, CoreInstance>> sourceMiscontained = sourceElementsPair.getTwo();
                            builder.append("\n\t").append(sourceElementsPair.getOne()).append(" (").append(sourceMiscontained.size()).append(')');
                            sourceMiscontained.sortThisBy(p -> p.getOne().getSourceInformation()).forEach(pair ->
                            {
                                builder.append("\n\t\t").append(pair.getOne());
                                CoreInstance instance = pair.getOne();
                                instance.getSourceInformation().appendIntervalMessage(builder.append(" (")).append(") is not contained in ");

                                CoreInstance incorrectContainingElement = pair.getTwo();
                                PackageableElement.writeUserPathForPackageableElement(builder, incorrectContainingElement);
                                incorrectContainingElement.getSourceInformation().appendIntervalMessage(builder.append(" (")).append(')');

                                ResolvedGraphPath path = GraphTools.findPathToInstance(instance, processorSupport, true);
                                if (path != null)
                                {
                                    path.getGraphPath().writeDescription(builder.append("\n\t\t\tfound at: "));
                                }
                            });
                        });
            }
            Assert.fail(builder.toString());
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
    public void testAllPackageChildrenHaveValidNames()
    {
        MutableMap<CoreInstance, MutableList<CoreInstance>> noNames = Maps.mutable.empty();
        MutableMap<CoreInstance, MutableList<ModelElement>> invalidNames = Maps.mutable.empty();
        M3Lexer lexer = new M3Lexer(null);
        lexer.removeErrorListeners();
        M3Parser parser = new M3Parser(null);
        parser.removeErrorListeners();
        parser.setErrorHandler(new BailErrorStrategy());
        parser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg -> pkg._children().forEach(child ->
        {
            String name = child._name();
            if (name == null)
            {
                noNames.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
            }
            else
            {
                lexer.setInputStream(CharStreams.fromString(name));
                parser.setTokenStream(new CommonTokenStream(lexer));
                try
                {
                    M3Parser.IdentifierContext context = parser.identifier();
                    if (context.getStop().getStopIndex() + 1 < name.length())
                    {
                        // some of the name was not parsed, so was not valid identifier text
                        invalidNames.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
                    }
                }
                catch (Exception ignore)
                {
                    invalidNames.getIfAbsentPut(pkg, Lists.mutable::empty).add(child);
                }
            }
        }));
        if (noNames.notEmpty() || invalidNames.notEmpty())
        {
            StringBuilder builder = new StringBuilder();
            if (noNames.notEmpty())
            {
                builder.append("The following packages have children with no name:");
                noNames.forEachKeyValue((pkg, children) ->
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.forEach(child ->
                    {
                        builder.append("\n\t\t");
                        CoreInstance classifier = processorSupport.getClassifier(child);
                        if (classifier == null)
                        {
                            builder.append(child);
                        }
                        else
                        {
                            PackageableElement.writeUserPathForPackageableElement(builder.append("instance of "), classifier);
                        }
                        SourceInformation sourceInfo = child.getSourceInformation();
                        if (sourceInfo != null)
                        {
                            sourceInfo.appendMessage(builder.append("\n\t\t\tsource: "));
                        }
                    });
                });
            }
            if (invalidNames.notEmpty())
            {
                (noNames.isEmpty() ? builder : builder.append('\n')).append("The following packages have children with invalid names:");
                invalidNames.forEachKeyValue((pkg, children) ->
                {
                    PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t"), pkg);
                    children.forEach(child ->
                    {
                        builder.append("\n\t\t").append(child._name());
                        CoreInstance classifier = processorSupport.getClassifier(child);
                        if (classifier != null)
                        {
                            PackageableElement.writeUserPathForPackageableElement(builder.append("\n\t\t\tclassifier: "), classifier);
                        }
                        SourceInformation sourceInfo = child.getSourceInformation();
                        if (sourceInfo != null)
                        {
                            sourceInfo.appendMessage(builder.append("\n\t\t\tsource: "));
                        }
                    });
                });
            }
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
        Assert.assertEquals(Lists.fixedSize.empty(), expectedModelElements.keysView().reject(actualModelElements::containsKey, Lists.mutable.empty()));

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
        PackageableElementIterable.fromProcessorSupport(processorSupport)
                .select(node -> node instanceof Class)
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
        PackageableElementIterable.fromProcessorSupport(processorSupport)
                .select(node -> node instanceof PropertyOwner)
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
        CompiledStateIntegrityTestTools.testPropertyValueMultiplicities(filterDependenciesFromRepoUnderTest(), processorSupport);
    }

    @Test
    public void testPropertyValueTypes()
    {
        CompiledStateIntegrityTestTools.testPropertyValueTypes(filterDependenciesFromRepoUnderTest(), processorSupport);
    }

    private LazyIterable<? extends CoreInstance> filterDependenciesFromRepoUnderTest()
    {
        if (baseRepositories == null)
        {
            return GraphNodeIterable.fromModelRepository(repository);
        }
        return PackageableElementIterable.fromProcessorSupport(processorSupport)
                .select(packagebleElement -> packagebleElement.getSourceInformation() != null)
                .select(packagebleElement -> baseRepositories.contains(CompositeCodeStorage.getSourceRepoName(packagebleElement.getSourceInformation().getSourceId())))
                .flatCollect(packageableElement -> GraphTools.getComponentInstances(packageableElement, processorSupport));
    }

    @Test
    public void testFunctionApplications()
    {
        CoreInstance functionClass = runtime.getCoreInstance(M3Paths.Function);
        CoreInstance functionExpressionClass = runtime.getCoreInstance(M3Paths.FunctionExpression);

        MutableMap<CoreInstance, MutableSet<CoreInstance>> expected = Maps.mutable.empty();
        MutableMap<CoreInstance, MutableSet<CoreInstance>> actual = Maps.mutable.empty();

        filterDependenciesFromRepoUnderTest().forEach(instance ->
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

        Assert.assertEquals(expected, actual);

        MutableMap<CoreInstance, MutableList<CoreInstance>> noSourceInfo = Maps.mutable.empty();
        expected.forEachKeyValue((function, applications) ->
        {
            MutableList<CoreInstance> noSourceInfoApplications = applications.select(a -> a.getSourceInformation() == null, Lists.mutable.empty());
            if (noSourceInfoApplications.notEmpty())
            {
                noSourceInfo.put(function, noSourceInfoApplications);
            }
        });
        if (noSourceInfo.notEmpty())
        {
            StringBuilder builder = new StringBuilder("There are ").append(noSourceInfo.size()).append(" functions with applications with no source info:");
            noSourceInfo.forEachKeyValue((function, applications) ->
            {
                builder.append("\n\t");
                if (PackageableElement.isPackageableElement(function, processorSupport))
                {
                    PackageableElement.writeUserPathForPackageableElement(builder, function);
                }
                else
                {
                    builder.append(function);
                }
                SourceInformation sourceInfo = function.getSourceInformation();
                if (sourceInfo == null)
                {
                    ResolvedGraphPath path = GraphTools.findPathToInstance(function, processorSupport);
                    if (path != null)
                    {
                        path.getGraphPath().writeDescription(builder.append(" (")).append(')');
                    }
                }
                else
                {
                    sourceInfo.appendMessage(builder.append(" (")).append(')');
                }
                builder.append(" [").append(applications.size()).append("]:");
                MutableSet<CoreInstance> remaining = CompiledStateIntegrityTestTools.forEachInstancePath(applications, processorSupport, (application, path) ->
                {
                    builder.append("\n\t\t").append(application);
                    path.getGraphPath().writeDescription(builder.append(" (")).append(')');
                });
                remaining.forEach(application -> builder.append("\n\t\t").append(application));
            });
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testFunctionsHaveFunctionTypes()
    {
        CoreInstance functionClass = runtime.getCoreInstance(M3Paths.Function);
        MutableList<String> errorMessages = Lists.mutable.empty();
        filterDependenciesFromRepoUnderTest()
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
    public void testLambdaFunctionsHaveUniqueNames()
    {
        MutableMap<String, MutableList<CoreInstance>> lambdasByName = Maps.mutable.empty();
        CoreInstance lambdaFunctionClass = runtime.getCoreInstance(M3Paths.LambdaFunction);
        GraphNodeIterable.fromModelRepository(repository)
                .select(n -> lambdaFunctionClass == n.getClassifier())
                .forEach(lambda -> lambdasByName.getIfAbsentPut(lambda.getName(), Lists.mutable::empty).add(lambda));

        lambdasByName.removeIf((name, lambdas) -> lambdas.size() == 1);
        if (lambdasByName.notEmpty())
        {
            StringBuilder message = new StringBuilder(lambdasByName.size() * 128);
            message.append("There are ").append(lambdasByName.size()).append(" lambda name conflicts:");
            lambdasByName.forEachKeyValue((name, lambdas) ->
            {
                message.append("\n\t").append(name).append(": ").append(lambdas.size()).append(" (");
                MutableList<SourceInformation> sourceInfo = lambdas.collect(CoreInstance::getSourceInformation);
                sourceInfo.removeIf(Objects::isNull);
                if (sourceInfo.isEmpty())
                {
                    message.append("none with source info");
                }
                else
                {
                    sourceInfo.forEachWithIndex((si, i) -> si.appendMessage((i == 0) ? message : message.append(", ")));
                    int withNoSourceInfo = lambdas.size() - sourceInfo.size();
                    if (withNoSourceInfo > 0)
                    {
                        message.append(" plus ").append(withNoSourceInfo).append(" with no source info");
                    }
                }
                message.append(')');
            });
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

        MutableMap<String, MutableSet<String>> expected = Maps.mutable.empty();
        MutableMap<String, MutableSet<String>> actual = Maps.mutable.empty();

        filterDependenciesFromRepoUnderTest()
                .select(instance -> Instance.instanceOf(instance, typeClass, processorSupport))
                .forEach(type ->
                {
                    type.getValueForMetaPropertyToMany(M3Properties.generalizations).forEach(generalization ->
                    {
                        CoreInstance general = Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.general, M3Properties.rawType, processorSupport);
                        if (general != topType)
                        {
                            expected.getIfAbsentPut(typetoString(general), Sets.mutable::empty).add(generalizationToString(generalization));
                        }
                    });

                    ListIterable<? extends CoreInstance> specializations = type.getValueForMetaPropertyToMany(M3Properties.specializations);
                    if (specializations.notEmpty())
                    {
                        actual.put(typetoString(type), specializations.collect(this::generalizationToString, Sets.mutable.empty()));
                    }
                });

        // Filter out types where expected and actual are equal to reduce noise in failure message
        Sets.mutable.withAll(expected.keySet()).withAll(actual.keySet()).forEach(type ->
        {
            MutableSet<String> expectedSpecs = expected.get(type);
            MutableSet<String> actualSpecs = actual.get(type);
            if (Objects.equals(expectedSpecs, actualSpecs))
            {
                expected.remove(type);
                actual.remove(type);
            }
        });
        Assert.assertEquals(expected, actual);
    }

    private String typetoString(CoreInstance type)
    {
        if (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            return PackageableElement.getUserPathForPackageableElement(type);
        }
        if (type instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)
        {
            return FunctionType.print(type, true, processorSupport);
        }
        if (type instanceof Unit)
        {
            return Measure.getUserPathForUnit(type);
        }
        throw new RuntimeException("Cannot generate string for: " + type);
    }

    private String generalizationToString(CoreInstance generalization)
    {
        StringBuilder builder = new StringBuilder();
        CoreInstance specific = Instance.getValueForMetaPropertyToOneResolved(generalization, M3Properties.specific, processorSupport);
        if (specific instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)
        {
            PackageableElement.writeUserPathForPackageableElement(builder, specific);
        }
        else if (specific instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType)
        {
            FunctionType.print(builder, specific, true, processorSupport);
        }
        else if (specific instanceof Unit)
        {
            Measure.writeUserPathForUnit(builder, specific);
        }
        else
        {
            throw new RuntimeException("Cannot generate string for: " + specific);
        }
        builder.append(" -> ");
        CoreInstance general = generalization.getValueForMetaPropertyToOne(M3Properties.general);
        GenericType.print(builder, general, true, processorSupport);
        return builder.toString();
    }

    @Test
    public void testSourceSerialization()
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        runtime.getSourceRegistry().getSources()
                .select(source -> source.getId() != null)
                .select(source -> baseRepositories == null || baseRepositories.contains(CompositeCodeStorage.getSourceRepoName(source.getId())))
                .forEach(source ->
                {
                    stream.reset();
                    BinaryModelSourceSerializer.serialize(BinaryWriters.newBinaryWriter(stream), source, runtime);
                    BinaryModelSourceDeserializer.deserialize(BinaryReaders.newBinaryReader(stream.toByteArray()), ExternalReferenceSerializerLibrary.newLibrary(runtime));
                });
    }

    @Test
    public void testAnnotationModelElements()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(processorSupport).selectInstancesOf(Profile.class).forEach(profile ->
        {
            profile._p_stereotypes().forEach(st ->
            {
                MutableList<AnnotatedElement> missingStereotype = Lists.mutable.empty();
                MutableList<AnnotatedElement> noSourceInfo = Lists.mutable.empty();
                st._modelElements().forEach(e ->
                {
                    if (!e._stereotypes().contains(st))
                    {
                        missingStereotype.add(e);
                    }
                    if (e.getSourceInformation() == null)
                    {
                        noSourceInfo.add(e);
                    }
                });
                if (noSourceInfo.notEmpty() || missingStereotype.notEmpty())
                {
                    StringBuilder builder = new StringBuilder("Stereotype ");
                    PackageableElement.writeUserPathForPackageableElement(builder, profile).append('.').append(st._value());
                    if (missingStereotype.notEmpty())
                    {
                        builder.append("\n\t\thas ").append(missingStereotype.size()).append(" modelElements without the stereotype:");
                        missingStereotype.forEach(e ->
                        {
                            builder.append("\n\t\t\t").append(e);
                            SourceInformation sourceInfo = e.getSourceInformation();
                            if (sourceInfo != null)
                            {
                                sourceInfo.appendMessage(builder.append(" (")).append(')');
                            }
                        });
                    }
                    if (noSourceInfo.notEmpty())
                    {
                        builder.append("\n\t\thas ").append(missingStereotype.size()).append(" modelElements without no source info:");
                        MutableSet<AnnotatedElement> remaining = CompiledStateIntegrityTestTools.forEachInstancePath(noSourceInfo, processorSupport,
                                (instance, path) -> path.getGraphPath().writeDescription(builder.append("\n\t\t\t").append(instance).append(" (")).append(')'));
                        remaining.forEach(instance -> builder.append("\n\t\t\t").append(instance).append(" (no path found)"));
                    }
                    errorMessages.add(builder.toString());
                }
            });
            profile._p_tags().forEach(tag ->
            {
                MutableList<AnnotatedElement> noSourceInfo = Lists.mutable.empty();
                MutableList<AnnotatedElement> missingTag = Lists.mutable.empty();
                tag._modelElements().forEach(e ->
                {
                    if (e.getSourceInformation() == null)
                    {
                        noSourceInfo.add(e);
                    }
                    if (e._taggedValues().noneSatisfy(tv -> tag == tv._tag()))
                    {
                        missingTag.add(e);
                    }
                });
                if (noSourceInfo.notEmpty() || missingTag.notEmpty())
                {
                    StringBuilder builder = new StringBuilder("Stereotype ");
                    PackageableElement.writeUserPathForPackageableElement(builder, profile).append('.').append(tag._value());
                    if (missingTag.notEmpty())
                    {
                        builder.append("\n\t\thas ").append(missingTag.size()).append(" modelElements without the tag:");
                        missingTag.forEach(e ->
                        {
                            builder.append("\n\t\t\t").append(e);
                            SourceInformation sourceInfo = e.getSourceInformation();
                            if (sourceInfo != null)
                            {
                                sourceInfo.appendMessage(builder.append(" (")).append(')');
                            }
                        });
                    }
                    if (noSourceInfo.notEmpty())
                    {
                        builder.append("\n\t\thas ").append(missingTag.size()).append(" modelElements without no source info:");
                        MutableSet<AnnotatedElement> remaining = CompiledStateIntegrityTestTools.forEachInstancePath(noSourceInfo, processorSupport,
                                (instance, path) -> path.getGraphPath().writeDescription(builder.append("\n\t\t\t").append(instance).append(" (")).append(')'));
                        remaining.forEach(instance -> builder.append("\n\t\t\t").append(instance).append(" (no path found)"));
                    }
                    errorMessages.add(builder.toString());
                }
            });
        });
        if (errorMessages.notEmpty())
        {
            StringBuilder builder = new StringBuilder("There are ").append(errorMessages.size()).append(" annotations with invalid model elements");
            errorMessages.forEach(m -> builder.append("\n\t").append(m));
            Assert.fail(builder.toString());
        }
    }

    @Test
    public void testReferencerUsageOwners()
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(processorSupport).forEach(element ->
        {
            SourceInformation elementSourceInfo = element.getSourceInformation();
            MutableSet<CoreInstance> internalNodes = GraphNodeIterable.builder()
                    .withStartingNode(element)
                    .withKeyFilter((node, key) -> !M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS.contains(node.getRealKeyByName(key)))
                    .withNodeFilter(node ->
                    {
                        SourceInformation nodeSourceInfo = node.getSourceInformation();
                        boolean internal = (nodeSourceInfo == null) ? !(node instanceof Package) : elementSourceInfo.subsumes(nodeSourceInfo);
                        return internal ? GraphWalkFilterResult.ACCEPT_AND_CONTINUE : GraphWalkFilterResult.REJECT_AND_STOP;
                    })
                    .build()
                    .toSet();
            MutableMap<CoreInstance, MutableList<CoreInstance>> badRefUsages = Maps.mutable.empty();
            internalNodes.forEach(node ->
            {
                if (node instanceof Referenceable)
                {
                    ((Referenceable) node)._referenceUsages().forEach(refUsage ->
                    {
                        CoreInstance owner = refUsage._ownerCoreInstance();
                        if ((owner.getSourceInformation() == null) && !internalNodes.contains(owner))
                        {
                            badRefUsages.getIfAbsentPut(node, Lists.mutable::empty).add(refUsage);
                        }
                    });
                }
            });
            badRefUsages.forEachKeyValue((node, refUsages) ->
            {
                StringBuilder builder = new StringBuilder();
                if (node == element)
                {
                    PackageableElement.writeUserPathForPackageableElement(builder, node);
                }
                else
                {
                    ResolvedGraphPath pathToNode = GraphTools.findPathToInstance(node, processorSupport);
                    if (pathToNode == null)
                    {
                        builder.append(node);
                    }
                    else
                    {
                        pathToNode.getGraphPath().writeDescription(builder);
                    }
                }
                SourceInformation nodeSourceInfo = node.getSourceInformation();
                if (nodeSourceInfo != null)
                {
                    nodeSourceInfo.appendMessage(builder.append(" (")).append(')');
                }
                else
                {
                    elementSourceInfo.appendMessage(builder.append(" (within ")).append(')');
                }
                builder.append(" [").append(refUsages.size()).append("]:");
                refUsages.forEach(refUsage -> ReferenceUsage.writeReferenceUsage(builder.append("\n\t\t"), refUsage));
                errorMessages.add(builder.toString());
            });
        });
        if (errorMessages.notEmpty())
        {
            StringBuilder builder = new StringBuilder("There are ").append(errorMessages.size()).append(" instances with ReferenceUsages with no owner source information");
            errorMessages.forEach(m -> builder.append("\n\t").append(m));
            Assert.fail(builder.toString());
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
