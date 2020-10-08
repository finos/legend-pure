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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class AbstractBinaryReaderWriter
{
    protected static final byte TRUE_BYTE = 1;
    protected static final byte FALSE_BYTE = 0;

    protected static final Charset STRING_CODING_CHARSET = StandardCharsets.UTF_8;

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
}
