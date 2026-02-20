// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestCompiledPrimitiveValueResolver
{
    private final CompiledPrimitiveValueResolver resolver = new CompiledPrimitiveValueResolver();

    @Test
    public void testResolveBoolean()
    {
        Assert.assertSame(Boolean.TRUE, this.resolver.resolveBoolean(true));
        Assert.assertSame(Boolean.FALSE, this.resolver.resolveBoolean(false));
    }

    @Test
    public void testResolveByte()
    {
        for (byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b++)
        {
            Assert.assertEquals(b, this.resolver.resolveByte(b));
        }
        Assert.assertEquals(Byte.MAX_VALUE, this.resolver.resolveByte(Byte.MAX_VALUE));
    }

    @Test
    public void testResolveDate()
    {
        Lists.mutable.with(
                DateFunctions.newPureDate(2025),
                DateFunctions.newPureDate(2025, 6),
                DateFunctions.newPureDate(2025, 6, 25),
                DateFunctions.newPureDate(2025, 6, 25, 16),
                DateFunctions.newPureDate(2025, 6, 25, 16, 46),
                DateFunctions.newPureDate(2025, 6, 25, 16, 46, 13),
                DateFunctions.newPureDate(2025, 6, 25, 16, 46, 13, "00132453")
        ).forEach(date -> Assert.assertSame(date, this.resolver.resolveDate(date)));
    }

    @Test
    public void testResolveDateTime()
    {
        Lists.mutable.with(
                DateFunctions.newPureDate(2025, 6, 25, 16),
                DateFunctions.newPureDate(2025, 6, 25, 16, 46),
                DateFunctions.newPureDate(2025, 6, 25, 16, 46, 13),
                DateFunctions.newPureDate(2025, 6, 25, 16, 46, 13, "00132453")
        ).forEach(date -> Assert.assertSame(date, this.resolver.resolveDateTime(date)));
    }

    @Test
    public void testResolveStrictDate()
    {
        Lists.mutable.with(
                DateFunctions.newPureDate(2025, 6, 25),
                DateFunctions.newPureDate(2024, 1, 1),
                DateFunctions.newPureDate(2012, 10, 18)
        ).forEach(date -> Assert.assertSame(date, this.resolver.resolveStrictDate(date)));
    }

    @Test
    public void testResolveLatestDate()
    {
        Assert.assertSame(LatestDate.instance, this.resolver.resolveLatestDate());
    }

    @Test
    public void testResolveDecimal()
    {
        Lists.mutable.with(
                "0",
                "0.0",
                "1.5",
                "-6.77777",
                Double.toString(Double.MIN_VALUE),
                Double.toString(Double.MAX_VALUE),
                "1238761238457619832764598716200093876598761892374.98759874359871248674500000023425241"
        ).forEach(s ->
        {
            BigDecimal d = new BigDecimal(s);
            Assert.assertSame(d, this.resolver.resolveDecimal(d));
        });
    }

    @Test
    public void testResolveFloat()
    {
        DoubleLists.mutable.with(
                0.0,
                -0.000002314,
                -1.57,
                1.74,
                3.14159,
                Double.MIN_VALUE,
                Double.MAX_VALUE
        ).forEach(d -> Assert.assertEquals(d, this.resolver.resolveFloat(new BigDecimal(d))));
    }

    @Test
    public void testResolveInteger()
    {
        Assert.assertEquals(0L, this.resolver.resolveInteger(0));
        Assert.assertEquals(-1L, this.resolver.resolveInteger(-1));
        Assert.assertEquals(1L, this.resolver.resolveInteger(1));
        Assert.assertEquals((long) Byte.MIN_VALUE, this.resolver.resolveInteger(Byte.MIN_VALUE));
        Assert.assertEquals((long) Byte.MAX_VALUE, this.resolver.resolveInteger(Byte.MAX_VALUE));
        Assert.assertEquals((long) Integer.MIN_VALUE, this.resolver.resolveInteger(Integer.MIN_VALUE));
        Assert.assertEquals((long) Integer.MAX_VALUE, this.resolver.resolveInteger(Integer.MAX_VALUE));
        Assert.assertEquals(Long.MIN_VALUE, this.resolver.resolveInteger(Long.MIN_VALUE));
        Assert.assertEquals(Long.MAX_VALUE, this.resolver.resolveInteger(Long.MAX_VALUE));
        Assert.assertEquals(0L, this.resolver.resolveInteger(BigInteger.valueOf(0L)));
        Assert.assertEquals(Long.MAX_VALUE, this.resolver.resolveInteger(BigInteger.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void testResolveStrictTime()
    {
        Lists.mutable.<PureStrictTime>with(
                StrictTimeFunctions.newPureStrictTime(13, 5),
                StrictTimeFunctions.newPureStrictTime(7, 48, 30),
                StrictTimeFunctions.newPureStrictTime(21, 59, 57, "999999999999")
        ).forEach(t -> Assert.assertSame(t, this.resolver.resolveStrictTime(t)));
    }

    @Test
    public void testResolveString()
    {
        Lists.mutable.with(
                "",
                "\t\n\r\b ",
                "the quick brown fox jumped over the lazy dog",
                "It was the best of times, it was the worst of times"
        ).forEach(s -> Assert.assertSame(s, this.resolver.resolveString(s)));
    }
}
