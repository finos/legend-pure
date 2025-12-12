// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.coreinstance.lazy.simple;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.lazy.LazyCoreInstanceUtilities;
import org.finos.legend.pure.m3.coreinstance.lazy.ManyValues;
import org.finos.legend.pure.m3.coreinstance.lazy.PrimitiveValueResolver;
import org.finos.legend.pure.m3.coreinstance.lazy.PropertyValue;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.serialization.compiler.element.ElementBuilder;
import org.finos.legend.pure.m3.serialization.compiler.element.InstanceData;
import org.finos.legend.pure.m3.serialization.compiler.metadata.BackReference;
import org.finos.legend.pure.m3.serialization.compiler.reference.ReferenceIdResolver;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstanceMutableState;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.util.function.IntFunction;
import java.util.function.Supplier;

class SimpleLazyCoreInstanceState extends AbstractCoreInstanceMutableState
{
    private static final ImmutableMap<String, ImmutableList<String>> BACK_REF_PROPERTY_PATH_MAP = M3PropertyPaths.BACK_REFERENCE_PROPERTY_PATHS
            .groupByUniqueKey(ImmutableList::getLast);

    private final ConcurrentMutableMap<String, ExtendedPropertyValue> values;

    SimpleLazyCoreInstanceState()
    {
        this.values = ConcurrentHashMap.newMap();
    }

    SimpleLazyCoreInstanceState(int initialCapacity)
    {
        this.values = ConcurrentHashMap.newMap(initialCapacity);
    }

    private SimpleLazyCoreInstanceState(SimpleLazyCoreInstanceState source)
    {
        this.values = ConcurrentHashMap.newMap(source.values.size());
        source.values.forEachKeyValue((k, v) -> this.values.put(k, v.copy()));
    }

    RichIterable<String> getKeys()
    {
        return this.values.keysView();
    }

    ImmutableList<String> getRealKey(String property)
    {
        ExtendedPropertyValue extValue = this.values.get(property);
        return (extValue == null) ? null : extValue.realKey;
    }

    PropertyValue<CoreInstance> getValue(String property)
    {
        ExtendedPropertyValue extValue = this.values.get(property);
        return (extValue == null) ? null : extValue.value;
    }

    void setKeyValue(ImmutableList<String> realKey, PropertyValue<CoreInstance> value)
    {
        this.values.put(realKey.getLast(), new ExtendedPropertyValue(realKey, value));
    }

    void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> values)
    {
        this.values.updateValue(key.getLast(),
                ExtendedPropertyValue::new,
                epv ->
                {
                    epv.value.setValues(values);
                    epv.possiblySetRealKey(key);
                    return epv;
                });
    }

    void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        this.values.updateValue(key.getLast(),
                ExtendedPropertyValue::new,
                epv ->
                {
                    epv.value.addValue(value);
                    epv.possiblySetRealKey(key);
                    return epv;
                });
    }

    void removeProperty(String property)
    {
        this.values.remove(property);
    }

    void fromInstanceData(InstanceData instanceData, ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, PrimitiveValueResolver primitiveValueResolver, ElementBuilder elementBuilder)
    {
        MutableMap<String, MutableList<Supplier<? extends CoreInstance>>> backRefMap = collectBackReferences(backReferences, referenceIdResolver, internalIdResolver, elementBuilder);
        instanceData.getPropertyValues().forEach(pv ->
        {
            String name = pv.getPropertyName();
            ListIterable<? extends Supplier<? extends CoreInstance>> extraSuppliers = backRefMap.remove(name);
            ManyValues<CoreInstance> values = LazyCoreInstanceUtilities.newToManyPropertyValue(pv, referenceIdResolver, internalIdResolver, primitiveValueResolver, false, extraSuppliers);
            MutableList<String> realKey = _Package.convertM3PathToM4(pv.getPropertySourceType()).with(M3Properties.properties).with(name);
            this.values.put(name, new ExtendedPropertyValue(realKey.toImmutable(), values));
        });
        if (backRefMap.notEmpty())
        {
            backRefMap.forEachKeyValue((property, suppliers) -> this.values.put(property, new ExtendedPropertyValue(BACK_REF_PROPERTY_PATH_MAP.get(property), ManyValues.fromSuppliers(suppliers))));
        }
        setCompileStateBitSet(instanceData.getCompileStateBitSet());
    }

    SimpleLazyCoreInstanceState copy()
    {
        return new SimpleLazyCoreInstanceState(this);
    }

    private static class ExtendedPropertyValue
    {
        private volatile ImmutableList<String> realKey;
        private final PropertyValue<CoreInstance> value;

        private ExtendedPropertyValue(ImmutableList<String> realKey, PropertyValue<CoreInstance> value)
        {
            this.realKey = realKey;
            this.value = value;
        }

        private ExtendedPropertyValue()
        {
            this(null, ManyValues.fromValues(null));
        }

        void possiblySetRealKey(ListIterable<String> key)
        {
            if (this.realKey == null)
            {
                synchronized (this)
                {
                    if (this.realKey == null)
                    {
                        this.realKey = key.toImmutable();
                    }
                }
            }
        }

        ExtendedPropertyValue copy()
        {
            return new ExtendedPropertyValue(this.realKey, this.value.copy());
        }
    }

    private static MutableMap<String, MutableList<Supplier<? extends CoreInstance>>> collectBackReferences(ListIterable<? extends BackReference> backReferences, ReferenceIdResolver referenceIdResolver, IntFunction<? extends CoreInstance> internalIdResolver, ElementBuilder elementBuilder)
    {
        MutableList<Supplier<? extends CoreInstance>> applications = Lists.mutable.empty();
        MutableList<Supplier<? extends CoreInstance>> modelElements = Lists.mutable.empty();
        MutableList<Supplier<? extends CoreInstance>> propertiesFromAssociations = Lists.mutable.empty();
        MutableList<Supplier<? extends CoreInstance>> qualifiedPropertiesFromAssociations = Lists.mutable.empty();
        MutableList<Supplier<? extends CoreInstance>> referenceUsages = Lists.mutable.empty();
        MutableList<Supplier<? extends CoreInstance>> specializations = Lists.mutable.empty();
        LazyCoreInstanceUtilities.collectBackReferences(backReferences, referenceIdResolver, internalIdResolver, elementBuilder, applications, modelElements, propertiesFromAssociations, qualifiedPropertiesFromAssociations, referenceUsages, specializations);

        MutableMap<String, MutableList<Supplier<? extends CoreInstance>>> backRefMap = Maps.mutable.ofInitialCapacity(6);
        if (applications.notEmpty())
        {
            backRefMap.put(M3Properties.applications, applications);
        }
        if (modelElements.notEmpty())
        {
            backRefMap.put(M3Properties.modelElements, modelElements);
        }
        if (propertiesFromAssociations.notEmpty())
        {
            backRefMap.put(M3Properties.propertiesFromAssociations, propertiesFromAssociations);
        }
        if (qualifiedPropertiesFromAssociations.notEmpty())
        {
            backRefMap.put(M3Properties.qualifiedPropertiesFromAssociations, qualifiedPropertiesFromAssociations);
        }
        if (referenceUsages.notEmpty())
        {
            backRefMap.put(M3Properties.referenceUsages, referenceUsages);
        }
        if (specializations.notEmpty())
        {
            backRefMap.put(M3Properties.specializations, specializations);
        }
        return backRefMap;
    }
}
