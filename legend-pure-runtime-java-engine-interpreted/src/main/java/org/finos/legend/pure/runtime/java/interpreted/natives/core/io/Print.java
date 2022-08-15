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

package org.finos.legend.pure.runtime.java.interpreted.natives.core.io;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.Printer;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ConsoleInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.core.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Print extends NativeFunction
{
    private Console console = new ConsoleInterpreted();
    private ModelRepository repository;

    public Print(ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        if (this.console.isEnabled())
        {
            ListIterable<? extends CoreInstance> toPrint = Instance.getValueForMetaPropertyToManyResolved(params.get(0), M3Properties.values, processorSupport);
            CoreInstance stringType = processorSupport.package_getByUserPath(M3Paths.String);
            ImmutableSet<CoreInstance> primitiveTypeClassifiers = ModelRepository.PRIMITIVE_TYPE_NAMES.collect(new Function<String, CoreInstance>()
            {
                @Override
                public CoreInstance valueOf(String s)
                {
                    return repository.getTopLevel(s);
                }
            });
            int max = PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport)).intValue();
            if (toPrint.size() == 1)
            {
                CoreInstance toPrintVal = toPrint.get(0);
                String result;
                if (this.console.isConsole())
                {
                    if (Instance.instanceOf(toPrintVal, stringType, processorSupport))
                    {
                        String name = toPrintVal.getName();
                        result = ("\n".equals(name) || name.isEmpty()) ? name : '\'' + name + '\'';
                    }
                    else if (primitiveTypeClassifiers.contains(processorSupport.getClassifier(toPrintVal)))
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
                CoreInstance potentialUnitType = Instance.getValueForMetaPropertyToOneResolved(params.getFirst(), M3Properties.genericType, M3Properties.rawType, processorSupport);
                if (Measure.isUnitOrMeasureInstance(params.getFirst(), processorSupport))
                {
                    CoreInstance numericValue = Instance.getValueForMetaPropertyToOneResolved(params.getFirst(), M3Properties.values, M3Properties.values, processorSupport);
                    result = null == numericValue ? potentialUnitType.getName() : numericValue.getName() + " " + potentialUnitType.getName();
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
                            builder.append('\'');
                            builder.append(name);
                            builder.append('\'');
                        }
                    }
                    else if (primitiveTypeClassifiers.contains(processorSupport.getClassifier(instance)))
                    {
                        builder.append("   ");
                        builder.append(instance.getName());
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
        return ValueSpecificationBootstrap.wrapValueSpecification(Lists.immutable.<CoreInstance>with(), true, processorSupport);
    }

    public Console getConsole()
    {
        return this.console;
    }
}
