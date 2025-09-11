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

package org.finos.legend.pure.m3.serialization.compiler.file.v1;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProviderExtension;
import org.finos.legend.pure.m3.tools.FilePathTools;

public class FilePathProviderExtensionV1 implements FilePathProviderExtension
{
    private static final String ELEMENT_FILE_EXTENSION = ".pelt";

    private static final ImmutableList<String> MODULE_FILE_DIR = Lists.immutable.with("org", "finos", "legend", "pure", "module");
    private static final String MODULE_MANIFEST_FILE_EXTENSION = ".pmf";
    private static final String MODULE_SOURCE_METADATA_FILE_EXTENSION = ".psr";
    private static final String MODULE_EXT_REF_FILE_EXTENSION = ".pxr";
    private static final String MODULE_ELEMENT_BACK_REF_FILE_EXTENSION = ".pbr";

    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public String getElementFilePath(String elementPath, String fsSeparator)
    {
        return PackageableElement.DEFAULT_PATH_SEPARATOR.equals(elementPath) ?
               (M3Paths.Root + ELEMENT_FILE_EXTENSION) :
               FilePathTools.toFilePath(elementPath, PackageableElement.DEFAULT_PATH_SEPARATOR, fsSeparator, ELEMENT_FILE_EXTENSION);
    }

    @Override
    public String getModuleManifestFilePath(String moduleName, String fsSeparator)
    {
        return getModuleMetadataFilePath(moduleName, fsSeparator, MODULE_MANIFEST_FILE_EXTENSION);
    }

    @Override
    public String getModuleSourceMetadataFilePath(String moduleName, String fsSeparator)
    {
        return getModuleMetadataFilePath(moduleName, fsSeparator, MODULE_SOURCE_METADATA_FILE_EXTENSION);
    }

    @Override
    public String getModuleExternalReferenceMetadataFilePath(String moduleName, String fsSeparator)
    {
        return getModuleMetadataFilePath(moduleName, fsSeparator, MODULE_EXT_REF_FILE_EXTENSION);
    }

    @Override
    public String getModuleElementBackReferenceMetadataFilePath(String moduleName, String elementPath, String fsSeparator)
    {
        StringBuilder builder = new StringBuilder(moduleName.length() + elementPath.length() + MODULE_ELEMENT_BACK_REF_FILE_EXTENSION.length() + (MODULE_FILE_DIR.size() * fsSeparator.length()) + 24);
        FilePathTools.appendFilePathName(appendModuleFileDir(builder, fsSeparator), moduleName).append(fsSeparator);
        return FilePathTools.toFilePath(builder, PackageableElement.DEFAULT_PATH_SEPARATOR.equals(elementPath) ? M3Paths.Root : elementPath, PackageableElement.DEFAULT_PATH_SEPARATOR, fsSeparator, MODULE_ELEMENT_BACK_REF_FILE_EXTENSION).toString();
    }

    private String getModuleMetadataFilePath(String moduleName, String fsSeparator, String extension)
    {
        StringBuilder builder = new StringBuilder(moduleName.length() + extension.length() + (MODULE_FILE_DIR.size() * fsSeparator.length()) + 24);
        return FilePathTools.appendFilePathName(appendModuleFileDir(builder, fsSeparator), moduleName, extension).toString();
    }

    private StringBuilder appendModuleFileDir(StringBuilder builder, String fsSeparator)
    {
        MODULE_FILE_DIR.forEach(name -> builder.append(name).append(fsSeparator));
        return builder;
    }
}
