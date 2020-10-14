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

package org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation;

import org.eclipse.collections.api.block.function.Function0;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class E_
{
    private E_()
    {
    }

    public static <T> T e_(Function0<T> func, String sourceId, int line, int column, int endLine, int endColumn)
    {
        try
        {
            return func.value();
        }
        catch (PureCompiledExecutionException e)
        {
            SourceInformation sourceInformation = new SourceInformation(sourceId, line, column, endLine, endColumn);
            e.addStackTraceElement(sourceInformation);
            throw e;
        }
        catch (Exception e)
        {
            SourceInformation sourceInformation = new SourceInformation(sourceId, line, column, endLine, endColumn);
            throw new PureCompiledExecutionException(sourceInformation, e.getMessage(), e);
        }
    }
}
