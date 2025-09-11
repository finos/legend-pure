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
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.Predicate;

public class TestModuleManifest extends AbstractMetadataTest
{
    @Test
    public void testEmptyModule()
    {
        String name = "empty_module";
        ModuleManifest emptyModule = ModuleManifest.builder(name).build();
        Assert.assertEquals(name, emptyModule.getModuleName());
        Assert.assertEquals(0, emptyModule.getElementCount());
        Assert.assertEquals(Lists.immutable.empty(), emptyModule.getElements());
        assertForEachElement(emptyModule);

        Assert.assertEquals(emptyModule, ModuleManifest.builder(name).build());
        Assert.assertNotEquals(emptyModule, ModuleManifest.builder(name + "_" + name).build());
        Assert.assertNotEquals(emptyModule, ModuleManifest.builder("non_empty_module")
                .withElement(newClass("model::MyClass", "/non_empty_module/file.pure", 1, 1, 5, 1))
                .build());
    }

    @Test
    public void testMultiSourceModule()
    {
        String name = "test_module";

        ConcreteElementMetadata mySimpleClass = newClass("model::classes::MySimpleClass", "/test_module/model/classes.pure", 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::classes::MyOtherClass", "/test_module/model/classes.pure", 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::associations::SimpleToOther", "/test_module/model/associations.pure", 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::associations::SimpleToThird", "/test_module/model/associations.pure", 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::associations::OtherToThird", "/test_module/model/associations.pure", 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::enums::MyFirstEnumeration", "/test_module/model/enums.pure", 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::enums::MySecondEnumeration", "/test_module/model/enums.pure", 8, 1, 10, 1);
        ConcreteElementMetadata notInTheModule = newEnumeration("model::enums::NotInTheModule", "/test_module/model/other_enums.pure", 1, 1, 3, 1);

        ModuleManifest simpleModule = ModuleManifest.builder(name)
                .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration)
                .build();
        Assert.assertEquals(name, simpleModule.getModuleName());
        Assert.assertEquals(8, simpleModule.getElementCount());
        Assert.assertEquals(Lists.immutable.with(otherToThird, simpleToOther, simpleToThird, myOtherClass, mySimpleClass, myThirdClass, myFirstEnumeration, mySecondEnumeration), simpleModule.getElements());
        assertForEachElement(simpleModule, otherToThird, simpleToOther, simpleToThird, myOtherClass, mySimpleClass, myThirdClass, myFirstEnumeration, mySecondEnumeration);

        Assert.assertEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(otherToThird, simpleToOther, simpleToThird, myOtherClass, mySimpleClass, myThirdClass, myFirstEnumeration, mySecondEnumeration)
                        .build());
        Assert.assertEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration)
                        .build());
        Assert.assertEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(myFirstEnumeration, mySimpleClass, myOtherClass, simpleToOther, simpleToThird, otherToThird, mySecondEnumeration, myThirdClass)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, mySecondEnumeration)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, mySecondEnumeration)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration, notInTheModule)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration, notInTheModule)
                        .build());
    }

    @Test
    public void testInvalidModuleName()
    {
        NullPointerException eNull = Assert.assertThrows(NullPointerException.class, ModuleManifest.builder()::build);
        Assert.assertEquals("module name may not be null", eNull.getMessage());
    }

    @Test
    public void testElementPathConflict()
    {
        // this should work, since the two are equal
        ModuleManifest.builder("non_empty_module").withElements(
                newClass("model::MyClass", "/non_empty_module/file.pure", 1, 1, 5, 1),
                newClass("model::MyClass", "/non_empty_module/file.pure", 1, 1, 5, 1)
        ).build();

        IllegalArgumentException e = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ModuleManifest.builder("test_module").withElements(
                        newClass("model::MyClass", "/test_module/file.pure", 1, 1, 5, 1),
                        newAssociation("model::MyAssociation", "/test_module/file.pure", 6, 1, 8, 1),
                        newClass("model::MyClass", "/test_module/file.pure", 9, 1, 15, 1)
                ).build());
        Assert.assertEquals("Conflict for element: model::MyClass", e.getMessage());
    }

    @Test
    public void testWithElement()
    {
        String name = "test_module";
        ConcreteElementMetadata mySimpleClass = newClass("model::classes::MySimpleClass", "/test_module/model/classes.pure", 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::classes::MyOtherClass", "/test_module/model/classes.pure", 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::associations::SimpleToOther", "/test_module/model/associations.pure", 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::associations::SimpleToThird", "/test_module/model/associations.pure", 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::associations::OtherToThird", "/test_module/model/associations.pure", 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::enums::MyFirstEnumeration", "/test_module/model/enums.pure", 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::enums::MySecondEnumeration", "/test_module/model/enums.pure", 8, 1, 10, 1);
        ConcreteElementMetadata notInTheModule = newEnumeration("model::enums::NotInTheModule", "/test_module/model/more_enums.pure", 1, 1, 3, 1);

        ModuleManifest simpleModule = ModuleManifest.builder(name).withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration).build();
        simpleModule.forEachElement(e -> Assert.assertEquals(e.getPath(), simpleModule, simpleModule.withElement(e)));

        ModuleManifest simpleModulePlus = simpleModule.withElement(notInTheModule);
        Assert.assertNotEquals(simpleModule, simpleModulePlus);
        Assert.assertEquals(simpleModule.getElementCount() + 1, simpleModulePlus.getElementCount());
        Assert.assertEquals(simpleModule.getElements().toList().with(notInTheModule).sortThisBy(PackageableElementMetadata::getPath), simpleModulePlus.getElements());

        ConcreteElementMetadata myThirdClassReplacement = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 14, 1, 20, 1);
        ModuleManifest simpleModuleWithReplacement = simpleModule.withElement(myThirdClassReplacement);
        Assert.assertNotEquals(simpleModule, simpleModuleWithReplacement);
        Assert.assertEquals(simpleModule.getElementCount(), simpleModuleWithReplacement.getElementCount());
        Assert.assertEquals(simpleModule.getElements().collect(PackageableElementMetadata::getPath), simpleModuleWithReplacement.getElements().collect(PackageableElementMetadata::getPath));

        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> simpleModule.withElement(null));
        Assert.assertEquals("element metadata may not be null", e.getMessage());
    }

    @Test
    public void testWithElements()
    {
        String name = "test_module";
        ConcreteElementMetadata mySimpleClass = newClass("model::classes::MySimpleClass", "/test_module/model/classes.pure", 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::classes::MyOtherClass", "/test_module/model/classes.pure", 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::associations::SimpleToOther", "/test_module/model/associations.pure", 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::associations::SimpleToThird", "/test_module/model/associations.pure", 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::associations::OtherToThird", "/test_module/model/associations.pure", 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::enums::MyFirstEnumeration", "/test_module/model/enums.pure", 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::enums::MySecondEnumeration", "/test_module/model/enums.pure", 8, 1, 10, 1);
        ConcreteElementMetadata notInTheModule = newEnumeration("model::enums::NotInTheModule", "/test_module/model/more_enums.pure", 1, 1, 3, 1);

        ModuleManifest simpleModule = ModuleManifest.builder(name).withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration).build();
        Assert.assertEquals(simpleModule, simpleModule.withElements(Lists.immutable.empty()));
        Assert.assertEquals(simpleModule, simpleModule.withElements(simpleModule.getElements()));

        ModuleManifest simpleModulePlus = simpleModule.withElements(notInTheModule, myFirstEnumeration, mySecondEnumeration);
        Assert.assertNotEquals(simpleModule, simpleModulePlus);
        Assert.assertEquals(simpleModule.getElementCount() + 1, simpleModulePlus.getElementCount());
        Assert.assertEquals(simpleModule.getElements().toList().with(notInTheModule).sortThisBy(PackageableElementMetadata::getPath), simpleModulePlus.getElements());

        ConcreteElementMetadata myThirdClassReplacement = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 14, 1, 20, 1);
        ModuleManifest simpleModuleWithReplacement = simpleModule.withElements(mySimpleClass, myOtherClass, myThirdClassReplacement);
        Assert.assertNotEquals(simpleModule, simpleModuleWithReplacement);
        Assert.assertEquals(simpleModule.getElementCount(), simpleModuleWithReplacement.getElementCount());
        Assert.assertEquals(simpleModule.getElements().collect(PackageableElementMetadata::getPath), simpleModuleWithReplacement.getElements().collect(PackageableElementMetadata::getPath));

        ModuleManifest simpleModulePlusWithReplacement = simpleModule.withElements(mySimpleClass, myOtherClass, myThirdClassReplacement, notInTheModule);
        Assert.assertNotEquals(simpleModule, simpleModulePlusWithReplacement);
        Assert.assertEquals(simpleModule.getElementCount() + 1, simpleModulePlusWithReplacement.getElementCount());
        Assert.assertEquals(simpleModule.getElements().toList().without(myThirdClass).with(myThirdClassReplacement).with(notInTheModule).sortThisBy(PackageableElementMetadata::getPath), simpleModulePlusWithReplacement.getElements());

        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> simpleModule.withElements(notInTheModule, null, myThirdClassReplacement));
        Assert.assertEquals("element metadata may not be null", e.getMessage());
    }

    @Test
    public void testWithoutElements()
    {
        String name = "test_module";
        ConcreteElementMetadata mySimpleClass = newClass("model::classes::MySimpleClass", "/test_module/model/classes.pure", 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::classes::MyOtherClass", "/test_module/model/classes.pure", 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::associations::SimpleToOther", "/test_module/model/associations.pure", 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::associations::SimpleToThird", "/test_module/model/associations.pure", 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::associations::OtherToThird", "/test_module/model/associations.pure", 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::enums::MyFirstEnumeration", "/test_module/model/enums.pure", 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::enums::MySecondEnumeration", "/test_module/model/enums.pure", 8, 1, 10, 1);

        ModuleManifest baseModule = ModuleManifest.builder(name).withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration).build();
        Assert.assertSame(baseModule, baseModule.withoutElements());
        Assert.assertSame(baseModule, baseModule.withoutElements(Lists.immutable.empty()));
        Assert.assertEquals(baseModule, baseModule.withoutElements(emd -> false));

        Assert.assertEquals(baseModule, baseModule.withoutElements("model::enums::NotInTheModule", "model::enums::AlsoNotInTheModule"));
        Assert.assertEquals(baseModule, baseModule.withoutElements(Lists.immutable.with("model::enums::NotInTheModule", "model::enums::AlsoNotInTheModule")));
        Assert.assertEquals(baseModule, baseModule.withoutElements(emd -> "model::enums::NotInTheModule".equals(emd.getPath())));

        Assert.assertEquals(
                ModuleManifest.builder(name).withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToThird, otherToThird).build(),
                baseModule.withoutElements("model::associations::SimpleToOther", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration"));
        Assert.assertEquals(
                ModuleManifest.builder(name).withElements(simpleToOther, otherToThird, myFirstEnumeration, mySecondEnumeration).build(),
                baseModule.withoutElements(Lists.mutable.with("model::classes::MySimpleClass", "model::associations::SimpleToThird", "model::classes::MyOtherClass", "model::classes::MyThirdClass")));
        Assert.assertEquals(
                ModuleManifest.builder(name).withElements(mySimpleClass, myOtherClass, myThirdClass).build(),
                baseModule.withoutElements(emd -> emd.getPath().contains("::associations::") || emd.getPath().contains("::enums::")));
        Assert.assertEquals(
                ModuleManifest.builder(name).build(),
                baseModule.withoutElements(emd -> emd.getPath().startsWith("model::")));
    }

    @Test
    public void testUpdate()
    {
        String name = "test_module";
        ConcreteElementMetadata mySimpleClass = newClass("model::classes::MySimpleClass", "/test_module/model/classes.pure", 1, 1, 5, 1);
        ConcreteElementMetadata myOtherClass = newClass("model::classes::MyOtherClass", "/test_module/model/classes.pure", 6, 1, 10, 1);
        ConcreteElementMetadata myThirdClass = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 12, 1, 20, 1);
        ConcreteElementMetadata simpleToOther = newAssociation("model::associations::SimpleToOther", "/test_module/model/associations.pure", 2, 1, 7, 1);
        ConcreteElementMetadata simpleToThird = newAssociation("model::associations::SimpleToThird", "/test_module/model/associations.pure", 9, 1, 16, 1);
        ConcreteElementMetadata otherToThird = newAssociation("model::associations::OtherToThird", "/test_module/model/associations.pure", 18, 1, 25, 1);
        ConcreteElementMetadata myFirstEnumeration = newEnumeration("model::enums::MyFirstEnumeration", "/test_module/model/enums.pure", 3, 1, 6, 1);
        ConcreteElementMetadata mySecondEnumeration = newEnumeration("model::enums::MySecondEnumeration", "/test_module/model/enums.pure", 8, 1, 10, 1);

        ConcreteElementMetadata notInTheModule = newEnumeration("model::enums::NotInTheModule", "/test_module/model/more_enums.pure", 1, 1, 3, 1);
        ConcreteElementMetadata myThirdClassReplacement = newClass("model::classes::MyThirdClass", "/test_module/model/classes.pure", 14, 1, 20, 1);

        ModuleManifest baseModule = ModuleManifest.builder(name)
                .withElements(mySimpleClass, myOtherClass, myThirdClass, simpleToOther, simpleToThird, otherToThird, myFirstEnumeration, mySecondEnumeration)
                .build();
        Assert.assertSame(baseModule, baseModule.update(null, (Iterable<String>) null));
        Assert.assertSame(baseModule, baseModule.update(null, (Predicate<ConcreteElementMetadata>) null));
        Assert.assertSame(baseModule, baseModule.update(Lists.immutable.empty(), Lists.immutable.empty()));
        Assert.assertEquals(baseModule, baseModule.update(baseModule.getElements(), Lists.immutable.with("model::enums::NotInTheModule")));

        Assert.assertEquals(
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myThirdClassReplacement, simpleToOther, simpleToThird, otherToThird)
                        .build(),
                baseModule.update(
                        Lists.immutable.with(mySimpleClass, myThirdClassReplacement),
                        Lists.immutable.with("model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration", "model::classes::MyOtherClass")));
        Assert.assertEquals(
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myThirdClassReplacement, myFirstEnumeration, mySecondEnumeration)
                        .build(),
                baseModule.update(
                        Lists.immutable.with(mySimpleClass, myThirdClassReplacement),
                        emd -> emd.getPath().contains("::associations::") || "model::classes::MyOtherClass".equals(emd.getPath())));
        Assert.assertEquals(
                ModuleManifest.builder(name)
                        .withElements(mySimpleClass, myOtherClass, myThirdClassReplacement, simpleToOther, otherToThird, myFirstEnumeration, mySecondEnumeration, notInTheModule)
                        .build(),
                baseModule.update(
                        Lists.immutable.with(myThirdClassReplacement, notInTheModule),
                        Lists.immutable.with("model::associations::SimpleToThird")));

        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> baseModule.update(Lists.immutable.with(notInTheModule, null, myThirdClassReplacement), Lists.immutable.empty()));
        Assert.assertEquals("element metadata may not be null", e.getMessage());
    }

    private void assertForEachElement(ModuleManifest module, ConcreteElementMetadata... expectedElements)
    {
        MutableList<ConcreteElementMetadata> actual = Lists.mutable.empty();
        module.forEachElement(actual::add);
        Assert.assertEquals(Arrays.asList(expectedElements), actual);
    }
}
