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
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.serialization.compiler.file.AbstractFilePathProviderExtensionTest;
import org.finos.legend.pure.m3.serialization.compiler.file.FilePathProviderExtension;

public class TestFilePathProviderExtensionV1 extends AbstractFilePathProviderExtensionTest
{
    @Override
    protected ListIterable<String> getExpectedElementPrefixDirs()
    {
        return null;
    }

    @Override
    protected String getExpectedElementFilenameExtension()
    {
        return ".pelt";
    }

    @Override
    protected ListIterable<String> getExpectedModuleMetadataPrefixDirs()
    {
        return Lists.immutable.with("org", "finos", "legend", "pure", "module");
    }

    @Override
    protected String getExpectedModuleManifestFilenameExtension()
    {
        return ".pmf";
    }

    @Override
    protected String getExpectedModuleSourceMetadataFilenameExtension()
    {
        return ".psr";
    }

    @Override
    protected String getExpectedModuleExternalReferenceMetadataFilenameExtension()
    {
        return ".pxr";
    }

    @Override
    protected String getExpectedModuleElementBackReferenceMetadataFilenameExtension()
    {
        return ".pbr";
    }

    @Override
    protected FilePathProviderExtension getExtension()
    {
        return new FilePathProviderExtensionV1();
    }
}
