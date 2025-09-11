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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public abstract class Value<T> extends ValueOrReference
{
    private final T value;

    private Value(T value)
    {
        this.value = Objects.requireNonNull(value, "value may not be null");
    }

    private Value()
    {
        this.value = null;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (getClass() != other.getClass())
        {
            return false;
        }
        Value<?> that = (Value<?>) other;
        return this.getClassifierPath().equals(that.getClassifierPath()) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode()
    {
        return getClassifierPath().hashCode() + 31 * Objects.hashCode(this.value);
    }

    @Override
    StringBuilder appendString(StringBuilder builder)
    {
        builder.append("Value{classifier=").append(getClassifierPath());
        if (this.value != null)
        {
            appendValueString(builder.append(" value="));
        }
        return builder.append('}');
    }

    void appendValueString(StringBuilder builder)
    {
        builder.append(this.value);
    }

    public abstract String getClassifierPath();

    public T getValue()
    {
        return this.value;
    }

    public static BooleanValue newBooleanValue(boolean value)
    {
        return value ? BooleanValue.TRUE : BooleanValue.FALSE;
    }

    public static ByteValue newByteValue(byte b)
    {
        return new ByteValue(b);
    }

    public static DateValue newDateValue(PureDate value)
    {
        return new DateValue(value);
    }

    public static DateTimeValue newDateTimeValue(PureDate value)
    {
        return new DateTimeValue(value);
    }

    public static StrictDateValue newStrictDateValue(PureDate value)
    {
        return new StrictDateValue(value);
    }

    public static LatestDateValue newLatestDateValue()
    {
        return LatestDateValue.INSTANCE;
    }

    public static DecimalValue newDecimalValue(String value)
    {
        int lastIndex = value.length() - 1;
        char lastChar = value.charAt(lastIndex);
        boolean endsWithD = (lastChar == 'd') || (lastChar == 'D');
        return newDecimalValue(new BigDecimal(endsWithD ? value.substring(0, lastIndex) : value));
    }

    public static DecimalValue newDecimalValue(BigDecimal value)
    {
        return new DecimalValue(value);
    }

    public static FloatValue newFloatValue(String value)
    {
        int lastIndex = value.length() - 1;
        char lastChar = value.charAt(lastIndex);
        boolean endsWithF = (lastChar == 'f') || (lastChar == 'F');
        return newFloatValue(new BigDecimal(endsWithF ? value.substring(0, lastIndex) : value));
    }

    public static FloatValue newFloatValue(float value)
    {
        return newFloatValue(BigDecimal.valueOf(value));
    }

    public static FloatValue newFloatValue(double value)
    {
        return newFloatValue(BigDecimal.valueOf(value));
    }

    public static FloatValue newFloatValue(BigDecimal value)
    {
        return new FloatValue(value);
    }

    public static IntegerValue newIntegerValue(int value)
    {
        return new IntegerValue(value);
    }

    public static IntegerValue newIntegerValue(long value)
    {
        return new IntegerValue(value);
    }

    public static IntegerValue newIntegerValue(BigInteger value)
    {
        return new IntegerValue(value);
    }

    public static StrictTimeValue newStrictTimeValue(PureStrictTime value)
    {
        return new StrictTimeValue(value);
    }

    public static StringValue newStringValue(String value)
    {
        return new StringValue(value);
    }

    public static class BooleanValue extends Value<Boolean>
    {
        private static final BooleanValue TRUE = new BooleanValue(true);
        private static final BooleanValue FALSE = new BooleanValue(false);

        private BooleanValue(boolean value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.Boolean;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }
    }

    public static class ByteValue extends Value<Byte>
    {
        private ByteValue(Byte value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.Byte;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }
    }

    public static class DateValue extends Value<PureDate>
    {
        private DateValue(PureDate value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.Date;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        void appendValueString(StringBuilder builder)
        {
            getValue().appendString(builder);
        }
    }

    public static class DateTimeValue extends Value<PureDate>
    {
        private DateTimeValue(PureDate value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.DateTime;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        void appendValueString(StringBuilder builder)
        {
            getValue().appendString(builder);
        }
    }

    public static class StrictDateValue extends Value<PureDate>
    {
        private StrictDateValue(PureDate value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.StrictDate;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        void appendValueString(StringBuilder builder)
        {
            getValue().appendString(builder);
        }
    }

    public static class LatestDateValue extends Value<Void>
    {
        private static final LatestDateValue INSTANCE = new LatestDateValue();

        private LatestDateValue()
        {
            super();
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.LatestDate;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }
    }

    public static class DecimalValue extends Value<BigDecimal>
    {
        private DecimalValue(BigDecimal value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.Decimal;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }
    }

    public static class FloatValue extends Value<BigDecimal>
    {
        private FloatValue(BigDecimal value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.Float;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }
    }

    public static class IntegerValue extends Value<Number>
    {
        private IntegerValue(Number value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.Integer;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }
    }

    public static class StrictTimeValue extends Value<PureStrictTime>
    {
        private StrictTimeValue(PureStrictTime value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.StrictTime;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        void appendValueString(StringBuilder builder)
        {
            getValue().writeString(builder);
        }
    }

    public static class StringValue extends Value<String>
    {
        private StringValue(String value)
        {
            super(value);
        }

        @Override
        public String getClassifierPath()
        {
            return M3Paths.String;
        }

        @Override
        public <V> V visit(ValueOrReferenceVisitor<V> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        void appendValueString(StringBuilder builder)
        {
            builder.append('\'').append(getValue()).append('\'');
        }
    }
}
