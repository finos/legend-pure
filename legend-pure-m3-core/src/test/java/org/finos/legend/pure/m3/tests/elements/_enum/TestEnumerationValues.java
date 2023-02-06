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

package org.finos.legend.pure.m3.tests.elements._enum;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestEnumerationValues extends AbstractPureTestWithCoreCompiledPlatform
{
    @BeforeClass
    public static void setUp() {
        setUpRuntime(getExtra());
    }

    @Test
    public void testEnumerationWithDuplicateValues()
    {
        try
        {
            compileTestSource("testSource.pure",
                    "Enum test::TestEnum\n" +
                            "{\n" +
                            "    VAL1,\n" +
                            "    VAL2,\n" +
                            "    VAL1,\n" +
                            "    VAL3,\n" +
                            "    VAL2,\n" +
                            "    VAL1\n" +
                            "}");
            Assert.fail("Expected compilation exception");
        }
        catch (Exception e)
        {
            assertPureException(PureCompilationException.class, "Enumeration test::TestEnum has duplicate values: VAL1, VAL2", "testSource.pure", 1, 1, 1, 12, 9, 1, e);
        }
    }
}
