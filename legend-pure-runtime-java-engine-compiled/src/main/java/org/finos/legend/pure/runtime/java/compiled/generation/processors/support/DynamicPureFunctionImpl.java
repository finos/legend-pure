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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.tools.ListHelper;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Objects;

/*
An implementation of SharedPureFunction that implements the execution by dynamically
looking at the expression sequence and reactivates / evaluates them individually to compose
the overall result of the function.
 */
public class DynamicPureFunctionImpl<T> implements SharedPureFunction<T> {
    private final MutableMap<String, Object> openVariables;
    private final FunctionDefinition<?> func;
    private final Bridge bridge;

    public DynamicPureFunctionImpl(FunctionDefinition<?> func, MutableMap<String, Object> openVariables, Bridge bridge) {
        this.func = Objects.requireNonNull(func, "func");
        this.openVariables = Objects.requireNonNull(openVariables, "openVariables").asUnmodifiable();
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public MutableMap<String, Object> getOpenVariables() {
        return this.openVariables;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T execute(ListIterable<?> vars, ExecutionSupport es) {
        Objects.requireNonNull(vars, "vars");
        Objects.requireNonNull(es, "es");

        MutableMap<String, List<?>> executionOpenVars = Maps.mutable.ofInitialCapacity(this.openVariables.size());
        this.openVariables.forEachKeyValue((key, value) -> executionOpenVars.put(key, toPureList(value)));

        FunctionType ft = (FunctionType) this.func._classifierGenericType()._typeArguments().getFirst()._rawType();
        ListHelper.wrapListIterable(ft._parameters()).forEachWithIndex((var, i) -> executionOpenVars.put(var._name(), toPureList(vars.get(i))));

        PureMap executionOpenVarsPureMap = new PureMap(executionOpenVars);
        return (T) this.func._expressionSequence().injectInto(null, (previousResult, expression) ->
        {
            Object result = Reactivator.reactivateWithoutJavaCompilation(this.bridge, expression, executionOpenVarsPureMap, es);
            if (expression instanceof SimpleFunctionExpression) {
                SimpleFunctionExpression sfe = (SimpleFunctionExpression) expression;
                Function<?> sfeFunc = sfe._func();
                if ((sfeFunc instanceof NativeFunction) && "letFunction_String_1__T_m__T_m_".equals(sfeFunc._name())) {
                    String varName = (String) ((InstanceValue) sfe._parametersValues().getFirst())._values().getFirst();
                    executionOpenVars.put(varName, toPureList(result));
                }
            }
            return result;
        });
    }

    private List<Object> toPureList(Object value) {
        List<Object> list = this.bridge.buildList();
        if (value instanceof Iterable) {
            list._values(Lists.mutable.withAll((Iterable<?>) value));
        } else if (value != null) {
            list._values(Lists.immutable.with(value));
        }
        return list;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{func=" + this.func + ", openVariables=" + this.openVariables + "}";
    }

    public static SharedPureFunction<Object> createPureFunction(FunctionDefinition<?> func, MutableMap<String, Object> openVariables, Bridge bridge)
    {
        DynamicPureFunctionImpl<Object> impl = new DynamicPureFunctionImpl<>(func, openVariables, bridge);
        return (func instanceof LambdaFunction) ? createPureLambdaFunctionWrapper(impl) : impl;
    }

    private static <X> PureLambdaFunction<X> createPureLambdaFunctionWrapper(DynamicPureFunctionImpl<X> inner) {
        RichIterable<? extends VariableExpression> params = ((FunctionType) inner.func._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters();
        switch (params.size()) {
            case 0: {
                return new PureLambdaFunction0<X>() {
                    @Override
                    public MutableMap<String, Object> getOpenVariables() {
                        return inner.getOpenVariables();
                    }

                    @Override
                    public X valueOf(ExecutionSupport executionSupport) {
                        return execute(Lists.immutable.empty(), executionSupport);
                    }

                    @Override
                    public X execute(ListIterable<?> vars, ExecutionSupport es) {
                        return inner.execute(vars, es);
                    }

                    @Override
                    public String toString() {
                        return getClass().getSimpleName() + "{inner=" + inner + "}";
                    }
                };
            }
            case 1: {
                return new PureLambdaFunction1<Object, X>() {
                    @Override
                    public MutableMap<String, Object> getOpenVariables() {
                        return inner.getOpenVariables();
                    }

                    @Override
                    public X value(Object o, ExecutionSupport executionSupport) {
                        return execute(Lists.immutable.with(o), executionSupport);
                    }

                    @Override
                    public X execute(ListIterable<?> vars, ExecutionSupport es) {
                        return inner.execute(vars, es);
                    }

                    @Override
                    public String toString() {
                        return getClass().getSimpleName() + "{inner=" + inner + "}";
                    }
                };
            }
            case 2: {
                return new PureLambdaFunction2<Object, Object, X>() {
                    @Override
                    public MutableMap<String, Object> getOpenVariables() {
                        return inner.getOpenVariables();
                    }

                    @Override
                    public X value(Object o, Object o2, ExecutionSupport executionSupport) {
                        return execute(Lists.immutable.with(o, o2), executionSupport);
                    }

                    @Override
                    public X execute(ListIterable<?> vars, ExecutionSupport es) {
                        return inner.execute(vars, es);
                    }

                    @Override
                    public String toString() {
                        return getClass().getSimpleName() + "{inner=" + inner + "}";
                    }
                };
            }
            case 3: {
                return new PureLambdaFunction3<Object, Object, Object, X>() {
                    @Override
                    public MutableMap<String, Object> getOpenVariables() {
                        return inner.getOpenVariables();
                    }

                    @Override
                    public X value(Object o, Object o2, Object o3, ExecutionSupport executionSupport) {
                        return execute(Lists.immutable.with(o, o2, o3), executionSupport);
                    }

                    @Override
                    public X execute(ListIterable<?> vars, ExecutionSupport es) {
                        return inner.execute(vars, es);
                    }

                    public String toString() {
                        return getClass().getSimpleName() + "{inner=" + inner + "}";
                    }
                };
            }
            default:
             {
                return new PureLambdaFunction<X>()
                {
                    @Override
                    public X execute(ListIterable<?> vars, ExecutionSupport es)
                    {
                        return inner.execute(vars, es);
                    }

                    @Override
                    public MutableMap<String, Object> getOpenVariables()
                    {
                        return inner.getOpenVariables();
                    }
                };
            }
        }
    }
}
