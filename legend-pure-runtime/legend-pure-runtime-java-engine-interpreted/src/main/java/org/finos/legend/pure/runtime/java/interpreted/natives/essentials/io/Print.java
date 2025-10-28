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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.io;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Print extends NativeFunction
{
    private final ModelRepository repository;
    private final Console console;

    public Print(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
        this.console = functionExecution.getConsole();
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        if (this.console.isEnabled())
        {
            ListIterable<? extends CoreInstance> toPrint = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
            CoreInstance stringType = processorSupport.package_getByUserPath(M3Paths.String);
            int max = PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport)).intValue();
            if (toPrint.size() == 1)
            {
                CoreInstance toPrintVal = toPrint.get(0);
                String result;
                if (Measure.isUnitOrMeasureInstance(params.getFirst(), processorSupport))
                {
                    CoreInstance potentialUnitType = Instance.getValueForMetaPropertyToOneResolved(params.getFirst(), M3Properties.genericType, M3Properties.rawType, processorSupport);
                    CoreInstance numericValue = Instance.getValueForMetaPropertyToOneResolved(params.getFirst(), M3Properties.values, M3Properties.values, processorSupport);
                    result = (numericValue == null) ? potentialUnitType.getName() : (numericValue.getName() + " " + potentialUnitType.getName());
                }
                else if (this.console.isConsole())
                {
                    if (Instance.instanceOf(toPrintVal, stringType, processorSupport))
                    {
                        String name = toPrintVal.getName();
                        result = ("\n".equals(name) || name.isEmpty()) ? name : '\'' + name + '\'';
                    }
                    else if (Type.isPrimitiveType(processorSupport.getClassifier(toPrintVal), processorSupport))
                    {
                        result = toPrintVal.getName();
                    }
                    else
                    {
                        result = Printer.print(toPrintVal, "", max, processorSupport);
                    }
                }
                else
                {
                    result = toPrintVal.getName();
                }
                this.console.print(result);
            }
            else
            {
                StringBuilder builder = new StringBuilder();
                if (this.console.isConsole())
                {
                    builder.append("[\n");
                }
                for (CoreInstance instance : toPrint)
                {
                    if (Instance.instanceOf(instance, stringType, processorSupport))
                    {
                        builder.append("   ");
                        String name = instance.getName().replace("\n", "\n   ");
                        if ("\n".equals(name) || name.isEmpty())
                        {
                            builder.append(name);
                        }
                        else
                        {
                            builder.append('\'').append(name).append('\'');
                        }
                    }
                    else if (Type.isPrimitiveType(processorSupport.getClassifier(instance), processorSupport))
                    {
                        builder.append("   ").append(instance.getName());
                    }
                    else
                    {
                        Printer.print(builder, instance, "   ", max, processorSupport);
                    }
                    builder.append('\n');
                }
                if (this.console.isConsole())
                {
                    builder.append("]");
                }
                this.console.print(builder);
            }
        }
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.with(), true, processorSupport);
    }

    public Console getConsole()
    {
        return this.console;
    }
}
