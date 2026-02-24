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
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.serialization.compiler.ModuleHelper;
import org.finos.legend.pure.m3.serialization.compiler.reference.AbstractReferenceTest;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestModuleMetadataGenerator extends AbstractReferenceTest
{
    private ModuleMetadataGenerator generator;
    private final MutableList<String> testSources = Lists.mutable.empty();

    @Before
    public void setUpGenerator()
    {
        this.generator = newGenerator();
    }

    @Before
    public void clearTestSources()
    {
        this.testSources.clear();
    }

    @After
    public void deleteTestSources()
    {
        if (this.testSources.notEmpty())
        {
            this.testSources.forEach(runtime::delete);
            runtime.compile();
        }
    }

    @Test
    public void testEmptyModule()
    {
        String name = "empty";
        Assert.assertEquals(ModuleMetadata.builder(name).withReferenceIdVersion(this.generator.getReferenceIdVersion()).build(), this.generator.generateModuleMetadata(name));
    }

    @Test
    public void testRefTestModule()
    {
        String name = "ref_test";
        Assert.assertEquals(getModuleMetadata(name), this.generator.generateModuleMetadata(name));
    }

    @Test
    public void testPlatformModule()
    {
        String name = "platform";
        Assert.assertEquals(getModuleMetadata(name), this.generator.generateModuleMetadata(name));
    }

    @Test
    public void testMultiModules()
    {
        String[] names = {"platform", "ref_test"};
        Assert.assertEquals(
                getModuleMetadata(names).sortThisBy(ModuleMetadata::getName),
                this.generator.generateModuleMetadata(names).sortThisBy(ModuleMetadata::getName));
    }

    @Test
    public void testAllModules()
    {
        Assert.assertEquals(
                getAllModuleMetadata().sortThisBy(ModuleMetadata::getName),
                this.generator.generateAllModuleMetadata().sortThisBy(ModuleMetadata::getName)
        );
    }

    private ModuleMetadata getModuleMetadata(String moduleName)
    {
        ModuleMetadata.Builder builder = newModuleMetadataBuilder(moduleName);
        ConcreteElementMetadataGenerator elementGenerator = this.generator.getElementMetadataGenerator();
        GraphTools.getTopLevelAndPackagedElements(repository).forEach(e ->
        {
            if (ModuleHelper.isElementInModule(e, moduleName))
            {
                elementGenerator.computeMetadata(builder, e);
            }
        });
        builder.addSources(runtime.getSourceRegistry().getSources().asLazy().collectIf(
                        s -> ModuleHelper.isSourceInModule(s, moduleName),
                        this.generator.getSourceMetadataGenerator()::generateSourceMetadata));
        return builder.build();
    }

    private MutableList<ModuleMetadata> getModuleMetadata(String... moduleNames)
    {
        MutableMap<String, ModuleMetadata.Builder> byModule = Maps.mutable.ofInitialCapacity(moduleNames.length);
        ArrayIterate.forEach(moduleNames, name -> byModule.put(name, newModuleMetadataBuilder(name)));
        GraphTools.getTopLevelAndPackagedElements(repository).forEach(element ->
        {
            ModuleMetadata.Builder builder = byModule.get(ModuleHelper.getElementModule(element));
            if (builder != null)
            {
                this.generator.getElementMetadataGenerator().computeMetadata(builder, element);
            }
        });
        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            ModuleMetadata.Builder builder = byModule.get(ModuleHelper.getSourceModule(source));
            if (builder != null)
            {
                builder.addSource(this.generator.getSourceMetadataGenerator().generateSourceMetadata(source));
            }
        });
        return byModule.collect(ModuleMetadata.Builder::build, Lists.mutable.ofInitialCapacity(byModule.size()));
    }

    private MutableList<ModuleMetadata> getAllModuleMetadata()
    {
        return getAllModuleMetadata(true);
    }

    private MutableList<ModuleMetadata> getAllModuleMetadata(boolean includeRoot)
    {
        MutableMap<String, ModuleMetadata.Builder> byModule = Maps.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(repository).forEach(element ->
        {
            String module = ModuleHelper.getElementModule(element);
            if ((module != null) && (includeRoot || ModuleHelper.isNonRootModule(module)))
            {
                ModuleMetadata.Builder builder = byModule.getIfAbsentPutWithKey(ModuleHelper.getElementModule(element), this::newModuleMetadataBuilder);
                this.generator.getElementMetadataGenerator().computeMetadata(builder, element);
            }
        });
        runtime.getSourceRegistry().getSources().forEach(source ->
        {
            String module = ModuleHelper.getSourceModule(source);
            if ((module != null) && (includeRoot || ModuleHelper.isNonRootModule(module)))
            {
                SourceMetadata metadata = this.generator.getSourceMetadataGenerator().generateSourceMetadata(source);
                byModule.getIfAbsentPutWithKey(module, this::newModuleMetadataBuilder).addSource(metadata);
            }
        });
        return byModule.collect(ModuleMetadata.Builder::build, Lists.mutable.ofInitialCapacity(byModule.size()));
    }

    private ModuleMetadata.Builder newModuleMetadataBuilder(String moduleName)
    {
        ModuleMetadata.Builder builder = ModuleMetadata.builder(moduleName).withReferenceIdVersion(this.generator.getReferenceIdVersion());
        CodeRepository repository = runtime.getCodeStorage().getRepository(moduleName);
        if (repository instanceof GenericCodeRepository)
        {
            builder.withDependencies(((GenericCodeRepository) repository).getDependencies());
        }
        return builder;
    }

    private static ModuleMetadataGenerator newGenerator()
    {
        return ModuleMetadataGenerator.fromPureRuntime(runtime);
    }
}
