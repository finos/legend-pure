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

public class TestFloatParsing extends AbstractPrimitiveParsingTest
{
    @Test
    public void testSimplePositiveFloat()
    {
        assertParsesTo("17.3", "17.3");
        assertParsesTo("17.3", "+17.3");
        assertParsesTo("17.3", "0000017.300000");
        assertParsesTo("17.0", "0000017.00000");
    }

    @Test
    public void testPositiveFloatLessThanOne()
    {
        assertParsesTo("0.3", "0.3");
        assertParsesTo("0.3", ".3");
    }

    @Test
    public void testSimpleNegativeFloat()
    {
        assertParsesTo("-17.3", "-17.3");
        assertParsesTo("-17.3", "-0000017.300000");
        assertParsesTo("-17.0", "-0000017.00000");
    }

    @Test
    public void testNegativeFloatLessThanOne()
    {
        assertParsesTo("-0.3", "-0.3");
        assertParsesTo("-0.3", "-.3");
    }

    @Test
    public void testZero()
    {
        assertParsesTo("0.0", "0.0");
        assertParsesTo("0.0", "-0.0");
        assertParsesTo("0.0", "0000000000000.00000000000000000000");
        assertParsesTo("0.0", "-0000000000000.00000000000000000000");
    }

    @Test
    public void testENotation()
    {
        assertParsesTo("173.0", "1.73e2");
        assertParsesTo("173.0", "1.73e+2");
        assertParsesTo("-173.0", "-1.730000e2");
        assertParsesTo("-173.0", "-17.3e1");
        assertParsesTo("173.0", "1730.0e-1");
        assertParsesTo("173.0", "17300.0e-2");
    }

    @Override
    protected String getPrimitiveTypeName()
    {
        return ModelRepository.FLOAT_TYPE_NAME;
    }
}
