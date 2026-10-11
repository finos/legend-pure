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

package org.finos.legend.pure.rust.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.rust.RustEngineHost;
import org.finos.legend.pure.runtime.java.interpreted.LambdaWithContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.MapCoreInstance;

/**
 * The interpreted engine: nodes know their keys, primitives come from the
 * {@link ModelRepository} factories.
 */
public final class InterpretedHost implements RustEngineHost
{
    private final ModelRepository repository;
    private final ProcessorSupport processorSupport;

    public InterpretedHost(ModelRepository repository, ProcessorSupport processorSupport)
    {
        this.repository = repository;
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
        return node.getKeys().toList();
    }

    @Override
    public ListIterable<? extends CoreInstance> capturedValues(CoreInstance lambda, String name)
    {
        if (!(lambda instanceof LambdaWithContext))
        {
            return null;
        }
        CoreInstance value = ((LambdaWithContext) lambda).getVariableContext().getValue(name);
        if (value == null)
        {
            return Lists.immutable.empty();
        }
        return Instance.instanceOf(value, M3Paths.InstanceValue, this.processorSupport)
                ? Instance.getValueForMetaPropertyToManyResolved(value, M3Properties.values, this.processorSupport)
                : Lists.immutable.with(value);
    }

    /**
     * The lambda node's repository synthetic id, unique to the node within
     * the runtime's repository; a {@link LambdaWithContext} (a new wrapper per
     * evaluation) reports the id of the lambda it wraps.
     */
    @Override
    public Object lambdaDefinition(CoreInstance lambda)
    {
        int syntheticId = lambda.getSyntheticId();
        return (syntheticId < 0) ? null : syntheticId;
    }

    @Override
    public CoreInstance newBoolean(boolean value)
    {
        return this.repository.newBooleanCoreInstance(value);
    }

    @Override
    public CoreInstance newInteger(long value)
    {
        return this.repository.newIntegerCoreInstance(value);
    }

    @Override
    public CoreInstance newFloat(String text)
    {
        return this.repository.newFloatCoreInstance(text);
    }

    @Override
    public CoreInstance newDecimal(String text)
    {
        return this.repository.newDecimalCoreInstance(text);
    }

    @Override
    public CoreInstance newString(String value)
    {
        return this.repository.newStringCoreInstance(value);
    }

    @Override
    public CoreInstance newDate(PureDate date)
    {
        return this.repository.newDateCoreInstance(date);
    }

    @Override
    public CoreInstance newLatestDate()
    {
        return this.repository.newLatestDateCoreInstance();
    }

    @Override
    public CoreInstance newStrictTime(PureStrictTime time)
    {
        return this.repository.newStrictTimeCoreInstance(time);
    }

    /**
     * A {@link MapCoreInstance} as the engine's {@code newMap} builds one:
     * keys hashed with the engine's equality, classified by {@code Map}.
     */
    @Override
    public CoreInstance newMap(CoreInstance[] keys, CoreInstance[] values)
    {
        CoreInstance mapClass = this.processorSupport.package_getByUserPath(M3Paths.Map);
        MapCoreInstance map = new MapCoreInstance(Lists.immutable.empty(), "", null, mapClass, -1, this.repository, false, this.processorSupport);
        CoreInstance genericType = this.processorSupport.newGenericType(null, map, false);
        Instance.addValueToProperty(genericType, M3Properties.rawType, mapClass, this.processorSupport);
        Instance.addValueToProperty(map, M3Properties.classifierGenericType, genericType, this.processorSupport);
        for (int i = 0; i < keys.length; i++)
        {
            map.getMap().put(keys[i], values[i]);
        }
        return map;
    }
}
