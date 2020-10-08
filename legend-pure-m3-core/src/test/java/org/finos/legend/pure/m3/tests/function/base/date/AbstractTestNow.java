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

package org.finos.legend.pure.m3.tests.function.base.date;

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestNow extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testNow()
    {
        compileTestSource("function test::nowWrapper():DateTime[1] { meta::pure::functions::date::now() }");
        CoreInstance result = execute("test::nowWrapper():DateTime[1]");
        long expected = System.currentTimeMillis();
        long tolerance = 100;

        CoreInstance date = Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, this.processorSupport);
        PureDate pureDate = PrimitiveUtilities.getDateValue(date);

        // Check that the date has millisecond precision
        Assert.assertTrue(pureDate.hasSubsecond());
        Assert.assertEquals(3, pureDate.getSubsecond().length());

        long actual = pureDate.getCalendar().getTimeInMillis();
        long difference = Math.abs(expected - actual);
        Assert.assertTrue("Expected millisecond difference to be less than" + tolerance + ", got: " + difference, difference < tolerance);
    }
}
