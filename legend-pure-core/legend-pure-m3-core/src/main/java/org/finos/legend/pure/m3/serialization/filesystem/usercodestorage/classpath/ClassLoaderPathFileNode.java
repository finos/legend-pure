// Copyright 2020 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

class ClassLoaderPathFileNode extends ClassLoaderFileNode
{
    private final Path filePath;

    ClassLoaderPathFileNode(String path, Path filePath)
    {
        super(path, filePath);
        Objects.requireNonNull(filePath, "filePath");
        this.filePath = filePath;
    }

    @Override
    protected void writeToStringMessage(StringBuilder message)
    {
        message.append(" path='");
        message.append(this.filePath);
        message.append('\'');
    }

    @Override
    InputStream getContent() throws IOException
    {
        return Files.newInputStream(this.filePath);
    }

    @Override
    byte[] getContentAsBytes() throws IOException
    {
        return Files.readAllBytes(this.filePath);
    }
}
