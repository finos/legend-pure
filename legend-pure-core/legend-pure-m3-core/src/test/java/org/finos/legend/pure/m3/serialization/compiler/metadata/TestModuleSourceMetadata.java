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

public class TestModuleSourceMetadata extends AbstractMetadataTest
{
    @Test
    public void testEmptyModule()
    {
        String name = "empty_module";
        ModuleSourceMetadata emptyModule = ModuleSourceMetadata.builder(name).build();
        Assert.assertEquals(name, emptyModule.getModuleName());
        Assert.assertEquals(0, emptyModule.getSourceCount());
        Assert.assertEquals(Lists.immutable.empty(), emptyModule.getSources());
        assertForEachSource(emptyModule);

        Assert.assertEquals(emptyModule, ModuleSourceMetadata.builder(name).build());
        Assert.assertNotEquals(emptyModule, ModuleSourceMetadata.builder(name + "_" + name).build());
        Assert.assertNotEquals(emptyModule, ModuleSourceMetadata.builder("non_empty_module")
                .withSource(newSource("/non_empty_module/model/classes.pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass", "model::classes::MyThirdClass"))
                .build());
    }

    @Test
    public void testMultiSourceModule()
    {
        String name = "test_module";
        SourceMetadata classesSource = newSource("/test_module/model/classes.pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass", "model::classes::MyThirdClass");
        SourceMetadata associationsSource = newSource("/test_module/model/associations.pure", "model::associations::SimpleToOther", "model::associations::SimpleToThird", "model::associations::OtherToThird");
        SourceMetadata enumsSource = newSource("/test_module/model/enums.pure", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration");
        SourceMetadata otherEnumsSource = newSource("/test_module/model/other_enums.pure", "model::enums::NotInTheModule");

        ModuleSourceMetadata simpleModule = ModuleSourceMetadata.builder(name)
                .withSources(classesSource, associationsSource, enumsSource)
                .build();
        Assert.assertEquals(name, simpleModule.getModuleName());
        Assert.assertEquals(3, simpleModule.getSourceCount());
        Assert.assertEquals(Lists.immutable.with(associationsSource, classesSource, enumsSource), simpleModule.getSources());
        assertForEachSource(simpleModule, associationsSource, classesSource, enumsSource);

        Assert.assertEquals(
                simpleModule,
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource, associationsSource, enumsSource)
                        .build());
        Assert.assertEquals(
                simpleModule,
                ModuleSourceMetadata.builder(name)
                        .withSources(enumsSource, associationsSource, classesSource)
                        .build());
        Assert.assertEquals(
                simpleModule,
                ModuleSourceMetadata.builder(name)
                        .withSources(associationsSource, enumsSource, classesSource)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource, associationsSource)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource, associationsSource, enumsSource, otherEnumsSource)
                        .build());
        Assert.assertNotEquals(
                simpleModule,
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource, associationsSource, enumsSource, otherEnumsSource)
                        .build());
    }

    @Test
    public void testInvalidModuleName()
    {
        NullPointerException eNull = Assert.assertThrows(NullPointerException.class, ModuleSourceMetadata.builder()::build);
        Assert.assertEquals("module name may not be null", eNull.getMessage());
    }

    @Test
    public void testSourceIdConflict()
    {
        // this should work, since the two are equal
        ModuleSourceMetadata.builder("non_empty_module").withSources(
                newSource("/non_empty_module/file.pure", "model::MyClass"),
                newSource("/non_empty_module/file.pure", "model::MyClass")
        ).build();

        IllegalArgumentException e = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> ModuleSourceMetadata.builder("test_module").withSources(
                        newSource("/non_empty_module/file.pure", "model::MyClass"),
                        newSource("/non_empty_module/file.pure", "model::MyClass", "model::MyOtherClass")
                ).build());
        Assert.assertEquals("Conflict for source: /non_empty_module/file.pure", e.getMessage());
    }

    @Test
    public void testWithSources()
    {
        String name = "test_module";
        SourceMetadata classesSource = newSource("/test_module/model/classes.pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass", "model::classes::MyThirdClass");
        SourceMetadata associationsSource = newSource("/test_module/model/associations.pure", "model::associations::SimpleToOther", "model::associations::SimpleToThird", "model::associations::OtherToThird");
        SourceMetadata enumsSource = newSource("/test_module/model/enums.pure", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration");
        SourceMetadata otherEnumsSource = newSource("/test_module/model/other_enums.pure", "model::enums::NotInTheModule");

        ModuleSourceMetadata simpleModule = ModuleSourceMetadata.builder(name)
                .withSources(classesSource, associationsSource, enumsSource)
                .build();
        Assert.assertEquals(simpleModule, simpleModule.withSources(Lists.immutable.empty()));
        Assert.assertEquals(simpleModule, simpleModule.withSources(simpleModule.getSources()));

        ModuleSourceMetadata simpleModulePlus = simpleModule.withSources(otherEnumsSource, enumsSource, associationsSource);
        Assert.assertNotEquals(simpleModule, simpleModulePlus);
        Assert.assertEquals(simpleModule.getSourceCount() + 1, simpleModulePlus.getSourceCount());
        Assert.assertEquals(simpleModule.getSources().toList().with(otherEnumsSource).sortThisBy(SourceMetadata::getSourceId), simpleModulePlus.getSources());

        SourceMetadata enumsSourceReplacement = newSource("/test_module/model/enums.pure", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration", "model::enums::MyThirdEnumeration");
        ModuleSourceMetadata simpleModuleWithReplacement = simpleModule.withSources(classesSource, associationsSource, enumsSourceReplacement);
        Assert.assertNotEquals(simpleModule, simpleModuleWithReplacement);
        Assert.assertEquals(simpleModule.getSourceCount(), simpleModuleWithReplacement.getSourceCount());
        Assert.assertEquals(simpleModule.getSources().collect(SourceMetadata::getSourceId), simpleModuleWithReplacement.getSources().collect(SourceMetadata::getSourceId));

        ModuleSourceMetadata simpleModulePlusWithReplacement = simpleModule.withSources(classesSource, associationsSource, enumsSourceReplacement, otherEnumsSource);
        Assert.assertNotEquals(simpleModule, simpleModulePlusWithReplacement);
        Assert.assertEquals(simpleModule.getSourceCount() + 1, simpleModulePlusWithReplacement.getSourceCount());
        Assert.assertEquals(simpleModule.getSources().toList().without(enumsSource).with(enumsSourceReplacement).with(otherEnumsSource).sortThisBy(SourceMetadata::getSourceId), simpleModulePlusWithReplacement.getSources());

        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> simpleModule.withSources(otherEnumsSource, null, associationsSource));
        Assert.assertEquals("source metadata may not be null", e.getMessage());
    }

    @Test
    public void testWithoutSources()
    {
        String name = "test_module";
        SourceMetadata classesSource = newSource("/test_module/model/classes.pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass", "model::classes::MyThirdClass");
        SourceMetadata associationsSource = newSource("/test_module/model/associations.pure", "model::associations::SimpleToOther", "model::associations::SimpleToThird", "model::associations::OtherToThird");
        SourceMetadata enumsSource = newSource("/test_module/model/enums.pure", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration");

        ModuleSourceMetadata baseModule = ModuleSourceMetadata.builder(name)
                .withSources(classesSource, associationsSource, enumsSource)
                .build();
        Assert.assertSame(baseModule, baseModule.withoutSources());
        Assert.assertSame(baseModule, baseModule.withoutSources(Lists.immutable.empty()));
        Assert.assertEquals(baseModule, baseModule.withoutSources(emd -> false));

        Assert.assertEquals(baseModule, baseModule.withoutSources("/test_module/model/other_enums.pure", "/test_module/model/not_in_the_model.pure"));
        Assert.assertEquals(baseModule, baseModule.withoutSources(Lists.immutable.with("/test_module/model/other_enums.pure", "/test_module/model/not_in_the_model.pure")));
        Assert.assertEquals(baseModule, baseModule.withoutSources(smd -> "/test_module/model/not_in_the_model.pure".equals(smd.getSourceId())));

        Assert.assertEquals(
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource)
                        .build(),
                baseModule.withoutSources("/test_module/model/associations.pure", "/test_module/model/enums.pure"));
        Assert.assertEquals(
                ModuleSourceMetadata.builder(name)
                        .withSources(associationsSource)
                        .build(),
                baseModule.withoutSources(Lists.mutable.with("/test_module/model/classes.pure", "/test_module/model/enums.pure")));
        Assert.assertEquals(
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource)
                        .build(),
                baseModule.withoutSources(smd -> smd.getSourceId().contains("associations") || smd.getSourceId().contains("enums")));
        Assert.assertEquals(
                ModuleSourceMetadata.builder(name).build(),
                baseModule.withoutSources(smd -> smd.getSourceId().startsWith("/test_module")));
    }

    @Test
    public void testUpdate()
    {
        String name = "test_module";
        SourceMetadata classesSource = newSource("/test_module/model/classes.pure", "model::classes::MySimpleClass", "model::classes::MyOtherClass", "model::classes::MyThirdClass");
        SourceMetadata associationsSource = newSource("/test_module/model/associations.pure", "model::associations::SimpleToOther", "model::associations::SimpleToThird", "model::associations::OtherToThird");
        SourceMetadata enumsSource = newSource("/test_module/model/enums.pure", "model::enums::MyFirstEnumeration", "model::enums::MySecondEnumeration");
        SourceMetadata moreEnumsSource = newSource("/test_module/model/more_enums.pure", "model::enums::NotInTheModule");

        SourceMetadata classesSourceReplacement = newSource("/test_module/model/classes.pure", "model::classes::MySimpleClass", "model::classes::MyThirdClass");
        SourceMetadata associationsSourceReplacement = newSource("/test_module/model/associations.pure", "model::associations::SimpleToOther", "model::associations::OtherToThird");

        ModuleSourceMetadata baseModule = ModuleSourceMetadata.builder(name)
                .withSources(classesSource, associationsSource, enumsSource)
                .build();
        Assert.assertSame(baseModule, baseModule.update(null, (Iterable<String>) null));
        Assert.assertSame(baseModule, baseModule.update(null, (Predicate<SourceMetadata>) null));
        Assert.assertSame(baseModule, baseModule.update(Lists.immutable.empty(), Lists.immutable.empty()));
        Assert.assertEquals(baseModule, baseModule.update(baseModule.getSources(), Lists.immutable.with("/test_module/model/other_enums.pure")));

        Assert.assertEquals(
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSourceReplacement, associationsSource)
                        .build(),
                baseModule.update(
                        Lists.immutable.with(associationsSource, classesSourceReplacement),
                        Lists.immutable.with("/test_module/model/enums.pure")));
        Assert.assertEquals(
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSourceReplacement, enumsSource)
                        .build(),
                baseModule.update(
                        Lists.immutable.with(classesSourceReplacement),
                        smd -> smd.getSourceId().contains("associations")));
        Assert.assertEquals(
                ModuleSourceMetadata.builder(name)
                        .withSources(classesSource, associationsSourceReplacement, enumsSource, moreEnumsSource)
                        .build(),
                baseModule.update(
                        Lists.immutable.with(moreEnumsSource, associationsSourceReplacement),
                        Lists.immutable.empty()));

        NullPointerException e = Assert.assertThrows(NullPointerException.class, () -> baseModule.update(Lists.immutable.with(moreEnumsSource, null), Lists.immutable.empty()));
        Assert.assertEquals("source metadata may not be null", e.getMessage());
    }

    private void assertForEachSource(ModuleSourceMetadata module, SourceMetadata... expectedSources)
    {
        MutableList<SourceMetadata> actual = Lists.mutable.empty();
        module.forEachSource(actual::add);
        Assert.assertEquals(Arrays.asList(expectedSources), actual);
    }
}
