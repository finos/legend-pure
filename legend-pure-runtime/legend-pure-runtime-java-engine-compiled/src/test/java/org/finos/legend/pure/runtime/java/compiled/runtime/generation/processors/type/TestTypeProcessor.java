// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.runtime.generation.processors.type;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.empty.EmptyCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTypeProcessor extends AbstractPureTestWithCoreCompiled
{
    @BeforeClass
    public static void setUp()
    {
        RichIterable<? extends CodeRepository> repositories = AbstractPureTestWithCoreCompiled.getCodeRepositories();
        MutableRepositoryCodeStorage codeStorage = new CompositeCodeStorage(
                new ClassLoaderCodeStorage(repositories),
                new EmptyCodeStorage(new GenericCodeRepository("test", "test::.*", "platform")));

        setUpRuntime(codeStorage, getExtra());
    }

    public static Pair<String, String> getExtra()
    {
        return Tuples.pair(
                "/test/compiledgen/tests.pure",
                "Class test::generation::compiled::SimpleClass\n" +
                        "{\n" +
                        "}\n" +
                        "\n" +
                        "Class test::generation::compiled::extra::ExtraClass\n" +
                        "{\n" +
                        "}\n"
        );
    }

    @Test
    public void testJavaInterfaceForType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass", TypeProcessor.javaInterfaceForType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass", TypeProcessor.javaInterfaceForType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class", TypeProcessor.javaInterfaceForType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association", TypeProcessor.javaInterfaceForType(getElement(M3Paths.Association), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration", TypeProcessor.javaInterfaceForType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum", TypeProcessor.javaInterfaceForType(getElement(M3Paths.Enum), processorSupport));
    }

    @Test
    public void testJavaInterfaceNameForType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass", TypeProcessor.javaInterfaceNameForType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass", TypeProcessor.javaInterfaceNameForType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Class", TypeProcessor.javaInterfaceNameForType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Association", TypeProcessor.javaInterfaceNameForType(getElement(M3Paths.Association), processorSupport));
        Assert.assertEquals("Enumeration", TypeProcessor.javaInterfaceNameForType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Enum", TypeProcessor.javaInterfaceNameForType(getElement(M3Paths.Enum), processorSupport));
    }

    @Test
    public void testFullyQualifiedJavaInterfaceNameForType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass", TypeProcessor.fullyQualifiedJavaInterfaceNameForType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass", TypeProcessor.fullyQualifiedJavaInterfaceNameForType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class", TypeProcessor.fullyQualifiedJavaInterfaceNameForType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association", TypeProcessor.fullyQualifiedJavaInterfaceNameForType(getElement(M3Paths.Association), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration", TypeProcessor.fullyQualifiedJavaInterfaceNameForType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum", TypeProcessor.fullyQualifiedJavaInterfaceNameForType(getElement(M3Paths.Enum), processorSupport));
    }

    @SuppressWarnings("unchecked")
    private static <T extends PackageableElement> T getElement(String userPath)
    {
        CoreInstance element = runtime.getCoreInstance(userPath);
        Assert.assertNotNull(userPath, element);
        return (T) element;
    }
}
