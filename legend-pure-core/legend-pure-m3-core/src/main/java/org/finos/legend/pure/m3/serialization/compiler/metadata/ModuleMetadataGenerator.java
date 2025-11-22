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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.ModuleHelper;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.tools.GraphTools;

import java.util.Arrays;
import java.util.Objects;

public class ModuleMetadataGenerator
{
    private final ProcessorSupport processorSupport;
    private final SourceRegistry sourceRegistry;
    private final ConcreteElementMetadataGenerator elementGenerator;
    private final SourceMetadataGenerator sourceGenerator;
    private final int referenceIdVersion;

    private ModuleMetadataGenerator(ProcessorSupport processorSupport, SourceRegistry sourceRegistry, ReferenceIdProvider referenceIdProvider)
    {
        this.processorSupport = processorSupport;
        this.sourceRegistry = sourceRegistry;
        this.elementGenerator = new ConcreteElementMetadataGenerator(referenceIdProvider, processorSupport);
        this.sourceGenerator = (sourceRegistry == null) ? null : new SourceMetadataGenerator();
        this.referenceIdVersion = referenceIdProvider.version();
    }

    public ModuleMetadata generateModuleMetadata(String name)
    {
        String moduleName = ModuleHelper.resolveModuleName(name);
        return generateModuleMetadata(moduleName, moduleMetadataBuilder(name)).build();
    }

    private ModuleMetadata.Builder generateModuleMetadata(String moduleName, ModuleMetadata.Builder builder)
    {
        GraphTools.getTopLevelAndPackagedElements(this.processorSupport).forEach(e ->
        {
            if (ModuleHelper.isElementInModule(e, moduleName))
            {
                this.elementGenerator.computeMetadata(builder, e);
            }
        });
        if (this.sourceRegistry != null)
        {
            this.sourceRegistry.getSources().forEach(s ->
            {
                if (ModuleHelper.isSourceInModule(s, moduleName))
                {
                    builder.addSource(this.sourceGenerator.generateSourceMetadata(s));
                }
            });
        }
        return builder;
    }

    public MutableList<ModuleMetadata> generateModuleMetadata(Iterable<? extends String> moduleNames)
    {
        MutableMap<String, ModuleMetadata.Builder> buildersByModule = Maps.mutable.empty();
        moduleNames.forEach(name ->
        {
            String moduleName = ModuleHelper.resolveModuleName(name);
            buildersByModule.put(moduleName, moduleMetadataBuilder(moduleName));
        });
        if (buildersByModule.isEmpty())
        {
            return Lists.mutable.empty();
        }
        if (buildersByModule.size() == 1)
        {
            return Lists.mutable.with(generateModuleMetadata(buildersByModule.keysView().getAny(), buildersByModule.valuesView().getAny()).build());
        }
        GraphTools.getTopLevelAndPackagedElements(this.processorSupport).forEach(element ->
        {
            String moduleName = ModuleHelper.getElementModule(element);
            if (moduleName != null)
            {
                ModuleMetadata.Builder builder = buildersByModule.get(moduleName);
                if (builder != null)
                {
                    this.elementGenerator.computeMetadata(builder, element);
                }
            }
        });
        if (this.sourceRegistry != null)
        {
            this.sourceRegistry.getSources().forEach(source ->
            {
                String moduleName = ModuleHelper.getSourceModule(source);
                if (moduleName != null)
                {
                    ModuleMetadata.Builder builder = buildersByModule.get(moduleName);
                    if (builder != null)
                    {
                        builder.addSource(this.sourceGenerator.generateSourceMetadata(source));
                    }
                }
            });
        }

        return buildersByModule.collect(ModuleMetadata.Builder::build, Lists.mutable.ofInitialCapacity(buildersByModule.size()));
    }

    public MutableList<ModuleMetadata> generateModuleMetadata(String... moduleNames)
    {
        switch (moduleNames.length)
        {
            case 0:
            {
                return Lists.mutable.empty();
            }
            case 1:
            {
                return Lists.mutable.with(generateModuleMetadata(moduleNames[0]));
            }
            default:
            {
                return generateModuleMetadata(Arrays.asList(moduleNames));
            }
        }
    }

    public MutableList<ModuleMetadata> generateAllModuleMetadata()
    {
        return generateAllModuleMetadata(true);
    }

    public MutableList<ModuleMetadata> generateAllModuleMetadata(boolean includeRootModule)
    {
        MutableMap<String, ModuleMetadata.Builder> buildersByModule = Maps.mutable.empty();
        GraphTools.getTopLevelAndPackagedElements(this.processorSupport).forEach(element ->
        {
            String moduleName = ModuleHelper.getElementModule(element);
            if ((moduleName != null) && (includeRootModule || ModuleHelper.isNonRootModule(moduleName)))
            {
                this.elementGenerator.computeMetadata(buildersByModule.getIfAbsentPutWithKey(moduleName, this::moduleMetadataBuilder), element);
            }
        });
        if (this.sourceRegistry != null)
        {
            this.sourceRegistry.getSources().forEach(source ->
            {
                String moduleName = ModuleHelper.getSourceModule(source);
                if ((moduleName != null) && (includeRootModule || ModuleHelper.isNonRootModule(moduleName)))
                {
                    buildersByModule.getIfAbsentPutWithKey(moduleName, this::moduleMetadataBuilder).addSource(this.sourceGenerator.generateSourceMetadata(source));
                }
            });
        }
        return buildersByModule.collect(ModuleMetadata.Builder::build, Lists.mutable.ofInitialCapacity(buildersByModule.size()));
    }

    int getReferenceIdVersion()
    {
        return this.referenceIdVersion;
    }

    ConcreteElementMetadataGenerator getElementMetadataGenerator()
    {
        return this.elementGenerator;
    }

    SourceMetadataGenerator getSourceMetadataGenerator()
    {
        return this.sourceGenerator;
    }

    private ModuleMetadata.Builder moduleMetadataBuilder(String moduleName)
    {
        return ModuleMetadata.builder(moduleName).withReferenceIdVersion(this.referenceIdVersion);
    }

    public static ModuleMetadataGenerator fromPureRuntime(PureRuntime runtime)
    {
        return builder().withPureRuntime(runtime).build();
    }

    public static ModuleMetadataGenerator fromProcessorSupport(ProcessorSupport processorSupport)
    {
        return builder().withProcessorSupport(processorSupport).build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private ProcessorSupport processorSupport;
        private SourceRegistry sourceRegistry;
        private ReferenceIdProvider referenceIdProvider;

        private Builder()
        {
        }

        public Builder withProcessorSupport(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
            return this;
        }

        public Builder withSourceRegistry(SourceRegistry sourceRegistry)
        {
            this.sourceRegistry = sourceRegistry;
            return this;
        }

        public Builder withPureRuntime(PureRuntime pureRuntime)
        {
            return withProcessorSupport(pureRuntime.getProcessorSupport())
                    .withSourceRegistry(pureRuntime.getSourceRegistry());
        }

        public Builder withReferenceIdProvider(ReferenceIdProvider referenceIdProvider)
        {
            this.referenceIdProvider = referenceIdProvider;
            return this;
        }

        public ModuleMetadataGenerator build()
        {
            Objects.requireNonNull(this.processorSupport);
            ReferenceIdProvider idProvider = (this.referenceIdProvider == null) ?
                                             ReferenceIdProviders.builder().withProcessorSupport(this.processorSupport).withAvailableExtensions().build().provider() :
                                             this.referenceIdProvider;
            return new ModuleMetadataGenerator(this.processorSupport, this.sourceRegistry, idProvider);
        }
    }
}
