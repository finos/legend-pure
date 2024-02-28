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

package org.finos.legend.pure.m4.serialization.binary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class AbstractBinaryReaderWriter
{
    protected static final byte TRUE_BYTE = 1;
    protected static final byte FALSE_BYTE = 0;

    protected static final Charset STRING_CODING_CHARSET = StandardCharsets.UTF_8;

    protected static byte booleanToByte(boolean b)
    {
        return b ? TRUE_BYTE : FALSE_BYTE;
    }

    protected static boolean byteToBoolean(byte b)
    {
        switch (b)
        {
            case TRUE_BYTE:
            {
                return true;
            }
            case FALSE_BYTE:
            {
                return false;
            }
            default:
            {
                throw new UncheckedIOException(new IOException("Expected " + TRUE_BYTE + " or " + FALSE_BYTE + "; found " + b));
            }
        }
    }

    protected static byte[] stringToByteArray(String string)
    {
        return string.getBytes(STRING_CODING_CHARSET);
    }

    protected static String byteArrayToString(byte[] bytes)
    {
        return byteArrayToString(bytes, 0, bytes.length);
    }

    protected static String byteArrayToString(byte[] bytes, int offset, int length)
    {
        return new String(bytes, offset, length, STRING_CODING_CHARSET);
    }

    protected static void checkByteArray(byte[] bytes, int offset, int length)
    {
        Objects.requireNonNull(bytes, "byte array may not be null");
        if ((offset < 0) || (length < 0) || (length > (bytes.length - offset)))
        {
            throw new IndexOutOfBoundsException("Range [" + offset + ", " + offset + " + " + length + ") out of bounds for length " + bytes.length);
        }
    }
}
