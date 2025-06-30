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

package org.finos.legend.pure.runtime.java.compiled.runtime.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.empty.EmptyCodeStorage;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
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
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildImplClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_Impl", JavaPackageAndImportBuilder.buildImplClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyImplClassNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromUserPath(M3Paths.Class));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyImplClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyConcreteElementClassNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Package_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath(M3Paths.Package));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath(M3Paths.Class));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyConcreteElementClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Package_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getElement(M3Paths.Package), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyConcreteElementCompClassNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Package_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath(M3Paths.Package));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath(M3Paths.Class));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyConcreteElementCompClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Package_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getElement(M3Paths.Package), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyComponentInstanceClassNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath(M3Paths.Class));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyComponentInstanceClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyComponentInstanceCompClassNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromUserPath(M3Paths.Class));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyComponentInstanceCompClassNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Class_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enumeration_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyVirtualPackageClassName()
    {
        Assert.assertEquals("Package_LazyVirtual", JavaPackageAndImportBuilder.buildLazyVirtualPackageClassName());
    }

    @Test
    public void testBuildImplClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildImplClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_Impl", JavaPackageAndImportBuilder.buildImplClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyImplClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyImplClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyImpl", JavaPackageAndImportBuilder.buildLazyImplClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyConcreteElementClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Package_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath(M3Paths.Package));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyConcreteElementClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Package_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getElement(M3Paths.Package), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcrete", JavaPackageAndImportBuilder.buildLazyConcreteElementClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyConcreteElementCompClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Package_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath(M3Paths.Package));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyConcreteElementCompClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Package_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getElement(M3Paths.Package), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyConcreteComp", JavaPackageAndImportBuilder.buildLazyConcreteElementCompClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyComponentInstanceClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyComponentInstanceClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponent", JavaPackageAndImportBuilder.buildLazyComponentInstanceClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyComponentInstanceCompClassReferenceFromUserPath()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath(M3Paths.Class));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath(M3Paths.Enumeration));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildLazyComponentInstanceCompClassReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Enumeration_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium_LazyComponentComp", JavaPackageAndImportBuilder.buildLazyComponentInstanceCompClassReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildLazyVirtualPackageClassReference()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Package_LazyVirtual", JavaPackageAndImportBuilder.buildLazyVirtualPackageClassReference());
    }

    @Test
    public void testBuildInterfaceNameFromUserPath()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("test::generation::compiled::SimpleClass"));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("test::generation::compiled::extra::ExtraClass"));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enum", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath(M3Paths.Enum));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium", JavaPackageAndImportBuilder.buildInterfaceNameFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
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
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Pes"));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium", JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("meta::pure::functions::meta::tests::model::RomanLength~Stadium"));
    }

    @Test
    public void testBuildInterfaceNameFromType()
    {
        Assert.assertEquals("Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("Root_meta_pure_metamodel_type_Enum", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement(M3Paths.Enum), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getElement("meta::pure::functions::meta::tests::model::RomanLength"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Pes", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium", JavaPackageAndImportBuilder.buildInterfaceNameFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @Test
    public void testBuildInterfaceReferenceFromType()
    {
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_SimpleClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement("test::generation::compiled::SimpleClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_test_generation_compiled_extra_ExtraClass", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement("test::generation::compiled::extra::ExtraClass"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement(M3Paths.Class), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement(M3Paths.Enumeration), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getElement("meta::pure::functions::meta::tests::model::RomanLength"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Pes", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Pes"), processorSupport));
        Assert.assertEquals("org.finos.legend.pure.generated.Root_meta_pure_functions_meta_tests_model_RomanLength$Stadium", JavaPackageAndImportBuilder.buildInterfaceReferenceFromType(getUnit("meta::pure::functions::meta::tests::model::RomanLength~Stadium"), processorSupport));
    }

    @SuppressWarnings("unchecked")
    private static <T extends PackageableElement> T getElement(String userPath)
    {
        CoreInstance element = runtime.getCoreInstance(userPath);
        Assert.assertNotNull(userPath, element);
        return (T) element;
    }

    private static Unit getUnit(String unitPath)
    {
        Unit unit = (Unit) org.finos.legend.pure.m3.navigation.measure.Measure.getUnitByUserPath(unitPath, processorSupport);
        Assert.assertNotNull(unitPath, unit);
        return unit;
    }
}
