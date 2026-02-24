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

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.serialization.compiler.ModuleHelper;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProvider;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdProviders;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.tools.GraphTools;

import java.util.Arrays;
import java.util.Objects;

public class ModuleMetadataGenerator
{
    private final PureRuntime runtime;
    private final ConcreteElementMetadataGenerator elementGenerator;
    private final SourceMetadataGenerator sourceGenerator;
    private final int referenceIdVersion;

    private ModuleMetadataGenerator(PureRuntime runtime, ReferenceIdProvider referenceIdProvider)
    {
        this.runtime = runtime;
        this.elementGenerator = new ConcreteElementMetadataGenerator(referenceIdProvider, runtime.getProcessorSupport());
        this.sourceGenerator = new SourceMetadataGenerator();
        this.referenceIdVersion = referenceIdProvider.version();
    }

    public ModuleMetadata generateModuleMetadata(String name)
    {
        String moduleName = ModuleHelper.resolveModuleName(name);
        return generateModuleMetadata(moduleName, moduleMetadataBuilder(name)).build();
    }

    private ModuleMetadata.Builder generateModuleMetadata(String moduleName, ModuleMetadata.Builder builder)
    {
        GraphTools.getTopLevelAndPackagedElements(this.runtime.getProcessorSupport()).forEach(e ->
        {
            if (ModuleHelper.isElementInModule(e, moduleName))
            {
                this.elementGenerator.computeMetadata(builder, e);
            }
        });
        this.runtime.getSourceRegistry().getSources().forEach(s ->
        {
            if (ModuleHelper.isSourceInModule(s, moduleName))
            {
                builder.addSource(this.sourceGenerator.generateSourceMetadata(s));
            }
        });
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
        GraphTools.getTopLevelAndPackagedElements(this.runtime.getProcessorSupport()).forEach(element ->
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
        this.runtime.getSourceRegistry().getSources().forEach(source ->
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
        LazyIterable<String> repos = this.runtime.getCodeStorage().getAllRepositories().asLazy().collect(CodeRepository::getName);
        return generateModuleMetadata(includeRootModule ? repos : repos.select(ModuleHelper::isNonRootModule));
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
        ModuleMetadata.Builder builder = ModuleMetadata.builder(moduleName).withReferenceIdVersion(this.referenceIdVersion);
        CodeRepository repository = this.runtime.getCodeStorage().getRepository(moduleName);
        if (repository instanceof GenericCodeRepository)
        {
            builder.withDependencies(((GenericCodeRepository) repository).getDependencies());
        }
        return builder;
    }

    public static ModuleMetadataGenerator fromPureRuntime(PureRuntime runtime)
    {
        return builder().withPureRuntime(runtime).build();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private PureRuntime pureRuntime;
        private ReferenceIdProvider referenceIdProvider;

        private Builder()
        {
        }

        public Builder withPureRuntime(PureRuntime pureRuntime)
        {
            this.pureRuntime = pureRuntime;
            return this;
        }

        public Builder withReferenceIdProvider(ReferenceIdProvider referenceIdProvider)
        {
            this.referenceIdProvider = referenceIdProvider;
            return this;
        }

        public ModuleMetadataGenerator build()
        {
            Objects.requireNonNull(this.pureRuntime, "PureRuntime is required");
            ReferenceIdProvider idProvider = (this.referenceIdProvider == null) ?
                                             ReferenceIdProviders.builder().withProcessorSupport(this.pureRuntime.getProcessorSupport()).withAvailableExtensions().build().provider() :
                                             this.referenceIdProvider;
            return new ModuleMetadataGenerator(this.pureRuntime, idProvider);
        }
    }
}
