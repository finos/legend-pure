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

package org.finos.legend.pure.m3.serialization.runtime.binary.reference;

import java.math.BigInteger;

public interface ReferenceFactory
{
    Reference booleanReference(boolean value);

    Reference booleanReference(String name);

    Reference integerReference(int value);

    Reference integerReference(long value);

    Reference integerReference(BigInteger value);

    Reference integerReference(String name);

    Reference floatReference(String name);

    Reference decimalReference(String name);

    Reference dateReference(String name);

    Reference dateTimeReference(String name);

    Reference strictDateReference(String name);

    Reference latestDateReference();

    Reference stringReference(String name);

    Reference packageReference(String path);

    Reference packagedElementReference(String path);
}
