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

package org.finos.legend.pure.m3.serialization.runtime;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.BinaryModelRepositorySerializer;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJar;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarTools;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJars;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.pattern.PurePattern;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class TestGraphLoader extends AbstractPureTestWithCoreCompiledPlatform
{
    private static final MutableList<PureRepositoryJar> jars = Lists.mutable.empty();

    private PureRuntime runtime2;
    private ModelRepository repository2;
    private Context context2;
    private ProcessorSupport processorSupport2;
    private GraphLoader loader;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        CodeStorage codeStorage = runtime.getCodeStorage();
        RichIterable<String> repoNames = codeStorage.getAllRepoNames();
        if (codeStorage.isFile(PureCodeStorage.WELCOME_FILE_PATH))
        {
            repoNames = repoNames.toList().with(null);
        }
        for (String repoName : repoNames)
        {
            outStream.reset();
            try
            {
                BinaryModelRepositorySerializer.serialize(outStream, repoName, runtime);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error writing cache for " + repoName, e);
            }
            jars.add(PureRepositoryJars.get(outStream));
        }
    }

    @Before
    public void setUpSecondRuntimeAndGraphLoader()
    {
        MutableList<PureRepositoryJar> jars = Lists.mutable.empty();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        CodeStorage codeStorage = runtime.getCodeStorage();
        RichIterable<String> repoNames = codeStorage.getAllRepoNames();
        if (codeStorage.isFile(PureCodeStorage.WELCOME_FILE_PATH))
        {
            repoNames = repoNames.toList().with(null);
        }
        for (String repoName : repoNames)
        {
            outStream.reset();
            try
            {
                BinaryModelRepositorySerializer.serialize(outStream, repoName, runtime);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Error writing cache for " + repoName, e);
            }
            jars.add(PureRepositoryJars.get(outStream));
        }

        this.runtime2 = new PureRuntimeBuilder(runtime.getCodeStorage())
                .withRuntimeStatus(getPureRuntimeStatus())
                .build();
        this.repository2 = this.runtime2.getModelRepository();
        this.context2 = this.runtime2.getContext();
        this.processorSupport2 = this.runtime2.getProcessorSupport();
        IncrementalCompiler compiler = this.runtime2.getIncrementalCompiler();
        this.loader = buildGraphLoader(this.repository2, this.context2, compiler.getParserLibrary(), compiler.getDslLibrary(), this.runtime2.getSourceRegistry(), this.runtime2.getURLPatternLibrary(), SimplePureRepositoryJarLibrary.newLibrary(jars));
    }

    protected abstract GraphLoader buildGraphLoader(ModelRepository repository, Context context, ParserLibrary parserLibrary, InlineDSLLibrary dslLibrary, SourceRegistry sourceRegistry, URLPatternLibrary patternLibrary, PureRepositoryJarLibrary jarLibrary);

    @Test
    public void testIsKnownRepository()
    {
        assertInitialState();
        Assert.assertTrue(this.loader.isKnownRepository("platform"));
        Assert.assertFalse(this.loader.isKnownRepository("not a repo"));
    }

    @Test
    public void testIsKnownFile()
    {
        assertInitialState();
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/grammar/m3.pc"));
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/grammar/milestoning.pc"));

        Assert.assertFalse(this.loader.isKnownFile("not a file at all"));
        Assert.assertFalse(this.loader.isKnownFile("datamart_datamt/something/somethingelse.pure"));
    }

    @Test
    public void testIsKnownInstance()
    {
        assertInitialState();
        Assert.assertTrue(this.loader.isKnownInstance("meta::pure::metamodel::type::Class"));
        Assert.assertTrue(this.loader.isKnownInstance("meta::pure::metamodel::type::Type"));
        Assert.assertTrue(this.loader.isKnownInstance("meta::pure::metamodel::multiplicity::PureOne"));

        Assert.assertFalse(this.loader.isKnownInstance("not an instance"));
        Assert.assertFalse(this.loader.isKnownInstance("meta::pure::metamodel::multiplicity::PureOneThousand"));
    }

    @Test
    public void testLoadAll()
    {
        assertInitialState();
        this.loader.loadAll();
        assertAllOfRuntimeLoaded(Sets.immutable.with("platform", "system"));
    }

    @Test
    public void testLoadM3()
    {
        assertInitialState();

        MutableSet<String> alreadyLoaded = this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty());
        String m3SourceId = "/platform/pure/grammar/m3.pure";
        this.loader.loadFile(m3SourceId);

        assertSetsEqual(Sets.mutable.with(m3SourceId).withAll(alreadyLoaded), this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty()));
        assertSetsEqual(Sets.mutable.with(m3SourceId).withAll(alreadyLoaded), this.runtime2.getSourceRegistry().getSourceIds().toSet());
        assertSourcesEqual(m3SourceId);

        for (CoreInstance instance : GraphNodeIterable.fromModelRepository(this.repository2))
        {
            SourceInformation sourceInfo = instance.getSourceInformation();
            if (sourceInfo != null && !alreadyLoaded.contains(sourceInfo.getSourceId()))
            {
                Assert.assertEquals("Wrong source id for " + instance + " (" + sourceInfo + ")", m3SourceId, sourceInfo.getSourceId());
            }
        }

        for (CoreInstance instance : runtime.getSourceById(m3SourceId).getNewInstances())
        {
            String path = PackageableElement.getUserPathForPackageableElement(instance);
            if (M3Paths.Root.equals(path))
            {
                path = "::";
            }
            Assert.assertNotNull(path, this.runtime2.getCoreInstance(path));
        }

        assertSetsEqual(repository.getTopLevels().collect(CoreInstance::getName, Sets.mutable.empty()), this.repository2.getTopLevels().collect(CoreInstance::getName, Sets.mutable.empty()));
    }

    @Test
    public void testLoadCollection()
    {
        assertInitialState();

        String m3SourceId = "/platform/pure/grammar/m3.pure";
        String collectionSourceId = "/platform/pure/anonymousCollections.pure";

        this.loader.loadFile(collectionSourceId);
        MutableSet<String> loadedFiles = this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty());

        Verify.assertContains(collectionSourceId, loadedFiles);
        assertSourcesEqual(collectionSourceId);

        Verify.assertContains(m3SourceId, loadedFiles);
        assertSourcesEqual(m3SourceId);

        assertSetsEqual(repository.getTopLevels().collect(CoreInstance::getName, Sets.mutable.empty()), this.repository2.getTopLevels().collect(CoreInstance::getName, Sets.mutable.empty()));
    }

    @Test
    public void testLoadRepository()
    {
        assertInitialState();
        this.loader.loadRepository("platform");
        assertAllOfRuntimeLoaded(Sets.immutable.with("platform"));
    }

    @Test
    public void testLoadRepositories()
    {
        assertInitialState();
        ImmutableSet<String> repos = Sets.immutable.with("platform", "system");
        this.loader.loadRepositories(repos);
        assertAllOfRuntimeLoaded(repos);
    }

    private void assertInitialState()
    {
        Assert.assertEquals(Sets.immutable.empty(), this.loader.getLoadedFiles());
        Assert.assertTrue(this.repository2.getTopLevels().isEmpty());
    }

    private void assertAllOfRuntimeLoaded(SetIterable<String> repos)
    {
        Predicate<CoreInstance> isInRelevantRepo = i -> isInSomeRepo(i, repos);

        // Compare top level elements
        assertSetsEqual(repository.getTopLevels().collectIf(isInRelevantRepo, CoreInstance::getName, Sets.mutable.empty()), this.repository2.getTopLevels().collect(CoreInstance::getName, Sets.mutable.empty()));

        // Compare packaged elements by path
        assertSetsEqual(getAllRelevantPackagedElementPaths(repository, isInRelevantRepo), getAllPackagedElementPaths(this.repository2));

        // Compare functionsByName
        MutableMap<String, SetIterable<String>> functionsByNameBefore = Maps.mutable.empty();
        for (String functionName : context.getAllFunctionNames())
        {
            SetIterable<CoreInstance> functions = context.getFunctionsForName(functionName);
            SetIterable<String> res = functions.collectIf(isInRelevantRepo, PackageableElement::getUserPathForPackageableElement, Sets.mutable.ofInitialCapacity(functions.size()));
            if (!res.isEmpty())
            {
                functionsByNameBefore.put(functionName, res);
            }
        }
        MutableMap<String, SetIterable<String>> functionsByNameAfter = Maps.mutable.empty();
        for (String functionName : this.context2.getAllFunctionNames())
        {
            SetIterable<CoreInstance> functions = this.context2.getFunctionsForName(functionName);
            Verify.assertAllSatisfy(functions, function -> Instance.instanceOf(function, M3Paths.Function, this.processorSupport2));
            functionsByNameAfter.put(functionName, functions.collect(PackageableElement::getUserPathForPackageableElement, Sets.mutable.ofInitialCapacity(functions.size())));
        }
        Verify.assertMapsEqual(functionsByNameBefore, functionsByNameAfter);

        // Compare sources
        SourceRegistry sourceRegistry1 = runtime.getSourceRegistry();
        SourceRegistry sourceRegistry2 = this.runtime2.getSourceRegistry();
        MutableSet<String> relevantSourceIds1 = sourceRegistry1.getSourceIds().select(id -> repos.contains(PureCodeStorage.getSourceRepoName(id)), Sets.mutable.empty());
        assertSetsEqual(relevantSourceIds1, sourceRegistry2.getSourceIds().toSet());
        assertSetsEqual(relevantSourceIds1, this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty()));
        relevantSourceIds1.forEach(this::assertSourcesEqual);

        // Compare pattern library
        assertURLPatternLibrariesEqual(runtime.getURLPatternLibrary(), this.runtime2.getURLPatternLibrary());
    }

    private boolean isInSomeRepo(CoreInstance instance, SetIterable<String> repos)
    {
        SourceInformation sourceInfo = instance.getSourceInformation();
        return (sourceInfo != null) && repos.contains(PureCodeStorage.getSourceRepoName(sourceInfo.getSourceId()));
    }

    private void assertSourcesEqual(String sourceId)
    {
        Source source1 = runtime.getSourceById(sourceId);
        Source source2 = this.runtime2.getSourceById(sourceId);
        String message = "Mismatch for " + sourceId;
        Assert.assertEquals(message, source1.getContent(), source2.getContent());
        Assert.assertEquals(message, source1.isCompiled(), source2.isCompiled());
        Assert.assertEquals(message, source1.isInMemory(), source2.isInMemory());
        Assert.assertEquals(message, source1.isImmutable(), source2.isImmutable());
        Verify.assertMapsEqual(message, getElementsByParser(source1), getElementsByParser(source2));
    }

    private void assertURLPatternLibrariesEqual(URLPatternLibrary library1, URLPatternLibrary library2)
    {
        ListIterable<PurePattern> patterns1 = library1.getPatterns();
        ListIterable<PurePattern> patterns2 = library2.getPatterns();

        assertSetsEqual(patterns1.collect(PurePattern::getSrcPattern, Sets.mutable.empty()), patterns2.collect(PurePattern::getSrcPattern, Sets.mutable.empty()));
        assertSetsEqual(patterns1.collect(PurePattern::getRealPattern, Sets.mutable.empty()), patterns2.collect(PurePattern::getRealPattern, Sets.mutable.empty()));
        assertSetsEqual(patterns1.collect(p -> PackageableElement.getUserPathForPackageableElement(p.getFunction()), Sets.mutable.empty()), patterns2.collect(p -> PackageableElement.getUserPathForPackageableElement(p.getFunction()), Sets.mutable.empty()));
    }

    private MutableMap<String, ListIterable<String>> getElementsByParser(Source source)
    {
        MutableMap<String, ListIterable<String>> result = Maps.mutable.empty();
        source.getElementsByParser().forEachKeyMultiValues((parser, elements) -> result.put(parser.getName(), Iterate.collect(elements, PackageableElement::getUserPathForPackageableElement, Lists.mutable.empty())));
        return result;
    }

    private MutableSet<String> getAllPackagedElementPaths(ModelRepository repository)
    {
        MutableSet<String> paths = Sets.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg ->
        {
            if (pkg.getSourceInformation() != null)
            {
                paths.add(PackageableElement.getUserPathForPackageableElement(pkg));
            }
            pkg.getValueForMetaPropertyToMany(M3Properties.children).collectIf(c -> c.getSourceInformation() != null, PackageableElement::getUserPathForPackageableElement, paths);
        });
        return paths;
    }

    private MutableSet<String> getAllRelevantPackagedElementPaths(ModelRepository repository, Predicate<CoreInstance> isRelevant)
    {
        MutableSet<String> paths = Sets.mutable.empty();
        PackageTreeIterable.newRootPackageTreeIterable(repository).forEach(pkg ->
        {
            if (isRelevant.accept(pkg))
            {
                paths.add(PackageableElement.getUserPathForPackageableElement(pkg));
            }
            pkg.getValueForMetaPropertyToMany(M3Properties.children).collectIf(isRelevant, PackageableElement::getUserPathForPackageableElement, paths);
        });
        return paths;
    }

    private <T> void assertSetsEqual(SetIterable<T> expected, SetIterable<T> actual)
    {
        if (!expected.equals(actual))
        {
            MutableList<T> missing = expected.reject(actual::contains, Lists.mutable.empty());
            MutableList<T> extra = actual.reject(expected::contains, Lists.mutable.empty());
            StringBuilder builder = new StringBuilder("Sets not equal");
            if (missing.notEmpty())
            {
                missing.appendString(builder, "\n\tmissing: ", ", ", "");
            }
            if (extra.notEmpty())
            {
                extra.appendString(builder, "\n\textra: ", ", ", "");
            }
            Assert.fail(builder.toString());
        }
    }
}
