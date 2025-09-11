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

package org.finos.legend.pure.m3.serialization.compiler.metadata;

import java.util.Objects;

public abstract class PackageableElementMetadata
{
    protected final String path;
    protected final String classifierPath;

    PackageableElementMetadata(String path, String classifierPath)
    {
        this.path = Objects.requireNonNull(path, "path is required");
        this.classifierPath = Objects.requireNonNull(classifierPath, "classifier path is required");
    }

    public String getPath()
    {
        return this.path;
    }

    public String getClassifierPath()
    {
        return this.classifierPath;
    }

    @Override
    public String toString()
    {
        return appendString(new StringBuilder().append('<').append(getClass().getSimpleName()).append(' ')).append('>').toString();
    }

    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("path=").append(this.path)
                .append(" classifier=").append(this.classifierPath);
        appendStringInfo(builder);
        return builder;
    }

    protected void appendStringInfo(StringBuilder builder)
    {
    }
}
