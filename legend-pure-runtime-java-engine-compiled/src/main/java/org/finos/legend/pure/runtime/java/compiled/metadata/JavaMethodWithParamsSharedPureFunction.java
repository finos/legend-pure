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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class JavaMethodWithParamsSharedPureFunction implements SharedPureFunction<Object>
{
    private final Method method;
    private final Class<?>[] paramClasses;
    private final SourceInformation sourceInformation;

    public JavaMethodWithParamsSharedPureFunction(Method method, Class<?>[] paramClasses, SourceInformation sourceInformation)
    {
        this.method = method;
        this.paramClasses = paramClasses;
        this.sourceInformation = sourceInformation;
    }

    public Class<?>[] getParametersTypes()
    {
        return this.paramClasses;
    }

    @Override
    public Object execute(ListIterable vars, ExecutionSupport es)
    {
        try
        {
            return this.method.invoke(null, vars.toArray());
        }
        catch (IllegalArgumentException e)
        {
            vars.forEachWithIndex((var, i) ->
            {
                if (!this.paramClasses[i].isInstance(var))
                {
                    String argumentType = CompiledSupport.getPureClassName(var);
                    String paramType = CompiledSupport.getPureClassName(this.paramClasses[i]);
                    throw new PureExecutionException(this.sourceInformation, "Error during dynamic function evaluation. The type " + argumentType + " is not compatible with the type " + paramType, e);
                }
            });
            throw e;
        }
        catch (IllegalAccessException e)
        {
            throw new PureExecutionException("Failed to invoke java function.", e);
        }
        catch (Exception e)
        {
            PureException pureException = PureException.findPureException(e);
            if (pureException != null)
            {
                throw pureException;
            }
            StringBuilder builder = new StringBuilder("Unexpected error executing function");
            if (vars.notEmpty() && vars.anySatisfy(v -> !(v instanceof ExecutionSupport)))
            {
                vars.asLazy().reject(v -> v instanceof ExecutionSupport).appendString(builder, " with params [", ", ", "]");
            }
            throw new RuntimeException(builder.toString(), (e instanceof InvocationTargetException) ? e.getCause() : e);
        }
    }
}
