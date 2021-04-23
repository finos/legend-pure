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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
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
        setUpRuntime(getExtra());
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
                throw new RuntimeException("Error writing cache for " + repoName, e);
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
                throw new RuntimeException("Error writing cache for " + repoName, e);
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
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/m3.pc"));
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/graph.pc"));
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/milestoning.pc"));

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
        assertAllOfRuntimeLoaded();
    }

    @Test
    public void testLoadM3()
    {
        assertInitialState();

        MutableSet<String> alreadyLoaded = this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty());
        String m3SourceId = "/platform/pure/m3.pure";
        this.loader.loadFile(m3SourceId);

        Verify.assertSetsEqual(Sets.mutable.with(m3SourceId).union(alreadyLoaded), this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty()));
        Verify.assertSetsEqual(Sets.mutable.with(m3SourceId).union(alreadyLoaded), this.runtime2.getSourceRegistry().getSourceIds().toSet());
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

        Verify.assertSetsEqual(repository.getTopLevels().collect(CoreInstance.GET_NAME).toSet(), this.repository2.getTopLevels().collect(CoreInstance.GET_NAME).toSet());
    }

    @Test
    public void testLoadCollection()
    {
        assertInitialState();

        String m3SourceId = "/platform/pure/m3.pure";
        String collectionSourceId = "/platform/pure/collection.pure";

        this.loader.loadFile(collectionSourceId);
        MutableSet<String> loadedFiles = this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty());

        Verify.assertContains(collectionSourceId, loadedFiles);
        assertSourcesEqual(collectionSourceId);

        Verify.assertContains(m3SourceId, loadedFiles);
        assertSourcesEqual(m3SourceId);

        Verify.assertSetsEqual(repository.getTopLevels().collect(CoreInstance.GET_NAME).toSet(), this.repository2.getTopLevels().collect(CoreInstance.GET_NAME).toSet());
    }

    @Test
    public void testLoadRepository()
    {
        assertInitialState();
        this.loader.loadRepository("platform");
        assertAllOfRuntimeLoaded();
    }

    @Test
    public void testLoadRepositories()
    {
        assertInitialState();
        this.loader.loadRepositories(Lists.immutable.with("platform"));
        assertAllOfRuntimeLoaded();
    }

    private void assertInitialState()
    {
        Assert.assertEquals(Sets.immutable.empty(), this.loader.getLoadedFiles());
        Assert.assertTrue(this.repository2.getTopLevels().isEmpty());
    }

    private void assertAllOfRuntimeLoaded()
    {
        // Compare top level elements
        Verify.assertSetsEqual(repository.getTopLevels().collect(CoreInstance::getName).toSet(), this.repository2.getTopLevels().collect(CoreInstance::getName).toSet());

        // Compare packaged elements by path
        //Set<String> fromExtra = Sets.mutable.with("meta::pure::functions::io", "meta::pure::functions::hash", "meta::pure::functions::date");
        Verify.assertSetsEqual(getAllPackagedElementPaths(repository), getAllPackagedElementPaths(this.repository2));

        // Compare functionsByName
        MutableMap<String, SetIterable<String>> functionsByNameBefore = Maps.mutable.empty();
        for (String functionName : context.getAllFunctionNames())
        {
            SetIterable<CoreInstance> functions = context.getFunctionsForName(functionName);
            SetIterable<String> res = functions.collectIf(s -> !"/system/extra.pure".equals(s.getSourceInformation().getSourceId()), PackageableElement::getUserPathForPackageableElement, UnifiedSet.newSet(functions.size()));
            if (!res.isEmpty())
            {
                functionsByNameBefore.put(functionName, res);
            }
        }
        MutableMap<String, SetIterable<String>> functionsByNameAfter = Maps.mutable.empty();
        for (String functionName : this.context2.getAllFunctionNames())
        {
            SetIterable<CoreInstance> functions = this.context2.getFunctionsForName(functionName);
            Verify.assertAllSatisfy(functions, (Predicate<CoreInstance>) function -> Instance.instanceOf(function, M3Paths.Function, this.processorSupport2));
            functionsByNameAfter.put(functionName, functions.collect(PackageableElement::getUserPathForPackageableElement, UnifiedSet.newSet(functions.size())));
        }
        Verify.assertMapsEqual(functionsByNameBefore, functionsByNameAfter);

        // Compare sources
        SourceRegistry sourceRegistry1 = runtime.getSourceRegistry();
        SourceRegistry sourceRegistry2 = this.runtime2.getSourceRegistry();
        Verify.assertSetsEqual(sourceRegistry1.getSources().select(s -> !s.isInMemory()).collect(Source::getId).toSet(), sourceRegistry2.getSourceIds().toSet());
        Verify.assertSetsEqual(sourceRegistry1.getSources().select(s -> !s.isInMemory()).collect(Source::getId).toSet(), this.loader.getLoadedFiles().collect(PureRepositoryJarTools::binaryPathToPurePath, Sets.mutable.empty()));
        for (String sourceId : sourceRegistry1.getSources().select(s -> !s.isInMemory()).collect(Source::getId))
        {
            assertSourcesEqual(sourceId);
        }

        // Compare pattern library
        assertURLPatternLibrariesEqual(runtime.getURLPatternLibrary(), this.runtime2.getURLPatternLibrary());
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

        Verify.assertSetsEqual(patterns1.collect(PurePattern::getSrcPattern, Sets.mutable.empty()), patterns2.collect(PurePattern::getSrcPattern, Sets.mutable.empty()));
        Verify.assertSetsEqual(patterns1.collect(PurePattern.GET_REAL_PATTERN, Sets.mutable.empty()), patterns2.collect(PurePattern.GET_REAL_PATTERN, Sets.mutable.empty()));
        Verify.assertSetsEqual(patterns1.collect(Functions.chain(PurePattern::getFunction, PackageableElement::getUserPathForPackageableElement), Sets.mutable.empty()), patterns2.collect(Functions.chain(PurePattern::getFunction, PackageableElement::getUserPathForPackageableElement), Sets.mutable.empty()));
    }

    private MutableMap<String, ListIterable<String>> getElementsByParser(Source source)
    {
        MutableMap<String, ListIterable<String>> result = Maps.mutable.empty();
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        for (Parser parser : elementsByParser.keysView())
        {
            result.put(parser.getName(), elementsByParser.get(parser).collect(PackageableElement::getUserPathForPackageableElement));
        }
        return result;
    }

    private MutableSet<String> getAllPackagedElementPaths(ModelRepository repository)
    {
        MutableSet<String> paths = Sets.mutable.empty();
        for (CoreInstance pkg : PackageTreeIterable.newRootPackageTreeIterable(repository))
        {
            if (pkg.getSourceInformation() != null && !"/system/extra.pure".equals(pkg.getSourceInformation().getSourceId()))
            {
                paths.add(PackageableElement.getUserPathForPackageableElement(pkg));
            }
            for (CoreInstance instance : pkg.getValueForMetaPropertyToMany(M3Properties.children))
            {
                if (instance.getSourceInformation() != null && !"/system/extra.pure".equals(instance.getSourceInformation().getSourceId()))
                {
                    paths.add(PackageableElement.getUserPathForPackageableElement(instance));
                }

            }
        }
        return paths;
    }
}
