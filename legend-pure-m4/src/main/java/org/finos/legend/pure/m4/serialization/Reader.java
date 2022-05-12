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

package org.finos.legend.pure.m4.serialization;

import java.io.Closeable;
import java.util.Objects;

/**
 * <p>Reader for reading data from a binary source.</p>
 *
 * <p>If there are not enough bytes in the underlying source to perform the
 * requested read operation, an {@link org.finos.legend.pure.m4.serialization.binary.UnexpectedEndException UnexpectedEndException}
 * is thrown. In this case, the state of the Reader is uncertain, as some bytes may or may not have been read from the
 * underlying source during the operation. So it is not advisable to continue use of the Reader after such an exception.</p>
 *
 * <p>Any {@link java.io.IOException IOException} thrown by an underlying data source should be wrapped in
 * {@link java.io.UncheckedIOException UncheckedIOException} by implementing classes.</p>
 */
public interface Reader extends Closeable
{
    /**
     * Read a single byte of data.
     *
     * @return the byte read
     */
    byte readByte();

    /**
     * Read n bytes.
     *
     * @param n number of bytes to read
     * @return array of the n bytes read
     */
    default byte[] readBytes(int n)
    {
        return readBytes(new byte[n], 0, n);
    }

    /**
     * Read bytes into the given byte array to fill the array, and return the array. If there are not enough bytes to
     * fill the array, an {@link org.finos.legend.pure.m4.serialization.binary.UnexpectedEndException UnexpectedEndException}
     * will be thrown. In this case, some bytes may or may not be written to the given array.
     *
     * @param bytes byte array to write bytes into
     * @return given byte array
     */
    default byte[] readBytes(byte[] bytes)
    {
        return readBytes(Objects.requireNonNull(bytes, "byte array may not be null"), 0, bytes.length);
    }

    /**
     * Read n bytes into the given byte array at the given offset. Returns the given byte array. Throws an
     * {@link org.finos.legend.pure.m4.serialization.binary.UnexpectedEndException UnexpectedEndException} if there are
     * fewer than n bytes left to read. In this case, some bytes may or may not be written to the given array.
     *
     * @param bytes  byte array to write bytes into
     * @param offset offset at which to start writing bytes
     * @param n      number of bytes to read
     * @return given byte array
     */
    byte[] readBytes(byte[] bytes, int offset, int n);

    /**
     * Skip n bytes.
     *
     * @param n number of bytes to skip
     */
    void skipBytes(long n);

    /**
     * Read an array of bytes.
     *
     * @return array of bytes
     */
    byte[] readByteArray();

    /**
     * Skip an array of bytes.
     */
    void skipByteArray();

    /**
     * Read a boolean value.
     *
     * @return boolean value
     */
    boolean readBoolean();

    /**
     * Skip a boolean value
     */
    void skipBoolean();

    /**
     * Read a short value.
     *
     * @return short value
     */
    short readShort();

    /**
     * Skip a short value.
     */
    void skipShort();

    /**
     * Read an array of short values.
     *
     * @return array of short values
     */
    short[] readShortArray();

    /**
     * Skip an array of short values.
     */
    void skipShortArray();

    /**
     * Read an int value.
     *
     * @return int value
     */
    int readInt();

    /**
     * Skip an int value.
     */
    void skipInt();

    /**
     * Read an array of int values.
     *
     * @return array of int values
     */
    int[] readIntArray();

    /**
     * Skip an array of int values.
     */
    void skipIntArray();

    /**
     * Read a long value.
     *
     * @return long value
     */
    long readLong();

    /**
     * Skip a long value
     */
    void skipLong();

    /**
     * Read an array of long values.
     *
     * @return array of long values
     */
    long[] readLongArray();

    /**
     * Skip an array of long values.
     */
    void skipLongArray();

    /**
     * Read a float value.
     *
     * @return float value
     */
    float readFloat();

    /**
     * Skip a float value.
     */
    void skipFloat();

    /**
     * Read an array of float values.
     *
     * @return array of float values
     */
    float[] readFloatArray();

    /**
     * Skip an array of float values.
     */
    void skipFloatArray();

    /**
     * Read a double value.
     *
     * @return double value
     */
    double readDouble();

    /**
     * Skip a double value.
     */
    void skipDouble();

    /**
     * Read an array of double values.
     *
     * @return array of double values
     */
    double[] readDoubleArray();

    /**
     * Skip an array of double values.
     */
    void skipDoubleArray();

    /**
     * Read a string.
     *
     * @return string
     */
    String readString();

    /**
     * Skip a string.
     */
    void skipString();

    /**
     * Read an array of strings.
     *
     * @return array of strings
     */
    String[] readStringArray();

    /**
     * Skip an array of strings.
     */
    void skipStringArray();

    /**
     * Close the reader. This will also close any underlying source the reader is reading from.
     */
    @Override
    void close();
}
