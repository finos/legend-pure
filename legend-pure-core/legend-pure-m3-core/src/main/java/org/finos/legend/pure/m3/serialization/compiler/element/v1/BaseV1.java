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

import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.finos.legend.pure.m4.serialization.Reader;
import org.finos.legend.pure.m4.serialization.Writer;

import java.math.BigInteger;
import java.util.Arrays;

class BaseV1
{
    static final int VALUE_PRESENT_MASK = 0b1000_0000;
    static final int VALUE_PRESENT = 0b1000_0000;
    static final int VALUE_NOT_PRESENT = 0b0000_0000;

    static final int INTEGRAL_WIDTH_MASK = 0b0000_0011;
    static final int BYTE_WIDTH = 0b0000_0000;
    static final int SHORT_WIDTH = 0b0000_0001;
    static final int INT_WIDTH = 0b0000_0010;
    static final int LONG_WIDTH = 0b0000_0011;

    static final int NEGATIVE_MASK = 0b0000_0001;
    static final int NON_NEGATIVE = 0b0000_0000;
    static final int NEGATIVE = 0b0000_0001;

    static final int NODE_TYPE_MASK = 0b1110_0000;
    static final int INTERNAL_REFERENCE = 0b0000_0000;
    static final int EXTERNAL_REFERENCE = 0b1000_0000;
    static final int BOOLEAN = 0b0100_0000;
    static final int BYTE = 0b0010_0000;
    static final int DATE = 0b1100_0000;
    static final int NUMBER = 0b1010_0000;
    static final int STRICT_TIME = 0b0110_0000;
    static final int STRING = 0b1110_0000;

    static final int BOOLEAN_MASK = 0b0000_0001;
    static final int BOOLEAN_FALSE = 0b0000_0000;
    static final int BOOLEAN_TRUE = 0b0000_0001;

    static final int NUMBER_TYPE_MASK = 0b0001_1000;
    static final int INTEGER_TYPE = 0b0000_0000;
    static final int BIG_INTEGER_TYPE = 0b0000_1000;
    static final int FLOAT_TYPE = 0b0001_0000;
    static final int DECIMAL_TYPE = 0b0001_1000;

    static final int DECIMAL_POINT_MASK = 0b0000_0010;
    static final int HAS_DECIMAL_POINT = 0b0000_0010;
    static final int HAS_NO_DECIMAL_POINT = 0b0000_0000;

    static final int DATE_TYPE_MASK = 0b0001_1000;
    static final int DATE_TYPE = 0b0000_0000;
    static final int STRICT_DATE_TYPE = 0b0000_1000;
    static final int DATE_TIME_TYPE = 0b0001_0000;
    static final int LATEST_DATE_TYPE = 0b0001_1000;

    static final int DATE_WIDTH_MASK = 0b0000_0111;
    static final int YEAR_DATE_WIDTH = 0b0000_0000;
    static final int MONTH_DATE_WIDTH = 0b0000_0001;
    static final int DAY_DATE_WIDTH = 0b0000_0010;
    static final int HOUR_DATE_WIDTH = 0b0000_0100;
    static final int MINUTE_DATE_WIDTH = 0b0000_0011;
    static final int SECOND_DATE_WIDTH = 0b0000_0110;
    static final int SUBSECOND_DATE_WIDTH = 0b0000_0101;

    static final int DIGIT_STRING_MASK = 0b1100_0000;
    static final int EMPTY_DIGIT_STRING = 0b1000_0000;
    static final int SINGLE_DIGIT_STRING = 0b0100_0000;
    static final int REPEATED_DIGIT_STRING = 0b1100_0000;

    // Value present

    static boolean isValuePresent(int code)
    {
        switch (code & VALUE_PRESENT_MASK)
        {
            case VALUE_PRESENT:
            {
                return true;
            }
            case VALUE_NOT_PRESENT:
            {
                return false;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown value present code: %02x", code & VALUE_PRESENT_MASK));
            }
        }
    }

    // Boolean

    static void serializeBoolean(Writer writer, CoreInstance booleanInstance)
    {
        serializeBoolean(writer, PrimitiveUtilities.getBooleanValue(booleanInstance));
    }

    static void serializeBoolean(Writer writer, boolean value)
    {
        writer.writeByte((byte) (BOOLEAN | (value ? BOOLEAN_TRUE : BOOLEAN_FALSE)));
    }

    static boolean deserializeBoolean(int code)
    {
        if ((code & NODE_TYPE_MASK) != BOOLEAN)
        {
            throw new RuntimeException(String.format("Not a Boolean: %02x", code & NODE_TYPE_MASK));
        }
        switch (code & BOOLEAN_MASK)
        {
            case BOOLEAN_FALSE:
            {
                return false;
            }
            case BOOLEAN_TRUE:
            {
                return true;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown Boolean value code: %02x", code & BOOLEAN_MASK));
            }
        }
    }

    // Byte

    static void serializeByte(Writer writer, CoreInstance byteInstance)
    {
        serializeByte(writer, PrimitiveUtilities.getByteValue(byteInstance));
    }

    static void serializeByte(Writer writer, byte value)
    {
        writer.writeByte((byte) BYTE);
        writer.writeByte(value);
    }

    static byte deserializeByte(Reader reader)
    {
        return reader.readByte();
    }

    // Decimal/Float

    static void serializeDecimal(Writer writer, CoreInstance decimalInstance)
    {
        serializeFloatOrDecimal(writer, DECIMAL_TYPE, decimalInstance.getName());
    }

    static void serializeFloat(Writer writer, CoreInstance floatInstance)
    {
        serializeFloatOrDecimal(writer, FLOAT_TYPE, floatInstance.getName());
    }

    private static void serializeFloatOrDecimal(Writer writer, int type, String name)
    {
        boolean negative = name.charAt(0) == '-';
        int decimalIndex = name.indexOf('.');
        boolean hasDecimal = decimalIndex != -1;
        writer.writeByte((byte) (NUMBER | type | getHasDecimalPoint(hasDecimal) | getNegative(negative)));
        if (hasDecimal)
        {
            writeDigitString(writer, name.substring(negative ? 1 : 0, decimalIndex));
            writeDigitString(writer, name.substring(decimalIndex + 1));
        }
        else
        {
            writeDigitString(writer, name.substring(negative ? 1 : 0));
        }
    }

    static String deserializeDecimal(Reader reader, int code)
    {
        return deserializeFloatOrDecimal(reader, code);
    }

    static String deserializeFloat(Reader reader, int code)
    {
        return deserializeFloatOrDecimal(reader, code);
    }

    private static String deserializeFloatOrDecimal(Reader reader, int code)
    {
        StringBuilder builder = new StringBuilder();
        if (isNegative(code))
        {
            builder.append('-');
        }
        builder.append(readDigitString(reader));
        if (hasDecimalPoint(code))
        {
            builder.append('.').append(readDigitString(reader));
        }
        return builder.toString();
    }

    // Integer

    static void serializeInteger(Writer writer, CoreInstance integerInstance)
    {
        Number value = PrimitiveUtilities.getIntegerValue(integerInstance);
        if (value instanceof BigInteger)
        {
            boolean negative = ((BigInteger) value).signum() < 0;
            writer.writeByte((byte) (NUMBER | BIG_INTEGER_TYPE | getNegative(negative)));
            String name = integerInstance.getName();
            writeDigitString(writer, negative ? name.substring(1) : name);
        }
        else
        {
            long l = value.longValue();
            int width = getLongWidth(l);
            writer.writeByte((byte) (NUMBER | INTEGER_TYPE | width));
            writeLongOfWidth(writer, l, width);
        }
    }

    static Number deserializeInteger(Reader reader, int code)
    {
        switch (code & NUMBER_TYPE_MASK)
        {
            case INTEGER_TYPE:
            {
                if ((code & INTEGRAL_WIDTH_MASK) == LONG_WIDTH)
                {
                    return deserializeOrdinaryIntegerAsLong(reader, code);
                }
                return deserializeOrdinaryIntegerAsInt(reader, code);
            }
            case BIG_INTEGER_TYPE:
            {
                return deserializeBigInteger(reader, code);
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown Integer type code: %02x", code & BOOLEAN_MASK));
            }
        }
    }

    static long deserializeOrdinaryIntegerAsLong(Reader reader, int code)
    {
        return readLongOfWidth(reader, code);
    }

    static int deserializeOrdinaryIntegerAsInt(Reader reader, int code)
    {
        return readIntOfWidth(reader, code);
    }

    static BigInteger deserializeBigInteger(Reader reader, int code)
    {
        boolean negative = isNegative(code);
        String string = readDigitString(reader);
        return new BigInteger(negative ? ('-' + string) : string);
    }

    static int getIntWidth(int... ints)
    {
        int type = BYTE_WIDTH;
        for (int i : ints)
        {
            switch (getIntWidth(i))
            {
                case INT_WIDTH:
                {
                    return INT_WIDTH;
                }
                case SHORT_WIDTH:
                {
                    type = SHORT_WIDTH;
                }
            }
        }
        return type;
    }

    static int getLongWidth(long... longs)
    {
        int type = BYTE_WIDTH;
        for (long l : longs)
        {
            switch (getLongWidth(l))
            {
                case LONG_WIDTH:
                {
                    return LONG_WIDTH;
                }
                case INT_WIDTH:
                {
                    type = INT_WIDTH;
                    break;
                }
                case SHORT_WIDTH:
                {
                    if (type == BYTE_WIDTH)
                    {
                        type = SHORT_WIDTH;
                    }
                }
            }
        }
        return type;
    }

    static int getIntWidth(int i)
    {
        return (i < 0) ?
               (i >= Byte.MIN_VALUE) ? BYTE_WIDTH : ((i >= Short.MIN_VALUE) ? SHORT_WIDTH : INT_WIDTH) :
               (i <= Byte.MAX_VALUE) ? BYTE_WIDTH : ((i <= Short.MAX_VALUE) ? SHORT_WIDTH : INT_WIDTH);
    }

    static int getLongWidth(long l)
    {
        return (l < 0) ?
               (l >= Byte.MIN_VALUE) ? BYTE_WIDTH : ((l >= Short.MIN_VALUE) ? SHORT_WIDTH : ((l >= Integer.MIN_VALUE) ? INT_WIDTH : LONG_WIDTH)) :
               (l <= Byte.MAX_VALUE) ? BYTE_WIDTH : ((l <= Short.MAX_VALUE) ? SHORT_WIDTH : ((l <= Integer.MAX_VALUE) ? INT_WIDTH : LONG_WIDTH));

    }

    static void writeIntOfWidth(Writer writer, int i, int widthCode)
    {
        switch (widthCode & INTEGRAL_WIDTH_MASK)
        {
            case BYTE_WIDTH:
            {
                writer.writeByte((byte) i);
                return;
            }
            case SHORT_WIDTH:
            {
                writer.writeShort((short) i);
                return;
            }
            case INT_WIDTH:
            {
                writer.writeInt(i);
                return;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown long width code: %02x", widthCode & INTEGRAL_WIDTH_MASK));
            }
        }
    }

    static void writeLongOfWidth(Writer writer, long l, int intType)
    {
        switch (intType & INTEGRAL_WIDTH_MASK)
        {
            case BYTE_WIDTH:
            {
                writer.writeByte((byte) l);
                return;
            }
            case SHORT_WIDTH:
            {
                writer.writeShort((short) l);
                return;
            }
            case INT_WIDTH:
            {
                writer.writeInt((int) l);
                return;
            }
            case LONG_WIDTH:
            {
                writer.writeLong(l);
                return;
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown long width code: %02x", intType & INTEGRAL_WIDTH_MASK));
            }
        }
    }

    static int readIntOfWidth(Reader reader, int widthCode)
    {
        switch (widthCode & INTEGRAL_WIDTH_MASK)
        {
            case BYTE_WIDTH:
            {
                return reader.readByte();
            }
            case SHORT_WIDTH:
            {
                return reader.readShort();
            }
            case INT_WIDTH:
            {
                return reader.readInt();
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown int width code: %02x", widthCode & INTEGRAL_WIDTH_MASK));
            }
        }
    }

    static long readLongOfWidth(Reader reader, int widthCode)
    {
        switch (widthCode & INTEGRAL_WIDTH_MASK)
        {
            case BYTE_WIDTH:
            {
                return reader.readByte();
            }
            case SHORT_WIDTH:
            {
                return reader.readShort();
            }
            case INT_WIDTH:
            {
                return reader.readInt();
            }
            case LONG_WIDTH:
            {
                return reader.readLong();
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown long width code: %02x", widthCode & INTEGRAL_WIDTH_MASK));
            }
        }
    }

    static int getNegative(boolean negative)
    {
        return negative ? NEGATIVE : NON_NEGATIVE;
    }

    static boolean isNegative(int parityCode)
    {
        return (parityCode & NEGATIVE_MASK) == NEGATIVE;
    }


    static int getHasDecimalPoint(boolean hasDecimalPoint)
    {
        return hasDecimalPoint ? HAS_DECIMAL_POINT : HAS_NO_DECIMAL_POINT;
    }

    static boolean hasDecimalPoint(int code)
    {
        return (code & DECIMAL_POINT_MASK) == HAS_DECIMAL_POINT;
    }


    private static void writeDigitString(Writer writer, String string)
    {
        int len = string.length();
        if (len == 0)
        {
            writer.writeByte((byte) EMPTY_DIGIT_STRING);
            return;
        }
        if (len == 1)
        {
            writer.writeByte((byte) SINGLE_DIGIT_STRING);
            writer.writeByte((byte) string.charAt(0));
            return;
        }

        int lenWidth = getIntWidth(len);
        char firstChar = string.charAt(0);
        if (string.chars().allMatch(c -> c == firstChar))
        {
            writer.writeByte((byte) (REPEATED_DIGIT_STRING | lenWidth));
            writeIntOfWidth(writer, len, lenWidth);
            writer.writeByte((byte) firstChar);
            return;
        }

        writer.writeByte((byte) lenWidth);
        writeIntOfWidth(writer, len, lenWidth);
        int index = 0;
        while (index < len)
        {
            int i = string.charAt(index++) - '0';
            if (index < len)
            {
                i = (10 * i) + (string.charAt(index++) - '0');
            }
            writer.writeByte((byte) i);
        }
    }

    private static String readDigitString(Reader reader)
    {
        int code = reader.readByte();
        switch (code & DIGIT_STRING_MASK)
        {
            case EMPTY_DIGIT_STRING:
            {
                return "";
            }
            case SINGLE_DIGIT_STRING:
            {
                return String.valueOf((char) reader.readByte());
            }
            case REPEATED_DIGIT_STRING:
            {
                int len = readIntOfWidth(reader, code);
                char c = (char) reader.readByte();
                char[] chars = new char[len];
                Arrays.fill(chars, c);
                return String.valueOf(chars);
            }
            default:
            {
                int len = readIntOfWidth(reader, code);
                StringBuilder builder = new StringBuilder(len);
                int remaining = len;
                while (remaining > 1)
                {
                    byte b = reader.readByte();
                    char c1;
                    char c2;
                    if (b < 10)
                    {
                        c1 = '0';
                        c2 = (char) ('0' + b);
                    }
                    else
                    {
                        c1 = (char) ('0' + (b / 10));
                        c2 = (char) ('0' + (b % 10));
                    }
                    builder.append(c1).append(c2);
                    remaining -= 2;
                }
                if (remaining == 1)
                {
                    byte b = reader.readByte();
                    builder.append((char) ('0' + b));
                }
                return builder.toString();
            }
        }
    }

    // Date/Time

    static void serializeDate(Writer writer, CoreInstance instance)
    {
        serializePureDate(writer, instance, DATE_TYPE);
    }

    static void serializeDateTime(Writer writer, CoreInstance instance)
    {
        serializePureDate(writer, instance, DATE_TIME_TYPE);
    }

    static void serializeStrictDate(Writer writer, CoreInstance instance)
    {
        serializePureDate(writer, instance, STRICT_DATE_TYPE);
    }

    static void serializeLatestDate(Writer writer, CoreInstance instance)
    {
        serializeLatestDate(writer);
    }

    static void serializeLatestDate(Writer writer)
    {
        writer.writeByte((byte) (DATE | LATEST_DATE_TYPE));
    }

    static void serializeStrictTime(Writer writer, CoreInstance instance)
    {
        PureStrictTime value = PrimitiveUtilities.getStrictTimeValue(instance);
        writer.writeByte((byte) (STRICT_TIME | getStrictTimeWidth(value)));
        writer.writeByte((byte) value.getHour());
        writer.writeByte((byte) value.getMinute());
        if (value.hasSecond())
        {
            writer.writeByte((byte) value.getSecond());
            if (value.hasSubsecond())
            {
                writeDigitString(writer, value.getSubsecond());
            }
        }
    }

    static PureDate deserializeDate(Reader reader, int code)
    {
        switch (code & DATE_TYPE_MASK)
        {
            case LATEST_DATE_TYPE:
            {
                return null;
            }
            case DATE_TYPE:
            case DATE_TIME_TYPE:
            case STRICT_DATE_TYPE:
            {
                int year = reader.readInt();
                switch (code & DATE_WIDTH_MASK)
                {
                    case YEAR_DATE_WIDTH:
                    {
                        return DateFunctions.newPureDate(year);
                    }
                    case MONTH_DATE_WIDTH:
                    {
                        int month = reader.readByte();
                        return DateFunctions.newPureDate(year, month);
                    }
                    case DAY_DATE_WIDTH:
                    {
                        int month = reader.readByte();
                        int day = reader.readByte();
                        return DateFunctions.newPureDate(year, month, day);
                    }
                    case HOUR_DATE_WIDTH:
                    {
                        int month = reader.readByte();
                        int day = reader.readByte();
                        int hour = reader.readByte();
                        return DateFunctions.newPureDate(year, month, day, hour);
                    }
                    case MINUTE_DATE_WIDTH:
                    {
                        int month = reader.readByte();
                        int day = reader.readByte();
                        int hour = reader.readByte();
                        int minute = reader.readByte();
                        return DateFunctions.newPureDate(year, month, day, hour, minute);
                    }
                    case SECOND_DATE_WIDTH:
                    {
                        int month = reader.readByte();
                        int day = reader.readByte();
                        int hour = reader.readByte();
                        int minute = reader.readByte();
                        int second = reader.readByte();
                        return DateFunctions.newPureDate(year, month, day, hour, minute, second);
                    }
                    case SUBSECOND_DATE_WIDTH:
                    {
                        int month = reader.readByte();
                        int day = reader.readByte();
                        int hour = reader.readByte();
                        int minute = reader.readByte();
                        int second = reader.readByte();
                        String subsecond = readDigitString(reader);
                        return DateFunctions.newPureDate(year, month, day, hour, minute, second, subsecond);
                    }
                    default:
                    {
                        throw new RuntimeException(String.format("Unknown Date width code: %02x", code & DATE_WIDTH_MASK));
                    }
                }
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown Date type code: %02x", code & DATE_TYPE_MASK));
            }
        }
    }

    static PureStrictTime deserializeStrictTime(Reader reader, int code)
    {
        switch (code & DATE_WIDTH_MASK)
        {
            case MINUTE_DATE_WIDTH:
            {
                int hour = reader.readByte();
                int minute = reader.readByte();
                return StrictTimeFunctions.newPureStrictTime(hour, minute);
            }
            case SECOND_DATE_WIDTH:
            {
                int hour = reader.readByte();
                int minute = reader.readByte();
                int second = reader.readByte();
                return StrictTimeFunctions.newPureStrictTime(hour, minute, second);
            }
            case SUBSECOND_DATE_WIDTH:
            {
                int hour = reader.readByte();
                int minute = reader.readByte();
                int second = reader.readByte();
                String subsecond = readDigitString(reader);
                return StrictTimeFunctions.newPureStrictTime(hour, minute, second, subsecond);
            }
            default:
            {
                throw new RuntimeException(String.format("Unknown StrictTime width code: %02x", code & DATE_WIDTH_MASK));
            }
        }
    }

    private static void serializePureDate(Writer writer, CoreInstance instance, int dateTypeCode)
    {
        serializePureDate(writer, PrimitiveUtilities.getDateValue(instance), dateTypeCode);
    }

    private static void serializePureDate(Writer writer, PureDate date, int dateTypeCode)
    {
        writer.writeByte((byte) (DATE | dateTypeCode | getDateWidth(date)));
        writer.writeInt(date.getYear());
        if (date.hasMonth())
        {
            writer.writeByte((byte) date.getMonth());
            if (date.hasDay())
            {
                writer.writeByte((byte) date.getDay());
                if (date.hasHour())
                {
                    writer.writeByte((byte) date.getHour());
                    if (date.hasMinute())
                    {
                        writer.writeByte((byte) date.getMinute());
                        if (date.hasSecond())
                        {
                            writer.writeByte((byte) date.getSecond());
                            if (date.hasSubsecond())
                            {
                                writeDigitString(writer, date.getSubsecond());
                            }
                        }
                    }
                }
            }
        }
    }

    private static int getDateWidth(PureDate date)
    {
        if (date.hasSubsecond())
        {
            return SUBSECOND_DATE_WIDTH;
        }
        if (date.hasSecond())
        {
            return SECOND_DATE_WIDTH;
        }
        if (date.hasMinute())
        {
            return MINUTE_DATE_WIDTH;
        }
        if (date.hasHour())
        {
            return HOUR_DATE_WIDTH;
        }
        if (date.hasDay())
        {
            return DAY_DATE_WIDTH;
        }
        if (date.hasMonth())
        {
            return MONTH_DATE_WIDTH;
        }
        return YEAR_DATE_WIDTH;
    }

    private static int getStrictTimeWidth(PureStrictTime time)
    {
        if (time.hasSubsecond())
        {
            return SUBSECOND_DATE_WIDTH;
        }
        if (time.hasSecond())
        {
            return SECOND_DATE_WIDTH;
        }
        return MINUTE_DATE_WIDTH;
    }

    // String

    static void serializeString(Writer writer, CoreInstance instance)
    {
        writer.writeByte((byte) STRING);
        writer.writeString(PrimitiveUtilities.getStringValue(instance));
    }

    static String deserializeString(Reader reader)
    {
        return reader.readString();
    }
}
