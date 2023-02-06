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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.PlatformCodeRepository;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

public class TestBinarySourceSerializer extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), PureCodeStorage.createCodeStorage(getCodeStorageRoot(), getCodeRepositories()), getFactoryRegistryOverride(), getOptions(), getExtra(), false);
    }

    protected static RichIterable<? extends CodeRepository> getCodeRepositories()
    {
        return Lists.immutable.with(CodeRepository.newPlatformCodeRepository(),
                GenericCodeRepository.build("test", "test(::.*)?", PlatformCodeRepository.NAME));
    }

    @Test
    public void testPlatform()
    {
        testSerializationForCurrentRuntime();
    }

    @Test
    public void testWithEmptyFile()
    {
        compileTestSource("/test/emptySource.pure", "");
        Assert.assertEquals("", runtime.getSourceById("/test/emptySource.pure").getContent());
        testSerializationForCurrentRuntime();
    }

    @Test
    public void testWithUncompiledFile()
    {
        runtime.createInMemorySource("/test/uncompiledSource.pure", "Class test::Class1 {}");
        Assert.assertFalse(runtime.getSourceById("/test/uncompiledSource.pure").isCompiled());
        testSerializationForCurrentRuntime();
    }

    private void testSerializationForCurrentRuntime()
    {
        ByteArrayOutputStream serialization = new ByteArrayOutputStream();
        BinarySourceSerializer.serialize(serialization, runtime.getSourceRegistry());

        SourceRegistry sourceRegistry = runtime.getSourceRegistry();
        MutableIntObjectMap<CoreInstance> instancesById = IntObjectMaps.mutable.empty();
        MutableSet<CoreInstance> classifiers = Sets.mutable.empty();
        sourceRegistry.getSources().forEach(source ->
        {
            ListIterable<? extends CoreInstance> newInstances = source.getNewInstances();
            if (newInstances != null)
            {
                newInstances.forEach(instance ->
                {
                    instancesById.put(instance.getSyntheticId(), instance);
                    classifiers.add(instance.getClassifier());
                });
            }
        });

        SourceRegistry targetRegistry = new SourceRegistry(runtime.getCodeStorage(), runtime.getIncrementalCompiler().getParserLibrary());
        Context targetContext = new Context();
        BinarySourceSerializer.build(serialization.toByteArray(), targetRegistry, instancesById, runtime.getIncrementalCompiler().getParserLibrary(), targetContext);

        Assert.assertEquals(sourceRegistry.getSourceIds().toSet(), targetRegistry.getSourceIds().toSet());
        sourceRegistry.getSources().forEach(source ->
        {
            Source targetSource = targetRegistry.getSource(source.getId());
            Assert.assertEquals(source.getId(), source.getContent(), targetSource.getContent());
            Assert.assertEquals(source.getId(), source.isImmutable(), targetSource.isImmutable());
            Assert.assertEquals(source.getId(), source.isCompiled(), targetSource.isCompiled());
            Assert.assertEquals(getElementsByParserName(source), getElementsByParserName(targetSource));
        });
        classifiers.forEach(classifier -> Assert.assertEquals(PackageableElement.getUserPathForPackageableElement(classifier), context.getClassifierInstances(classifier), targetContext.getClassifierInstances(classifier)));
    }

    private ListMultimap<String, CoreInstance> getElementsByParserName(Source source)
    {
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        if (elementsByParser == null)
        {
            return null;
        }

        MutableListMultimap<String, CoreInstance> elementsByParserName = Multimaps.mutable.list.empty();
        elementsByParser.forEachKeyMultiValues((parser, elements) -> elementsByParserName.putAll(parser.getName(), elements));
        return elementsByParserName;
    }
}
