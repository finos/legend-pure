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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;

class BinaryGraphSerializationTypes
{
    static final byte OBJ_REF = 1;
    static final byte ENUM_REF = 2;
    static final byte PRIMITIVE_BOOLEAN = 3;
    static final byte PRIMITIVE_DOUBLE = 4;
    static final byte PRIMITIVE_LONG = 5;
    static final byte PRIMITIVE_STRING = 6;
    static final byte PRIMITIVE_DATE = 7;
    static final byte PRIMITIVE_DECIMAL = 8;

    private static final int IS_ENUM = 0b1;
    private static final int HAS_NAME = 0b10;
    private static final int HAS_SOURCE_INFO = 0b100;

    static byte getObjSerializationCode(Obj obj)
    {
        int code = 0;
        if (obj.isEnum())
        {
            code |= IS_ENUM;
        }
        if (obj.getName() != null)
        {
            code |= HAS_NAME;
        }
        if (obj.getSourceInformation() != null)
        {
            code |= HAS_SOURCE_INFO;
        }
        return (byte) code;
    }

    static boolean isEnum(byte code)
    {
        return hasFlag(code, IS_ENUM);
    }

    static boolean hasName(byte code)
    {
        return hasFlag(code, HAS_NAME);
    }

    static boolean hasSourceInfo(byte code)
    {
        return hasFlag(code, HAS_SOURCE_INFO);
    }

    private static boolean hasFlag(byte code, int flag)
    {
        return (code & flag) == flag;
    }
}
