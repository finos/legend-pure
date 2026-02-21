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
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.ByteCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.tools.SafeAppendable;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public abstract class ValCoreInstance<T> extends AbstractCompiledCoreInstance implements PrimitiveCoreInstance<T>
{
    private final T val;

    private ValCoreInstance(T val)
    {
        this.val = val;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ValCoreInstance))
        {
            return false;
        }
        ValCoreInstance<?> that = (ValCoreInstance<?>) o;
        return Objects.equals(this.val, that.val) && Objects.equals(getType(), that.getType());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), this.val, getType());
    }

    public T getValue()
    {
        return this.val;
    }

    @Override
    public String toString()
    {
        return getName() + " instanceOf " + getType();
    }

    public abstract String getType();

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
    public ListIterable<CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return Lists.immutable.empty();
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
    public void print(Appendable appendable, String tab, int max)
    {
        SafeAppendable.wrap(appendable).append(tab).append(this.val);
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

    public static CoreInstance newVal(String val, String type)
    {
        switch (type)
        {
            case M3Paths.Boolean:
            {
                return new BooleanVal(ModelRepository.getBooleanValue(val));
            }
            case M3Paths.Byte:
            {
                return new ByteVal(Byte.parseByte(val));
            }
            case M3Paths.Date:
            {
                return new DateVal(DateFunctions.parsePureDate(val));
            }
            case M3Paths.DateTime:
            {
                return new DateTimeVal(DateFunctions.parsePureDate(val));
            }
            case M3Paths.Decimal:
            {
                return new DecimalVal(new BigDecimal(val));
            }
            case M3Paths.Float:
            {
                return new FloatVal(Double.valueOf(val));
            }
            case M3Paths.Integer:
            {
                return new IntegerVal(Long.valueOf(val));
            }
            case M3Paths.LatestDate:
            {
                return new LatestDateVal();
            }
            case M3Paths.StrictDate:
            {
                return new StrictDateVal(DateFunctions.parsePureDate(val));
            }
            case M3Paths.StrictTime:
            {
                return new StrictTimeVal(StrictTimeFunctions.parsePureStrictTime(val));
            }
            case M3Paths.String:
            {
                return new StringVal(val);
            }
            default:
            {
                throw new RuntimeException(type + " is not supported yet");
            }
        }
    }

    public static CoreInstance toCoreInstance(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof CoreInstance)
        {
            return (CoreInstance) value;
        }
        if (value instanceof Boolean)
        {
            return new BooleanVal((Boolean) value);
        }
        if (value instanceof Byte)
        {
            return new ByteVal((Byte) value);
        }
        if (value instanceof PureDate)
        {
            PureDate date = (PureDate) value;
            String type = DateFunctions.datePrimitiveType(date);
            switch (type)
            {
                case M3Paths.DateTime:
                {
                    return new DateTimeVal(date);
                }
                case M3Paths.StrictDate:
                {
                    return new StrictDateVal(date);
                }
                case M3Paths.LatestDate:
                {
                    return new LatestDateVal();
                }
                case M3Paths.Date:
                {
                    return new DateVal(date);
                }
                default:
                {
                    throw new RuntimeException("Unknown date type '" + type + "' for date: " + date);
                }
            }
        }
        if ((value instanceof Long) || (value instanceof Integer) || (value instanceof BigInteger))
        {
            return new IntegerVal(((Number) value).longValue());
        }
        if ((value instanceof Double) || (value instanceof Float))
        {
            return new FloatVal(((Number) value).doubleValue());
        }
        if (value instanceof BigDecimal)
        {
            return new DecimalVal((BigDecimal) value);
        }
        if (value instanceof String)
        {
            return new StringVal((String) value);
        }
        if (value instanceof PureStrictTime)
        {
            return new StrictTimeVal((PureStrictTime) value);
        }
        throw new RuntimeException("TODO " + value.getClass());
    }

    public static ListIterable<CoreInstance> toCoreInstances(RichIterable<?> values)
    {
        return ((values == null) || values.isEmpty()) ? Lists.immutable.empty() : values.collect(ValCoreInstance::toCoreInstance, Lists.mutable.ofInitialCapacity(values.size()));
    }

    private static class BooleanVal extends ValCoreInstance<Boolean> implements BooleanCoreInstance
    {
        private BooleanVal(boolean value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.Boolean;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue().booleanValue());
        }

        @Override
        public BooleanCoreInstance copy()
        {
            return new BooleanVal(getValue());
        }
    }

    private static class ByteVal extends ValCoreInstance<Byte> implements ByteCoreInstance
    {
        private ByteVal(byte value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.Byte;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue().byteValue());
        }

        @Override
        public ByteCoreInstance copy()
        {
            return new ByteVal(getValue());
        }
    }

    private static class DateVal extends ValCoreInstance<PureDate> implements DateCoreInstance
    {
        private DateVal(PureDate value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.Date;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public DateCoreInstance copy()
        {
            return new DateVal(getValue());
        }
    }

    private static class StrictDateVal extends ValCoreInstance<PureDate> implements DateCoreInstance
    {
        private StrictDateVal(PureDate value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.StrictDate;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public DateCoreInstance copy()
        {
            return new StrictDateVal(getValue());
        }
    }

    private static class DateTimeVal extends ValCoreInstance<PureDate> implements DateCoreInstance
    {
        private DateTimeVal(PureDate value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.DateTime;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public DateCoreInstance copy()
        {
            return new DateTimeVal(getValue());
        }
    }

    private static class LatestDateVal extends ValCoreInstance<PureDate> implements DateCoreInstance
    {
        private LatestDateVal()
        {
            super(LatestDate.instance);
        }

        @Override
        public String getType()
        {
            return M3Paths.LatestDate;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public DateCoreInstance copy()
        {
            return new LatestDateVal();
        }
    }

    private static class IntegerVal extends ValCoreInstance<Long>
    {
        private IntegerVal(Long value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.Integer;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public PrimitiveCoreInstance<Long> copy()
        {
            return new IntegerVal(getValue());
        }
    }

    private static class FloatVal extends ValCoreInstance<Double>
    {
        private FloatVal(Double value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.Float;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue().doubleValue());
        }

        @Override
        public PrimitiveCoreInstance<Double> copy()
        {
            return new FloatVal(getValue());
        }
    }

    private static class DecimalVal extends ValCoreInstance<BigDecimal> implements DecimalCoreInstance
    {
        private DecimalVal(BigDecimal value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.Decimal;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public DecimalCoreInstance copy()
        {
            return new DecimalVal(getValue());
        }
    }

    private static class StringVal extends ValCoreInstance<String> implements StringCoreInstance
    {
        private StringVal(String value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.String;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public StringCoreInstance copy()
        {
            return new StringVal(getValue());
        }
    }

    private static class StrictTimeVal extends ValCoreInstance<PureStrictTime> implements StrictTimeCoreInstance
    {
        private StrictTimeVal(PureStrictTime value)
        {
            super(value);
        }

        @Override
        public String getType()
        {
            return M3Paths.StrictTime;
        }

        @Override
        public String getName()
        {
            return CompiledSupport.primitiveToString(getValue());
        }

        @Override
        public StrictTimeCoreInstance copy()
        {
            return new StrictTimeVal(getValue());
        }
    }
}
