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

import org.finos.legend.pure.m3.serialization.compiler.ExtensibleSerializer;

import java.nio.file.Path;
import java.util.Arrays;

public class FilePathProvider extends ExtensibleSerializer<FilePathProviderExtension>
{
    private static final String RESOURCE_FS_SEPARATOR = "/";

    private FilePathProvider(Iterable<? extends FilePathProviderExtension> extensions, int defaultVersion)
    {
        super(extensions, defaultVersion);
    }

    // Element

    public Path getElementFilePath(Path directory, String elementPath)
    {
        return directory.resolve(getElementFilePath(elementPath, getFSSeparator(directory)));
    }

    public Path getElementFilePath(Path directory, String elementPath, int version)
    {
        return directory.resolve(getElementFilePath(elementPath, getFSSeparator(directory), version));
    }

    public String getElementResourceName(String elementPath)
    {
        return getElementFilePath(elementPath, RESOURCE_FS_SEPARATOR);
    }

    public String getElementResourceName(String elementPath, int version)
    {
        return getElementFilePath(elementPath, RESOURCE_FS_SEPARATOR, version);
    }

    public String getElementFilePath(String elementPath, String fsSeparator)
    {
        return getElementFilePath(elementPath, fsSeparator, getDefaultExtension());
    }

    public String getElementFilePath(String elementPath, String fsSeparator, int version)
    {
        return getElementFilePath(elementPath, fsSeparator, getExtension(version));
    }

    private String getElementFilePath(String elementPath, String fsSeparator, FilePathProviderExtension extension)
    {
        return extension.getElementFilePath(
                validateNonEmpty(elementPath, "element path"),
                validateNonEmpty(fsSeparator, "file path separator"));
    }

    // Module manifest
    
    public Path getModuleManifestFilePath(Path directory, String moduleName)
    {
        return directory.resolve(getModuleManifestFilePath(moduleName, getFSSeparator(directory)));
    }
    
    public Path getModuleManifestFilePath(Path directory, String moduleName, int version)
    {
        return directory.resolve(getModuleManifestFilePath(moduleName, getFSSeparator(directory), version));
    }
    
    public String getModuleManifestResourceName(String moduleName)
    {
        return getModuleManifestFilePath(moduleName, RESOURCE_FS_SEPARATOR);
    }
    
    public String getModuleManifestResourceName(String moduleName, int version)
    {
        return getModuleManifestFilePath(moduleName, RESOURCE_FS_SEPARATOR, version);
    }
    
    public String getModuleManifestFilePath(String moduleName, String fsSeparator)
    {
        return getModuleManifestFilePath(moduleName, fsSeparator, getDefaultExtension());
    }
    
    public String getModuleManifestFilePath(String moduleName, String fsSeparator, int version)
    {
        return getModuleManifestFilePath(moduleName, fsSeparator, getExtension(version));
    }
    
    private String getModuleManifestFilePath(String moduleName, String fsSeparator, FilePathProviderExtension extension)
    {
        return extension.getModuleManifestFilePath(
                validateNonEmpty(moduleName, "module name"),
                validateNonEmpty(fsSeparator, "file path separator"));
    }
    
    // Module source metadata

    public Path getModuleSourceMetadataFilePath(Path directory, String moduleName)
    {
        return directory.resolve(getModuleSourceMetadataFilePath(moduleName, getFSSeparator(directory)));
    }

    public Path getModuleSourceMetadataFilePath(Path directory, String moduleName, int version)
    {
        return directory.resolve(getModuleSourceMetadataFilePath(moduleName, getFSSeparator(directory), version));
    }

    public String getModuleSourceMetadataResourceName(String moduleName)
    {
        return getModuleSourceMetadataFilePath(moduleName, RESOURCE_FS_SEPARATOR);
    }

    public String getModuleSourceMetadataResourceName(String moduleName, int version)
    {
        return getModuleSourceMetadataFilePath(moduleName, RESOURCE_FS_SEPARATOR, version);
    }

    public String getModuleSourceMetadataFilePath(String moduleName, String fsSeparator)
    {
        return getModuleSourceMetadataFilePath(moduleName, fsSeparator, getDefaultExtension());
    }

    public String getModuleSourceMetadataFilePath(String moduleName, String fsSeparator, int version)
    {
        return getModuleSourceMetadataFilePath(moduleName, fsSeparator, getExtension(version));
    }

    private String getModuleSourceMetadataFilePath(String moduleName, String fsSeparator, FilePathProviderExtension extension)
    {
        return extension.getModuleSourceMetadataFilePath(
                validateNonEmpty(moduleName, "module name"),
                validateNonEmpty(fsSeparator, "file path separator"));
    }

    // Module external reference metadata

    public Path getModuleExternalReferenceMetadataFilePath(Path directory, String moduleName)
    {
        return directory.resolve(getModuleExternalReferenceMetadataFilePath(moduleName, getFSSeparator(directory)));
    }

    public Path getModuleExternalReferenceMetadataFilePath(Path directory, String moduleName, int version)
    {
        return directory.resolve(getModuleExternalReferenceMetadataFilePath(moduleName, getFSSeparator(directory), version));
    }

    public String getModuleExternalReferenceMetadataResourceName(String moduleName)
    {
        return getModuleExternalReferenceMetadataFilePath(moduleName, RESOURCE_FS_SEPARATOR);
    }

    public String getModuleExternalReferenceMetadataResourceName(String moduleName, int version)
    {
        return getModuleExternalReferenceMetadataFilePath(moduleName, RESOURCE_FS_SEPARATOR, version);
    }

    public String getModuleExternalReferenceMetadataFilePath(String moduleName, String fsSeparator)
    {
        return getModuleExternalReferenceMetadataFilePath(moduleName, fsSeparator, getDefaultExtension());
    }

    public String getModuleExternalReferenceMetadataFilePath(String moduleName, String fsSeparator, int version)
    {
        return getModuleExternalReferenceMetadataFilePath(moduleName, fsSeparator, getExtension(version));
    }

    private String getModuleExternalReferenceMetadataFilePath(String moduleName, String fsSeparator, FilePathProviderExtension extension)
    {
        return extension.getModuleExternalReferenceMetadataFilePath(
                validateNonEmpty(moduleName, "module name"),
                validateNonEmpty(fsSeparator, "file path separator"));
    }
    
    // Module element back reference metadata
    
    public Path getModuleElementBackReferenceMetadataFilePath(Path directory, String moduleName, String elementPath)
    {
        return directory.resolve(getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, getFSSeparator(directory)));
    }

    public Path getModuleElementBackReferenceMetadataFilePath(Path directory, String moduleName, String elementPath, int version)
    {
        return directory.resolve(getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, getFSSeparator(directory), version));
    }

    public String getModuleElementBackReferenceMetadataResourceName(String moduleName, String elementPath)
    {
        return getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, RESOURCE_FS_SEPARATOR);
    }

    public String getModuleElementBackReferenceMetadataResourceName(String moduleName, String elementPath, int version)
    {
        return getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, RESOURCE_FS_SEPARATOR, version);
    }

    public String getModuleElementBackReferenceMetadataFilePath(String moduleName, String elementPath, String fsSeparator)
    {
        return getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, fsSeparator, getDefaultExtension());
    }

    public String getModuleElementBackReferenceMetadataFilePath(String moduleName, String elementPath, String fsSeparator, int version)
    {
        return getModuleElementBackReferenceMetadataFilePath(moduleName, elementPath, fsSeparator, getExtension(version));
    }

    private String getModuleElementBackReferenceMetadataFilePath(String moduleName, String elementPath, String fsSeparator, FilePathProviderExtension extension)
    {
        return extension.getModuleElementBackReferenceMetadataFilePath(
                validateNonEmpty(moduleName, "module name"),
                validateNonEmpty(elementPath, "element path"),
                validateNonEmpty(fsSeparator, "file path separator"));
    }

    // Helpers
    
    private static String validateNonEmpty(String string, String description)
    {
        if ((string == null) || string.isEmpty())
        {
            throw new IllegalArgumentException(description + " may not be null or empty");
        }
        return string;
    }

    private static String getFSSeparator(Path path)
    {
        return path.getFileSystem().getSeparator();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder extends ExtensibleSerializer.AbstractBuilder<FilePathProviderExtension, FilePathProvider>
    {
        private Builder()
        {
        }

        public Builder withExtension(FilePathProviderExtension extension)
        {
            addExtension(extension);
            return this;
        }

        public Builder withExtensions(Iterable<? extends FilePathProviderExtension> extensions)
        {
            addExtensions(extensions);
            return this;
        }

        public Builder withExtensions(FilePathProviderExtension... extensions)
        {
            return withExtensions(Arrays.asList(extensions));
        }

        public Builder withLoadedExtensions(ClassLoader classLoader)
        {
            loadExtensions(classLoader);
            return this;
        }

        public Builder withLoadedExtensions()
        {
            loadExtensions();
            return this;
        }

        public Builder withDefaultVersion(int defaultVersion)
        {
            setDefaultVersion(defaultVersion);
            return this;
        }

        @Override
        protected FilePathProvider build(Iterable<FilePathProviderExtension> extensions, int defaultVersion)
        {
            return new FilePathProvider(extensions, defaultVersion);
        }

        @Override
        protected Class<FilePathProviderExtension> getExtensionClass()
        {
            return FilePathProviderExtension.class;
        }
    }
}
