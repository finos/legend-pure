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

package org.finos.legend.pure.runtime.java.compiled.execution;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.execution.OutputWriter;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class OutputWriterCompiled implements OutputWriter<Object>
{
    @Override
    public void write(Object result, OutputStream outputStream) throws IOException
    {
        if (result instanceof RichIterable)
        {
            RichIterable list = (RichIterable)result;
            for (Object object : list)
            {
                this.write(object, outputStream);
            }
        }
        else if (result instanceof String || result instanceof Boolean || result instanceof PureDate ||
                result instanceof Integer || result instanceof Long || result instanceof BigInteger ||
                result instanceof Float || result instanceof Double || result instanceof BigDecimal)
        {
            outputStream.write(result.toString().getBytes(StandardCharsets.UTF_8));
        }
        else
        {
            throw new RuntimeException("Non-primitive instances are not supported with this method at this time");
        }
    }
}