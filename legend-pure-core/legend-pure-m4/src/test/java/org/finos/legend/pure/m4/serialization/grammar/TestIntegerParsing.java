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

package org.finos.legend.pure.m4.serialization.grammar;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Test;

public class TestIntegerParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testPositiveInteger()
    {
        assertParsesTo("17", "17");
        assertParsesTo("17", "+17");
        assertParsesTo("17", "0000017");
    }

    @Test
    public void testNegativeInteger()
    {
        assertParsesTo("-17", "-17");
        assertParsesTo("-17", "-000000017");
    }

    @Test
    public void testZero()
    {
        assertParsesTo("0", "0");
        assertParsesTo("0", "-0");
        assertParsesTo("0", "000000000");
        assertParsesTo("0", "-000000000");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.INTEGER_TYPE_NAME;
    }
}
