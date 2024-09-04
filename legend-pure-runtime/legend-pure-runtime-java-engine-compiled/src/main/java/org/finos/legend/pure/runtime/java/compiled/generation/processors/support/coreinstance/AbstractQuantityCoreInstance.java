// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ElementOverride;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationContext;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

import java.util.Objects;

public abstract class AbstractQuantityCoreInstance extends ReflectiveCoreInstance implements QuantityCoreInstance
{
    private static final KeyIndex KEY_INDEX = keyIndexBuilder(6)
            .withKeys("Root::meta::pure::metamodel::valuespecification::ValueSpecification", "genericType", "multiplicity", "usageContext")
            .withKey("Root::meta::pure::metamodel::valuespecification::InstanceValue", "values")
            .withKeys("Root::meta::pure::metamodel::type::Any", "classifierGenericType", "elementOverride")
            .build();

    private final Number value;
    private Unit unit;
    private final CompiledExecutionSupport executionSupport;
    private final boolean wrapped;

    protected AbstractQuantityCoreInstance(Number value, String unitPath, CompiledExecutionSupport executionSupport)
    {
        super(value + " " + unitPath);
        this.value = Objects.requireNonNull(value, "value may not be null");
        this.executionSupport = Objects.requireNonNull(executionSupport, "execution support may not be null");
        this.wrapped = true;
    }

    protected AbstractQuantityCoreInstance(Number value, String unitPath, ExecutionSupport executionSupport)
    {
        this(value, unitPath, (CompiledExecutionSupport) executionSupport);
    }

    protected AbstractQuantityCoreInstance(AbstractQuantityCoreInstance src, boolean wrapped)
    {
        super(src.getName());
        this.value = src.value;
        this.unit = src.unit;
        this.executionSupport = src.executionSupport;
        this.wrapped = wrapped;
    }

    protected AbstractQuantityCoreInstance(AbstractQuantityCoreInstance src)
    {
        this(src, src.wrapped);
    }

    @Override
    public Unit getUnit()
    {
        Unit result = this.unit;
        if (result == null)
        {
            String path = getUnitPath();
            this.unit = result = this.executionSupport.getMetadataAccessor().getUnit("Root::" + path);
            if (result == null)
            {
                throw new IllegalStateException("Cannot find unit: " + path);
            }
        }
        return result;
    }

    @Override
    public Number getValue()
    {
        return this.value;
    }

    @Override
    public boolean pureEquals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof QuantityCoreInstance))
        {
            return false;
        }

        QuantityCoreInstance other = (QuantityCoreInstance) obj;
        return getUnitPath().equals(other.getUnitPath()) && CompiledSupport.eq(this.value, other.getValue());
    }

    @Override
    public int pureHashCode()
    {
        return getUnitPath().hashCode() + (31 * this.value.hashCode());
    }

    @Override
    public String toString(ExecutionSupport executionSupport)
    {
        return getName();
    }

    @Override
    public CoreInstance getClassifier()
    {
        return getClassifierType();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return KEY_INDEX.getKeys();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return KEY_INDEX.getRealKeyByName(name);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return M3Properties.values.equals(propertyName) ? getValueCoreInstance() : super.getValueForMetaPropertyToOne(propertyName);
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return M3Properties.values.equals(keyName) ? Lists.immutable.with(getValueCoreInstance()) : super.getValueForMetaPropertyToMany(keyName);
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        if (M3Properties.values.equals(keyName))
        {
            CoreInstance valueCoreInstance = getValueCoreInstance();
            if (Objects.equals(keyInIndex, indexSpec.getIndexKey(valueCoreInstance)))
            {
                return valueCoreInstance;
            }
            return null;
        }
        return super.getValueInValueForMetaPropertyToManyByIDIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        if (M3Properties.values.equals(keyName))
        {
            CoreInstance valueCoreInstance = getValueCoreInstance();
            if (Objects.equals(keyInIndex, indexSpec.getIndexKey(valueCoreInstance)))
            {
                return Lists.immutable.with(valueCoreInstance);
            }
            return Lists.immutable.empty();
        }
        return super.getValueInValueForMetaPropertyToManyByIndex(keyName, indexSpec, keyInIndex);
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return M3Properties.values.equals(keyName) || super.isValueDefinedForKey(keyName);
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFullSystemPath()
    {
        return "Root::" + M3Paths.InstanceValue;
    }

    @Override
    public RichIterable<?> _values()
    {
        return Lists.immutable.with(getValuePossiblyWrapped());
    }

    @Override
    public RichIterable<? extends CoreInstance> _valuesCoreInstance()
    {
        return Lists.immutable.with(getValueCoreInstance());
    }

    @Override
    public InstanceValue _values(RichIterable<?> values)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _valuesRemove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _valuesAdd(Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _valuesAddAll(RichIterable<?> values)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _valuesRemove(Object value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Multiplicity _multiplicity()
    {
        return (Multiplicity) this.executionSupport.getMetadata().getMetadata(M3Paths.Multiplicity, "Root::" + M3Paths.PureOne);
    }

    @Override
    public InstanceValue _multiplicity(Multiplicity value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _multiplicityRemove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public GenericType _genericType()
    {
        return wrapGenericType(getUnit());
    }

    @Override
    public InstanceValue _genericType(GenericType value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _genericTypeRemove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueSpecificationContext _usageContext()
    {
        return null;
    }

    @Override
    public InstanceValue _usageContext(ValueSpecificationContext value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _usageContextRemove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public GenericType _classifierGenericType()
    {
        return wrapGenericType(getClassifierType());
    }

    @Override
    public InstanceValue _classifierGenericType(GenericType value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _classifierGenericTypeRemove()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ElementOverride _elementOverride()
    {
        return null;
    }

    @Override
    public InstanceValue _elementOverride(ElementOverride value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public InstanceValue _elementOverrideRemove()
    {
        throw new UnsupportedOperationException();
    }

    protected abstract QuantityCoreInstance unwrap();

    private Object getValuePossiblyWrapped()
    {
        return this.wrapped ? unwrap() : getValue();
    }

    private CoreInstance getValueCoreInstance()
    {
        return this.wrapped ? unwrap() : ValCoreInstance.toCoreInstance(getValue());
    }

    private Type getClassifierType()
    {
        return this.executionSupport.getMetadataAccessor().getClass("Root::" + M3Paths.InstanceValue);
    }

    private GenericType wrapGenericType(Type rawType)
    {
        return ((GenericType) this.executionSupport.getProcessorSupport().newGenericType(null, null, false))._rawType(rawType);
    }
}
