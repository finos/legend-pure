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

package org.finos.legend.pure.m3.serialization.runtime.binary;

class BinaryModelSerializationTypes
{
    // Primitives
    static final byte BOOLEAN = 0;
    static final byte DATE = 1;
    static final byte STRICT_DATE = 2;
    static final byte DATE_TIME = 3;
    static final byte LATEST_DATE = 17;
    static final byte FLOAT = 4;
    static final byte INTEGER_INT = 5;
    static final byte INTEGER_LONG = 6;
    static final byte INTEGER_BIG = 7;
    static final byte STRING = 8;
    static final byte DECIMAL = 18;

    // References
    static final byte PACKAGE_REFERENCE = 9;
    static final byte INTERNAL_REFERENCE = 10;
    static final byte EXTERNAL_PACKAGEABLE_ELEMENT_REFERENCE = 11;
    static final byte EXTERNAL_OTHER_REFERENCE = 12;

    // Instance types
    static final byte TOP_LEVEL_INSTANCE = 13;
    static final byte PACKAGED_INSTANCE = 14;
    static final byte ANONYMOUS_INSTANCE = 15;
    static final byte OTHER_INSTANCE = 16;
    static final byte ENUM_INSTANCE = 17;
}
