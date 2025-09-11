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

public class ModuleMetadataNotFoundException extends NotFoundException
{
    private final String moduleName;

    ModuleMetadataNotFoundException(String moduleName, String metadataDescription, String detail, Throwable cause)
    {
        super(buildMessage(moduleName, metadataDescription, detail), cause);
        this.moduleName = moduleName;
    }

    ModuleMetadataNotFoundException(String moduleName, String detail, Throwable cause)
    {
        this(moduleName, null, detail, cause);
    }

    ModuleMetadataNotFoundException(String moduleName, Throwable cause)
    {
        this(moduleName, null, cause);
    }

    ModuleMetadataNotFoundException(String moduleName, String metadataDescription, String detail)
    {
        super(buildMessage(moduleName, metadataDescription, detail));
        this.moduleName = moduleName;

    }

    ModuleMetadataNotFoundException(String moduleName, String detail)
    {
        this(moduleName, null, detail);
    }

    ModuleMetadataNotFoundException(String moduleName)
    {
        this(moduleName, (String) null);
    }

    public String getModuleName()
    {
        return this.moduleName;
    }

    private static String buildMessage(String moduleName, String metadataDescription, String detail)
    {
        StringBuilder builder = new StringBuilder(stringLength(moduleName) + stringLength(detail) + 22);
        builder.append("Module ");
        if (moduleName == null)
        {
            builder.append("<null>");
        }
        else
        {
            builder.append('\'').append(moduleName).append('\'');
        }
        builder.append(' ').append((metadataDescription == null) ? "metadata" : metadataDescription).append(" not found");
        if (detail != null)
        {
            builder.append(": ").append(detail);
        }
        return builder.toString();
    }
}
