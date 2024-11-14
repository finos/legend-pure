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

package org.finos.legend.pure.runtime.java.interpreted.natives.essentials.lang.cast;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationTypeCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.generictype.match.GenericTypeMatch;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.DefaultConstraintHandler;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Stack;

public class Cast extends NativeFunction
{
    private FunctionExecutionInterpreted functionExecutionInterpreted;
    private ModelRepository repository;

    public Cast(FunctionExecutionInterpreted functionExecutionInterpreted, ModelRepository repository)
    {
        this.functionExecutionInterpreted = functionExecutionInterpreted;
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance valuesParam = params.get(0);
        CoreInstance sourceGenericType = valuesParam.getValueForMetaPropertyToOne(M3Properties.genericType);
        CoreInstance _targetGenericType = params.get(1).getValueForMetaPropertyToOne(M3Properties.genericType);
        CoreInstance targetGenericType = makeGenericTypeAsConcreteAsPossible(_targetGenericType, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);

        CoreInstance inst = this.repository.newAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), processorSupport.getClassifier(valuesParam));
        Instance.addValueToProperty(inst, M3Properties.genericType, targetGenericType, processorSupport);
        Instance.addValueToProperty(inst, M3Properties.multiplicity, Instance.getValueForMetaPropertyToOneResolved(valuesParam, M3Properties.multiplicity, processorSupport), processorSupport);

        CoreInstance sourceRawType = Instance.getValueForMetaPropertyToOneResolved(sourceGenericType, M3Properties.rawType, processorSupport);
        CoreInstance targetRawType = Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.rawType, processorSupport);
        if (GenericType.testContainsExtendedPrimitiveTypes(targetGenericType, processorSupport))
        {
            CoreInstance relationType = processorSupport.package_getByUserPath(M3Paths.Relation);
            if (Type.isExtendedPrimitiveType(targetRawType, processorSupport))
            {
                if (canBeCastedTo(sourceRawType, targetRawType, processorSupport))
                {
                    return managePrimitiveTypeExtension(instantiationContext, executionSupport, processorSupport, valuesParam, targetGenericType, inst, functionExpressionCallStack.peek().getSourceInformation(), functionExpressionCallStack);
                }
                else
                {
                    throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Cast exception: " + GenericType.print(sourceGenericType, processorSupport) + " cannot be cast to " + GenericType.print(targetGenericType, processorSupport), functionExpressionCallStack);
                }
            }
            else if (processorSupport.type_subTypeOf(sourceRawType, relationType) && processorSupport.type_subTypeOf(targetRawType, relationType))
            {
                RelationType<?> source = RelationTypeCoreInstanceWrapper.toRelationType(sourceGenericType.getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToOne(M3Properties.rawType));
                RelationType<?> target = RelationTypeCoreInstanceWrapper.toRelationType(targetGenericType.getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToOne(M3Properties.rawType));
                Pair<ListIterable<? extends Column<?, ?>>, ListIterable<? extends Column<?, ?>>> cols = _RelationType.alignColumnSets(source._columns(), target._columns(), processorSupport);
                if (!cols.getOne().zip(cols.getTwo()).injectInto(true, (a, b) -> a && canBeCastedTo(_Column.getColumnType(b.getOne())._rawType(), _Column.getColumnType(b.getTwo())._rawType(), processorSupport)))
                {
                    throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Cast exception: " + GenericType.print(sourceGenericType, processorSupport) + " cannot be cast to " + GenericType.print(targetGenericType, processorSupport), functionExpressionCallStack);
                }
                Instance.setValuesForProperty(inst, M3Properties.values, valuesParam.getValueForMetaPropertyToMany(M3Properties.values).collect(r ->
                {
                    CoreInstance val = ((AbstractCoreInstance) r).copy();
                    val.removeProperty(M3Properties.classifierGenericType);
                    val.addKeyValue(Lists.mutable.with(M3Properties.classifierGenericType), targetGenericType);
                    return val;
                }), processorSupport);
                return inst;
            }
            else
            {
                if (!GenericTypeMatch.genericTypeMatches(targetGenericType, sourceGenericType, true, ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
                {
                    throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Cast exception: ExtendedPrimitiveTypes are not supported in Generics for cast! " + GenericType.print(sourceGenericType, processorSupport) + " cannot be cast to " + GenericType.print(targetGenericType, processorSupport), functionExpressionCallStack);
                }
            }
        }

        if (GenericTypeMatch.genericTypeMatches(targetGenericType, sourceGenericType, true, ParameterMatchBehavior.MATCH_CAUTIOUSLY, ParameterMatchBehavior.MATCH_CAUTIOUSLY, processorSupport))
        {
            // If up-casting unit type to measure type, keep unit type.
            if (sourceRawType instanceof Unit && targetRawType instanceof Measure)
            {
                Instance.setValueForProperty(inst, M3Properties.genericType, sourceGenericType, processorSupport);
            }
            // Up cast (e.g., List<Integer> to Any) - no further type checking required
            Instance.setValuesForProperty(inst, M3Properties.values, valuesParam.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);
        }
        else
        {
            // Down cast (e.g., Number to Integer) - must check types of individual values
            ListIterable<? extends CoreInstance> values = valuesParam.getValueForMetaPropertyToMany(M3Properties.values);
            for (CoreInstance val : values)
            {
                CoreInstance valGenericType = Instance.extractGenericTypeFromInstance(val, processorSupport);
                if (!GenericTypeMatch.genericTypeMatches(targetGenericType, valGenericType, true, ParameterMatchBehavior.MATCH_ANYTHING, ParameterMatchBehavior.MATCH_ANYTHING, processorSupport))
                {
                    throw new PureExecutionException(functionExpressionCallStack.peek().getSourceInformation(), "Cast exception: " + GenericType.print(valGenericType, processorSupport) + " cannot be cast to " + GenericType.print(targetGenericType, processorSupport), functionExpressionCallStack);
                }
            }
            Instance.setValuesForProperty(inst, M3Properties.values, valuesParam.getValueForMetaPropertyToMany(M3Properties.values), processorSupport);
        }
        return inst;
    }

    private boolean canBeCastedTo(CoreInstance sourceRawType, CoreInstance targetRawType, ProcessorSupport processorSupport)
    {
        return Type.getGeneralizationResolutionOrder(targetRawType, processorSupport).contains(sourceRawType);
    }

    private CoreInstance managePrimitiveTypeExtension(InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport, CoreInstance valuesParam, CoreInstance targetGenericType, CoreInstance inst, SourceInformation sourceInformation, MutableStack<CoreInstance> functionExpressionCallStack) throws PureExecutionException
    {
        evaluateConstraints(valuesParam, targetGenericType, functionExecutionInterpreted, instantiationContext, functionExpressionCallStack, sourceInformation, executionSupport, processorSupport);

        // Special Handling for Primitive Extension.... Also modifies the type of the value itself (as if it were written in the grammar i.e. 1d or 1f)
        ListIterable<CoreInstance> newRes = valuesParam.getValueForMetaPropertyToMany(M3Properties.values).collect(x ->
        {
            CoreInstance res = ((AbstractCoreInstance) x).copy();
            res.setClassifier(Instance.getValueForMetaPropertyToOneResolved(targetGenericType, M3Properties.rawType, processorSupport));
            //Instance.setValueForProperty(res, M3Properties.classifierGenericType, targetGenericType, processorSupport);
            return res;
        });

        Instance.setValuesForProperty(inst, M3Properties.values, newRes, processorSupport);
        return inst;
    }

    public static void evaluateConstraints(CoreInstance valuesParam, CoreInstance targetGenericType, FunctionExecutionInterpreted functionExecutionInterpreted, InstantiationContext instantiationContext, MutableStack<CoreInstance> functionExpressionCallStack, SourceInformation sourceInformation, ExecutionSupport executionSupport, ProcessorSupport processorSupport)
    {
        for (CoreInstance genericType : GenericType.getAllSuperTypesIncludingSelf(targetGenericType, processorSupport))
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(genericType, M3Properties.rawType, processorSupport);
            DefaultConstraintHandler.defaultHandleConstraints(rawType.getValueForMetaPropertyToMany(M3Properties.constraints), valuesParam, genericType, sourceInformation, functionExecutionInterpreted, functionExpressionCallStack, instantiationContext, executionSupport);
        }
    }

    private CoreInstance makeGenericTypeAsConcreteAsPossible(CoreInstance genericType, Stack<MutableMap<String, CoreInstance>> resolvedTypeParametersStack, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParametersStack, ProcessorSupport processorSupport)
    {
        if (GenericType.isGenericTypeFullyConcrete(genericType, processorSupport))
        {
            return genericType;
        }
        CoreInstance result = genericType;
        for (int i = resolvedTypeParametersStack.size() - 2; i >= 0; i--)
        {
            MutableMap<String, CoreInstance> resolvedTypeParameters = resolvedTypeParametersStack.elementAt(i);
            MutableMap<String, CoreInstance> resolvedMultiplicityParameters = resolvedMultiplicityParametersStack.elementAt(i);
            if (resolvedTypeParameters.notEmpty() || resolvedMultiplicityParameters.notEmpty())
            {
                result = GenericType.makeTypeArgumentAsConcreteAsPossible(result, resolvedTypeParameters, resolvedMultiplicityParameters, processorSupport);
                if (GenericType.isGenericTypeFullyConcrete(result, processorSupport))
                {
                    return result;
                }
            }
        }
        return result;
    }
}
