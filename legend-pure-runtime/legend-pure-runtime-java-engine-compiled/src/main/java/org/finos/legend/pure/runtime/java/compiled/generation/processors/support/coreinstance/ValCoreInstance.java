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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public class ValCoreInstance extends AbstractCompiledCoreInstance
{
    private final String val;
    private final String type;

    public ValCoreInstance(String val, String type)
    {
        this.val = val;
        this.type = type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ValCoreInstance))
        {
            return false;
        }
        ValCoreInstance that = (ValCoreInstance) o;
        return Objects.equals(val, that.val) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), val, type);
    }

    public Object getValue()
    {
        switch (this.type)
        {
            case M3Paths.Integer:
            {
                return Long.valueOf(this.val);
            }
            case M3Paths.String:
            {
                return this.val;
            }
            case M3Paths.Boolean:
            {
                return ModelRepository.getBooleanValue(this.val);
            }
            case M3Paths.Date:
            case M3Paths.StrictDate:
            case M3Paths.DateTime:
            {
                return DateFunctions.parsePureDate(this.val);
            }
            case M3Paths.LatestDate:
            {
                return LatestDate.instance;
            }
            case M3Paths.Float:
            {
                return Double.valueOf(this.val);
            }
            case M3Paths.Decimal:
            {
                return new BigDecimal(this.val);
            }
            default:
            {
                throw new RuntimeException(this.type + " is not supported yet");
            }
        }
    }

    @Override
    public String toString()
    {
        return this.val + " instanceOf " + this.type;
    }

    public String getType()
    {
        return this.type;
    }

    @Override
    public String getName()
    {
        return this.val;
    }

    @Override
    public ModelRepository getRepository()
    {
        return null;
    }

    @Override
    public int getSyntheticId()
    {
        return -1;
    }

    @Override
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return null;
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProperty(String keyName)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return null;
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        return null;
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
    {
        return null;
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return Lists.immutable.empty();
    }

    @Override
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
    {
        return Lists.immutable.empty();
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToMany(String keyName, String keyInMany)
    {
        return null;
    }

    @Override
    public CoreInstance getValueInValueForMetaPropertyToManyWithKey(String keyName, String key, String keyInMany)
    {
        return null;
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return null;
    }

    @Override
    public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return Lists.immutable.empty();
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return false;
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return Lists.immutable.empty();
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return null;
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
    {
    }

    @Override
    public CoreInstance copy()
    {
        return new ValCoreInstance(this.val, this.type);
    }

    @Override
    public void printFull(Appendable appendable, String tab)
    {

    }

    @Override
    public void print(Appendable appendable, String tab)
    {

    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {

    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab)
    {

    }

    @Override
    public void printWithoutDebug(Appendable appendable, String tab, int max)
    {

    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
    }

    @Override
    public void rollback(ModelRepositoryTransaction transaction)
    {
    }

    public static CoreInstance toCoreInstance(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof CoreInstance)
        {
            return (CoreInstance)value;
        }
        String type;
        if (value instanceof Boolean)
        {
            type = M3Paths.Boolean;
        }
        else if (value instanceof PureDate)
        {
            type = DateFunctions.datePrimitiveType((PureDate)value);
        }
        else if ((value instanceof Long) || (value instanceof Integer) || (value instanceof BigInteger))
        {
            type = M3Paths.Integer;
        }
        else if ((value instanceof Double) || (value instanceof Float))
        {
            type = M3Paths.Float;
        }
        else if (value instanceof BigDecimal)
        {
            type = M3Paths.Decimal;
        }
        else if (value instanceof String)
        {
            type = M3Paths.String;
        }
        else
        {
            throw new RuntimeException("TODO " + value.getClass());
        }
        return new ValCoreInstance(CompiledSupport.primitiveToString(value), type);
    }

    public static ListIterable<CoreInstance> toCoreInstances(RichIterable<?> values)
    {
        return ((values == null) || values.isEmpty()) ? Lists.immutable.empty() : values.collect(ValCoreInstance::toCoreInstance, Lists.mutable.ofInitialCapacity(values.size()));
    }
}
