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

public class UnexpectedEndException extends RuntimeException
{
    private final long expectedByteCount;
    private final long actualByteCount;

    UnexpectedEndException(long expected, long actual)
    {
        super(createMessage(expected, actual));
        this.expectedByteCount = expected;
        this.actualByteCount = actual;
    }

    UnexpectedEndException(long expectedByteCount)
    {
        this(expectedByteCount, -1);
    }

    public long getExpectedByteCount()
    {
        return this.expectedByteCount;
    }

    public long getActualByteCount()
    {
        return this.actualByteCount;
    }

    private static String createMessage(long expected, long actual)
    {
        StringBuilder message = new StringBuilder("Unexpected end: expected ");
        message.append(expected);
        message.append(" bytes");
        if (actual >= 0)
        {
            message.append(", found ");
            message.append(actual);
        }
        return message.toString();
    }
}
