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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction0;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction1;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction2;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction3;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Objects;

/*
An implementation of PureLambdaFunction that implements the execution by dynamically
looking at the expression sequence and reactivates / evaluates them individually to compose
the overall result of the function.
 */
public class DynamicPureLambdaFunctionImpl<T> implements PureLambdaFunction<T>
{
    private final MutableMap<String, Object> openVariables;
    private final LambdaFunction func;
    private final Bridge bridge;

    public DynamicPureLambdaFunctionImpl(
            LambdaFunction func,
            MutableMap<String, Object> openVariables,
            Bridge bridge
    )
    {
        Objects.requireNonNull(func, "func");
        Objects.requireNonNull(openVariables, "openVariables");

        this.func = func;
        this.openVariables = openVariables.asUnmodifiable();
        this.bridge = bridge;
    }

    @Override
    public MutableMap<String, Object> getOpenVariables()
    {
        return openVariables;
    }

    @Override
    public T execute(ListIterable vars, ExecutionSupport es)
    {
        Objects.requireNonNull(vars, "vars");
        Objects.requireNonNull(es, "es");

        PureMap runningOpenVariablesMap = new PureMap(UnifiedMap.newMap());
        for (Pair entry : openVariables.keyValuesView())
        {
            runningOpenVariablesMap.getMap().put(entry.getOne(), createList()._valuesAddAll(CompiledSupport.toPureCollection(entry.getTwo())));
        }

        FunctionType ft = (FunctionType)func._classifierGenericType()._typeArguments().getFirst()._rawType();
        ImmutableList<? extends VariableExpression> parameters = Lists.immutable.withAll(ft._parameters());
        for (int i = 0; i < parameters.size(); i++)
        {
            runningOpenVariablesMap.getMap().put(parameters.get(i)._name(), createList()._valuesAddAll(CompiledSupport.toPureCollection(vars.get(i))));
        }

        Object finalResult = null;
        for (Object expressionSequenceItem : func._expressionSequence())
        {
            ValueSpecification vs = (ValueSpecification)expressionSequenceItem;
            Object result = Reactivator.reactivateWithoutJavaCompilation(bridge, vs, runningOpenVariablesMap, es);

            if (vs instanceof SimpleFunctionExpression)
            {
                SimpleFunctionExpression sfe = (SimpleFunctionExpression)vs;
                if (sfe._func() instanceof NativeFunction && sfe._func()._name().equals("letFunction_String_1__T_m__T_m_"))
                {
                    String varName = (String)((InstanceValue)sfe._parametersValues().getFirst())._values().getFirst();
                    runningOpenVariablesMap.getMap().put(varName, createList()._valuesAddAll(CompiledSupport.toPureCollection(result)));
                }
            }

            finalResult = result;
        }
        return (T)finalResult;
    }

    private List<Object> createList()
    {
        return this.bridge.listBuilder().value();
    }

    @Override
    public String toString()
    {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .add("func", func)
                .add("openVariables", openVariables)
                .toString();
    }

    private LambdaFunction lambdaFunction()
    {
        return this.func;
    }

    public static PureLambdaFunction<Object> createPureLambdaFunction(
            final LambdaFunction func,
            final MutableMap<String, Object> openVariables, Bridge bridge)
    {
        return createPureLambdaFunctionWrapper(new DynamicPureLambdaFunctionImpl<Object>(func, openVariables, bridge));
    }

    public static <X> PureLambdaFunction<X> createPureLambdaFunctionWrapper(final DynamicPureLambdaFunctionImpl<X> inner)
    {

        LambdaFunction func = inner.lambdaFunction();
        RichIterable<? extends VariableExpression> params = ((FunctionType)func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();

        if (params.size() == 0)
        {
            return new PureLambdaFunction0<X>()
            {
                @Override
                public MutableMap<String, Object> getOpenVariables()
                {
                    return inner.getOpenVariables();
                }

                @Override
                public X valueOf(ExecutionSupport executionSupport)
                {
                    return execute(Lists.immutable.empty(), executionSupport);
                }

                @Override
                public X execute(ListIterable vars, ExecutionSupport es)
                {
                    return inner.execute(vars, es);
                }

                @Override
                public String toString()
                {
                    return com.google.common.base.MoreObjects.toStringHelper(this)
                            .add("inner", inner)
                            .toString();
                }
            };
        }
        else if (params.size() == 1)
        {
            return new PureLambdaFunction1<Object, X>()
            {
                @Override
                public MutableMap<String, Object> getOpenVariables()
                {
                    return inner.getOpenVariables();
                }

                @Override
                public X value(Object o, ExecutionSupport executionSupport)
                {
                    return execute(Lists.immutable.with(o), executionSupport);
                }

                @Override
                public X execute(ListIterable vars, ExecutionSupport es)
                {
                    return inner.execute(vars, es);
                }

                public String toString()
                {
                    return com.google.common.base.MoreObjects.toStringHelper(this)
                            .add("inner", inner)
                            .toString();
                }
            };
        }
        else if (params.size() == 2)
        {
            return new PureLambdaFunction2<Object, Object, X>()
            {
                @Override
                public MutableMap<String, Object> getOpenVariables()
                {
                    return inner.getOpenVariables();
                }

                @Override
                public X value(Object o, Object o2, ExecutionSupport executionSupport)
                {
                    return execute(Lists.immutable.with(o, o2), executionSupport);
                }

                @Override
                public X execute(ListIterable vars, ExecutionSupport es)
                {
                    return inner.execute(vars, es);
                }

                public String toString()
                {
                    return com.google.common.base.MoreObjects.toStringHelper(this)
                            .add("inner", inner)
                            .toString();
                }
            };
        }
        else if (params.size() == 3)
        {
            return new PureLambdaFunction3<Object, Object, Object, X>()
            {
                @Override
                public MutableMap<String, Object> getOpenVariables()
                {
                    return inner.getOpenVariables();
                }

                @Override
                public X value(Object o, Object o2, Object o3, ExecutionSupport executionSupport)
                {
                    return execute(Lists.immutable.with(o, o2, o3), executionSupport);
                }

                @Override
                public X execute(ListIterable vars, ExecutionSupport es)
                {
                    return inner.execute(vars, es);
                }

                public String toString()
                {
                    return com.google.common.base.MoreObjects.toStringHelper(this)
                            .add("inner", inner)
                            .toString();
                }
            };
        }
        else
        {
            return inner;
        }
    }
}
