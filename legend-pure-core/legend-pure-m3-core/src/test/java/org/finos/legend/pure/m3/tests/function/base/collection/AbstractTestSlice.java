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

package org.finos.legend.pure.m3.tests.function.base.collection;

import org.finos.legend.pure.m3.tests.function.base.PureExpressionTest;
import org.junit.Test;

public abstract class AbstractTestSlice extends PureExpressionTest
{
    @Test
    public void testSliceError() throws Exception
    {
        assertExpressionWithManyMultiplicityReturnRaisesPureException("The low bound (3) can't be higher than the high bound (2) in a slice operation", 3, 22, "[1,2,3,4,5]->slice(3, 2)");
    }
}
