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

import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNodeStatus;

abstract class ClassLoaderCodeStorageNode implements CodeStorageNode
{
    private final String path;
    private final String name;

    protected ClassLoaderCodeStorageNode(String path)
    {
        this.path = path;
        this.name = extractName(path);
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getPath()
    {
        return this.path;
    }

    @Override
    public CodeStorageNodeStatus getStatus()
    {
        return CodeStorageNodeStatus.NORMAL;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder().append('<').append(getClass().getSimpleName())
                .append(" path='").append(this.path)
                .append("' directory=").append(isDirectory());
        writeToStringMessage(builder);
        builder.append('>');
        return builder.toString();
    }

    protected void writeToStringMessage(StringBuilder message)
    {
    }

    private static String extractName(String path)
    {
        int index = path.lastIndexOf('/');
        return (index == -1) ? path : path.substring(index + 1);
    }
}
