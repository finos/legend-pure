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

package org.finos.legend.pure.m3.navigation.graph;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.test.Verify;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGraphPathIterable extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(
                getFunctionExecution(),
                new CompositeCodeStorage(new ClassLoaderCodeStorage(CodeRepositoryProviderHelper.findPlatformCodeRepository(), GenericCodeRepository.build("test", "test(::.*)?", "platform"))),
                getFactoryRegistryOverride(),
                getOptions(),
                Tuples.pair("/test/_testModel.pure",
                        "import test::domain::*;\n" +
                                "Class test::domain::ClassA\n" +
                                "{\n" +
                                "  prop1 : String[1];\n" +
                                "  prop2 : ClassB[*];\n" +
                                "}\n" +
                                "\n" +
                                "Class test::domain::ClassB\n" +
                                "{\n" +
                                "  prop3 : String[0..1];\n" +
                                "}\n")
        );
    }

    @Test
    public void testFromPackage()
    {
        MutableSet<String> paths = GraphPathIterable.build("test::domain", GraphPathFilters.getMaxPathLengthFilter(1), null, processorSupport)
                .collect(rgp -> rgp.getGraphPath().getDescription())
                .toSet();
        Verify.assertContainsAll(
                paths,
                "test::domain.package", "test::domain.name", "test::domain", "test::domain.children[0]", "test::domain.children[1]");
    }

    @Test
    public void testClassAToClassB()
    {
        CoreInstance classB = runtime.getCoreInstance("test::domain::ClassB");
        MutableSet<String> paths = GraphPathIterable.build("test::domain::ClassA", GraphPathFilters.builder().withMaxPathLength(5).stopAtNode(classB).build(), null, processorSupport)
                .select(rgp -> classB.equals(rgp.getLastResolvedNode()))
                .collect(rgp -> rgp.getGraphPath().getDescription())
                .toSet();
        Verify.assertSetsEqual(
                Sets.mutable.with("test::domain::ClassA.package.children[1]", "test::domain::ClassA.properties[1].genericType.rawType.resolvedNode", "test::domain::ClassA.properties[1].classifierGenericType.typeArguments[1].rawType.resolvedNode"),
                paths);
    }

    @Test
    public void testFromClassA()
    {
        MutableSet<String> paths = GraphPathIterable.build("test::domain::ClassA", GraphPathFilters.getStopAtPackagedOrTopLevel(processorSupport), (rgp, p) -> !M3Properties._package.equals(p), processorSupport).collect(rgp -> rgp.getGraphPath().getDescription()).toSet();
        Verify.assertContainsAll(
                paths,
                "test::domain::ClassA.properties[1].genericType.rawType.resolvedNode", "test::domain::ClassA.properties[1].classifierGenericType.typeArguments[1].rawType.resolvedNode");
    }

    @Test
    public void testFromClassAWithLimitedProperties()
    {
        SetIterable<String> allowedProperties = Sets.immutable.with("properties", "genericType", "rawType");
        MutableSet<String> paths = GraphPathIterable.build("test::domain::ClassA", GraphPathFilters.getStopAtPackagedOrTopLevel(processorSupport), (rgp, p) -> allowedProperties.contains(p), processorSupport)
                .collect(rgp -> rgp.getGraphPath().getDescription())
                .toSet();
        Verify.assertSetsEqual(
                Sets.mutable.with("test::domain::ClassA", "test::domain::ClassA.properties[0]", "test::domain::ClassA.properties[0].genericType", "test::domain::ClassA.properties[0].genericType.rawType", "test::domain::ClassA.properties[1]", "test::domain::ClassA.properties[1].genericType", "test::domain::ClassA.properties[1].genericType.rawType"),
                paths);
    }

    @Test
    public void testFromProp1()
    {
        SetIterable<String> allowedProperties = Sets.immutable.with("properties", "genericType", "rawType");
        MutableSet<String> paths = GraphPathIterable.builder(processorSupport)
                .withStartPath("test::domain::ClassA.properties[0]")
                .withPathFilter(GraphPathFilters.getStopAtPackagedOrTopLevel(processorSupport))
                .withPropertyFilter((rgp, p) -> allowedProperties.contains(p))
                .build()
                .collect(rgp -> rgp.getGraphPath().getDescription())
                .toSet();
        Verify.assertSetsEqual(
                Sets.mutable.with("test::domain::ClassA.properties[0]", "test::domain::ClassA.properties[0].genericType", "test::domain::ClassA.properties[0].genericType.rawType"),
                paths
        );
    }
}
