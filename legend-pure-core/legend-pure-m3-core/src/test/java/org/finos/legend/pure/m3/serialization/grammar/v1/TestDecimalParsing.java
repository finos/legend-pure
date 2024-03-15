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

package org.finos.legend.pure.m3.serialization.grammar.v1;

import org.finos.legend.pure.m4.ModelRepository;
import org.junit.Test;

public class TestDecimalParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testSimpleDecimal()
    {
        assertParsesTo("17.3", "17.3d");
        assertParsesTo("17", "17d");
        assertParsesTo("17.300000", "0000017.300000d");
        assertParsesTo("17.00000", "0000017.00000d");
    }

    @Test
    public void testPositiveDecimalLessThanOne()
    {
        assertParsesTo("0.3", "0.3d");
        assertParsesTo("0.3", ".3d");
    }

    @Test
    public void testZero()
    {
        assertParsesTo("0.0", "0.0d");
        assertParsesTo("0.00000000000000000000", "0000000000000.00000000000000000000d");
    }

    @Test
    public void testENotation()
    {
        assertParsesTo("173", "1.73e2d");
        assertParsesTo("173", "1.73e+2d");
        assertParsesTo("173.00", "1730.0e-1d");
        assertParsesTo("173.000", "17300.0e-2d");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.DECIMAL_TYPE_NAME;
    }
}
