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

package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Stack;
import java.util.UUID;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

public class FunctionExecutionInterpretedWithCodeCoverage extends FunctionExecutionInterpreted
{
    private final PrintWriter writer;

    public FunctionExecutionInterpretedWithCodeCoverage(Path coverageDirectory)
    {
        super();
        try
        {
            Files.createDirectories(coverageDirectory);
            this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(coverageDirectory.resolve(UUID.randomUUID() + ".purecov"), StandardOpenOption.CREATE_NEW), Charset.defaultCharset())));

        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CoreInstance start(CoreInstance function, ListIterable<? extends CoreInstance> arguments)
    {
        try
        {
            return super.start(function, arguments);
        }
        finally
        {
            this.writer.flush();
        }
    }

    @Override
    public CoreInstance executeFunction(boolean limitScope, Function<?> function, ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext varContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport)
    {
        SourceInformation covered = function.getSourceInformation();
        if (covered != null)
        {
            this.writer.printf("%s %d %d %d %d%n", covered.getSourceId(), covered.getStartLine() - 1, covered.getStartColumn() - 1, covered.getEndLine() - 1, covered.getEndColumn());
        }
        return super.executeFunction(limitScope, function, params, resolvedTypeParameters, resolvedMultiplicityParameters, varContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
    }
}
