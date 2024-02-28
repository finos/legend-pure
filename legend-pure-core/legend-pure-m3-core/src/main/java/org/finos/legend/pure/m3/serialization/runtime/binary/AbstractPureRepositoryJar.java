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

package org.finos.legend.pure.m3.serialization.runtime.binary;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;

abstract class AbstractPureRepositoryJar implements PureRepositoryJar
{
    private final PureRepositoryJarMetadata metadata;

    protected AbstractPureRepositoryJar(PureRepositoryJarMetadata metadata)
    {
        this.metadata = metadata;
    }

    @Override
    public PureRepositoryJarMetadata getMetadata()
    {
        return this.metadata;
    }

    @Override
    public MapIterable<String, byte[]> readFiles(Iterable<String> filePaths)
    {
        SetIterable<String> filePathSet = getFileSet(filePaths);
        int size = filePathSet.size();
        switch (size)
        {
            case 0:
            {
                return Maps.immutable.empty();
            }
            case 1:
            {
                String filePath = filePathSet.getAny();
                return Maps.immutable.with(filePath, readFile(filePath));
            }
            default:
            {
                MutableMap<String, byte[]> fileBytes = Maps.mutable.withInitialCapacity(size);
                readFilesFromNonEmptySet(filePathSet, fileBytes);
                return fileBytes;
            }
        }
    }

    @Override
    public void readFiles(Iterable<String> filePaths, MutableMap<String, byte[]> fileBytes)
    {
        SetIterable<String> filePathSet = getFileSet(filePaths);
        if (filePathSet.notEmpty())
        {
            readFilesFromNonEmptySet(filePathSet, fileBytes);
        }
    }

    @Override
    public MapIterable<String, byte[]> readAllFiles()
    {
        MutableMap<String, byte[]> fileBytes = Maps.mutable.empty();
        readAllFiles(fileBytes);
        return fileBytes;
    }

    protected abstract void readFilesFromNonEmptySet(SetIterable<String> filePaths, MutableMap<String, byte[]> fileBytes);

    private static SetIterable<String> getFileSet(Iterable<String> filePaths)
    {
        return (filePaths instanceof SetIterable) ? (SetIterable<String>)filePaths : Sets.mutable.withAll(filePaths);
    }
}
