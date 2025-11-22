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

import org.finos.legend.pure.m3.serialization.compiler.element.ConcreteElementDeserializer;
import org.finos.legend.pure.m3.serialization.compiler.element.DeserializedConcreteElement;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ElementBackReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleExternalReferenceMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleFunctionNameMetadata;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleManifest;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleMetadataSerializer;
import org.finos.legend.pure.m3.serialization.compiler.metadata.ModuleSourceMetadata;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

public class FileDeserializer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeserializer.class);

    private final FilePathProvider filePathProvider;
    private final ConcreteElementDeserializer elementDeserializer;
    private final ModuleMetadataSerializer moduleSerializer;

    private FileDeserializer(FilePathProvider filePathProvider, ConcreteElementDeserializer elementDeserializer, ModuleMetadataSerializer moduleSerializer)
    {
        this.filePathProvider = filePathProvider;
        this.elementDeserializer = elementDeserializer;
        this.moduleSerializer = moduleSerializer;
    }

    // Deserialize element from directory

    public boolean elementExists(Path directory, String elementPath)
    {
        return elementExists(directory, elementPath, this.filePathProvider.getDefaultVersion());
    }

    public boolean elementExists(Path directory, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(elementPath, "element path is required");
        return Files.exists(this.filePathProvider.getElementFilePath(directory, elementPath, filePathVersion));
    }

    /**
     * Deserialize an element from a file in a directory. Throws an {@link ElementNotFoundException} if the element
     * cannot be found.
     *
     * @param directory   directory to search for the element file
     * @param elementPath element path
     * @return deserialized element
     * @throws ElementNotFoundException if the element cannot be found
     */
    public DeserializedConcreteElement deserializeElement(Path directory, String elementPath)
    {
        return deserializeElement(directory, elementPath, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize an element from a file in a directory using the given file path version. Throws an
     * {@link ElementNotFoundException} if the element cannot be found.
     *
     * @param directory       directory to search for the element file
     * @param elementPath     element path
     * @param filePathVersion file path version
     * @return deserialized element
     * @throws ElementNotFoundException if the element cannot be found
     */
    public DeserializedConcreteElement deserializeElement(Path directory, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(elementPath, "element path is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getElementFilePath(directory, elementPath, filePathVersion);
        LOGGER.debug("Deserializing {} from {}", elementPath, filePath);
        try (Reader reader = BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(filePath))))
        {
            return this.elementDeserializer.deserialize(reader);
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            LOGGER.error("Error deserializing {} from {}", elementPath, filePath, e);
            throw new ElementNotFoundException(elementPath, "cannot find file " + filePath, e);
        }
        catch (Exception e)
        {
            LOGGER.error("Error deserializing {} from {}", elementPath, filePath, e);
            if (Files.notExists(filePath))
            {
                throw new ElementNotFoundException(elementPath, "cannot find file " + filePath, e);
            }
            StringBuilder builder = new StringBuilder("Error deserializing element ").append(elementPath).append(" from ").append(filePath);
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
            LOGGER.debug("Finished deserializing {} from {} after {}s", elementPath, filePath, (end - start) / 1_000_000_000.0);
        }
    }

    // Deserialize element from ClassLoader

    public boolean elementExists(ClassLoader classLoader, String elementPath)
    {
        return elementExists(classLoader, elementPath, this.filePathProvider.getDefaultVersion());
    }

    public boolean elementExists(ClassLoader classLoader, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(elementPath, "element path is required");
        return classLoader.getResource(this.filePathProvider.getElementResourceName(elementPath, filePathVersion)) != null;
    }

    /**
     * Deserialize an element from a resource in a class loader. Throws an {@link ElementNotFoundException} if the
     * element cannot be found.
     *
     * @param classLoader class loader to search for the element resource
     * @param elementPath element path
     * @return deserialized element
     * @throws ElementNotFoundException if the element cannot be found
     */
    public DeserializedConcreteElement deserializeElement(ClassLoader classLoader, String elementPath)
    {
        return deserializeElement(classLoader, elementPath, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize an element from a resource in a class loader using the given file path version. Throws an
     * {@link ElementNotFoundException} if the element cannot be found.
     *
     * @param classLoader     class loader to search for the element resource
     * @param elementPath     element path
     * @param filePathVersion file path version
     * @return deserialized element
     * @throws ElementNotFoundException if the element cannot be found
     */
    public DeserializedConcreteElement deserializeElement(ClassLoader classLoader, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(elementPath, "element path is required");

        long start = System.nanoTime();
        String resourceName = this.filePathProvider.getElementResourceName(elementPath, filePathVersion);
        LOGGER.debug("Deserializing {} from resource '{}'", elementPath, resourceName);
        try
        {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
            {
                LOGGER.error("Error deserializing {} from resource '{}': cannot find resource", elementPath, resourceName);
                throw new ElementNotFoundException(elementPath, "cannot find resource " + resourceName);
            }
            LOGGER.debug("Deserializing {} from resource '{}': {}", elementPath, resourceName, url);
            try (Reader reader = BinaryReaders.newBinaryReader(url.openStream()))
            {
                return this.elementDeserializer.deserialize(reader);
            }
            catch (Exception e)
            {
                LOGGER.error("Error deserializing {} from resource '{}'", elementPath, resourceName, e);
                StringBuilder builder = new StringBuilder("Error deserializing element ").append(elementPath)
                        .append(" from resource ").append(resourceName)
                        .append(" (").append(url).append(")");
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
            }
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing {} from resource '{}' in {}s", elementPath, resourceName, (end - start) / 1_000_000_000.0);
        }
    }


    // Deserialize module manifest from directory

    public boolean moduleManifestExists(Path directory, String moduleName)
    {
        return moduleManifestExists(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleManifestExists(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return Files.exists(this.filePathProvider.getModuleManifestFilePath(directory, moduleName, filePathVersion));
    }

    /**
     * Deserialize module manifest from a file in a directory. Throws a {@link ModuleMetadataNotFoundException} if the
     * module manifest cannot be found.
     *
     * @param directory  directory to search for the module manifest file
     * @param moduleName module name
     * @return module manifest
     * @throws ModuleMetadataNotFoundException if the module manifest cannot be found
     */
    public ModuleManifest deserializeModuleManifest(Path directory, String moduleName)
    {
        return deserializeModuleManifest(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module manifest from a file in a directory using the given file path version. Throws a
     * {@link ModuleMetadataNotFoundException} if the module manifest cannot be found.
     *
     * @param directory       directory to search for the module manifest file
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module manifest
     * @throws ModuleMetadataNotFoundException if the module manifest cannot be found
     */
    public ModuleManifest deserializeModuleManifest(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleManifestFilePath(directory, moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} manifest from {}", moduleName, filePath);
        try (Reader reader = BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(filePath))))
        {
            return this.moduleSerializer.deserializeManifest(reader);
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            LOGGER.error("Error deserializing module {} manifest from {}", moduleName, filePath, e);
            throw new ModuleMetadataNotFoundException(moduleName, "manifest", "cannot find file " + filePath, e);
        }
        catch (Exception e)
        {
            LOGGER.error("Error deserializing module {} manifest from {}", moduleName, filePath, e);
            if (Files.notExists(filePath))
            {
                throw new ModuleMetadataNotFoundException(moduleName, "manifest", "cannot find file " + filePath, e);
            }
            StringBuilder builder = new StringBuilder("Error deserializing manifest for module ").append(moduleName).append(" from ").append(filePath);
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
            LOGGER.debug("Finished deserializing module {} manifest from {} in {}s", moduleName, filePath, (end - start) / 1_000_000_000.0);
        }
    }

    // Deserialize module manifest from ClassLoader

    public boolean moduleManifestExists(ClassLoader classLoader, String moduleName)
    {
        return moduleManifestExists(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleManifestExists(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return classLoader.getResource(this.filePathProvider.getModuleManifestResourceName(moduleName, filePathVersion)) != null;
    }

    /**
     * Deserialize module manifest from a resource in a class loader. Throws a {@link ModuleMetadataNotFoundException}
     * if the module manifest cannot be found.
     *
     * @param classLoader class loader to search for the module manifest resource
     * @param moduleName  module name
     * @return module manifest
     * @throws ModuleMetadataNotFoundException if the module manifest cannot be found
     */
    public ModuleManifest deserializeModuleManifest(ClassLoader classLoader, String moduleName)
    {
        return deserializeModuleManifest(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module manifest from a resource in a class loader using the given file path version. Throws a
     * {@link ModuleMetadataNotFoundException} if the module manifest cannot be found.
     *
     * @param classLoader     class loader to search for the module manifest resource
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module manifest
     * @throws ModuleMetadataNotFoundException if the module manifest cannot be found
     */
    public ModuleManifest deserializeModuleManifest(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        String resourceName = this.filePathProvider.getModuleManifestResourceName(moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} manifest from resource '{}'", moduleName, resourceName);
        try
        {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
            {
                throw new ModuleMetadataNotFoundException(moduleName, "manifest", "cannot find resource " + resourceName);
            }
            LOGGER.debug("Deserializing module {} manifest from resource '{}': {}", moduleName, resourceName, url);
            try (Reader reader = BinaryReaders.newBinaryReader(url.openStream()))
            {
                return this.moduleSerializer.deserializeManifest(reader);
            }
            catch (Exception e)
            {
                LOGGER.error("Error deserializing module {} manifest from resource '{}'", moduleName, resourceName, e);
                StringBuilder builder = new StringBuilder("Error deserializing manifest for module ").append(moduleName)
                        .append(" from resource ").append(resourceName)
                        .append(" (").append(url).append(")");
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
            }
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing module {} manifest from resource '{}' in {}s", moduleName, resourceName, (end - start) / 1_000_000_000.0);
        }
    }


    // Deserialize module source metadata from directory

    public boolean moduleSourceMetadataExists(Path directory, String moduleName)
    {
        return moduleSourceMetadataExists(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleSourceMetadataExists(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return Files.exists(this.filePathProvider.getModuleSourceMetadataFilePath(directory, moduleName, filePathVersion));
    }

    /**
     * Deserialize module source metadata from a file in a directory. Throws a {@link ModuleMetadataNotFoundException}
     * if the module source metadata cannot be found.
     *
     * @param directory  directory to search for the module source metadata file
     * @param moduleName module name
     * @return module source metadata
     * @throws ModuleMetadataNotFoundException if the module source metadata cannot be found
     */
    public ModuleSourceMetadata deserializeModuleSourceMetadata(Path directory, String moduleName)
    {
        return deserializeModuleSourceMetadata(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module source metadata from a file in a directory using the given file path version. Throws a
     * {@link ModuleMetadataNotFoundException} if the module source metadata cannot be found.
     *
     * @param directory       directory to search for the module source metadata file
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module source metadata
     * @throws ModuleMetadataNotFoundException if the module source metadata cannot be found
     */
    public ModuleSourceMetadata deserializeModuleSourceMetadata(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleSourceMetadataFilePath(directory, moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} source metadata from {}", moduleName, filePath);
        try (Reader reader = BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(filePath))))
        {
            return this.moduleSerializer.deserializeSourceMetadata(reader);
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            LOGGER.error("Error deserializing module {} source metadata from {}", moduleName, filePath, e);
            throw new ModuleMetadataNotFoundException(moduleName, "source metadata", "cannot find file " + filePath, e);
        }
        catch (Exception e)
        {
            LOGGER.error("Error deserializing module {} source metadata from {}", moduleName, filePath, e);
            if (Files.notExists(filePath))
            {
                throw new ModuleMetadataNotFoundException(moduleName, "source metadata", "cannot find file " + filePath, e);
            }
            StringBuilder builder = new StringBuilder("Error deserializing source metadata for module ").append(moduleName).append(" from ").append(filePath);
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
            LOGGER.debug("Finished deserializing module {} source metadata from {} in {}s", moduleName, filePath, (end - start) / 1_000_000_000.0);
        }
    }

    // Deserialize module source metadata from ClassLoader

    public boolean moduleSourceMetadataExists(ClassLoader classLoader, String moduleName)
    {
        return moduleSourceMetadataExists(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleSourceMetadataExists(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return classLoader.getResource(this.filePathProvider.getModuleSourceMetadataResourceName(moduleName, filePathVersion)) != null;
    }

    /**
     * Deserialize module source metadata from a resource in a class loader. Throws a
     * {@link ModuleMetadataNotFoundException} if the module source metadata cannot be found.
     *
     * @param classLoader class loader to search for the module source metadata resource
     * @param moduleName  module name
     * @return module source metadata
     * @throws ModuleMetadataNotFoundException if the module source metadata cannot be found
     */
    public ModuleSourceMetadata deserializeModuleSourceMetadata(ClassLoader classLoader, String moduleName)
    {
        return deserializeModuleSourceMetadata(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module source metadata from a resource in a class loader using the given file path version. Throws a
     * {@link ModuleMetadataNotFoundException} if the module source metadata cannot be found.
     *
     * @param classLoader     class loader to search for the module source metadata resource
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module source metadata
     * @throws ModuleMetadataNotFoundException if the module source metadata cannot be found
     */
    public ModuleSourceMetadata deserializeModuleSourceMetadata(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        String resourceName = this.filePathProvider.getModuleSourceMetadataResourceName(moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} source metadata from resource '{}'", moduleName, resourceName);
        try
        {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
            {
                throw new ModuleMetadataNotFoundException(moduleName, "source metadata", "cannot find resource " + resourceName);
            }
            LOGGER.debug("Deserializing module {} source metadata from resource '{}': {}", moduleName, resourceName, url);
            try (Reader reader = BinaryReaders.newBinaryReader(url.openStream()))
            {
                return this.moduleSerializer.deserializeSourceMetadata(reader);
            }
            catch (Exception e)
            {
                LOGGER.error("Error deserializing module {} source metadata from resource '{}'", moduleName, resourceName, e);
                StringBuilder builder = new StringBuilder("Error deserializing source metadata for module ").append(moduleName)
                        .append(" from resource ").append(resourceName)
                        .append(" (").append(url).append(")");
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
            }
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing module {} source metadata from resource '{}' in {}s", moduleName, resourceName, (end - start) / 1_000_000_000.0);
        }
    }


    // Deserialize module external reference metadata from directory

    public boolean moduleExternalReferenceMetadataExists(Path directory, String moduleName)
    {
        return moduleExternalReferenceMetadataExists(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleExternalReferenceMetadataExists(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return Files.exists(this.filePathProvider.getModuleExternalReferenceMetadataFilePath(directory, moduleName, filePathVersion));
    }

    /**
     * Deserialize module external reference metadata from a file in a directory. Throws a
     * {@link ModuleMetadataNotFoundException} if the module external reference metadata cannot be found.
     *
     * @param directory  directory to search for the module external reference metadata file
     * @param moduleName module name
     * @return module external reference metadata
     * @throws ModuleMetadataNotFoundException if the module external reference metadata cannot be found
     */
    public ModuleExternalReferenceMetadata deserializeModuleExternalReferenceMetadata(Path directory, String moduleName)
    {
        return deserializeModuleExternalReferenceMetadata(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module external reference metadata from a file in a directory using the given file path version.
     * Throws a {@link ModuleMetadataNotFoundException} if the module external reference metadata cannot be found.
     *
     * @param directory       directory to search for the module external reference metadata file
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module external reference metadata
     * @throws ModuleMetadataNotFoundException if the module external reference metadata cannot be found
     */
    public ModuleExternalReferenceMetadata deserializeModuleExternalReferenceMetadata(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleExternalReferenceMetadataFilePath(directory, moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} external reference metadata from {}", moduleName, filePath);
        try (Reader reader = BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(filePath))))
        {
            return this.moduleSerializer.deserializeExternalReferenceMetadata(reader);
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            LOGGER.error("Error deserializing module {} external reference metadata from {}", moduleName, filePath, e);
            throw new ModuleMetadataNotFoundException(moduleName, "external reference metadata", "cannot find file " + filePath, e);
        }
        catch (Exception e)
        {
            LOGGER.error("Error deserializing module {} external reference metadata from {}", moduleName, filePath, e);
            if (Files.notExists(filePath))
            {
                throw new ModuleMetadataNotFoundException(moduleName, "external reference metadata", "cannot find file " + filePath, e);
            }
            StringBuilder builder = new StringBuilder("Error deserializing external reference metadata for module ").append(moduleName).append(" from ").append(filePath);
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
            LOGGER.debug("Finished deserializing module {} external reference metadata from {} in {}s", moduleName, filePath, (end - start) / 1_000_000_000.0);
        }
    }

    // Deserialize module external reference metadata from ClassLoader

    public boolean moduleExternalReferenceMetadataExists(ClassLoader classLoader, String moduleName)
    {
        return moduleExternalReferenceMetadataExists(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleExternalReferenceMetadataExists(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return classLoader.getResource(this.filePathProvider.getModuleExternalReferenceMetadataResourceName(moduleName, filePathVersion)) != null;
    }

    /**
     * Deserialize module external reference metadata from a resource in a class loader. Throws a
     * {@link ModuleMetadataNotFoundException} if the module external reference metadata cannot be found.
     *
     * @param classLoader class loader to search for the module external reference metadata resource
     * @param moduleName  module name
     * @return module external reference metadata
     * @throws ModuleMetadataNotFoundException if the module external reference metadata cannot be found
     */
    public ModuleExternalReferenceMetadata deserializeModuleExternalReferenceMetadata(ClassLoader classLoader, String moduleName)
    {
        return deserializeModuleExternalReferenceMetadata(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module external reference metadata from a resource in a class loader using the given file path
     * version. Throws a {@link ModuleMetadataNotFoundException} if the module external reference metadata cannot be
     * found.
     *
     * @param classLoader     class loader to search for the module external reference metadata resource
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module external reference metadata
     * @throws ModuleMetadataNotFoundException if the module external reference metadata cannot be found
     */
    public ModuleExternalReferenceMetadata deserializeModuleExternalReferenceMetadata(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        String resourceName = this.filePathProvider.getModuleExternalReferenceMetadataResourceName(moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} external reference metadata from resource '{}'", moduleName, resourceName);
        try
        {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
            {
                throw new ModuleMetadataNotFoundException(moduleName, "external reference metadata", "cannot find resource " + resourceName);
            }
            LOGGER.debug("Deserializing module {} external reference metadata from resource '{}': {}", moduleName, resourceName, url);
            try (Reader reader = BinaryReaders.newBinaryReader(url.openStream()))
            {
                return this.moduleSerializer.deserializeExternalReferenceMetadata(reader);
            }
            catch (Exception e)
            {
                LOGGER.error("Error deserializing module {} external reference metadata from resource '{}'", moduleName, resourceName, e);
                StringBuilder builder = new StringBuilder("Error deserializing external reference metadata for module ").append(moduleName)
                        .append(" from resource ").append(resourceName)
                        .append(" (").append(url).append(")");
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
            }
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing module {} external reference metadata from resource '{}' in {}s", moduleName, resourceName, (end - start) / 1_000_000_000.0);
        }
    }


    // Deserialize module element back reference metadata from directory

    public boolean moduleElementBackReferenceMetadataExists(Path directory, String moduleName, String elementPath)
    {
        return moduleElementBackReferenceMetadataExists(directory, moduleName, elementPath, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleElementBackReferenceMetadataExists(Path directory, String moduleName, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        Objects.requireNonNull(elementPath, "element path is required");
        return Files.exists(this.filePathProvider.getModuleElementBackReferenceMetadataFilePath(directory, moduleName, elementPath, filePathVersion));
    }

    /**
     * Deserialize module element back reference metadata from a file in a directory. Throws a
     * {@link ModuleElementMetadataNotFoundException} if the module element back reference metadata cannot be found.
     *
     * @param directory  directory to search for the module element back reference metadata file
     * @param moduleName module name
     * @return module element back reference metadata
     * @throws ModuleElementMetadataNotFoundException if the module element back reference metadata cannot be found
     */
    public ElementBackReferenceMetadata deserializeModuleElementBackReferenceMetadata(Path directory, String moduleName, String elementPath)
    {
        return deserializeModuleElementBackReferenceMetadata(directory, moduleName, elementPath, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module element back reference metadata from a file in a directory using the given file path version.
     * Throws a {@link ModuleElementMetadataNotFoundException} if the module element back reference metadata cannot be
     * found.
     *
     * @param directory       directory to search for the module element back reference metadata file
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module element back reference metadata
     * @throws ModuleElementMetadataNotFoundException if the module element back reference metadata cannot be found
     */
    public ElementBackReferenceMetadata deserializeModuleElementBackReferenceMetadata(Path directory, String moduleName, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        Objects.requireNonNull(elementPath, "element path is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleElementBackReferenceMetadataFilePath(directory, moduleName, elementPath, filePathVersion);
        LOGGER.debug("Deserializing module {} element back reference metadata from {}", moduleName, filePath);
        try (Reader reader = BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(filePath))))
        {
            return this.moduleSerializer.deserializeBackReferenceMetadata(reader);
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            LOGGER.error("Error deserializing module {} element {} back reference metadata from {}", moduleName, elementPath, filePath, e);
            throw new ModuleElementMetadataNotFoundException(moduleName, elementPath, "cannot find file " + filePath, e);
        }
        catch (Exception e)
        {
            LOGGER.error("Error deserializing module {} element {} back reference metadata from {}", moduleName, elementPath, filePath, e);
            if (Files.notExists(filePath))
            {
                throw new ModuleElementMetadataNotFoundException(moduleName, elementPath, "cannot find file " + filePath, e);
            }
            StringBuilder builder = new StringBuilder("Error deserializing element ").append(elementPath).append(" back reference metadata for module ").append(moduleName).append(" from ").append(filePath);
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
            LOGGER.debug("Finished deserializing module {} element {} back reference metadata from {} in {}s", moduleName, elementPath, filePath, (end - start) / 1_000_000_000.0);
        }
    }

    // Deserialize module element back reference metadata from ClassLoader

    public boolean moduleElementBackReferenceMetadataExists(ClassLoader classLoader, String moduleName, String elementPath)
    {
        return moduleElementBackReferenceMetadataExists(classLoader, moduleName, elementPath, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleElementBackReferenceMetadataExists(ClassLoader classLoader, String moduleName, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");
        Objects.requireNonNull(elementPath, "element path is required");
        return classLoader.getResource(this.filePathProvider.getModuleElementBackReferenceMetadataResourceName(moduleName, elementPath, filePathVersion)) != null;
    }

    /**
     * Deserialize module element back reference metadata from a resource in a class loader. Throws a
     * {@link ModuleElementMetadataNotFoundException} if the module element back reference metadata cannot be found.
     *
     * @param classLoader class loader to search for the module element back reference metadata resource
     * @param moduleName  module name
     * @return module element back reference metadata
     * @throws ModuleElementMetadataNotFoundException if the module element back reference metadata cannot be found
     */
    public ElementBackReferenceMetadata deserializeModuleElementBackReferenceMetadata(ClassLoader classLoader, String moduleName, String elementPath)
    {
        return deserializeModuleElementBackReferenceMetadata(classLoader, moduleName, elementPath, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module element back reference metadata from a resource in a class loader using the given file path
     * version. Throws a {@link ModuleElementMetadataNotFoundException} if the module element back reference metadata
     * cannot be found.
     *
     * @param classLoader     class loader to search for the module element back reference metadata resource
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module element back reference metadata
     * @throws ModuleElementMetadataNotFoundException if the module element back reference metadata cannot be found
     */
    public ElementBackReferenceMetadata deserializeModuleElementBackReferenceMetadata(ClassLoader classLoader, String moduleName, String elementPath, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");
        Objects.requireNonNull(elementPath, "element path is required");

        long start = System.nanoTime();
        String resourceName = this.filePathProvider.getModuleElementBackReferenceMetadataResourceName(moduleName, elementPath, filePathVersion);
        LOGGER.debug("Deserializing module {} element {} back reference metadata from resource '{}'", moduleName, elementPath, resourceName);
        try
        {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
            {
                throw new ModuleElementMetadataNotFoundException(moduleName, elementPath, "cannot find resource " + resourceName);
            }
            LOGGER.debug("Deserializing module {} element {} back reference metadata from resource '{}': {}", moduleName, elementPath, resourceName, url);
            try (Reader reader = BinaryReaders.newBinaryReader(url.openStream()))
            {
                return this.moduleSerializer.deserializeBackReferenceMetadata(reader);
            }
            catch (Exception e)
            {
                LOGGER.error("Error deserializing module {} element {} back reference metadata from resource '{}'", moduleName, elementPath, resourceName, e);
                StringBuilder builder = new StringBuilder("Error deserializing element ").append(elementPath).append(" back reference metadata for module ").append(moduleName)
                        .append(" from resource ").append(resourceName)
                        .append(" (").append(url).append(")");
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
            }
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing module {} element {} back reference metadata from resource '{}' in {}s", moduleName, elementPath, resourceName, (end - start) / 1_000_000_000.0);
        }
    }


    // Deserialize module function name metadata from directory

    public boolean moduleFunctionNameMetadataExists(Path directory, String moduleName)
    {
        return moduleFunctionNameMetadataExists(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleFunctionNameMetadataExists(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return Files.exists(this.filePathProvider.getModuleFunctionNameMetadataFilePath(directory, moduleName, filePathVersion));
    }

    /**
     * Deserialize module function name metadata from a file in a directory. Throws a
     * {@link ModuleMetadataNotFoundException} if the module function name metadata cannot be found.
     *
     * @param directory  directory to search for the module function name metadata file
     * @param moduleName module name
     * @return module function name metadata
     * @throws ModuleMetadataNotFoundException if the module function name metadata cannot be found
     */
    public ModuleFunctionNameMetadata deserializeModuleFunctionNameMetadata(Path directory, String moduleName)
    {
        return deserializeModuleFunctionNameMetadata(directory, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module function name metadata from a file in a directory using the given file path version. Throws a
     * {@link ModuleMetadataNotFoundException} if the module function name metadata cannot be found.
     *
     * @param directory       directory to search for the module function name metadata file
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module function name metadata
     * @throws ModuleMetadataNotFoundException if the module function name metadata cannot be found
     */
    public ModuleFunctionNameMetadata deserializeModuleFunctionNameMetadata(Path directory, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(directory, "directory is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        Path filePath = this.filePathProvider.getModuleFunctionNameMetadataFilePath(directory, moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} function name metadata from {}", moduleName, filePath);
        try (Reader reader = BinaryReaders.newBinaryReader(new BufferedInputStream(Files.newInputStream(filePath))))
        {
            return this.moduleSerializer.deserializeFunctionNameMetadata(reader);
        }
        catch (NoSuchFileException | FileNotFoundException e)
        {
            LOGGER.error("Error deserializing module {} function name metadata from {}", moduleName, filePath, e);
            throw new ModuleMetadataNotFoundException(moduleName, "source metadata", "cannot find file " + filePath, e);
        }
        catch (Exception e)
        {
            LOGGER.error("Error deserializing module {} function name metadata from {}", moduleName, filePath, e);
            if (Files.notExists(filePath))
            {
                throw new ModuleMetadataNotFoundException(moduleName, "source metadata", "cannot find file " + filePath, e);
            }
            StringBuilder builder = new StringBuilder("Error deserializing function name metadata for module ").append(moduleName).append(" from ").append(filePath);
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
            LOGGER.debug("Finished deserializing module {} function name metadata from {} in {}s", moduleName, filePath, (end - start) / 1_000_000_000.0);
        }
    }

    // Deserialize module function name metadata from ClassLoader

    public boolean moduleFunctionNameMetadataExists(ClassLoader classLoader, String moduleName)
    {
        return moduleFunctionNameMetadataExists(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    public boolean moduleFunctionNameMetadataExists(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");
        return classLoader.getResource(this.filePathProvider.getModuleFunctionNameMetadataResourceName(moduleName, filePathVersion)) != null;
    }

    /**
     * Deserialize module function name metadata from a resource in a class loader. Throws a
     * {@link ModuleMetadataNotFoundException} if the module function name metadata cannot be found.
     *
     * @param classLoader class loader to search for the module function name metadata resource
     * @param moduleName  module name
     * @return module function name metadata
     * @throws ModuleMetadataNotFoundException if the module source metadata cannot be found
     */
    public ModuleFunctionNameMetadata deserializeModuleFunctionNameMetadata(ClassLoader classLoader, String moduleName)
    {
        return deserializeModuleFunctionNameMetadata(classLoader, moduleName, this.filePathProvider.getDefaultVersion());
    }

    /**
     * Deserialize module function name metadata from a resource in a class loader using the given file path version.
     * Throws a {@link ModuleMetadataNotFoundException} if the module function name metadata cannot be found.
     *
     * @param classLoader     class loader to search for the module function name metadata resource
     * @param moduleName      module name
     * @param filePathVersion file path version
     * @return module function name metadata
     * @throws ModuleMetadataNotFoundException if the module function name metadata cannot be found
     */
    public ModuleFunctionNameMetadata deserializeModuleFunctionNameMetadata(ClassLoader classLoader, String moduleName, int filePathVersion)
    {
        Objects.requireNonNull(classLoader, "class loader is required");
        Objects.requireNonNull(moduleName, "module name is required");

        long start = System.nanoTime();
        String resourceName = this.filePathProvider.getModuleFunctionNameMetadataResourceName(moduleName, filePathVersion);
        LOGGER.debug("Deserializing module {} function name metadata from resource '{}'", moduleName, resourceName);
        try
        {
            URL url = classLoader.getResource(resourceName);
            if (url == null)
            {
                throw new ModuleMetadataNotFoundException(moduleName, "function name metadata", "cannot find resource " + resourceName);
            }
            LOGGER.debug("Deserializing module {} function name metadata from resource '{}': {}", moduleName, resourceName, url);
            try (Reader reader = BinaryReaders.newBinaryReader(url.openStream()))
            {
                return this.moduleSerializer.deserializeFunctionNameMetadata(reader);
            }
            catch (Exception e)
            {
                LOGGER.error("Error deserializing module {} function name metadata from resource '{}'", moduleName, resourceName, e);
                StringBuilder builder = new StringBuilder("Error deserializing function name metadata for module ").append(moduleName)
                        .append(" from resource ").append(resourceName)
                        .append(" (").append(url).append(")");
                String eMessage = e.getMessage();
                if (eMessage != null)
                {
                    builder.append(": ").append(eMessage);
                }
                throw (e instanceof IOException) ? new UncheckedIOException(builder.toString(), (IOException) e) : new RuntimeException(builder.toString(), e);
            }
        }
        finally
        {
            long end = System.nanoTime();
            LOGGER.debug("Finished deserializing module {} function name metadata from resource '{}' in {}s", moduleName, resourceName, (end - start) / 1_000_000_000.0);
        }
    }

    // Builder

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private FilePathProvider filePathProvider;
        private ConcreteElementDeserializer elementDeserializer;
        private ModuleMetadataSerializer moduleSerializer;

        private Builder()
        {
        }

        public Builder withFilePathProvider(FilePathProvider filePathProvider)
        {
            this.filePathProvider = filePathProvider;
            return this;
        }

        public Builder withConcreteElementDeserializer(ConcreteElementDeserializer elementDeserializer)
        {
            this.elementDeserializer = elementDeserializer;
            return this;
        }

        public Builder withModuleMetadataSerializer(ModuleMetadataSerializer moduleSerializer)
        {
            this.moduleSerializer = moduleSerializer;
            return this;
        }

        public Builder withSerializers(ConcreteElementDeserializer elementDeserializer, ModuleMetadataSerializer moduleSerializer)
        {
            return withConcreteElementDeserializer(elementDeserializer)
                    .withModuleMetadataSerializer(moduleSerializer);
        }

        public FileDeserializer build()
        {
            Objects.requireNonNull(this.filePathProvider, "file path provider is required");
            Objects.requireNonNull(this.elementDeserializer, "concrete element serializer is required");
            Objects.requireNonNull(this.moduleSerializer, "module serializer is required");
            return new FileDeserializer(this.filePathProvider, this.elementDeserializer, this.moduleSerializer);
        }
    }
}
