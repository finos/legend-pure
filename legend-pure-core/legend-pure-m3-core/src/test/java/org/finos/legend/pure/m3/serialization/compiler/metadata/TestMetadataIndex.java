// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.junit.Assert;
import org.junit.Test;

public class TestMetadataIndex extends AbstractMetadataTest
{
    @Test
    public void testEmptyIndex()
    {
        MetadataIndex index = MetadataIndex.builder().build();
        Assert.assertFalse(index.hasModule("non_existent_module"));
        Assert.assertNull(index.getModule("non_existent_module"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllModuleNames()));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllModules()));

        Assert.assertEquals(0, index.getElementCount());
        Assert.assertFalse(index.hasElement("non::existent::element"));
        Assert.assertNull(index.getElement("non::existent::element"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllElementPaths()));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllElements()));

        Assert.assertFalse(index.hasSource("/non/existent/source.pure"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllSources()));
        Assert.assertNull(index.getSourceElements("/non/existent/source.pure"));

        Assert.assertFalse(index.hasClassifier("non::existent::classifier"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllClassifiers()));
        Assert.assertEquals(Lists.immutable.empty(), index.getClassifierElements("non::existent::classifier"));

        Assert.assertEquals(Lists.immutable.empty(), index.getTopLevelElements());
        Assert.assertFalse(index.hasPackage("non::existent::package"));
        Assert.assertNull(index.getPackageMetadata("non::existent::package"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllPackagePaths()));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllPackageMetadata()));
        Assert.assertNull(index.getPackageChildren("non::existent::package"));

        Assert.assertNull(index.getElementModuleName("non::existent::element"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getBackReferenceModuleNames("non::existent::element")));
    }

    @Test
    public void testSingleEmptyModule()
    {
        ModuleManifest emptyModule = ModuleManifest.builder("empty_module").build();

        MetadataIndex index = MetadataIndex.builder().withModule(emptyModule).build();
        Assert.assertTrue(index.hasModule(emptyModule.getModuleName()));
        Assert.assertFalse(index.hasModule("non_existent_module"));
        Assert.assertSame(emptyModule, index.getModule(emptyModule.getModuleName()));
        Assert.assertNull(index.getModule("non_existent_module"));
        Assert.assertEquals(Lists.fixedSize.with(emptyModule.getModuleName()), Lists.mutable.withAll(index.getAllModuleNames()));
        Assert.assertEquals(Lists.fixedSize.with(emptyModule), Lists.mutable.withAll(index.getAllModules()));

        Assert.assertEquals(0, index.getElementCount());
        Assert.assertFalse(index.hasElement("non::existent::element"));
        Assert.assertNull(index.getElement("non::existent::element"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllElementPaths()));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllElements()));

        Assert.assertFalse(index.hasSource("/non/existent/source.pure"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllSources()));
        Assert.assertNull(index.getSourceElements("/non/existent/source.pure"));

        Assert.assertFalse(index.hasClassifier("non::existent::classifier"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllClassifiers()));
        Assert.assertEquals(Lists.immutable.empty(), index.getClassifierElements("non::existent::classifier"));

        Assert.assertEquals(Lists.immutable.empty(), index.getTopLevelElements());
        Assert.assertFalse(index.hasPackage("non::existent::package"));
        Assert.assertNull(index.getPackageMetadata("non::existent::package"));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllPackagePaths()));
        Assert.assertEquals(Lists.fixedSize.empty(), Lists.mutable.withAll(index.getAllPackageMetadata()));
        Assert.assertNull(index.getPackageChildren("non::existent::package"));

        Assert.assertNull(index.getElementModuleName("non::existent::element"));
        Assert.assertEquals(Lists.mutable.with(emptyModule.getModuleName()), Lists.mutable.withAll(index.getBackReferenceModuleNames("non::existent::element")).sortThis());
    }

    @Test
    public void testSingleModule()
    {
        String classesSource = "/test_module/model/classes.pure";
        String associationsSource = "/test_module/model/associations.pure";
        String enumsSource = "/test_module/model/enums.pure";

        ConcreteElementMetadata mySimpleClass = newClass("model::test::classes::MySimpleClass", classesSource, 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::test::classes::MyOtherClass", classesSource, 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::test::classes::MyThirdClass", classesSource, 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::test::associations::SimpleToOther", associationsSource, 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::test::associations::SimpleToThird", associationsSource, 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::test::associations::OtherToThird", associationsSource, 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::test::enums::MyFirstEnumeration", enumsSource, 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::test::enums::MySecondEnumeration", enumsSource, 8, 1, 10, 1);
        MutableList<ConcreteElementMetadata> allElements = Lists.mutable.with(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration).sortThisBy(PackageableElementMetadata::getPath);

        VirtualPackageMetadata rootPackage = newVirtualPackage(M3Paths.Root);
        VirtualPackageMetadata modelPackage = newVirtualPackage("model");
        VirtualPackageMetadata testPackage = newVirtualPackage("model::test");
        VirtualPackageMetadata classesPackage = newVirtualPackage("model::test::classes");
        VirtualPackageMetadata associationsPackage = newVirtualPackage("model::test::associations");
        VirtualPackageMetadata enumsPackage = newVirtualPackage("model::test::enums");
        MutableList<VirtualPackageMetadata> allPackages = Lists.mutable.with(rootPackage, modelPackage, testPackage, classesPackage, associationsPackage, enumsPackage).sortThisBy(PackageableElementMetadata::getPath);

        ModuleManifest testModule = ModuleManifest.builder("test_module")
                .withElements(allElements)
                .build();
        MetadataIndex index = MetadataIndex.builder().withModule(testModule).build();
        Assert.assertTrue(index.hasModule(testModule.getModuleName()));
        Assert.assertFalse(index.hasModule("non_existent_module"));
        Assert.assertSame(testModule, index.getModule(testModule.getModuleName()));
        Assert.assertNull(index.getModule("non_existent_module"));
        Assert.assertEquals(Lists.fixedSize.with(testModule.getModuleName()), Lists.mutable.withAll(index.getAllModuleNames()));
        Assert.assertEquals(Lists.fixedSize.with(testModule), Lists.mutable.withAll(index.getAllModules()));

        Assert.assertEquals(allElements.size(), index.getElementCount());
        Assert.assertFalse(index.hasElement("non::existent::element"));
        Assert.assertNull(index.getElement("non::existent::element"));
        allElements.forEach(elt -> Assert.assertTrue(elt.getClassifierPath(), index.hasElement(elt.getPath())));
        Assert.assertEquals(allElements.collect(PackageableElementMetadata::getPath), Lists.mutable.withAll(index.getAllElementPaths()).sortThis());
        Assert.assertEquals(allElements, Lists.mutable.withAll(index.getAllElements()).sortThisBy(PackageableElementMetadata::getPath));

        Assert.assertFalse(index.hasSource("/non/existent/source.pure"));
        Assert.assertTrue(index.hasSource(classesSource));
        Assert.assertTrue(index.hasSource(associationsSource));
        Assert.assertTrue(index.hasSource(enumsSource));
        Assert.assertEquals(Lists.mutable.with(associationsSource, classesSource, enumsSource), Lists.mutable.withAll(index.getAllSources()).sortThis());
        Assert.assertNull(index.getSourceElements("/non/existent/source.pure"));
        Assert.assertEquals(Lists.immutable.with(mySimpleClass, myOtherClass, myThirdClass), index.getSourceElements(classesSource));
        Assert.assertEquals(Lists.immutable.with(simpleToOther, simpleToThird, otherToThird), index.getSourceElements(associationsSource));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration, mySecondEnumeration), index.getSourceElements(enumsSource));

        Assert.assertFalse(index.hasClassifier("non::existent::classifier"));
        Assert.assertTrue(index.hasClassifier(M3Paths.Class));
        Assert.assertTrue(index.hasClassifier(M3Paths.Association));
        Assert.assertTrue(index.hasClassifier(M3Paths.Enumeration));
        Assert.assertEquals(Lists.mutable.with(M3Paths.Class, M3Paths.Association, M3Paths.Enumeration).sortThis(), Lists.mutable.withAll(index.getAllClassifiers()).sortThis());
        Assert.assertEquals(Lists.immutable.empty(), Lists.mutable.withAll(index.getClassifierElements("non::existent::classifier")));
        Assert.assertEquals(Lists.immutable.with(myOtherClass, mySimpleClass, myThirdClass), index.getClassifierElements(M3Paths.Class));
        Assert.assertEquals(Lists.immutable.with(otherToThird, simpleToOther, simpleToThird), index.getClassifierElements(M3Paths.Association));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration, mySecondEnumeration), index.getClassifierElements(M3Paths.Enumeration));

        Assert.assertEquals(Lists.immutable.empty(), index.getTopLevelElements());
        Assert.assertFalse(index.hasPackage("non::existent::package"));
        Assert.assertNull(index.getPackageMetadata("non::existent::package"));
        Assert.assertEquals(allPackages.collect(PackageableElementMetadata::getPath), Lists.mutable.withAll(index.getAllPackagePaths()).sortThis());
        Assert.assertEquals(allPackages, Lists.mutable.withAll(index.getAllPackageMetadata()).sortThisBy(PackageableElementMetadata::getPath));
        Assert.assertNull(index.getPackageChildren("non::existent::package"));
        Assert.assertEquals(Lists.immutable.with(modelPackage), index.getPackageChildren(M3Paths.Root));
        Assert.assertEquals(Lists.immutable.with(testPackage), index.getPackageChildren("model"));
        Assert.assertEquals(Lists.immutable.with(associationsPackage, classesPackage, enumsPackage), index.getPackageChildren("model::test"));
        Assert.assertEquals(Lists.immutable.with(otherToThird, simpleToOther, simpleToThird), index.getPackageChildren("model::test::associations"));
        Assert.assertEquals(Lists.immutable.with(myOtherClass, mySimpleClass, myThirdClass), index.getPackageChildren("model::test::classes"));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration, mySecondEnumeration), index.getPackageChildren("model::test::enums"));

        // Element module names
        allElements.forEach(elt -> Assert.assertEquals(elt.getPath(), testModule.getModuleName(), index.getElementModuleName(elt.getPath())));
        Assert.assertNull(index.getElementModuleName("non::existent::element"));

        // Back-reference module names: single module with no dependencies, so every element maps to just that module
        allElements.forEach(elt -> Assert.assertEquals(elt.getPath(), Lists.mutable.with(testModule.getModuleName()), Lists.mutable.withAll(index.getBackReferenceModuleNames(elt.getPath())).sortThis()));
        // Virtual packages fall back to all modules
        Assert.assertEquals(Lists.mutable.with(testModule.getModuleName()), Lists.mutable.withAll(index.getBackReferenceModuleNames("model::test")).sortThis());
    }

    @Test
    public void testMultipleModules()
    {
        String fakeM3 = "/platform/m3.pure";
        ConcreteElementMetadata fakeRoot = newElement(M3Paths.Root, M3Paths.Package, fakeM3, 1, 1, 10, 1);
        MutableList<ConcreteElementMetadata> fakePlatformElements = Lists.mutable.with(fakeRoot);

        ModuleManifest fakePlatformModule = ModuleManifest.builder("platform")
                .withElements(fakePlatformElements)
                .build();


        String classesSource = "/test_module/model/classes.pure";
        String associationsSource = "/test_module/model/associations.pure";
        String enumsSource = "/test_module/model/enums.pure";

        ConcreteElementMetadata mySimpleClass = newClass("model::test::classes::MySimpleClass", classesSource, 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::test::classes::MyOtherClass", classesSource, 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::test::classes::MyThirdClass", classesSource, 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::test::associations::SimpleToOther", associationsSource, 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::test::associations::SimpleToThird", associationsSource, 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::test::associations::OtherToThird", associationsSource, 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::test::enums::MyFirstEnumeration", enumsSource, 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::test::enums::MySecondEnumeration", enumsSource, 8, 1, 10, 1);
        MutableList<ConcreteElementMetadata> testModelElements = Lists.mutable.with(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration).sortThisBy(PackageableElementMetadata::getPath);

        ModuleManifest testModule = ModuleManifest.builder("test_module")
                .withElements(testModelElements)
                .build();


        String classesSource2 = "/test_module2/model/classes.pure";
        String associationsSource2 = "/test_module2/model/associations.pure";
        String enumsSource2 = "/test_module2/model/enums.pure";

        ConcreteElementMetadata mySimpleClass2 = newClass("model::test2::domain::classes::MySimpleClass", classesSource2, 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass2 = newClass("model::test2::domain::classes::MyOtherClass", classesSource2, 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass2 = newClass("model::test2::domain::classes::MyThirdClass", classesSource2, 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther2 = newAssociation("model::test2::domain::associations::SimpleToOther", associationsSource2, 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird2 = newAssociation("model::test2::domain::associations::SimpleToThird", associationsSource2, 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird2 = newAssociation("model::test2::domain::associations::OtherToThird", associationsSource2, 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration2 = newEnumeration("model::test2::domain::enums::MyFirstEnumeration", enumsSource2, 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration2 = newEnumeration("model::test2::domain::enums::MySecondEnumeration", enumsSource2, 8, 1, 10, 1);
        MutableList<ConcreteElementMetadata> testModule2Elements = Lists.mutable.with(mySimpleClass2, myOtherClass2, myThirdClass2, simpleToOther2, simpleToThird2, otherToThird2, myFirstEnumeration2, mySecondEnumeration2).sortThisBy(PackageableElementMetadata::getPath);

        ModuleManifest testModule2 = ModuleManifest.builder("test_module2")
                .withElements(testModule2Elements)
                .build();

        MutableList<ConcreteElementMetadata> allElements = Lists.mutable.withAll(fakePlatformElements).withAll(testModelElements).withAll(testModule2Elements).sortThisBy(PackageableElementMetadata::getPath);

        VirtualPackageMetadata modelPackage = newVirtualPackage("model");
        VirtualPackageMetadata testPackage = newVirtualPackage("model::test");
        VirtualPackageMetadata classesPackage = newVirtualPackage("model::test::classes");
        VirtualPackageMetadata associationsPackage = newVirtualPackage("model::test::associations");
        VirtualPackageMetadata enumsPackage = newVirtualPackage("model::test::enums");
        VirtualPackageMetadata test2Package = newVirtualPackage("model::test2");
        VirtualPackageMetadata test2DomainPackage = newVirtualPackage("model::test2::domain");
        VirtualPackageMetadata classes2Package = newVirtualPackage("model::test2::domain::classes");
        VirtualPackageMetadata associations2Package = newVirtualPackage("model::test2::domain::associations");
        VirtualPackageMetadata enums2Package = newVirtualPackage("model::test2::domain::enums");
        MutableList<VirtualPackageMetadata> virtualPackages = Lists.mutable.with(modelPackage, testPackage, classesPackage, associationsPackage, enumsPackage,
                        test2Package, test2DomainPackage, classes2Package, associations2Package, enums2Package)
                .sortThisBy(PackageableElementMetadata::getPath);

        MetadataIndex index = MetadataIndex.builder()
                .withModules(fakePlatformModule, testModule,  testModule2)
                .build();

        Assert.assertTrue(index.hasModule(fakePlatformModule.getModuleName()));
        Assert.assertTrue(index.hasModule(testModule.getModuleName()));
        Assert.assertTrue(index.hasModule(testModule2.getModuleName()));
        Assert.assertFalse(index.hasModule("non_existent_module"));
        Assert.assertSame(fakePlatformModule, index.getModule(fakePlatformModule.getModuleName()));
        Assert.assertSame(testModule, index.getModule(testModule.getModuleName()));
        Assert.assertSame(testModule2, index.getModule(testModule2.getModuleName()));
        Assert.assertNull(index.getModule("non_existent_module"));
        Assert.assertEquals(Lists.fixedSize.with(fakePlatformModule.getModuleName(), testModule.getModuleName(), testModule2.getModuleName()), Lists.mutable.withAll(index.getAllModuleNames()));
        Assert.assertEquals(Lists.fixedSize.with(fakePlatformModule, testModule, testModule2), Lists.mutable.withAll(index.getAllModules()).sortThisBy(ModuleManifest::getModuleName));

        Assert.assertEquals(allElements.size(), index.getElementCount());
        Assert.assertFalse(index.hasElement("non::existent::element"));
        Assert.assertNull(index.getElement("non::existent::element"));
        allElements.forEach(elt -> Assert.assertTrue(elt.getClassifierPath(), index.hasElement(elt.getPath())));
        Assert.assertEquals(allElements.collect(PackageableElementMetadata::getPath), Lists.mutable.withAll(index.getAllElementPaths()).sortThis());
        Assert.assertEquals(allElements, Lists.mutable.withAll(index.getAllElements()).sortThisBy(PackageableElementMetadata::getPath));

        Assert.assertFalse(index.hasSource("/non/existent/source.pure"));
        Assert.assertTrue(index.hasSource(classesSource));
        Assert.assertTrue(index.hasSource(associationsSource));
        Assert.assertTrue(index.hasSource(enumsSource));
        Assert.assertEquals(
                Lists.mutable.with(fakeM3, associationsSource, classesSource, enumsSource, associationsSource2, classesSource2, enumsSource2),
                Lists.mutable.withAll(index.getAllSources()).sortThis());
        Assert.assertNull(index.getSourceElements("/non/existent/source.pure"));
        Assert.assertEquals(Lists.immutable.with(fakeRoot), index.getSourceElements(fakeM3));
        Assert.assertEquals(Lists.immutable.with(mySimpleClass, myOtherClass, myThirdClass), index.getSourceElements(classesSource));
        Assert.assertEquals(Lists.immutable.with(simpleToOther, simpleToThird, otherToThird), index.getSourceElements(associationsSource));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration, mySecondEnumeration), index.getSourceElements(enumsSource));
        Assert.assertEquals(Lists.immutable.with(mySimpleClass2, myOtherClass2, myThirdClass2), index.getSourceElements(classesSource2));
        Assert.assertEquals(Lists.immutable.with(simpleToOther2, simpleToThird2, otherToThird2), index.getSourceElements(associationsSource2));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration2, mySecondEnumeration2), index.getSourceElements(enumsSource2));

        Assert.assertFalse(index.hasClassifier("non::existent::classifier"));
        Assert.assertTrue(index.hasClassifier(M3Paths.Package));
        Assert.assertTrue(index.hasClassifier(M3Paths.Class));
        Assert.assertTrue(index.hasClassifier(M3Paths.Association));
        Assert.assertTrue(index.hasClassifier(M3Paths.Enumeration));
        Assert.assertEquals(Lists.mutable.with(M3Paths.Package, M3Paths.Class, M3Paths.Association, M3Paths.Enumeration).sortThis(), Lists.mutable.withAll(index.getAllClassifiers()).sortThis());
        Assert.assertEquals(Lists.immutable.empty(), Lists.mutable.withAll(index.getClassifierElements("non::existent::classifier")));
        Assert.assertEquals(Lists.immutable.with(fakeRoot), index.getClassifierElements(M3Paths.Package));
        Assert.assertEquals(Lists.immutable.with(myOtherClass2, mySimpleClass2, myThirdClass2, myOtherClass, mySimpleClass, myThirdClass), index.getClassifierElements(M3Paths.Class));
        Assert.assertEquals(Lists.immutable.with(otherToThird2, simpleToOther2, simpleToThird2, otherToThird, simpleToOther, simpleToThird), index.getClassifierElements(M3Paths.Association));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration2, mySecondEnumeration2, myFirstEnumeration, mySecondEnumeration), index.getClassifierElements(M3Paths.Enumeration));

        Assert.assertEquals(Lists.immutable.with(fakeRoot), index.getTopLevelElements());
        Assert.assertFalse(index.hasPackage("non::existent::package"));
        Assert.assertNull(index.getPackageMetadata("non::existent::package"));
        Assert.assertEquals(virtualPackages.collect(PackageableElementMetadata::getPath, Lists.mutable.with(M3Paths.Root)), Lists.mutable.withAll(index.getAllPackagePaths()).sortThis());
        Assert.assertEquals(Lists.mutable.<PackageableElementMetadata>with(fakeRoot).withAll(virtualPackages), Lists.mutable.withAll(index.getAllPackageMetadata()).sortThisBy(PackageableElementMetadata::getPath));
        Assert.assertNull(index.getPackageChildren("non::existent::package"));
        Assert.assertEquals(Lists.immutable.with(modelPackage), index.getPackageChildren(M3Paths.Root));
        Assert.assertEquals(Lists.immutable.with(testPackage, test2Package), index.getPackageChildren("model"));
        Assert.assertEquals(Lists.immutable.with(associationsPackage, classesPackage, enumsPackage), index.getPackageChildren("model::test"));
        Assert.assertEquals(Lists.immutable.with(otherToThird, simpleToOther, simpleToThird), index.getPackageChildren("model::test::associations"));
        Assert.assertEquals(Lists.immutable.with(myOtherClass, mySimpleClass, myThirdClass), index.getPackageChildren("model::test::classes"));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration, mySecondEnumeration), index.getPackageChildren("model::test::enums"));
        Assert.assertEquals(Lists.immutable.with(test2DomainPackage), index.getPackageChildren("model::test2"));
        Assert.assertEquals(Lists.immutable.with(associations2Package, classes2Package, enums2Package), index.getPackageChildren("model::test2::domain"));
        Assert.assertEquals(Lists.immutable.with(otherToThird2, simpleToOther2, simpleToThird2), index.getPackageChildren("model::test2::domain::associations"));
        Assert.assertEquals(Lists.immutable.with(myOtherClass2, mySimpleClass2, myThirdClass2), index.getPackageChildren("model::test2::domain::classes"));
        Assert.assertEquals(Lists.immutable.with(myFirstEnumeration2, mySecondEnumeration2), index.getPackageChildren("model::test2::domain::enums"));
    }

    @Test
    public void testModuleNameConflicts()
    {
        String classesSource = "/test_module/model/classes.pure";
        String associationsSource = "/test_module/model/associations.pure";
        String enumsSource = "/test_module/model/enums.pure";

        ConcreteElementMetadata mySimpleClass = newClass("model::test::classes::MySimpleClass", classesSource, 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::test::classes::MyOtherClass", classesSource, 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::test::classes::MyThirdClass", classesSource, 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::test::associations::SimpleToOther", associationsSource, 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::test::associations::SimpleToThird", associationsSource, 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::test::associations::OtherToThird", associationsSource, 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::test::enums::MyFirstEnumeration", enumsSource, 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::test::enums::MySecondEnumeration", enumsSource, 8, 1, 10, 1);

        ModuleManifest module1 = ModuleManifest.builder("test_module")
                .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird)
                .build();
        ModuleManifest module2 = ModuleManifest.builder("test_module")
                .withElements(myFirstEnumeration, mySecondEnumeration)
                .build();

        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, MetadataIndex.builder().withModule(module1).withModule(module2)::build);
        Assert.assertEquals("Multiple modules named 'test_module'", e.getMessage());
    }

    @Test
    public void testElementPathConflicts()
    {
        String classesSource = "/test_module/model/classes.pure";
        String associationsSource = "/test_module/model/associations.pure";
        String enumsSource = "/test_module/model/enums.pure";

        ConcreteElementMetadata mySimpleClass = newClass("model::test::classes::MySimpleClass", classesSource, 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::test::classes::MyOtherClass", classesSource, 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::test::classes::MyThirdClass", classesSource, 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::test::associations::SimpleToOther", associationsSource, 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::test::associations::SimpleToThird", associationsSource, 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::test::associations::OtherToThird", associationsSource, 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::test::enums::MyFirstEnumeration", enumsSource, 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::test::enums::MySecondEnumeration", enumsSource, 8, 1, 10, 1);

        ModuleManifest module1 = ModuleManifest.builder("test_module")
                .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration)
                .build();

        String classesSource2 = "/test_module2/model/classes.pure";
        String associationsSource2 = "/test_module2/model/associations.pure";
        String enumsSource2 = "/test_module2/model/enums.pure";

        ConcreteElementMetadata mySimpleClass2 = newClass("model::test::classes::MySimpleClass2", classesSource2, 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass2 = newClass("model::test::classes::MyOtherClass2", classesSource2, 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass2 = newClass("model::test::classes::MyThirdClass2", classesSource2, 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther2 = newAssociation("model::test::associations::SimpleToOther", associationsSource2, 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird2 = newAssociation("model::test::associations::SimpleToThird2", associationsSource2, 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird2 = newAssociation("model::test::associations::OtherToThird2", associationsSource2, 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration2 = newEnumeration("model::test::enums::MyFirstEnumeration2", enumsSource2, 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration2 = newEnumeration("model::test::enums::MySecondEnumeration2", enumsSource2, 8, 1, 10, 1);

        ModuleManifest module2 = ModuleManifest.builder("test_module2")
                .withElements(mySimpleClass2, myOtherClass2, myThirdClass2, simpleToOther2, simpleToThird2, otherToThird2, myFirstEnumeration2, mySecondEnumeration2)
                .build();

        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, MetadataIndex.builder().withModule(module1).withModule(module2)::build);
        Assert.assertEquals("Multiple elements with path model::test::associations::SimpleToOther: instance of meta::pure::metamodel::relationship::Association at /test_module/model/associations.pure:2c1-7c1 and instance of meta::pure::metamodel::relationship::Association at /test_module2/model/associations.pure:2c1-7c1", e.getMessage());
    }

    @Test
    public void testElementPackagePathConflicts()
    {
        String classesSource = "/test_module/model/classes.pure";
        ConcreteElementMetadata mySimpleClass = newClass("model::test::classes::MySimpleClass", classesSource, 1, 1, 5, 1);
        ModuleManifest module1 = ModuleManifest.builder("test_module")
                .withElement(mySimpleClass)
                .build();

        String classesSource2 = "/test_module2/model/classes.pure";
        ConcreteElementMetadata conflictingClass = newClass("model::test::classes", classesSource2, 1, 1, 5, 1);
        ModuleManifest module2 = ModuleManifest.builder("test_module2")
                .withElement(conflictingClass)
                .build();

        RuntimeException e = Assert.assertThrows(RuntimeException.class, MetadataIndex.builder().withModule(module1).withModule(module2)::build);
        Assert.assertEquals("Multiple elements with path model::test::classes: instance of meta::pure::metamodel::type::Class at /test_module2/model/classes.pure:1c1-5c1 and instance of Package", e.getMessage());
    }

    @Test
    public void testElementModuleNamesMultipleModules()
    {
        ConcreteElementMetadata platformElement = newElement(M3Paths.Root, M3Paths.Package, "/platform/m3.pure", 1, 1, 10, 1);
        ModuleManifest platformModule = ModuleManifest.builder("platform")
                .withElement(platformElement)
                .build();

        ConcreteElementMetadata testElement = newClass("model::test::MyClass", "/test/classes.pure", 1, 1, 5, 1);
        ModuleManifest testModule = ModuleManifest.builder("test")
                .withDependency("platform")
                .withElement(testElement)
                .build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(platformModule, testModule)
                .build();

        Assert.assertEquals("platform", index.getElementModuleName(M3Paths.Root));
        Assert.assertEquals("test", index.getElementModuleName("model::test::MyClass"));
        Assert.assertNull(index.getElementModuleName("non::existent::element"));
    }

    @Test
    public void testBackReferenceModulesWithDependencies()
    {
        // Dependency chain: A <- B <- C (C depends on B, B depends on A)
        // Also: D depends on A independently
        //
        //    A
        //   / \
        //  B   D
        //  |
        //  C

        ConcreteElementMetadata elementA = newClass("module_a::ElementA", "/a/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleA = ModuleManifest.builder("module_a")
                .withElement(elementA)
                .build();

        ConcreteElementMetadata elementB = newClass("module_b::ElementB", "/b/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleB = ModuleManifest.builder("module_b")
                .withDependency("module_a")
                .withElement(elementB)
                .build();

        ConcreteElementMetadata elementC = newClass("module_c::ElementC", "/c/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleC = ModuleManifest.builder("module_c")
                .withDependency("module_b")
                .withElement(elementC)
                .build();

        ConcreteElementMetadata elementD = newClass("module_d::ElementD", "/d/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleD = ModuleManifest.builder("module_d")
                .withDependency("module_a")
                .withElement(elementD)
                .build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(moduleA, moduleB, moduleC, moduleD)
                .build();

        // For element in A: A itself + B (depends on A) + C (transitively depends on A) + D (depends on A)
        Assert.assertEquals(
                Lists.mutable.with("module_a", "module_b", "module_c", "module_d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("module_a::ElementA")).sortThis());

        // For element in B: B itself + C (depends on B)
        Assert.assertEquals(
                Lists.mutable.with("module_b", "module_c"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("module_b::ElementB")).sortThis());

        // For element in C: C itself only (nothing depends on C)
        Assert.assertEquals(
                Lists.mutable.with("module_c"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("module_c::ElementC")).sortThis());

        // For element in D: D itself only (nothing depends on D)
        Assert.assertEquals(
                Lists.mutable.with("module_d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("module_d::ElementD")).sortThis());

        // Virtual package (not a concrete element): falls back to all modules
        Assert.assertEquals(
                Lists.mutable.with("module_a", "module_b", "module_c", "module_d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("module_a")).sortThis());
    }

    @Test
    public void testBackReferenceModulesWithNoDependencies()
    {
        // Independent modules: A, B, C - no dependencies between them
        ConcreteElementMetadata elementA = newClass("a::Element", "/a/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleA = ModuleManifest.builder("a").withElement(elementA).build();

        ConcreteElementMetadata elementB = newClass("b::Element", "/b/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleB = ModuleManifest.builder("b").withElement(elementB).build();

        ConcreteElementMetadata elementC = newClass("c::Element", "/c/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleC = ModuleManifest.builder("c").withElement(elementC).build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(moduleA, moduleB, moduleC)
                .build();

        // Each element should only map to its own module (no dependencies)
        Assert.assertEquals(Lists.mutable.with("a"), Lists.mutable.withAll(index.getBackReferenceModuleNames("a::Element")).sortThis());
        Assert.assertEquals(Lists.mutable.with("b"), Lists.mutable.withAll(index.getBackReferenceModuleNames("b::Element")).sortThis());
        Assert.assertEquals(Lists.mutable.with("c"), Lists.mutable.withAll(index.getBackReferenceModuleNames("c::Element")).sortThis());
    }

    @Test
    public void testBackReferenceModulesWithDiamondDependency()
    {
        // Diamond: A <- B, A <- C, B <- D, C <- D
        //    A
        //   / \
        //  B   C
        //   \ /
        //    D

        ConcreteElementMetadata elementA = newClass("a::Element", "/a/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleA = ModuleManifest.builder("a").withElement(elementA).build();

        ConcreteElementMetadata elementB = newClass("b::Element", "/b/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleB = ModuleManifest.builder("b").withDependency("a").withElement(elementB).build();

        ConcreteElementMetadata elementC = newClass("c::Element", "/c/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleC = ModuleManifest.builder("c").withDependency("a").withElement(elementC).build();

        ConcreteElementMetadata elementD = newClass("d::Element", "/d/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleD = ModuleManifest.builder("d").withDependencies("b", "c").withElement(elementD).build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(moduleA, moduleB, moduleC, moduleD)
                .build();

        // For element in A: all modules depend on A (directly or transitively)
        Assert.assertEquals(
                Lists.mutable.with("a", "b", "c", "d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("a::Element")).sortThis());

        // For element in B: B itself + D (depends on B)
        Assert.assertEquals(
                Lists.mutable.with("b", "d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("b::Element")).sortThis());

        // For element in C: C itself + D (depends on C)
        Assert.assertEquals(
                Lists.mutable.with("c", "d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("c::Element")).sortThis());

        // For element in D: D itself only (nothing depends on D)
        Assert.assertEquals(
                Lists.mutable.with("d"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("d::Element")).sortThis());
    }

    @Test
    public void testBackReferenceModulesWithIndex()
    {
        // Dependency chain: A <- B <- C
        // Back-reference indexes: B has back refs for ElementA, C does not
        ConcreteElementMetadata elementA = newClass("a::ElementA", "/a/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleA = ModuleManifest.builder("a").withElement(elementA).build();

        ConcreteElementMetadata elementB = newClass("b::ElementB", "/b/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleB = ModuleManifest.builder("b").withDependency("a").withElement(elementB).build();

        ConcreteElementMetadata elementC = newClass("c::ElementC", "/c/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleC = ModuleManifest.builder("c").withDependency("b").withElement(elementC).build();

        // B has back refs for ElementA only
        ModuleBackReferenceIndex indexB = ModuleBackReferenceIndex.builder()
                .withModuleName("b")
                .addElementPath("a::ElementA")
                .build();

        // C has no back refs for anything
        ModuleBackReferenceIndex indexC = ModuleBackReferenceIndex.builder()
                .withModuleName("c")
                .build();

        // A has back refs for ElementA (itself)
        ModuleBackReferenceIndex indexA = ModuleBackReferenceIndex.builder()
                .withModuleName("a")
                .addElementPath("a::ElementA")
                .build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(moduleA, moduleB, moduleC)
                .withBackReferenceIndex(indexA)
                .withBackReferenceIndex(indexB)
                .withBackReferenceIndex(indexC)
                .build();

        // For ElementA: without index would be {a, b, c}. With index, C is excluded (no back refs for ElementA in C)
        Assert.assertEquals(
                Lists.mutable.with("a", "b"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("a::ElementA")).sortThis());

        // For ElementB: without index would be {b, c}. B's index doesn't have ElementB, C's index doesn't have ElementB
        Assert.assertEquals(
                Lists.mutable.empty(),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("b::ElementB")).sortThis());

        // For ElementC: without index would be {c}. C's index is empty
        Assert.assertEquals(
                Lists.mutable.empty(),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("c::ElementC")).sortThis());
    }

    @Test
    public void testBackReferenceModulesWithPartialIndex()
    {
        // When index is only available for some modules, modules without index should be included as fallback
        ConcreteElementMetadata elementA = newClass("a::ElementA", "/a/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleA = ModuleManifest.builder("a").withElement(elementA).build();

        ConcreteElementMetadata elementB = newClass("b::ElementB", "/b/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleB = ModuleManifest.builder("b").withDependency("a").withElement(elementB).build();

        ConcreteElementMetadata elementC = newClass("c::ElementC", "/c/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleC = ModuleManifest.builder("c").withDependency("a").withElement(elementC).build();

        // Only module B has a back-reference index (with ElementA); modules A and C do not
        ModuleBackReferenceIndex indexB = ModuleBackReferenceIndex.builder()
                .withModuleName("b")
                .addElementPath("a::ElementA")
                .build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(moduleA, moduleB, moduleC)
                .withBackReferenceIndex(indexB)
                .build();

        // For ElementA: candidates are {a, b, c}
        // A has no index -> included (fallback)
        // B has index with ElementA -> included
        // C has no index -> included (fallback)
        Assert.assertEquals(
                Lists.mutable.with("a", "b", "c"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("a::ElementA")).sortThis());

        // For ElementB: candidates are {b}
        // B has index but doesn't have ElementB -> excluded
        Assert.assertEquals(
                Lists.mutable.empty(),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("b::ElementB")).sortThis());
    }

    @Test
    public void testBackReferenceModulesNoIndexFallback()
    {
        // When no indexes are provided at all, should behave exactly as before
        ConcreteElementMetadata elementA = newClass("a::ElementA", "/a/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleA = ModuleManifest.builder("a").withElement(elementA).build();

        ConcreteElementMetadata elementB = newClass("b::ElementB", "/b/source.pure", 1, 1, 5, 1);
        ModuleManifest moduleB = ModuleManifest.builder("b").withDependency("a").withElement(elementB).build();

        MetadataIndex index = MetadataIndex.builder()
                .withModules(moduleA, moduleB)
                .build();

        // Same as original behavior: element in A gets {a, b}
        Assert.assertEquals(
                Lists.mutable.with("a", "b"),
                Lists.mutable.withAll(index.getBackReferenceModuleNames("a::ElementA")).sortThis());
    }

}
