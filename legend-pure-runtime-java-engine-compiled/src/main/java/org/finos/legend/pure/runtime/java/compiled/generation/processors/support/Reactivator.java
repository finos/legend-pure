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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.*;
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
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

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

    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, Bridge bridge)
    {
        Objects.requireNonNull(valueSpecification, "valueSpecification");
        Objects.requireNonNull(es, "es");
        return canReactivateWithoutJavaCompilation(valueSpecification, es, new PureMap(Maps.mutable.empty()), bridge);
    }

    public static boolean canReactivateWithoutJavaCompilation(ValueSpecification valueSpecification, ExecutionSupport es, PureMap lambdaOpenVariablesMap, Bridge bridge)
    {
        Objects.requireNonNull(valueSpecification, "valueSpecification");
        Objects.requireNonNull(lambdaOpenVariablesMap, "lambdaOpenVariablesMap");
        Objects.requireNonNull(es, "es");

        return canReactivateWithoutJavaCompilationImpl(valueSpecification, es, true, lambdaOpenVariablesMap, bridge);
    }

    private static boolean canReactivateWithoutJavaCompilationImpl(ValueSpecification valueSpecification, ExecutionSupport es, boolean atRoot, PureMap lambdaOpenVariablesMap, Bridge bridge)
    {
        if (valueSpecification instanceof RoutedValueSpecification)
        {
            return canReactivateWithoutJavaCompilationImpl(((RoutedValueSpecification) valueSpecification)._value(), es, atRoot, lambdaOpenVariablesMap, bridge);
        }
        if (valueSpecification instanceof InstanceValue)
        {
            return atRoot || ((InstanceValue) valueSpecification)._values().allSatisfy(o ->
            {
                if (o instanceof ValueSpecification)
                {
                    return canReactivateWithoutJavaCompilationImpl((ValueSpecification) o, es, false, lambdaOpenVariablesMap, bridge);
                }
                if (o instanceof Function)
                {
                    return canReactivateWithoutJavaCompilationImpl((Function<?>) o, es, false, lambdaOpenVariablesMap, bridge);
                }
                return true;
            });
        }
        if (valueSpecification instanceof VariableExpression)
        {
            return lambdaOpenVariablesMap.getMap().containsKey(((VariableExpression) valueSpecification)._name());
        }
        if (valueSpecification instanceof SimpleFunctionExpression)
        {
            SimpleFunctionExpression sfe = (SimpleFunctionExpression) valueSpecification;
            return canReactivateWithoutJavaCompilationImpl(sfe._func(), es, atRoot, lambdaOpenVariablesMap, bridge) &&
                    sfe._parametersValues().allSatisfy(vs -> canReactivateWithoutJavaCompilationImpl(vs, es, false, lambdaOpenVariablesMap, bridge));
        }
        return false;
    }

    public static boolean canReactivateWithoutJavaCompilation(Function<?> func, ExecutionSupport es, PureMap lambdaOpenVariablesMap, Bridge bridge)
    {
        Objects.requireNonNull(func, "func");
        Objects.requireNonNull(lambdaOpenVariablesMap, "lambdaOpenVariablesMap");
        Objects.requireNonNull(es, "es");

        return canReactivateWithoutJavaCompilationImpl(func, es, true, lambdaOpenVariablesMap, bridge);
    }

    private static boolean canReactivateWithoutJavaCompilationImpl(Function<?> func, ExecutionSupport es, boolean atRoot, PureMap lambdaOpenVariablesMap, Bridge bridge)
    {
        if (func instanceof NativeFunction)
        {
            return Pure.canFindNativeOrLambdaFunction(es, func);
        }
        if (func instanceof LambdaFunction)
        {
            LambdaFunction<?> lambdaFunction = (LambdaFunction<?>) func;
            MutableSet<String> openVars = Sets.mutable.withAll(lambdaFunction._openVariables());
            openVars.removeAll(lambdaOpenVariablesMap.getMap().keySet());
            if (openVars.notEmpty() && !atRoot)
            {
                openVars.removeAll(Pure.getOpenVariables(lambdaFunction, bridge).getMap().keySet());
            }
            return openVars.isEmpty();
        }
        if (func instanceof QualifiedProperty)
        {
            return true;
        }
        if (func instanceof ConcreteFunctionDefinition)
        {
            return true;
        }
        if (func instanceof FunctionDefinition || func instanceof Property)
        {
            return Pure.findSharedPureFunction(func, bridge, es) != null;
        }
        return false;
    }

    public static Object reactivateWithoutJavaCompilation(Bridge bridge, ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es)
    {
        Objects.requireNonNull(valueSpecification, "valueSpecification");
        Objects.requireNonNull(lambdaOpenVariablesMap, "lambdaOpenVariablesMap");
        Objects.requireNonNull(es, "es");

        return reactivateWithoutJavaCompilationImpl(valueSpecification, lambdaOpenVariablesMap, es, true, bridge);
    }

    private static Object reactivateWithoutJavaCompilationImpl(ValueSpecification valueSpecification, PureMap lambdaOpenVariablesMap, ExecutionSupport es, boolean atRoot, Bridge bridge)
    {
        if (valueSpecification instanceof RoutedValueSpecification)
        {
            return reactivateWithoutJavaCompilationImpl(((RoutedValueSpecification) valueSpecification)._value(), lambdaOpenVariablesMap, es, atRoot, bridge);
        }
        if (valueSpecification instanceof InstanceValue)
        {
            MutableList<Object> result = reactivateWithoutJavaCompilationImpl((InstanceValue) valueSpecification, lambdaOpenVariablesMap, es, bridge);
            return (result.size() == 1) ? result.get(0) : result;
        }
        if (valueSpecification instanceof SimpleFunctionExpression)
        {
            return reactivateWithoutJavaCompilationImpl((SimpleFunctionExpression) valueSpecification, lambdaOpenVariablesMap, es, atRoot, bridge);
        }
        if (valueSpecification instanceof VariableExpression)
        {
            return reactivateWithoutJavaCompilationImpl((VariableExpression) valueSpecification, lambdaOpenVariablesMap);
        }
        throw new PureDynamicReactivateException(valueSpecification.getSourceInformation(), "Unexpected type to dynamically reactivate: " + valueSpecification.getClass().getName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static MutableList<Object> reactivateWithoutJavaCompilationImpl(InstanceValue instanceValue, PureMap lambdaOpenVariablesMap, ExecutionSupport es, Bridge bridge)
    {
        MutableList<Object> results = Lists.mutable.empty();
        instanceValue._values().each(value ->
        {
            if (value instanceof ValueSpecification)
            {
                Object res = reactivateWithoutJavaCompilationImpl((ValueSpecification) value, lambdaOpenVariablesMap, es, false, bridge);
                if (res instanceof Iterable)
                {
                    results.addAllIterable((Iterable<?>) res);
                }
                else if (res != null)
                {
                    results.add(res);
                }
            }
            else if (lambdaOpenVariablesMap.getMap().notEmpty() && (value instanceof KeyExpression))
            {
                KeyExpression ke = (KeyExpression) value;

                MutableList<Object> key = reactivateWithoutJavaCompilationImpl(ke._key(), lambdaOpenVariablesMap, es, bridge);
                InstanceValue rKey = (InstanceValue) ((CompiledExecutionSupport) es).getProcessorSupport().newCoreInstance("key", M3Paths.InstanceValue, null);
                rKey._values(key);

                Object expression = reactivateWithoutJavaCompilationImpl(ke._expression(), lambdaOpenVariablesMap, es, false, bridge);
                InstanceValue rExpression = (InstanceValue) ((CompiledExecutionSupport) es).getProcessorSupport().newCoreInstance("key", M3Paths.InstanceValue, null);
                rExpression._values(expression instanceof RichIterable ? (RichIterable<?>) expression : Lists.immutable.with(expression));

                results.add(ke.copy()._key(rKey)._expression(rExpression));
            }
            else if (value instanceof LambdaFunction)
            {
                LambdaFunction lambdaFunction = (LambdaFunction) value;

                MutableMap<String, Object> openVariables = Maps.mutable.empty();
                lambdaOpenVariablesMap.getMap().forEachKeyValue((key, values) -> openVariables.put((String) key, ((List<?>) values)._values()));
                Pure.getOpenVariables(lambdaFunction, bridge).getMap().forEachKeyValue((key, values) -> openVariables.put((String) key, ((List<?>) values)._values()));
                results.add(bridge.buildLambda(lambdaFunction, DynamicPureFunctionImpl.createPureFunction(lambdaFunction, openVariables, bridge)));
            }
            else if (value instanceof Iterable)
            {
                results.addAllIterable((Iterable<?>) value);
            }
            else if (value != null)
            {
                results.add(value);
            }
        });
        return results;
    }

    private static Object reactivateWithoutJavaCompilationImpl(SimpleFunctionExpression sfe, PureMap lambdaOpenVariablesMap, ExecutionSupport es, boolean atRoot, Bridge bridge)
    {
        Function<?> func = sfe._func();

        // Check that we can reactivate the function (before reactivating the arguments) so that we don't
        // waste time / go further down a broken call sequence

        if (!canReactivateWithoutJavaCompilationImpl(func, es, atRoot, lambdaOpenVariablesMap, bridge))
        {
            throw new PureDynamicReactivateException(sfe.getSourceInformation(), "Can not reactivate function, unexpected:" + func._name());
        }

        MutableList<RichIterable<?>> paramValues = sfe._parametersValues().collect(value ->
        {
            Object newValue = reactivateWithoutJavaCompilationImpl(value, lambdaOpenVariablesMap, es, false, bridge);
            if (newValue instanceof RichIterable)
            {
                return (RichIterable<?>) newValue;
            }
            if (newValue instanceof Iterable)
            {
                return Lists.mutable.withAll((Iterable<?>) newValue);
            }
            return Lists.fixedSize.of(newValue);
        }, Lists.mutable.empty());
        String funcName = func._name();
        if (funcName != null)
        {
            switch (func._name())
            {
                case "new_Class_1__String_1__KeyExpression_MANY__T_1_":
                {
                    //Have to get the first param from the generic type
                    paramValues.set(0, Lists.fixedSize.of(sfe._genericType()._rawType()));
                    break;
                }
                case "new_Class_1__String_1__T_1_":
                {
                    //Have to get the first param from the generic type
                    paramValues.set(0, Lists.fixedSize.of(sfe._genericType()._rawType()));
                    break;
                }
                case "cast_Any_m__T_1__T_m_":
                {
                    //Have to get the second param from the generic type
                    paramValues.set(1, Lists.fixedSize.of(sfe._genericType()));
                    break;
                }
            }
        }

        return Pure._evaluateToMany(es, bridge, func, paramValues);
    }

    @SuppressWarnings("unchecked")
    private static Object reactivateWithoutJavaCompilationImpl(VariableExpression variableExpression, PureMap lambdaOpenVariablesMap)
    {
        String varName = variableExpression._name();
        Object varValue = lambdaOpenVariablesMap.getMap().get(varName);
        if (varValue == null)
        {
            throw new PureDynamicReactivateException("Attempt to use out of scope variable: " + varName);
        }
        return ((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<Object>) varValue)._values();
    }
}