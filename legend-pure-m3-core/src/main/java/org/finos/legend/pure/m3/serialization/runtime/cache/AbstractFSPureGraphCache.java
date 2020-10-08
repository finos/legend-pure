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

package org.finos.legend.pure.m3.serialization.runtime.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

abstract class AbstractFSPureGraphCache extends AbstractPureGraphCache implements FSPureGraphCache
{
    private static final int DEFAULT_FILE_BUFFER_SIZE = 4 * 1024 * 1024;

    protected OutputStream newOutputStream(Path cacheFile) throws IOException
    {
        return new BufferedOutputStream(Files.newOutputStream(cacheFile), DEFAULT_FILE_BUFFER_SIZE);
    }

    protected InputStream newInputStream(Path cacheFile) throws IOException
    {
        return new BufferedInputStream(Files.newInputStream(cacheFile), DEFAULT_FILE_BUFFER_SIZE);
    }
}
