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

public class ModuleElementMetadataNotFoundException extends NotFoundException
{
    private final String moduleName;
    private final String elementPath;

    ModuleElementMetadataNotFoundException(String moduleName, String elementPath, String detail, Throwable cause)
    {
        super(buildMessage(moduleName, elementPath, detail), cause);
        this.moduleName = moduleName;
        this.elementPath = elementPath;
    }

    ModuleElementMetadataNotFoundException(String moduleName, String elementPath, String detail)
    {
        super(buildMessage(moduleName, elementPath, detail));
        this.moduleName = moduleName;
        this.elementPath = elementPath;
    }

    public String getModuleName()
    {
        return this.moduleName;
    }

    public String getElementPath()
    {
        return this.elementPath;
    }

    private static String buildMessage(String moduleName, String elementPath, String detail)
    {
        StringBuilder builder = new StringBuilder(stringLength(moduleName) + stringLength(elementPath) + stringLength(detail) + 36);
        builder.append("Metadata for element ");
        if (elementPath == null)
        {
            builder.append("<null>");
        }
        else
        {
            builder.append('\'').append(elementPath).append('\'');
        }
        builder.append(" in module ");
        if (moduleName == null)
        {
            builder.append("<null>");
        }
        else
        {
            builder.append('\'').append(moduleName).append('\'');
        }
        builder.append(" not found");
        if (detail != null)
        {
            builder.append(": ").append(detail);
        }
        return builder.toString();
    }
}
