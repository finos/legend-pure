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

package org.finos.legend.pure.runtime.java.interpreted;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class OutputWriterInterpreted implements OutputWriter<ListIterable<? extends CoreInstance>>
{
    @Override
    public void write(ListIterable<? extends CoreInstance> values, OutputStream outputStream) throws IOException
    {
        for (CoreInstance value : values)
        {
            outputStream.write(value.getName().getBytes(StandardCharsets.UTF_8));
        }
    }
}