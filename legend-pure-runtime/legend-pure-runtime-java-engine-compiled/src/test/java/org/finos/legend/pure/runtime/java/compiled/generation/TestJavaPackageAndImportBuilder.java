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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJavaPackageAndImportBuilder extends AbstractPureTestWithCoreCompiled
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
    public void testRootPackage()
    {
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.rootPackage());
    }

    @Test
    public void testPlatformJavaPackage()
    {
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.platformJavaPackage());
    }

    @Test
    public void testExternalizablePackage()
    {
        Assert.assertEquals("org.finos.legend.pure", JavaPackageAndImportBuilder.externalizablePackage());
    }

    @Test
    public void testBuildImports()
    {
        Assert.assertEquals("", JavaPackageAndImportBuilder.buildImports(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("", JavaPackageAndImportBuilder.buildImports(getElement("test::generation::compiled::extra::ExtraClass")));
    }

    @Test
    public void testBuildPackageFromPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageFromUserPath("test"));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageFromUserPath("test::generation"));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageFromUserPath("test::generation::compiled"));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageFromUserPath("test::generation::compiled::extra::ExtraClass"));
    }

    @Test
    public void testBuildPackageForPackageableElement()
    {
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageForPackageableElement(getElement("test")));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageForPackageableElement(getElement("test::generation")));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageForPackageableElement(getElement("test::generation::compiled")));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageForPackageableElement(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("org.finos.legend.pure.generated", JavaPackageAndImportBuilder.buildPackageForPackageableElement(getElement("test::generation::compiled::extra::ExtraClass")));
    }

    @Test
    public void testBuildImplClassNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromUserPath(M3Paths.Class));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromUserPath(M3Paths.Enumeration));
    }

    @Test
    public void testBuildImplClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement(M3Paths.Class)));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement(M3Paths.Enumeration)));
    }

    @Test
    public void testBuildLazyImplClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement(M3Paths.Class)));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement(M3Paths.Enumeration)));
    }

    @Test
    public void testBuildImplClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Enumeration));
    }

    @Test
    public void testBuildImplClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement(M3Paths.Class)));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement(M3Paths.Enumeration)));
    }

    @Test
    public void testBuildLazyImplClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement(M3Paths.Class)));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement(M3Paths.Enumeration)));
    }

    @Test
    public void testBuildInterfaceNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enum", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath(M3Paths.Enum));
    }

    @Test
    public void testBuildInterfaceReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.test.generation.compiled.SimpleClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("test::generation::compiled::SimpleClass", Sets.immutable.with("test::generation::compiled::SimpleClass", "test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.test.generation.compiled.extra.ExtraClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("test::generation::compiled::extra::ExtraClass", Sets.immutable.with("test::generation::compiled::SimpleClass", "test::generation::compiled::extra::ExtraClass")));
    }

    @Test
    public void testBuildInterfaceNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement("test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enum", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement(M3Paths.Enum)));
    }

    @Test
    public void testBuildInterfaceReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement("test::generation::compiled::SimpleClass")));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass")));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement(M3Paths.Class)));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement(M3Paths.Enumeration)));
    }

    @Test
    public void testBuildImplUnitInstanceClassNameFromType()
    {
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength_Tilde_Cubitum_Instance_Impl", JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(getElement("meta::pure::functions::meta::tests::model::RomanLength~Cubitum")));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength_Tilde_Pes_Instance_Impl", JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(getElement("meta::pure::functions::meta::tests::model::RomanLength~Pes")));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength_Tilde_Actus_Instance_Impl", JavaPackageAndImportBuilder.buildImplUnitInstanceClassNameFromType(getElement("meta::pure::functions::meta::tests::model::RomanLength~Actus")));
    }

    @SuppressWarnings("unchecked")
    private static <T extends PackageableElement> T getElement(String userPath)
    {
        CoreInstance element = runtime.getCoreInstance(userPath);
        Assert.assertNotNull(userPath, element);
        return (T) element;
    }
}
