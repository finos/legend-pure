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
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.execution.FunctionExecution;
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
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.finos.legend.pure.m3.tools.PackageTreeIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class TestGraphLoader extends AbstractPureTestWithCoreCompiledPlatform
{
    protected static FunctionExecution functionExecution2;
    protected static PureRuntime runtime2;
    protected static ModelRepository repository2;
    protected static Context context2;
    protected static ProcessorSupport processorSupport2;
    protected static GraphLoader loader;
    protected static MutableList<PureRepositoryJar> jars;

    public static void setUp()
    {
        setUpRuntime(getExtra());
        jars = Lists.mutable.empty();
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        CodeStorage codeStorage = runtime.getCodeStorage();
        RichIterable<String> repoNames = codeStorage.getAllRepoNames();
        if (codeStorage.isFile(PureCodeStorage.WELCOME_FILE_PATH))
        {
            repoNames = LazyIterate.concatenate(repoNames, Lists.immutable.with((String)null));
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

        runtime2 = new PureRuntimeBuilder(runtime.getCodeStorage())
                .withRuntimeStatus(getPureRuntimeStatus())
                .build();
        functionExecution2 = getFunctionExecution();
        functionExecution2.init(runtime2, new Message(""));
        repository2 = runtime2.getModelRepository();
        context2 = runtime2.getContext();
        processorSupport2 = runtime2.getProcessorSupport();
    }
//    protected abstract GraphLoader buildGraphLoader(ModelRepository repository, Context context, ParserLibrary parserLibrary, InlineDSLLibrary dslLibrary, SourceRegistry sourceRegistry, URLPatternLibrary patternLibrary, PureRepositoryJarLibrary jarLibrary);

    @Test
    public void testIsKnownRepository()
    {
        Assert.assertTrue(this.loader.isKnownRepository("platform"));
        Assert.assertFalse(this.loader.isKnownRepository("not a repo"));
    }

    @Test
    public void testIsKnownFile()
    {
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/m3.pc"));
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/graph.pc"));
        Assert.assertTrue(this.loader.isKnownFile("platform/pure/milestoning.pc"));

        Assert.assertFalse(this.loader.isKnownFile("not a file at all"));
        Assert.assertFalse(this.loader.isKnownFile("datamart_datamt/something/somethingelse.pure"));
    }

    @Test
    public void testIsKnownInstance()
    {
        Assert.assertTrue(this.loader.isKnownInstance("meta::pure::metamodel::type::Class"));
        Assert.assertTrue(this.loader.isKnownInstance("meta::pure::metamodel::type::Type"));
        Assert.assertTrue(this.loader.isKnownInstance("meta::pure::metamodel::multiplicity::PureOne"));

        Assert.assertFalse(this.loader.isKnownInstance("not an instance"));
        Assert.assertFalse(this.loader.isKnownInstance("meta::pure::metamodel::multiplicity::PureOneThousand"));
    }

    @Test
    public void testLoadAll()
    {
        this.loader.loadAll();

        // Compare top level elements
        Verify.assertSetsEqual(this.repository.getTopLevels().collect(CoreInstance.GET_NAME).toSet(), this.repository2.getTopLevels().collect(CoreInstance.GET_NAME).toSet());

        // Compare packaged elements by path
        //Set<String> fromExtra = Sets.mutable.with("meta::pure::functions::io", "meta::pure::functions::hash", "meta::pure::functions::date");
        Verify.assertSetsEqual(getAllPackagedElementPaths(this.repository), getAllPackagedElementPaths(this.repository2));

        // Compare functionsByName
        MutableMap<String, SetIterable<String>> functionsByNameBefore = Maps.mutable.empty();
        for (String functionName : this.context.getAllFunctionNames())
        {
            SetIterable<CoreInstance> functions = this.context.getFunctionsForName(functionName);
            SetIterable<String> res = functions.select(s->!"/system/extra.pure".equals(s.getSourceInformation().getSourceId())).collect(PackageableElement.GET_USER_PATH, UnifiedSet.<String>newSet(functions.size()));
            if (!res.isEmpty())
            {
                functionsByNameBefore.put(functionName, res);
            }
        }
        MutableMap<String, SetIterable<String>> functionsByNameAfter = Maps.mutable.empty();
        for (String functionName : this.context2.getAllFunctionNames())
        {
            SetIterable<CoreInstance> functions = this.context2.getFunctionsForName(functionName);
            Verify.assertAllSatisfy(functions, new Predicate<CoreInstance>()
            {
                @Override
                public boolean accept(CoreInstance function)
                {
                    return Instance.instanceOf(function, M3Paths.Function, TestGraphLoader.this.processorSupport2);
                }
            });
            functionsByNameAfter.put(functionName, functions.collect(PackageableElement.GET_USER_PATH, UnifiedSet.<String>newSet(functions.size())));
        }
        System.out.println(functionsByNameBefore);
        Verify.assertMapsEqual(functionsByNameBefore, functionsByNameAfter);

        // Compare sources
        SourceRegistry sourceRegistry1 = this.runtime.getSourceRegistry();
        SourceRegistry sourceRegistry2 = this.runtime2.getSourceRegistry();
        Verify.assertSetsEqual(sourceRegistry1.getSources().select(s->!s.isInMemory()).collect(Source::getId).toSet(), sourceRegistry2.getSourceIds().toSet());
        Verify.assertSetsEqual(sourceRegistry1.getSources().select(s->!s.isInMemory()).collect(Source::getId).toSet(), this.loader.getLoadedFiles().collect(PureRepositoryJarTools.BINARY_PATH_TO_PURE_PATH, Sets.mutable.<String>empty()));
        for (String sourceId : sourceRegistry1.getSources().select(s->!s.isInMemory()).collect(Source::getId))
        {
            assertSourcesEqual(sourceId);
        }

        // Compare pattern library
        assertURLPatternLibrariesEqual(this.runtime.getURLPatternLibrary(), this.runtime2.getURLPatternLibrary());
    }

    @Test
    public void testLoadM3()
    {
        MutableSet<String> alreadyLoaded = this.loader.getLoadedFiles().collect(PureRepositoryJarTools.BINARY_PATH_TO_PURE_PATH, Sets.mutable.<String>empty());
        String m3SourceId = "/platform/pure/m3.pure";
        this.loader.loadFile(m3SourceId);

        Verify.assertSetsEqual(Sets.mutable.with(m3SourceId).union(alreadyLoaded), this.loader.getLoadedFiles().collect(PureRepositoryJarTools.BINARY_PATH_TO_PURE_PATH, Sets.mutable.<String>empty()));
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

        for (CoreInstance instance : this.runtime.getSourceById(m3SourceId).getNewInstances())
        {
            String path = PackageableElement.getUserPathForPackageableElement(instance);
            if (M3Paths.Root.equals(path))
            {
                path = "::";
            }
            Assert.assertNotNull(path, this.runtime2.getCoreInstance(path));
        }

        Verify.assertSetsEqual(this.repository.getTopLevels().collect(CoreInstance.GET_NAME).toSet(), this.repository2.getTopLevels().collect(CoreInstance.GET_NAME).toSet());
    }

    @Test
    public void testLoadCollection()
    {
        String m3SourceId = "/platform/pure/m3.pure";
        String collectionSourceId = "/platform/pure/collection.pure";

        this.loader.loadFile(collectionSourceId);
        MutableSet<String> loadedFiles = this.loader.getLoadedFiles().collect(PureRepositoryJarTools.BINARY_PATH_TO_PURE_PATH, Sets.mutable.<String>empty());

        Verify.assertContains(collectionSourceId, loadedFiles);
        assertSourcesEqual(collectionSourceId);

        Verify.assertContains(m3SourceId, loadedFiles);
        assertSourcesEqual(m3SourceId);

        Verify.assertSetsEqual(this.repository.getTopLevels().collect(CoreInstance.GET_NAME).toSet(), this.repository2.getTopLevels().collect(CoreInstance.GET_NAME).toSet());
    }

    @Test
    public void testFunctionsByNameCache()
    {

    }

    private void assertSourcesEqual(String sourceId)
    {
        Source source1 = this.runtime.getSourceById(sourceId);
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

        Verify.assertSetsEqual(patterns1.collect(PurePattern.GET_SRC_PATTERN, Sets.mutable.<String>empty()), patterns2.collect(PurePattern.GET_SRC_PATTERN, Sets.mutable.<String>empty()));
        Verify.assertSetsEqual(patterns1.collect(PurePattern.GET_REAL_PATTERN, Sets.mutable.<String>empty()), patterns2.collect(PurePattern.GET_REAL_PATTERN, Sets.mutable.<String>empty()));
        Verify.assertSetsEqual(patterns1.collect(Functions.chain(PurePattern.GET_FUNCTION, PackageableElement.GET_USER_PATH), Sets.mutable.<String>empty()), patterns2.collect(Functions.chain(PurePattern.GET_FUNCTION, PackageableElement.GET_USER_PATH), Sets.mutable.<String>empty()));
    }

    private MutableMap<String, ListIterable<String>> getElementsByParser(Source source)
    {
        MutableMap<String, ListIterable<String>> result = Maps.mutable.empty();
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        for (Parser parser : elementsByParser.keysView())
        {
            result.put(parser.getName(), elementsByParser.get(parser).collect(PackageableElement.GET_USER_PATH));
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
