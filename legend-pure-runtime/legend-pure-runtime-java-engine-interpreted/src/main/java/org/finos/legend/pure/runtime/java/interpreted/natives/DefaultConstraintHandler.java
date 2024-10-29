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

package org.finos.legend.pure.runtime.java.interpreted.natives;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.interpreted.profiler.VoidProfiler;

import java.util.Stack;

public class DefaultConstraintHandler
{
    private DefaultConstraintHandler()
    {
        // No Instantiation
    }

    public static CoreInstance handleConstraints(CoreInstance _class, CoreInstance instance, SourceInformation sourceInformation, FunctionExecutionInterpreted functionExecution, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        ProcessorSupport processorSupport = functionExecution.getProcessorSupport();
        CoreInstance evaluatedSource = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties.values, processorSupport);
        CoreInstance override = Instance.getValueForMetaPropertyToOneResolved(evaluatedSource, M3Properties.elementOverride, processorSupport);
        return override != null && Instance.getValueForMetaPropertyToOneResolved(override, M3Properties.constraintsManager, processorSupport) != null ?
                functionExecution.executeLambdaFromNative(Instance.getValueForMetaPropertyToOneResolved(override, M3Properties.constraintsManager, processorSupport), Lists.mutable.with(instance), resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionToUseInStack, profiler, instantiationContext, executionSupport) :
                defaultHandleConstraints(_Class.computeConstraintsInHierarchy(_class, functionExecution.getProcessorSupport()), instance, instance.getValueForMetaPropertyToOne(M3Properties.genericType), sourceInformation, functionExecution, instantiationContext, executionSupport);
    }

    public static CoreInstance defaultHandleConstraints(ListIterable<? extends CoreInstance> constraints, CoreInstance instance, CoreInstance genericType, SourceInformation sourceInformation, FunctionExecutionInterpreted functionExecution, InstantiationContext instantiationContext, ExecutionSupport executionSupport) throws PureExecutionException
    {
        VariableContext ctx = VariableContext.newVariableContext();

        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, functionExecution.getProcessorSupport());

        DefaultConstraintHandler.addTypeVariablesToContext(genericType, rawType, ctx);

        try
        {
            ctx.registerValue("this", instance);
        }
        catch (VariableContext.VariableNameConflictException e)
        {
            throw new PureExecutionException(null, e.getMessage());
        }

        for (CoreInstance constraint : constraints)
        {
            evaluateOneConstraint(constraint, sourceInformation, functionExecution, instantiationContext, executionSupport, ctx);
        }
        return instance;
    }

    public static void evaluateOneConstraint(CoreInstance constraint, SourceInformation sourceInformation, FunctionExecutionInterpreted functionExecution, InstantiationContext instantiationContext, ExecutionSupport executionSupport, VariableContext ctx)
    {
        CoreInstance definition = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.functionDefinition, functionExecution.getProcessorSupport()).getValueForMetaPropertyToOne(M3Properties.expressionSequence);
        String ruleId = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.name, functionExecution.getProcessorSupport()).getName();
        CoreInstance owner = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.owner, functionExecution.getProcessorSupport());
        if (owner == null || "Global".equals(owner.getName()))
        {
            CoreInstance result = functionExecution.executeValueSpecification(definition, new Stack<>(), new Stack<>(), null, ctx, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport);
            CoreInstance constraintClass = Instance.getValueForMetaPropertyToOneResolved(definition, M3Properties.usageContext, functionExecution.getProcessorSupport()).getValueForMetaPropertyToOne(M3Properties.type);
            CoreInstance messageFunction = Instance.getValueForMetaPropertyToOneResolved(constraint, M3Properties.messageFunction, functionExecution.getProcessorSupport());
            if (!PrimitiveUtilities.getBooleanValue(result.getValueForMetaPropertyToOne(M3Properties.values)))
            {
                StringBuilder message = new StringBuilder("Constraint :[" + ruleId + "] violated in the Class " + constraintClass.getName());
                if (messageFunction != null)
                {
                    message.append(", Message: ");
                    message.append(PrimitiveUtilities.getStringValue(functionExecution.executeValueSpecification(messageFunction.getValueForMetaPropertyToOne(M3Properties.expressionSequence), new Stack<>(), new Stack<>(), null, ctx, VoidProfiler.VOID_PROFILER, instantiationContext, executionSupport).getValueForMetaPropertyToOne(M3Properties.values)));
                }
                throw new PureExecutionException(sourceInformation, message.toString());
            }
        }
    }

    public static void addTypeVariablesToContext(CoreInstance genericType, CoreInstance rawType, VariableContext ctx)
    {
        // Add type variables
        rawType.getValueForMetaPropertyToMany(M3Properties.typeVariables).collect(f -> f.getValueForMetaPropertyToOne("name").getName())
                .zip(genericType.getValueForMetaPropertyToMany(M3Properties.typeVariableValues))
                .forEach(p ->
                {
                    try
                    {
                        ctx.registerValue(p.getOne(), p.getTwo());
                    }
                    catch (VariableContext.VariableNameConflictException e)
                    {
                        throw new RuntimeException(e);
                    }
                });
    }
}
