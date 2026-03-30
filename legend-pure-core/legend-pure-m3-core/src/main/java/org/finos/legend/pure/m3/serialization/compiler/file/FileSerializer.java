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
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleBackReferenceIndex;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleFunctionNameMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
            writeIfModified(filePath, stream -> this.elementSerializer.serialize(stream, element, serializerVersion, referenceIdVersion));
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
            this.elementSerializer.serialize(zipStream, element, serializerVersion, referenceIdVersion);
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
            writeIfModified(filePath, stream -> this.moduleSerializer.serializeManifest(stream, moduleManifest, serializerVersion));
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
            this.moduleSerializer.serializeManifest(zipStream, moduleManifest, serializerVersion);
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
            writeIfModified(filePath, stream -> this.moduleSerializer.serializeSourceMetadata(stream, moduleSourceMetadata, serializerVersion));
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
            this.moduleSerializer.serializeSourceMetadata(zipStream, moduleSourceMetadata, serializerVersion);
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
            writeIfModified(filePath, stream -> this.moduleSerializer.serializeExternalReferenceMetadata(stream, moduleExtRefMetadata, serializerVersion));
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
            this.moduleSerializer.serializeExternalReferenceMetadata(zipStream, moduleExtRefMetadata, serializerVersion);
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
            serializeModuleBackReferenceIndex(directory, ModuleBackReferenceIndex.fromBackReferenceMetadata(moduleBackRefMetadata), filePathVersion, serializerVersion);
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
            serializeModuleBackReferenceIndex(zipStream, ModuleBackReferenceIndex.fromBackReferenceMetadata(moduleBackRefMetadata), filePathVersion, serializerVersion);
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
            writeIfModified(filePath, stream -> this.moduleSerializer.serializeBackReferenceMetadata(stream, elementBackRefMetadata, serializerVersion));
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
            this.moduleSerializer.serializeBackReferenceMetadata(zipStream, elementBackRefMetadata, serializerVersion);
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


    // Serialize module function name metadata to directory

    // Serialize module back reference index to directory

    public Path serializeModuleBackReferenceIndex(Path directory, ModuleBackReferenceIndex backReferenceIndex)
    {
        return serializeModuleBackReferenceIndex(directory, backReferenceIndex, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public Path serializeModuleBackReferenceIndex(Path directory, ModuleBackReferenceIndex backReferenceIndex, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(backReferenceIndex, "back reference index is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleBackReferenceIndexFilePath(directory, backReferenceIndex.getModuleName(), filePathVersion);
        LOGGER.debug("Serializing module {} back reference index to {}", backReferenceIndex.getModuleName(), filePath);
        try
        {
            Files.createDirectories(filePath.getParent());
            try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(filePath)))
            {
                this.moduleSerializer.serializeBackReferenceIndex(stream, backReferenceIndex, serializerVersion);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} back reference index to {}", backReferenceIndex.getModuleName(), filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing back reference index for module ").append(backReferenceIndex.getModuleName()).append(" to ").append(filePath);
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
            LOGGER.debug("Finished serializing module {} back reference index to {} in {}s", backReferenceIndex.getModuleName(), filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize module back reference index to zip

    public String serializeModuleBackReferenceIndex(ZipOutputStream zipStream, ModuleBackReferenceIndex backReferenceIndex)
    {
        return serializeModuleBackReferenceIndex(zipStream, backReferenceIndex, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public String serializeModuleBackReferenceIndex(ZipOutputStream zipStream, ModuleBackReferenceIndex backReferenceIndex, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(backReferenceIndex, "back reference index is required");

        long start = System.nanoTime();
        String entryName = this.filePathProvider.getModuleBackReferenceIndexFilePath(backReferenceIndex.getModuleName(), "/", filePathVersion);
        LOGGER.debug("Serializing module {} back reference index to zip entry '{}'", backReferenceIndex.getModuleName(), entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            this.moduleSerializer.serializeBackReferenceIndex(zipStream, backReferenceIndex, serializerVersion);
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} back reference index to zip entry '{}'", backReferenceIndex.getModuleName(), entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing back reference index for module ").append(backReferenceIndex.getModuleName()).append(" to ").append(entryName);
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
            LOGGER.debug("Finished serializing module {} back reference index to zip entry '{}' in {}s", backReferenceIndex.getModuleName(), entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }

    // Serialize module function name metadata to directory

    public Path serializeModuleFunctionNameMetadata(Path directory, ModuleFunctionNameMetadata moduleFunctionNameMetadata)
    {
        return serializeModuleFunctionNameMetadata(directory, moduleFunctionNameMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public Path serializeModuleFunctionNameMetadata(Path directory, ModuleFunctionNameMetadata moduleFunctionNameMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleFunctionNameMetadata, "module function name metadata is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleFunctionNameMetadataFilePath(directory, moduleFunctionNameMetadata.getModuleName(), filePathVersion);
        LOGGER.debug("Serializing module {} function name metadata to {}", moduleFunctionNameMetadata.getModuleName(), filePath);
        try
        {
            writeIfModified(filePath, stream -> this.moduleSerializer.serializeFunctionNameMetadata(stream, moduleFunctionNameMetadata, serializerVersion));
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} function name metadata to {}", moduleFunctionNameMetadata.getModuleName(), filePath, e);
            StringBuilder builder = new StringBuilder("Error serializing function name metadata for module ").append(moduleFunctionNameMetadata.getModuleName()).append(" to ").append(filePath);
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
            LOGGER.debug("Finished serializing module {} function name metadata to {} in {}s", moduleFunctionNameMetadata.getModuleName(), filePath, (end - start) / 1_000_000_000.0);
        }
        return filePath;
    }

    // Serialize module function name metadata to zip

    public String serializeModuleFunctionNameMetadata(ZipOutputStream zipStream, ModuleFunctionNameMetadata moduleFunctionNameMetadata)
    {
        return serializeModuleFunctionNameMetadata(zipStream, moduleFunctionNameMetadata, this.filePathProvider.getDefaultVersion(), this.moduleSerializer.getDefaultVersion());
    }

    public String serializeModuleFunctionNameMetadata(ZipOutputStream zipStream, ModuleFunctionNameMetadata moduleFunctionNameMetadata, int filePathVersion, int serializerVersion)
    {
        Objects.requireNonNull(zipStream, "zip stream is required");
        Objects.requireNonNull(moduleFunctionNameMetadata, "module function name metadata is required");

        long start = System.nanoTime();
        String entryName = this.filePathProvider.getModuleFunctionNameMetadataFilePath(moduleFunctionNameMetadata.getModuleName(), "/", filePathVersion);
        LOGGER.debug("Serializing module {} function name metadata to zip entry '{}'", moduleFunctionNameMetadata.getModuleName(), entryName);
        try
        {
            zipStream.putNextEntry(new ZipEntry(entryName));
            this.moduleSerializer.serializeFunctionNameMetadata(zipStream, moduleFunctionNameMetadata, serializerVersion);
            zipStream.closeEntry();
        }
        catch (Exception e)
        {
            LOGGER.error("Error serializing module {} function name metadata to zip entry '{}'", moduleFunctionNameMetadata.getModuleName(), entryName, e);
            StringBuilder builder = new StringBuilder("Error serializing function name metadata for module ").append(moduleFunctionNameMetadata.getModuleName()).append(" to ").append(entryName);
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
            LOGGER.debug("Finished serializing module {} function name metadata to zip entry '{}' in {}s", moduleFunctionNameMetadata.getModuleName(), entryName, (end - start) / 1_000_000_000.0);
        }
        return entryName;
    }

    // Atomic file write helper

    /**
     * Write serialized bytes to {@code targetPath} atomically, skipping the
     * replace when the new content is byte-for-byte identical to the existing file.
     * <p>
     * Content is first written to a sibling {@code <name>.tmp} file. Once the
     * write is complete:
     * <ol>
     *   <li>If the target file already exists <em>and</em> has the same size and
     *       identical content as the tmp file, the tmp file is deleted and the
     *       target is left untouched.</li>
     *   <li>Otherwise the tmp file is moved over the target using
     *       {@link StandardCopyOption#ATOMIC_MOVE} +
     *       {@link StandardCopyOption#REPLACE_EXISTING}. If the filesystem does
     *       not support atomic moves a non-atomic replacement is used as a
     *       fallback, which is still safer than writing directly because a reader
     *       will at worst see the previous complete file rather than a
     *       partially-written one.</li>
     * </ol>
     * </p>
     *
     * <p><strong>Concurrency note:</strong> the content comparison in step 1 and
     * the subsequent {@code Files.move} in step 2 are <em>not</em> performed as a
     * single atomic operation.  A second thread (or process) could replace the
     * target file between the comparison and the move.  In that window this
     * method might overwrite a newer version of the file with the same bytes it
     * just decided were already present, or might skip a replacement that is now
     * necessary.  This is acceptable for the intended use-case — serialising Pure
     * compiler artefacts where all writers produce deterministic, content-identical
     * output for the same logical element — but callers that require a
     * compare-and-swap guarantee must implement their own external synchronisation.
     * </p>
     *
     * <p>This method eliminates the race condition that occurs during {@code -T N}
     * parallel Maven builds where {@code exec:java} (in-process) invocations
     * running on separate threads can interleave a write to a {@code .pelt} file
     * with a read of the same file by a concurrently-compiling downstream module.
     * </p>
     */
    @FunctionalInterface
    private interface StreamWriter
    {
        void write(OutputStream stream) throws Exception;
    }

    private static final int COMPARE_BUFFER_SIZE = 8192;

    private static void writeIfModified(Path targetPath, StreamWriter writer) throws IOException
    {
        Files.createDirectories(targetPath.getParent());
        Path tmpPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(tmpPath)))
        {
            writer.write(stream);
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        // Skip the replace when the target already exists with identical content
        if (Files.exists(targetPath) && contentEquals(tmpPath, targetPath))
        {
            LOGGER.debug("Skipping replace of {} — content unchanged", targetPath);
            Files.delete(tmpPath);
            return;
        }

        try
        {
            Files.move(tmpPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException e)
        {
            LOGGER.warn("Atomic move not supported on this filesystem; falling back to non-atomic replace for {}", targetPath);
            Files.move(tmpPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Returns {@code true} iff {@code a} and {@code b} have the same size and
     * identical byte content.  Uses a fixed-size read buffer to avoid loading
     * large files into memory all at once.
     */
    private static boolean contentEquals(Path a, Path b) throws IOException
    {
        if (Files.size(a) != Files.size(b))
        {
            return false;
        }
        byte[] bufA = new byte[COMPARE_BUFFER_SIZE];
        byte[] bufB = new byte[COMPARE_BUFFER_SIZE];
        try (InputStream inA = new BufferedInputStream(Files.newInputStream(a), COMPARE_BUFFER_SIZE);
             InputStream inB = new BufferedInputStream(Files.newInputStream(b), COMPARE_BUFFER_SIZE))
        {
            int nA;
            while ((nA = inA.read(bufA)) != -1)
            {
                int nB = 0;
                while (nB < nA)
                {
                    int read = inB.read(bufB, nB, nA - nB);
                    if (read == -1)
                    {
                        return false; // b ended early — shouldn't happen after size check
                    }
                    nB += read;
                }
                for (int i = 0; i < nA; i++)
                {
                    if (bufA[i] != bufB[i])
                    {
                        return false;
                    }
                }
            }
        }
        return true;
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
