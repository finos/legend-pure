// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.ide.light.api.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.block.function.Function;
import org.finos.legend.pure.m3.serialization.runtime.SourceCoordinates;

public class UpdateSourceInput
{
    public static final Function<UpdateSourceInput, String> PATH = new Function<UpdateSourceInput, String>()
    {
        @Override
        public String valueOf(UpdateSourceInput input)
        {
            return input.getPath();
        }
    };

    private final String path;
    private final int line;
    private final int column;
    private final String message;
    private final boolean add;

    UpdateSourceInput(String path, int line, int column, String message, boolean add)
    {
        this.path = path;
        this.line = line;
        this.column = column;
        this.message = message;
        this.add = add;
    }

    @JsonCreator
    public static UpdateSourceInput newInput(
            @JsonProperty("path") String path,
            @JsonProperty("line") int line,
            @JsonProperty("column") int column,
            @JsonProperty("message") String message,
            @JsonProperty("add") boolean add
    )
    {
        return new UpdateSourceInput(path, line, column, message, add);
    }

    public String getPath()
    {
        return path;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    public String getMessage()
    {
        return message;
    }

    public boolean isAdd()
    {
        return add;
    }
}
