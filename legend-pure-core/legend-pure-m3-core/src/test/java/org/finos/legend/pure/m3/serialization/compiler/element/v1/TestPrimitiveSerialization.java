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

package org.finos.legend.pure.m3.serialization.compiler.element.v1;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;
import org.finos.legend.pure.m4.serialization.binary.BinaryReaders;
import org.finos.legend.pure.m4.serialization.binary.BinaryWriters;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestPrimitiveSerialization
{
    private final ModelRepository modelRepository = new ModelRepository();
    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    @Test
    public void testBooleanSerialization()
    {
        byte[] trueBytes = serialize(this.modelRepository.newBooleanCoreInstance(true), BaseV1::serializeBoolean);
        Assert.assertEquals(1, trueBytes.length);
        Assert.assertEquals(BaseV1.BOOLEAN, trueBytes[0] & BaseV1.NODE_TYPE_MASK);
        Assert.assertEquals(BaseV1.BOOLEAN_TRUE, trueBytes[0] & BaseV1.BOOLEAN_MASK);
        Assert.assertTrue(BaseV1.deserializeBoolean(trueBytes[0]));

        byte[] falseBytes = serialize(this.modelRepository.newBooleanCoreInstance(false), BaseV1::serializeBoolean);
        Assert.assertEquals(1, falseBytes.length);
        Assert.assertEquals(BaseV1.BOOLEAN, falseBytes[0] & BaseV1.NODE_TYPE_MASK);
        Assert.assertEquals(BaseV1.BOOLEAN_FALSE, falseBytes[0] & BaseV1.BOOLEAN_MASK);
        Assert.assertFalse(BaseV1.deserializeBoolean(falseBytes[0]));
    }

    @Test
    public void testByteSerialization()
    {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++)
        {
            byte b = (byte) i;
            String message = Integer.toString(i);
            byte[] bytes = serialize(b, BaseV1::serializeByte);
            Assert.assertEquals(message, 2, bytes.length);
            Reader reader = BinaryReaders.newBinaryReader(bytes);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.BYTE, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(message, b, BaseV1.deserializeByte(reader));
        }
    }

    @Test
    public void testDecimalSerialization()
    {
        for (String decimalString : new String[]{"-9876543210123456789098765432101234567890.9876543210123456789098765432101234567890",
                BigDecimal.valueOf(Double.MIN_VALUE).toPlainString(), Long.MIN_VALUE + ".0", Integer.MIN_VALUE + ".0", "-3.14159265358979323846264338327950288419716939937510", "-2", "-1.0", "-1.00000",
                "0.0", "0.00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
                "0.000000", "1.0", "2", "3.14159265358979323846264338327950288419716939937510", Integer.MAX_VALUE + ".0", Long.MAX_VALUE + ".0", BigDecimal.valueOf(Double.MAX_VALUE).toPlainString() + ".0",
                "9876543210123456789098765432101234567890.9876543210123456789098765432101234567890"})
        {
            byte[] serialized = serialize(this.modelRepository.newDecimalCoreInstance(decimalString), BaseV1::serializeDecimal);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.NUMBER, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(BaseV1.DECIMAL_TYPE, code & BaseV1.NUMBER_TYPE_MASK);
            Assert.assertEquals(decimalString, BaseV1.deserializeDecimal(reader, code));
        }
    }

    @Test
    public void testFloatSerialization()
    {
        for (String floatString : new String[]{BigDecimal.valueOf(Double.MIN_VALUE).toPlainString(), Long.MIN_VALUE + ".0", Integer.MIN_VALUE + ".0", "-3.14159265358979", "-1.0", "0.0",
                "1.0", "3.14159265358979", Integer.MAX_VALUE + ".0", Long.MAX_VALUE + ".0", BigDecimal.valueOf(Double.MAX_VALUE).toPlainString() + ".0"})
        {
            byte[] serialized = serialize(this.modelRepository.newFloatCoreInstance(floatString), BaseV1::serializeFloat);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.NUMBER, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(BaseV1.FLOAT_TYPE, code & BaseV1.NUMBER_TYPE_MASK);
            Assert.assertEquals(floatString, BaseV1.deserializeFloat(reader, code));
        }
    }

    @Test
    public void testIntegerSerialization()
    {
        for (int i : new int[]{Integer.MIN_VALUE, -1_000_000, -256, -1, 0, 1, 357, 1_000_000, Integer.MAX_VALUE})
        {
            byte[] serialized = serialize(this.modelRepository.newIntegerCoreInstance(i), BaseV1::serializeInteger);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.NUMBER, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(BaseV1.INTEGER_TYPE, code & BaseV1.NUMBER_TYPE_MASK);
            Assert.assertEquals(i, BaseV1.deserializeInteger(reader, code));
        }
        for (long l : new long[]{Long.MIN_VALUE, Integer.MIN_VALUE * 17L, Integer.MAX_VALUE * 17L, Long.MAX_VALUE})
        {
            byte[] serialized = serialize(this.modelRepository.newIntegerCoreInstance(l), BaseV1::serializeInteger);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.NUMBER, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(BaseV1.INTEGER_TYPE, code & BaseV1.NUMBER_TYPE_MASK);
            Assert.assertEquals(l, BaseV1.deserializeInteger(reader, code));
        }
        for (String integerString : new String[]{"-123456789098765432101234567890987654321", "9876543210123456789098765432101234567890"})
        {
            byte[] serialized = serialize(this.modelRepository.newIntegerCoreInstance(integerString), BaseV1::serializeInteger);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.NUMBER, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(BaseV1.BIG_INTEGER_TYPE, code & BaseV1.NUMBER_TYPE_MASK);
            Assert.assertEquals(new BigInteger(integerString), BaseV1.deserializeInteger(reader, code));
        }
    }

    @Test
    public void testDateSerialization()
    {
        for (PureDate date : new PureDate[]{DateFunctions.newPureDate(2024), DateFunctions.newPureDate(1835), DateFunctions.newPureDate(2024, 12), DateFunctions.newPureDate(1220, 4)})
        {
            String message = date.toString();
            byte[] serialized = serialize(this.modelRepository.newDateCoreInstance(date), BaseV1::serializeDate);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(message, BaseV1.DATE, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(message, BaseV1.DATE_TYPE, code & BaseV1.DATE_TYPE_MASK);
            Assert.assertEquals(message, date.hasMonth() ? BaseV1.MONTH_DATE_WIDTH : BaseV1.YEAR_DATE_WIDTH, code & BaseV1.DATE_WIDTH_MASK);
            Assert.assertEquals(message, date, BaseV1.deserializeDate(reader, code));
        }
    }

    @Test
    public void testDateTimeSerialization()
    {
        for (PureDate date : new PureDate[]{DateFunctions.newPureDate(2024, 12, 1, 17, 2, 35, "123456789"), DateFunctions.newPureDate(1900, 1, 1, 0), DateFunctions.newPureDate(1835, 4, 7, 0, 1), DateFunctions.newPureDate(2011, 1, 10, 11, 58, 0, "000000000"), DateFunctions.newPureDate(1220, 8, 28, 23, 59, 59, "999999999999999999999999999999999999999999999999999999999")})
        {
            String message = date.toString();
            byte[] serialized = serialize(this.modelRepository.newDateCoreInstance(date), BaseV1::serializeDateTime);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(message, BaseV1.DATE, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(message, BaseV1.DATE_TIME_TYPE, code & BaseV1.DATE_TYPE_MASK);
            Assert.assertEquals(message, date, BaseV1.deserializeDate(reader, code));
        }
    }

    @Test
    public void testStrictDateSerialization()
    {
        for (PureDate date : new PureDate[]{DateFunctions.newPureDate(2024, 12, 1), DateFunctions.newPureDate(1835, 4, 7), DateFunctions.newPureDate(2011, 1, 10), DateFunctions.newPureDate(1220, 8, 28)})
        {
            String message = date.toString();
            byte[] serialized = serialize(this.modelRepository.newDateCoreInstance(date), BaseV1::serializeStrictDate);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(message, BaseV1.DATE, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(message, BaseV1.STRICT_DATE_TYPE, code & BaseV1.DATE_TYPE_MASK);
            Assert.assertEquals(message, BaseV1.DAY_DATE_WIDTH, code & BaseV1.DATE_WIDTH_MASK);
            Assert.assertEquals(message, date, BaseV1.deserializeDate(reader, code));
        }
    }

    @Test
    public void testLatestDateSerialization()
    {
        byte[] serialized = serialize(BaseV1::serializeLatestDate);
        Assert.assertEquals(1, serialized.length);
        int code = serialized[0];
        Assert.assertEquals(BaseV1.DATE, code & BaseV1.NODE_TYPE_MASK);
        Assert.assertEquals(BaseV1.LATEST_DATE_TYPE, code & BaseV1.DATE_TYPE_MASK);
        Assert.assertNull(BaseV1.deserializeDate(BinaryReaders.newBinaryReader(new byte[0]), code));
    }

    @Test
    public void testStrictTimeSerialization()
    {
        for (int h = 0; h < 24; h++)
        {
            for (int m = 0; m < 60; m++)
            {
                for (int s = 0; s < 60; s++)
                {
                    for (String subsecond : new String[]{"0", "000", "000000", "000000000", "123456789", "1", "2", "3", "9876543210123456789"})
                    {
                        PureStrictTime time = StrictTimeFunctions.newPureStrictTime(h, m, s, subsecond);
                        byte[] serialized = serialize(this.modelRepository.newStrictTimeCoreInstance(time), BaseV1::serializeStrictTime);
                        Reader reader = BinaryReaders.newBinaryReader(serialized);
                        int code = reader.readByte();
                        Assert.assertEquals(BaseV1.STRICT_TIME, code & BaseV1.NODE_TYPE_MASK);
                        Assert.assertEquals(BaseV1.SUBSECOND_DATE_WIDTH, code & BaseV1.DATE_WIDTH_MASK);
                        Assert.assertEquals(time, BaseV1.deserializeStrictTime(reader, code));
                    }
                    PureStrictTime time = StrictTimeFunctions.newPureStrictTime(h, m, s);
                    byte[] serialized = serialize(this.modelRepository.newStrictTimeCoreInstance(time), BaseV1::serializeStrictTime);
                    Reader reader = BinaryReaders.newBinaryReader(serialized);
                    int code = reader.readByte();
                    Assert.assertEquals(BaseV1.STRICT_TIME, code & BaseV1.NODE_TYPE_MASK);
                    Assert.assertEquals(BaseV1.SECOND_DATE_WIDTH, code & BaseV1.DATE_WIDTH_MASK);
                    Assert.assertEquals(time, BaseV1.deserializeStrictTime(reader, code));
                }
                PureStrictTime time = StrictTimeFunctions.newPureStrictTime(h, m);
                byte[] serialized = serialize(this.modelRepository.newStrictTimeCoreInstance(time), BaseV1::serializeStrictTime);
                Reader reader = BinaryReaders.newBinaryReader(serialized);
                int code = reader.readByte();
                Assert.assertEquals(BaseV1.STRICT_TIME, code & BaseV1.NODE_TYPE_MASK);
                Assert.assertEquals(BaseV1.MINUTE_DATE_WIDTH, code & BaseV1.DATE_WIDTH_MASK);
                Assert.assertEquals(time, BaseV1.deserializeStrictTime(reader, code));
            }
        }
    }

    @Test
    public void testStringSerialization()
    {
        for (String string : new String[]{"The Quick Brown Fox Jumped Over The Lazy Dog", "", "a", "AbCdEfGhIjKlMnOpQrStUvWxYz0987654321!@#$%^&*()"})
        {
            byte[] serialized = serialize(this.modelRepository.newStringCoreInstance(string), BaseV1::serializeString);
            Reader reader = BinaryReaders.newBinaryReader(serialized);
            int code = reader.readByte();
            Assert.assertEquals(BaseV1.STRING, code & BaseV1.NODE_TYPE_MASK);
            Assert.assertEquals(string, BaseV1.deserializeString(reader));
        }
    }

    private <T> byte[] serialize(T value, BiConsumer<Writer, T> serializer)
    {
        return serialize(w -> serializer.accept(w, value));
    }

    private byte[] serialize(Consumer<Writer> serializer)
    {
        this.stream.reset();
        serializer.accept(BinaryWriters.newBinaryWriter(this.stream));
        return this.stream.toByteArray();
    }
}
