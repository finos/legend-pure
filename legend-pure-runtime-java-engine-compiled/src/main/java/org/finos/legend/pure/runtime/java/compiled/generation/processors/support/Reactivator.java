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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.router.RoutedValueSpecification;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureDynamicReactivateException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.List;
import java.util.Objects;

/*
Class is responsible for performing dynamic reactivation of ValueSpecifications.

It enables checking if a given value specification can be dynamically reactivated or if it needs full Java compilation
(as may be the case if the NativeFunction not registered with a "buildBody" function in the generator).

If it can be reactivated dynamically then iterates the value specs and reactivates them one by one, passing on the new state
 */
public class Reactivator
{
    private Reactivator()
    {
    }

    public static boolean canReactivateWithoutJavaCompilation(
            final ValueSpecification valueSpecification,
            final ExecutionSupport es,
            Bridge bridge
    )
    {
        Objects.requireNonNull(valueSpecification, "valueSpecification");
        Objects.requireNonNull(es, "es");

        return canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(UnifiedMap.newMap()), bridge);
    }

    public static boolean canReactivateWithoutJavaCompilation(
            final ValueSpecification valueSpecification,
            final ExecutionSupport es,
            final PureMap lambdaOpenVariablesMap,
            Bridge bridge
    )
    {
        Objects.requireNonNull(valueSpecification, "valueSpecification");
        Objects.requireNonNull(lambdaOpenVariablesMap, "lambdaOpenVariablesMap");
        Objects.requireNonNull(es, "es");

        return canReactivateWithoutJavaCompilationImpl(valueSpecification, es, true, lambdaOpenVariablesMap, bridge);
    }

    private static boolean canReactivateWithoutJavaCompilationImpl(
            final ValueSpecification valueSpecification,
            final ExecutionSupport es,
            boolean atRoot,
            final PureMap lambdaOpenVariablesMap,
            Bridge bridge
    )
    {
        if (valueSpecification instanceof RoutedValueSpecification)
        {
            return canReactivateWithoutJavaCompilationImpl(((RoutedValueSpecification)valueSpecification)._value(), es, atRoot, lambdaOpenVariablesMap, bridge);
        }
        else if (valueSpecification instanceof InstanceValue)
        {
            InstanceValue iv = (InstanceValue)valueSpecification;
            return atRoot || iv._values().allSatisfy(new Predicate()
            {
                @Override
                public boolean accept(Object o)
                {
                    if (o instanceof ValueSpecification)
                    {
                        return canReactivateWithoutJavaCompilationImpl((ValueSpecification)o, es, false, lambdaOpenVariablesMap, bridge);
                    }
                    else if (o instanceof Function)
                    {
                        Function func = (Function)o;
                        return canReactivateWithoutJavaCompilationImpl(func, es, false, lambdaOpenVariablesMap, bridge);
                    }
                    else
                    {
                        return true;
                    }
                }
            });
        }
        else if (valueSpecification instanceof VariableExpression)
        {
            return lambdaOpenVariablesMap.getMap().keySet().contains(((VariableExpression)valueSpecification)._name());
        }
        else if (valueSpecification instanceof SimpleFunctionExpression)
        {
            SimpleFunctionExpression sfe = (SimpleFunctionExpression)valueSpecification;
            if (!canReactivateWithoutJavaCompilationImpl(sfe._func(), es, atRoot, lambdaOpenVariablesMap, bridge))
            {
                return false;
            }

            return sfe._parametersValues().allSatisfy(new Predicate<ValueSpecification>()
            {
                @Override
                public boolean accept(ValueSpecification paramValueValueSpecification)
                {
                    return canReactivateWithoutJavaCompilationImpl(paramValueValueSpecification, es, false, lambdaOpenVariablesMap, bridge);
                }
            });
        }
        else
        {
            return false;
        }
    }

    public static boolean canReactivateWithoutJavaCompilation(
            final Function func,
            final ExecutionSupport es,
            final PureMap lambdaOpenVariablesMap,
            Bridge bridge
    )
    {
        Objects.requireNonNull(func, "func");
        Objects.requireNonNull(lambdaOpenVariablesMap, "lambdaOpenVariablesMap");
        Objects.requireNonNull(es, "es");

        return canReactivateWithoutJavaCompilationImpl(func, es, true, lambdaOpenVariablesMap, bridge);
    }

    private static boolean canReactivateWithoutJavaCompilationImpl(
            final Function func,
            final ExecutionSupport es,
            boolean atRoot,
            final PureMap lambdaOpenVariablesMap,
            Bridge bridge
    )
    {
        if (func instanceof NativeFunction)
        {
            return Pure.canFindNativeOrLambdaFunction(es, func);
        }
        else if (func instanceof LambdaFunction)
        {
            LambdaFunction lambdaFunction = (LambdaFunction)func;
            List openVars = lambdaFunction._openVariables().toList();
            if (!atRoot)
            {
                openVars.removeAll(Pure.getOpenVariables(lambdaFunction, bridge).getMap().keyValuesView().toList());
            }
            openVars.removeAll(lambdaOpenVariablesMap.getMap().keySet());
            return openVars.isEmpty();
        }
        else if (func instanceof QualifiedProperty)
        {
            return true;
        }
        else if (func instanceof FunctionDefinition || func instanceof Property)
        {
            return Pure.findSharedPureFunction(func, bridge, es) != null;
        }
        else
        {
            return false;
        }
    }

    public static Object reactivateWithoutJavaCompilation(Bridge bridge,
                                                          final ValueSpecification valueSpecification,
                                                          final PureMap lambdaOpenVariablesMap,
                                                          final ExecutionSupport es)
    {
        Objects.requireNonNull(valueSpecification, "valueSpecification");
        Objects.requireNonNull(lambdaOpenVariablesMap, "lambdaOpenVariablesMap");
        Objects.requireNonNull(es, "es");

        return reactivateWithoutJavaCompilationImpl(valueSpecification, lambdaOpenVariablesMap, es, true, bridge);
    }

    private static Object reactivateWithoutJavaCompilationImpl(final ValueSpecification valueSpecification,
                                                               final PureMap lambdaOpenVariablesMap,
                                                               final ExecutionSupport es,
                                                               final boolean atRoot,
                                                               Bridge bridge)
    {
        if (valueSpecification instanceof RoutedValueSpecification)
        {
            return reactivateWithoutJavaCompilationImpl(((RoutedValueSpecification)valueSpecification)._value(), lambdaOpenVariablesMap, es, atRoot, bridge);
        }
        else if (valueSpecification instanceof InstanceValue)
        {
            InstanceValue iv = (InstanceValue)valueSpecification;
            RichIterable<?> result = iv._values().flatCollect(new org.eclipse.collections.api.block.function.Function<Object, RichIterable<Object>>()
            {
                @Override
                public RichIterable<Object> valueOf(Object value)
                {
                    Object result;
                    if (value instanceof ValueSpecification)
                    {
                        result = reactivateWithoutJavaCompilationImpl((ValueSpecification)value, lambdaOpenVariablesMap, es, false, bridge);
                    }
                    else if ((!lambdaOpenVariablesMap.getMap().isEmpty()) && value instanceof KeyExpression)
                    {
                        KeyExpression ke = (KeyExpression)value;

                        CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport)es).getProcessorSupport();

                        Object key = reactivateWithoutJavaCompilationImpl(ke._key(), lambdaOpenVariablesMap, es, false, bridge);
                        InstanceValue rKey = (InstanceValue)((CompiledExecutionSupport)es).getProcessorSupport().newCoreInstance("key", M3Paths.InstanceValue, null);
                        rKey._values(key instanceof RichIterable ? (RichIterable)key : Lists.immutable.with(key));


                        Object expression = reactivateWithoutJavaCompilationImpl(ke._expression(), lambdaOpenVariablesMap, es, false, bridge);
                        InstanceValue rExpression = (InstanceValue)((CompiledExecutionSupport)es).getProcessorSupport().newCoreInstance("key", M3Paths.InstanceValue, null);
                        rExpression._values(expression instanceof RichIterable ? (RichIterable)expression : Lists.immutable.with(expression));

                        KeyExpression rKe = ke.copy();
                        rKe._key(rKey);
                        rKe._expression(rExpression);

                        result = rKe;
                    }
                    else if (value instanceof LambdaFunction)
                    {
                        LambdaFunction lambdaFunction = (LambdaFunction)value;

                        UnifiedMap openVariables = UnifiedMap.newMap();
                        for (Object entry : lambdaOpenVariablesMap.getMap().keyValuesView())
                        {
                            Pair<String, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List> pair = (Pair<String, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List>)entry;
                            openVariables.put(pair.getOne(), pair.getTwo()._values());
                        }
                        for (Object entry : Pure.getOpenVariables(lambdaFunction, bridge).getMap().keyValuesView())
                        {
                            Pair<String, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List> pair = (Pair<String, org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List>)entry;
                            openVariables.put(pair.getOne(), pair.getTwo()._values());
                        }
                        result = bridge.lambdaBuilder().value()
                                .lambdaFunction(lambdaFunction)
                                .pureFunction(DynamicPureLambdaFunctionImpl.createPureLambdaFunction(lambdaFunction, openVariables, bridge));
                    }
                    else
                    {
                        result = value;
                    }

                    return CompiledSupport.toPureCollection(result);
                }
            });

            return (result != null) && (result.size() == 1) ? result.getFirst() : result;
        }
        else if (valueSpecification instanceof SimpleFunctionExpression)
        {
            SimpleFunctionExpression sfe = (SimpleFunctionExpression)valueSpecification;
            Function func = (Function)sfe._func();

            // Check that we can reactivate the function (before reactivating the arguments) so that we don't
            // waste time / go further down a broken call sequence

            if (!canReactivateWithoutJavaCompilationImpl(func, es, atRoot, lambdaOpenVariablesMap, bridge))
            {
                throw new PureDynamicReactivateException(valueSpecification.getSourceInformation(), "Can not reactivate function, unexpected:" + func._name());
            }

            RichIterable<?> paramValues = sfe._parametersValues().collect(new org.eclipse.collections.api.block.function.Function<Object, Object>()
            {
                @Override
                public Object valueOf(Object value)
                {
                    if (value instanceof ValueSpecification)
                    {
                        Object newValue = reactivateWithoutJavaCompilationImpl((ValueSpecification)value, lambdaOpenVariablesMap, es, false, bridge);
                        if (newValue instanceof RichIterable)
                        {
                            return newValue;
                        }
                        else
                        {
                            return Lists.fixedSize.of(newValue);
                        }
                    }
                    else
                    {
                        return value;
                    }
                }
            });
            MutableList<Object> vars = Lists.mutable.withAll(paramValues);
            if (sfe._func()._name().equals("new_Class_1__String_1__KeyExpression_MANY__T_1_"))
            {
                //Have to get the first param from the generic type
                vars.set(0, Lists.fixedSize.of(sfe._genericType()._rawType()));
            }
            else if (sfe._func()._name().equals("cast_Any_m__T_1__T_m_"))
            {
                //Have to get the second param from the generic type
                vars.set(1, Lists.fixedSize.of(sfe._genericType()));
            }

            return Pure._evaluateToMany(es, bridge, func, vars);
        }
        else if (valueSpecification instanceof VariableExpression)
        {
            String varName = ((VariableExpression)valueSpecification)._name();
            if (!lambdaOpenVariablesMap.getMap().containsKey(varName))
            {
                throw new PureDynamicReactivateException("Attempt to use out of scope variable: " + varName);
            }
            Object result = lambdaOpenVariablesMap.getMap().get(varName);
            return ((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<Object>)result)._values();
        }
        else
        {
            throw new PureDynamicReactivateException(valueSpecification.getSourceInformation(), "Unexpected type to dynamically reactivate: " + valueSpecification.getClass().getName());
        }
    }
}