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

import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

public class TestBinarySourceSerializer extends AbstractPureTestWithCoreCompiledPlatform
{
    @Test
    public void testPlatform()
    {
        testSerializationForCurrentRuntime();
    }

    @Test
    public void testWithEmptyFile()
    {
        compileTestSource("/test/emptySource.pure", "");
        Assert.assertEquals("", this.runtime.getSourceById("/test/emptySource.pure").getContent());
        testSerializationForCurrentRuntime();
    }

    @Test
    public void testWithUncompiledFile()
    {
        this.runtime.createInMemorySource("/test/uncompiledSource.pure", "Class test::Class1 {}");
        Assert.assertFalse(this.runtime.getSourceById("/test/uncompiledSource.pure").isCompiled());
        testSerializationForCurrentRuntime();
    }

    private void testSerializationForCurrentRuntime()
    {
        ByteArrayOutputStream serialization = new ByteArrayOutputStream();
        BinarySourceSerializer.serialize(serialization, this.runtime.getSourceRegistry());

        SourceRegistry sourceRegistry = this.runtime.getSourceRegistry();
        MutableIntObjectMap<CoreInstance> instancesById = IntObjectMaps.mutable.empty();
        MutableSet<CoreInstance> classifiers = Sets.mutable.empty();
        for (Source source : sourceRegistry.getSources())
        {
            ListIterable<? extends CoreInstance> newInstances = source.getNewInstances();
            if (newInstances != null)
            {
                for (CoreInstance instance : newInstances)
                {
                    instancesById.put(instance.getSyntheticId(), instance);
                    classifiers.add(instance.getClassifier());
                }
            }
        }

        SourceRegistry targetRegistry = new SourceRegistry(this.runtime.getCodeStorage(), this.runtime.getIncrementalCompiler().getParserLibrary());
        Context targetContext = new Context();
        BinarySourceSerializer.build(serialization.toByteArray(), targetRegistry, instancesById, this.runtime.getIncrementalCompiler().getParserLibrary(), targetContext);

        Assert.assertEquals(sourceRegistry.getSourceIds().toSet(), targetRegistry.getSourceIds().toSet());
        for (Source source : sourceRegistry.getSources())
        {
            Source targetSource = targetRegistry.getSource(source.getId());
            Assert.assertEquals(source.getId(), source.getContent(), targetSource.getContent());
            Assert.assertEquals(source.getId(), source.isImmutable(), targetSource.isImmutable());
            Assert.assertEquals(source.getId(), source.isCompiled(), targetSource.isCompiled());
            Assert.assertEquals(getElementsByParserName(source), getElementsByParserName(targetSource));
        }
        for (CoreInstance classifier : classifiers)
        {
            Assert.assertEquals(PackageableElement.getUserPathForPackageableElement(classifier), this.context.getClassifierInstances(classifier), targetContext.getClassifierInstances(classifier));
        }
    }

    private ListMultimap<String, CoreInstance> getElementsByParserName(Source source)
    {
        ListMultimap<Parser, CoreInstance> elementsByParser = source.getElementsByParser();
        if (elementsByParser == null)
        {
            return null;
        }

        final MutableListMultimap<String, CoreInstance> elementsByParserName = Multimaps.mutable.list.empty();
        elementsByParser.forEachKeyMultiValues(new Procedure2<Parser, Iterable<CoreInstance>>()
        {
            @Override
            public void value(Parser parser, Iterable<CoreInstance> elements)
            {
                elementsByParserName.putAll(parser.getName(), elements);
            }
        });
        return elementsByParserName;
    }
}
