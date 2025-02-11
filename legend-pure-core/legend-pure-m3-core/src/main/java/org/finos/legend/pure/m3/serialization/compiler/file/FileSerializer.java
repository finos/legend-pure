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

package org.finos.legend.pure.m3.serialization.compiler.file;

import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileSerializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSerializer.class);

    private final FilePathProvider filePathProvider;
    private final ConcreteElementSerializer elementSerializer;
    private final ModuleMetadataSerializer moduleSerializer;

    private FileSerializer(FilePathProvider filePathProvider, ConcreteElementSerializer elementSerializer, ModuleMetadataSerializer moduleSerializer)
    {
        this.filePathProvider = filePathProvider;
        this.elementSerializer = elementSerializer;
        this.moduleSerializer = moduleSerializer;
    }

    // Serialize element to directory

    public Path serializeElement(Path directory, CoreInstance element)
    {
        return serializeElement(directory, element, this.filePathProvider.getDefaultVersion(), this.elementSerializer.getDefaultVersion(), this.elementSerializer.getReferenceIdProviders().getDefaultVersion());
    }

    public Path serializeElement(Path directory, CoreInstance element, int filePathVersion, int serializerVersion, int referenceIdVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(element, "element is required");

        long start = System.nanoTime();
        String elementPath = PackageableElement.getUserPathForPackageableElement(element);
        Path filePath = this.filePathProvider.getElementFilePath(directory, elementPath, filePathVersion);
        LOGGER.debug("Serializing {} to {}", elementPath, filePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = BinaryWriters.newBinaryWriter(new BufferedOutputStream(Files.newOutputStream(filePath))))
            {
                this.elementSerializer.serialize(writer, element, serializerVersion, referenceIdVersion);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing {} to {}", elementPath, filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing element ").append(elementPath);
            SourceInformation sourceInfo = element.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(builder.append(" (")).append(')');
            }
            builder.append(" to ").append(filePath);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing {} to {} in {}s", elementPath, filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize element to zip

    public String serializeElement(ZipOutputStream zipStream, CoreInstance element)
    {
        return serializeElement(zipStream, element, this.filePathProvider.getDefaultVersion(), this.elementSerializer.getDefaultVersion(), this.elementSerializer.getReferenceIdProviders().getDefaultVersion());
    }

    public String serializeElement(ZipOutputStream zipStream, CoreInstance element, int filePathVersion, int serializerVersion, int referenceIdVersion)
    {
        Objects.requireNonNull(zipStream, "directory is required");
        Objects.requireNonNull(element, "element is required");

        long start = System.nanoTime();
        String elementPath = PackageableElement.getUserPathForPackageableElement(element);
        String entryName = this.filePathProvider.getElementFilePath(elementPath, "/", filePathVersion);
        LOGGER.debug("Serializing {} to zip entry '{}'", elementPath, entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            try (Writer writer = BinaryWriters.newBinaryWriter(zipStream, false))
            {
                this.elementSerializer.serialize(writer, element, serializerVersion, referenceIdVersion);
            }
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing {} to zip entry '{}'", elementPath, entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing element ").append(elementPath);
            SourceInformation sourceInfo = element.getSourceInformation();
            if (sourceInfo != null)
            {
                sourceInfo.appendMessage(builder.append(" (")).append(')');
            }
            builder.append(" to ").append(entryName);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing {} to zip entry '{}' in {}s", elementPath, entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }

    // Serialize module manifest to directory

    public Path serializeModuleManifest(Path directory, ModuleManifest moduleManifest)
    {
        return serializeModuleManifest(directory, moduleManifest, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public Path serializeModuleManifest(Path directory, ModuleManifest moduleManifest, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleManifest, "module manifest is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleManifestFilePath(directory, moduleManifest.getModuleName(), filePathVersion);
        LOGGER.debug("Serializing module {} manifest to {}", moduleManifest.getModuleName(), filePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = BinaryWriters.newBinaryWriter(new BufferedOutputStream(Files.newOutputStream(filePath))))
            {
                this.moduleSerializer.serializeManifest(writer, moduleManifest, serializerVersion);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} manifest to {}", moduleManifest.getModuleName(), filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing manifest for module ").append(moduleManifest.getModuleName()).append(" to ").append(filePath);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} manifest to {} in {}s", moduleManifest.getModuleName(), filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize module manifest to zip

    public String serializeModuleManifest(ZipOutputStream zipStream, ModuleManifest moduleManifest)
    {
        return serializeModuleManifest(zipStream, moduleManifest, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public String serializeModuleManifest(ZipOutputStream zipStream, ModuleManifest moduleManifest, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(moduleManifest, "module manifest is required");

        long start = System.nanoTime();
        String entryName = this.filePathProvider.getModuleManifestFilePath(moduleManifest.getModuleName(), "/", filePathVersion);
        LOGGER.debug("Serializing module {} manifest to zip entry '{}'", moduleManifest.getModuleName(), entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            try (Writer writer = BinaryWriters.newBinaryWriter(zipStream, false))
            {
                this.moduleSerializer.serializeManifest(writer, moduleManifest, serializerVersion);
            }
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} manifest to zip entry '{}'", moduleManifest.getModuleName(), entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing manifest for module ").append(moduleManifest.getModuleName()).append(" to ").append(entryName);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} manifest to zip entry '{}' in {}s", moduleManifest.getModuleName(), entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }


    // Serialize module source metadata to directory

    public Path serializeModuleSourceMetadata(Path directory, ModuleSourceMetadata moduleSourceMetadata)
    {
        return serializeModuleSourceMetadata(directory, moduleSourceMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public Path serializeModuleSourceMetadata(Path directory, ModuleSourceMetadata moduleSourceMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleSourceMetadata, "module source metadata is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleSourceMetadataFilePath(directory, moduleSourceMetadata.getModuleName(), filePathVersion);
        LOGGER.debug("Serializing module {} source metadata to {}", moduleSourceMetadata.getModuleName(), filePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = BinaryWriters.newBinaryWriter(new BufferedOutputStream(Files.newOutputStream(filePath))))
            {
                this.moduleSerializer.serializeSourceMetadata(writer, moduleSourceMetadata, serializerVersion);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} source metadata to {}", moduleSourceMetadata.getModuleName(), filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing source metadata for module ").append(moduleSourceMetadata.getModuleName()).append(" to ").append(filePath);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} source metadata to {} in {}s", moduleSourceMetadata.getModuleName(), filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize module source metadata to zip

    public String serializeModuleSourceMetadata(ZipOutputStream zipStream, ModuleSourceMetadata moduleSourceMetadata)
    {
        return serializeModuleSourceMetadata(zipStream, moduleSourceMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public String serializeModuleSourceMetadata(ZipOutputStream zipStream, ModuleSourceMetadata moduleSourceMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(moduleSourceMetadata, "module source metadata is required");

        long start = System.nanoTime();
        String entryName = this.filePathProvider.getModuleSourceMetadataFilePath(moduleSourceMetadata.getModuleName(), "/", filePathVersion);
        LOGGER.debug("Serializing module {} source metadata to zip entry '{}'", moduleSourceMetadata.getModuleName(), entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            try (Writer writer = BinaryWriters.newBinaryWriter(zipStream, false))
            {
                this.moduleSerializer.serializeSourceMetadata(writer, moduleSourceMetadata, serializerVersion);
            }
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} source metadata to zip entry '{}'", moduleSourceMetadata.getModuleName(), entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing source metadata for module ").append(moduleSourceMetadata.getModuleName()).append(" to ").append(entryName);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} source metadata to zip entry '{}' in {}s", moduleSourceMetadata.getModuleName(), entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }

    
    // Serialize module external reference metadata to directory

    public Path serializeModuleExternalReferenceMetadata(Path directory, ModuleExternalReferenceMetadata moduleExtRefMetadata)
    {
        return serializeModuleExternalReferenceMetadata(directory, moduleExtRefMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public Path serializeModuleExternalReferenceMetadata(Path directory, ModuleExternalReferenceMetadata moduleExtRefMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleExtRefMetadata, "module external reference metadata is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleExternalReferenceMetadataFilePath(directory, moduleExtRefMetadata.getModuleName(), filePathVersion);
        LOGGER.debug("Serializing module {} external reference metadata to {}", moduleExtRefMetadata.getModuleName(), filePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = BinaryWriters.newBinaryWriter(new BufferedOutputStream(Files.newOutputStream(filePath))))
            {
                this.moduleSerializer.serializeExternalReferenceMetadata(writer, moduleExtRefMetadata, serializerVersion);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} external reference metadata to {}", moduleExtRefMetadata.getModuleName(), filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing external reference metadata for module ").append(moduleExtRefMetadata.getModuleName()).append(" to ").append(filePath);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} external reference metadata to {} in {}s", moduleExtRefMetadata.getModuleName(), filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize module external reference metadata to zip

    public String serializeModuleExternalReferenceMetadata(ZipOutputStream zipStream, ModuleExternalReferenceMetadata moduleExtRefMetadata)
    {
        return serializeModuleExternalReferenceMetadata(zipStream, moduleExtRefMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public String serializeModuleExternalReferenceMetadata(ZipOutputStream zipStream, ModuleExternalReferenceMetadata moduleExtRefMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(moduleExtRefMetadata, "module external reference metadata is required");

        long start = System.nanoTime();
        String entryName = this.filePathProvider.getModuleExternalReferenceMetadataFilePath(moduleExtRefMetadata.getModuleName(), "/", filePathVersion);
        LOGGER.debug("Serializing module {} external reference metadata to zip entry '{}'", moduleExtRefMetadata.getModuleName(), entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            try (Writer writer = BinaryWriters.newBinaryWriter(zipStream, false))
            {
                this.moduleSerializer.serializeExternalReferenceMetadata(writer, moduleExtRefMetadata, serializerVersion);
            }
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} external reference metadata to zip entry '{}'", moduleExtRefMetadata.getModuleName(), entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing external reference metadata for module ").append(moduleExtRefMetadata.getModuleName()).append(" to ").append(entryName);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} external reference metadata to zip entry '{}' in {}s", moduleExtRefMetadata.getModuleName(), entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }

    // Serialize module element back reference metadata to directory

    public void serializeModuleBackReferenceMetadata(Path directory, ModuleBackReferenceMetadata moduleBackRefMetadata)
    {
        serializeModuleBackReferenceMetadata(directory, moduleBackRefMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public void serializeModuleBackReferenceMetadata(Path directory, ModuleBackReferenceMetadata moduleBackRefMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleBackRefMetadata, "module back reference metadata is required");

        long start = System.nanoTime();
        LOGGER.debug("Serializing module {} back reference metadata", moduleBackRefMetadata.getModuleName());
        try
        {
            moduleBackRefMetadata.getBackReferences().forEach(br -> serializeModuleElementBackReferenceMetadata(directory, moduleBackRefMetadata.getModuleName(), br, filePathVersion, serializerVersion));
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} back reference metadata", moduleBackRefMetadata.getModuleName(), e);
            throw e;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} back reference metadata in {}s", moduleBackRefMetadata.getModuleName(), (end - start) / 1_000_000_000.0);
        }
    }

    // Serialize module element back reference metadata to zip

    public void serializeModuleBackReferenceMetadata(ZipOutputStream zipStream, ModuleBackReferenceMetadata moduleBackRefMetadata)
    {
        serializeModuleBackReferenceMetadata(zipStream, moduleBackRefMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public void serializeModuleBackReferenceMetadata(ZipOutputStream zipStream, ModuleBackReferenceMetadata moduleBackRefMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(moduleBackRefMetadata, "module back reference metadata is required");

        long start = System.nanoTime();
        LOGGER.debug("Serializing module {} back reference metadata", moduleBackRefMetadata.getModuleName());
        try
        {
            moduleBackRefMetadata.getBackReferences().forEach(br -> serializeModuleElementBackReferenceMetadata(zipStream, moduleBackRefMetadata.getModuleName(), br, filePathVersion, serializerVersion));
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} back reference metadata", moduleBackRefMetadata.getModuleName(), e);
            throw e;
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} back reference metadata in {}s", moduleBackRefMetadata.getModuleName(), (end - start) / 1_000_000_000.0);
        }
    }

    // Serialize element back reference metadata to directory

    public Path serializeElementBackReferenceMetadata(Path directory, String moduleName, ElementBackReferenceMetadata elementBackRefMetadata)
    {
        return serializeModuleElementBackReferenceMetadata(directory, moduleName, elementBackRefMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public Path serializeModuleElementBackReferenceMetadata(Path directory, String moduleName, ElementBackReferenceMetadata elementBackRefMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        Objects.requireNonNull(elementBackRefMetadata, "element back reference metadata is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleElementBackReferenceMetadataFilePath(directory, moduleName, elementBackRefMetadata.getElementPath(), filePathVersion);
        LOGGER.debug("Serializing module {} element {} back reference metadata to {}", moduleName, elementBackRefMetadata.getElementPath(), filePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            try (Writer writer = BinaryWriters.newBinaryWriter(new BufferedOutputStream(Files.newOutputStream(filePath))))
            {
                this.moduleSerializer.serializeBackReferenceMetadata(writer, elementBackRefMetadata, serializerVersion);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} element {} back reference metadata to {}", moduleName, elementBackRefMetadata.getElementPath(), filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing ").append(elementBackRefMetadata.getElementPath()).append(" back reference metadata for module ").append(moduleName).append(" to ").append(filePath);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} element {} back reference metadata to {} in {}s", moduleName, elementBackRefMetadata.getElementPath(), filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize element back reference metadata to zip

    public String serializeModuleElementBackReferenceMetadata(ZipOutputStream zipStream, String moduleName, ElementBackReferenceMetadata elementBackRefMetadata)
    {
        return serializeModuleElementBackReferenceMetadata(zipStream, moduleName, elementBackRefMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public String serializeModuleElementBackReferenceMetadata(ZipOutputStream zipStream, String moduleName, ElementBackReferenceMetadata elementBackRefMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(moduleName, "module name is required");
        Objects.requireNonNull(elementBackRefMetadata, "module source metadata is required");

        long start = System.nanoTime();
        String entryName = this.filePathProvider.getModuleElementBackReferenceMetadataFilePath(moduleName, elementBackRefMetadata.getElementPath(), "/", filePathVersion);
        LOGGER.debug("Serializing module {} element {} back reference metadata to zip entry '{}'", moduleName, elementBackRefMetadata.getElementPath(), entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            try (Writer writer = BinaryWriters.newBinaryWriter(zipStream, false))
            {
                this.moduleSerializer.serializeBackReferenceMetadata(writer, elementBackRefMetadata, serializerVersion);
            }
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} element {} back reference metadata to zip entry '{}'", moduleName, elementBackRefMetadata.getElementPath(), entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing ").append(elementBackRefMetadata.getElementPath()).append(" back reference metadata for module ").append(moduleName).append(" to ").append(entryName);
            String eMessage = e.getMessage();
            if (eMessage != null)
            {
                builder.append(": ").append(eMessage);
            }
            throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished serializing module {} element {} back reference metadata to zip entry '{}' in {}s", moduleName, elementBackRefMetadata.getElementPath(), entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }

    // Miscellaneous

    public FileDeserializer getDeserializer()
    {
        return FileDeserializer.builder()
                .withFilePathProvider(this.filePathProvider)
                .withConcreteElementDeserializer(this.elementSerializer.getDeserializer())
                .withModuleMetadataSerializer(this.moduleSerializer)
                .build();
    }

    // Builder

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private FilePathProvider filePathProvider;
        private ConcreteElementSerializer elementSerializer;
        private ModuleMetadataSerializer moduleSerializer;

        private Builder()
        {
        }

        public Builder withFilePathProvider(FilePathProvider filePathProvider)
        {
            this.filePathProvider = filePathProvider;
            return this;
        }

        public Builder withConcreteElementSerializer(ConcreteElementSerializer elementSerializer)
        {
            this.elementSerializer = elementSerializer;
            return this;
        }

        public Builder withModuleMetadataSerializer(ModuleMetadataSerializer moduleSerializer)
        {
            this.moduleSerializer = moduleSerializer;
            return this;
        }

        public Builder withSerializers(ConcreteElementSerializer elementSerializer, ModuleMetadataSerializer moduleSerializer)
        {
            return withConcreteElementSerializer(elementSerializer)
                    .withModuleMetadataSerializer(moduleSerializer);
        }

        public FileSerializer build()
        {
            Objects.requireNonNull(this.filePathProvider, "file path provider is required");
            Objects.requireNonNull(this.elementSerializer, "concrete element serializer is required");
            Objects.requireNonNull(this.moduleSerializer, "module serializer is required");
            return new FileSerializer(this.filePathProvider, this.elementSerializer, this.moduleSerializer);
        }
    }
}
