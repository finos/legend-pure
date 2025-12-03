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

package org.finos.legend.pure.m3.serialization.compiler;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.SetAdapter;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.compiler.file.FileSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataGenerator;
import org.finos.legend.pure.m3.tools.GraphTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipOutputStream;

public class PureCompilerSerializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PureCompilerSerializer.class);

    private final FileSerializer fileSerializer;
    private final ModuleMetadataGenerator moduleMetadataGenerator;
    private final ProcessorSupport processorSupport;

    private PureCompilerSerializer(FileSerializer fileSerializer, ModuleMetadataGenerator moduleMetadataGenerator, ProcessorSupport processorSupport)
    {
        this.fileSerializer = Objects.requireNonNull(fileSerializer);
        this.moduleMetadataGenerator = Objects.requireNonNull(moduleMetadataGenerator);
        this.processorSupport = Objects.requireNonNull(processorSupport);
    }

    // Serialize all

    public void serializeAll(Path directory)
    {
        serializeAll(directory, true);
    }

    public void serializeAll(Path directory, boolean includeRootModule)
    {
        serializeAll(newSerializer(directory), includeRootModule);
    }

    public void serializeAll(ZipOutputStream stream)
    {
        serializeAll(stream, true);
    }

    public void serializeAll(ZipOutputStream stream, boolean includeRootModule)
    {
        serializeAll(newSerializer(stream), includeRootModule);
    }

    // Serialize modules

    public void serializeModule(Path directory, String moduleName)
    {
        serializeModule(newSerializer(directory), moduleName);
    }

    public void serializeModule(ZipOutputStream stream, String moduleName)
    {
        serializeModule(newSerializer(stream), moduleName);
    }

    public void serializeModules(Path directory, String... moduleNames)
    {
        serializeModules(newSerializer(directory), moduleNames);
    }

    public void serializeModules(ZipOutputStream stream, String... moduleNames)
    {
        serializeModules(newSerializer(stream), moduleNames);
    }

    public void serializeModules(Path directory, Iterable<? extends String> moduleNames)
    {
        serializeModules(newSerializer(directory), moduleNames);
    }

    public void serializeModules(ZipOutputStream stream, Iterable<? extends String> moduleNames)
    {
        serializeModules(newSerializer(stream), moduleNames);
    }

    public void serializeModules(Path directory, Predicate<? super String> moduleFilter)
    {
        serializeModules(newSerializer(directory), moduleFilter);
    }

    public void serializeModules(ZipOutputStream stream, Predicate<? super String> moduleFilter)
    {
        serializeModules(newSerializer(stream), moduleFilter);
    }

    // Internals

    private Serializer newSerializer(Path directory)
    {
        return new DirectorySerializer(this.fileSerializer, directory);
    }

    private Serializer newSerializer(ZipOutputStream stream)
    {
        return new ZipStreamSerializer(this.fileSerializer, stream);
    }

    private void serializeAll(Serializer serializer, boolean includeRootModule)
    {
        serializeModules(serializer, includeRootModule ? m -> true : ModuleHelper::isNonRootModule);
    }

    private void serializeModule(Serializer serializer, String moduleName)
    {
        long start = System.nanoTime();
        LOGGER.info("Serializing module {}", moduleName);
        try
        {
            long eltStart = System.nanoTime();
            LOGGER.info("Starting element serialization");
            int[] eltCount = {0};
            try
            {
                GraphTools.getTopLevelAndPackagedElements(this.processorSupport).forEach(e ->
                {
                    if (ModuleHelper.isElementInModule(e, moduleName))
                    {
                        serializer.serializeElement(e);
                        eltCount[0]++;
                    }
                });
            }
            finally
            {
                long eltEnd = System.nanoTime();
                LOGGER.info("Finished serializing {} elements in {}s", eltCount[0], (eltEnd - eltStart) / 1_000_000_000.0);
            }

            long modMetaGenStart = System.nanoTime();
            LOGGER.info("Starting module metadata generation of {}", moduleName);
            ModuleMetadata moduleMetadata;
            try
            {
                moduleMetadata = this.moduleMetadataGenerator.generateModuleMetadata(ModuleHelper.resolveModuleName(moduleName));
            }
            finally
            {
                long modMetaGenEnd = System.nanoTime();
                LOGGER.info("Finished module metadata generation of {} in {}s", moduleName, (modMetaGenEnd - modMetaGenStart) / 1_000_000_000.0);
            }

            long modMetaSerStart = System.nanoTime();
            LOGGER.info("Starting serialization of module metadata for {}", moduleName);
            try
            {
                serializer.serializeModuleMetadata(moduleMetadata);
            }
            finally
            {
                long modMetaSerEnd = System.nanoTime();
                LOGGER.info("Finished serialization of module metadata for {} in {}s", moduleName, (modMetaSerEnd - modMetaSerStart) / 1_000_000_000.0);
            }
        }
        catch (Throwable t)
        {
            LOGGER.error("Error serializing module {}", moduleName, t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.info("Finished serializing module {} in {}s", moduleName, (end - start) / 1_000_000_000.0);
        }
    }

    private void serializeModules(Serializer serializer, String... moduleNames)
    {
        switch (moduleNames.length)
        {
            case 0:
            {
                return;
            }
            case 1:
            {
                serializeModule(serializer, moduleNames[0]);
                return;
            }
            default:
            {
                MutableSet<String> moduleNameSet = Sets.mutable.with(moduleNames);
                if (moduleNameSet.remove(null))
                {
                    moduleNameSet.add(ModuleHelper.ROOT_MODULE_NAME);
                }
                serializeModules(serializer, moduleNameSet);
            }
        }
    }

    private void serializeModules(Serializer serializer, Iterable<? extends String> moduleNames)
    {
        serializeModules(serializer, toModuleNameSet(moduleNames));
    }

    private void serializeModules(Serializer serializer, SetIterable<? extends String> moduleNames)
    {
        // moduleNames should not contain null at this point
        switch (moduleNames.size())
        {
            case 0:
            {
                return;
            }
            case 1:
            {
                serializeModule(serializer, moduleNames.getAny());
                break;
            }
            default:
            {
                long start = System.nanoTime();
                LOGGER.info("Serializing modules {}", moduleNames);
                try
                {
                    long eltStart = System.nanoTime();
                    LOGGER.info("Starting element serialization");
                    int[] eltCount = {0};
                    try
                    {
                        GraphTools.getTopLevelAndPackagedElements(this.processorSupport).forEach(e ->
                        {
                            if (moduleNames.contains(ModuleHelper.getElementModule(e)))
                            {
                                serializer.serializeElement(e);
                                eltCount[0]++;
                            }
                        });
                    }
                    finally
                    {
                        long eltEnd = System.nanoTime();
                        LOGGER.info("Finished serializing {} elements in {}s", eltCount[0], (eltEnd - eltStart) / 1_000_000_000.0);
                    }

                    generateAndSerializeModuleMetadata(serializer, moduleNames);
                }
                catch (Throwable t)
                {
                    LOGGER.error("Error serializing modules {}", moduleNames, t);
                    throw t;
                }
                finally
                {
                    long end = System.nanoTime();
                    LOGGER.info("Finished serializing modules {} in {}s", moduleNames, (end - start) / 1_000_000_000.0);
                }
            }
        }
    }

    private void serializeModules(Serializer serializer, Predicate<? super String> moduleFilter)
    {
        Objects.requireNonNull(moduleFilter);
        long start = System.nanoTime();
        LOGGER.info("Serializing selected modules");
        try
        {
            MutableSet<String> moduleNames = Sets.mutable.empty();
            long eltStart = System.nanoTime();
            LOGGER.info("Starting element serialization");
            int[] eltCount = {0};
            try
            {
                GraphTools.getTopLevelAndPackagedElements(this.processorSupport).forEach(e ->
                {
                    String moduleName = ModuleHelper.getElementModule(e);
                    if ((moduleName != null) && moduleFilter.test(moduleName))
                    {
                        moduleNames.add(moduleName);
                        serializer.serializeElement(e);
                        eltCount[0]++;
                    }
                });
            }
            finally
            {
                long eltEnd = System.nanoTime();
                LOGGER.info("Finished serializing {} elements in {}s", eltCount[0], (eltEnd - eltStart) / 1_000_000_000.0);
            }

            generateAndSerializeModuleMetadata(serializer, moduleNames);
        }
        catch (Throwable t)
        {
            LOGGER.error("Error serializing selected modules", t);
            throw t;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.info("Finished serializing selected modules in {}s", (end - start) / 1_000_000_000.0);
        }
    }

    private void generateAndSerializeModuleMetadata(Serializer serializer, SetIterable<? extends String> moduleNames)
    {
        long modMetaGenStart = System.nanoTime();
        MutableList<ModuleMetadata> moduleMetadata;
        LOGGER.info("Starting module metadata generation");
        try
        {
            moduleMetadata = this.moduleMetadataGenerator.generateModuleMetadata(moduleNames);
        }
        finally
        {
            long modMetaGenEnd = System.nanoTime();
            LOGGER.info("Finished module metadata generation in {}s", (modMetaGenEnd - modMetaGenStart) / 1_000_000_000.0);
        }

        moduleMetadata.forEach(modMeta ->
        {
            long modMetaSerStart = System.nanoTime();
            LOGGER.info("Starting serialization of module metadata for {}", modMeta.getName());
            try
            {
                serializer.serializeModuleMetadata(modMeta);
            }
            finally
            {
                long modMetaSerEnd = System.nanoTime();
                LOGGER.info("Finished serialization of module metadata for {} in {}s", modMeta.getName(), (modMetaSerEnd - modMetaSerStart) / 1_000_000_000.0);
            }
        });
    }

    private static SetIterable<? extends String> toModuleNameSet(Iterable<? extends String> moduleNames)
    {
        if (moduleNames instanceof SetIterable)
        {
            SetIterable<? extends String> moduleNameSet = (SetIterable<? extends String>) moduleNames;
            return moduleNameSet.contains(null) ?
                   Sets.mutable.<String>withAll(moduleNameSet).without(null).with(ModuleHelper.ROOT_MODULE_NAME) :
                   moduleNameSet;
        }
        if (moduleNames instanceof Set)
        {
            Set<? extends String> moduleNameSet = (Set<? extends String>) moduleNames;
            return moduleNameSet.contains(null) ?
                   Sets.mutable.<String>withAll(moduleNameSet).without(null).with(ModuleHelper.ROOT_MODULE_NAME) :
                   SetAdapter.adapt(moduleNameSet);
        }
        MutableSet<String> moduleNameSet = Sets.mutable.withAll(moduleNames);
        if (moduleNameSet.remove(null))
        {
            moduleNameSet.add(ModuleHelper.ROOT_MODULE_NAME);
        }
        return moduleNameSet;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private FileSerializer fileSerializer;
        private ModuleMetadataGenerator moduleMetadataGenerator;
        private ProcessorSupport processorSupport;

        private Builder()
        {
        }

        public Builder withProcessorSupport(ProcessorSupport processorSupport)
        {
            this.processorSupport = processorSupport;
            return this;
        }

        public Builder withFileSerializer(FileSerializer fileSerializer)
        {
            this.fileSerializer = fileSerializer;
            return this;
        }

        public Builder withModuleMetadataGenerator(ModuleMetadataGenerator moduleMetadataGenerator)
        {
            this.moduleMetadataGenerator = moduleMetadataGenerator;
            return this;
        }

        public PureCompilerSerializer build()
        {
            return new PureCompilerSerializer(this.fileSerializer, this.moduleMetadataGenerator, this.processorSupport);
        }
    }

    private interface Serializer
    {
        void serializeElement(CoreInstance element);

        void serializeModuleMetadata(ModuleMetadata moduleMetadata);
    }

    private static class DirectorySerializer implements Serializer
    {
        private final FileSerializer fileSerializer;
        private final Path directory;

        private DirectorySerializer(FileSerializer fileSerializer, Path directory)
        {
            this.fileSerializer = fileSerializer;
            this.directory = directory;
        }

        @Override
        public void serializeElement(CoreInstance element)
        {
            this.fileSerializer.serializeElement(this.directory, element);
        }

        @Override
        public void serializeModuleMetadata(ModuleMetadata moduleMetadata)
        {
            this.fileSerializer.serializeModuleManifest(this.directory, moduleMetadata.getManifest());
            this.fileSerializer.serializeModuleSourceMetadata(this.directory, moduleMetadata.getSourceMetadata());
            this.fileSerializer.serializeModuleExternalReferenceMetadata(this.directory, moduleMetadata.getExternalReferenceMetadata());
            this.fileSerializer.serializeModuleBackReferenceMetadata(this.directory, moduleMetadata.getBackReferenceMetadata());
            this.fileSerializer.serializeModuleFunctionNameMetadata(this.directory, moduleMetadata.getFunctionNameMetadata());
        }
    }

    private static class ZipStreamSerializer implements Serializer
    {
        private final FileSerializer fileSerializer;
        private final ZipOutputStream stream;

        private ZipStreamSerializer(FileSerializer fileSerializer, ZipOutputStream stream)
        {
            this.fileSerializer = fileSerializer;
            this.stream = stream;
        }

        @Override
        public void serializeElement(CoreInstance element)
        {
            this.fileSerializer.serializeElement(this.stream, element);
        }

        @Override
        public void serializeModuleMetadata(ModuleMetadata moduleMetadata)
        {
            this.fileSerializer.serializeModuleManifest(this.stream, moduleMetadata.getManifest());
            this.fileSerializer.serializeModuleSourceMetadata(this.stream, moduleMetadata.getSourceMetadata());
            this.fileSerializer.serializeModuleExternalReferenceMetadata(this.stream, moduleMetadata.getExternalReferenceMetadata());
            this.fileSerializer.serializeModuleBackReferenceMetadata(this.stream, moduleMetadata.getBackReferenceMetadata());
            this.fileSerializer.serializeModuleFunctionNameMetadata(this.stream, moduleMetadata.getFunctionNameMetadata());
        }
    }
}
