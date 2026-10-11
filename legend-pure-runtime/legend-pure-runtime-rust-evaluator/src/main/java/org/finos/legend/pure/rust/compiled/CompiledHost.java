// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.rust.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.rust.RustEngineHost;
import org.finos.legend.pure.runtime.java.compiled.CoreHelper;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.PureLambdaFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.math.BigDecimal;

/**
 * The compiled engine: a node's keys are its class's properties (compiled
 * instances do not enumerate their own), and primitives are the compiled
 * engine's {@link ValCoreInstance} wrappers over plain Java values, which
 * {@link CompiledEvalRust} unwraps before handing results to generated code.
 */
public final class CompiledHost implements RustEngineHost
{
    private final ProcessorSupport processorSupport;

    public CompiledHost(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    @Override
    public ProcessorSupport processorSupport()
    {
        return this.processorSupport;
    }

    @Override
    public ListIterable<String> propertyKeys(CoreInstance node)
    {
        CoreInstance classifier = this.processorSupport.getClassifier(node);
        return this.processorSupport.class_getSimplePropertiesByName(classifier).keysView().toList();
    }

    /**
     * A compiled lambda keeps its open variables in the generated function
     * object; a value is a plain Java value, a collection of them, or a
     * compiled graph object.
     */
    @Override
    public ListIterable<? extends CoreInstance> capturedValues(CoreInstance lambda, String name)
    {
        if (!(lambda instanceof LambdaCompiledExtended))
        {
            return null;
        }
        SharedPureFunction<?> function = ((LambdaCompiledExtended<?>) lambda).pureFunction();
        if (!(function instanceof PureLambdaFunction))
        {
            return null;
        }
        MutableMap<String, Object> openVariables = ((PureLambdaFunction<?>) function).getOpenVariables();
        if ((openVariables == null) || !openVariables.containsKey(name))
        {
            return Lists.immutable.empty();
        }
        Object value = openVariables.get(name);
        if (value == null)
        {
            return Lists.immutable.empty();
        }
        if (value instanceof RichIterable)
        {
            return ValCoreInstance.toCoreInstances((RichIterable<?>) value);
        }
        return Lists.immutable.with(ValCoreInstance.toCoreInstance(value));
    }

    /**
     * A compiled lambda value is a new object per evaluation, and its metadata
     * lambda (the stable identity) is not exposed; no key, so nothing is
     * cached for it.
     */
    @Override
    public Object lambdaDefinition(CoreInstance lambda)
    {
        return null;
    }

    @Override
    public CoreInstance newBoolean(boolean value)
    {
        return ValCoreInstance.toCoreInstance(value);
    }

    @Override
    public CoreInstance newInteger(long value)
    {
        return ValCoreInstance.toCoreInstance(value);
    }

    @Override
    public CoreInstance newFloat(String text)
    {
        return ValCoreInstance.toCoreInstance(Double.parseDouble(text));
    }

    @Override
    public CoreInstance newDecimal(String text)
    {
        return ValCoreInstance.toCoreInstance(new BigDecimal(text));
    }

    @Override
    public CoreInstance newString(String value)
    {
        return ValCoreInstance.toCoreInstance(value);
    }

    @Override
    public CoreInstance newDate(PureDate date)
    {
        return ValCoreInstance.toCoreInstance(date);
    }

    @Override
    public CoreInstance newLatestDate()
    {
        return ValCoreInstance.toCoreInstance(LatestDate.instance);
    }

    @Override
    public CoreInstance newStrictTime(PureStrictTime time)
    {
        return ValCoreInstance.toCoreInstance(time);
    }

    /**
     * A {@link PureMap} as the engine's {@code newMap} builds one (keys
     * hashed with {@code PureEqualsHashingStrategy}), over plain Java values.
     * A {@code PureMap} is not a {@link CoreInstance}, so it crosses in a
     * {@link CompiledValue}, which {@link CompiledEvalRust} unwraps.
     */
    @Override
    public CoreInstance newMap(CoreInstance[] keys, CoreInstance[] values)
    {
        PureMap map = CoreHelper.newMap();
        for (int i = 0; i < keys.length; i++)
        {
            map.getMap().put(CompiledValue.unwrap(keys[i]), CompiledValue.unwrap(values[i]));
        }
        return new CompiledValue(map, this.processorSupport.package_getByUserPath(M3Paths.Map));
    }
}
