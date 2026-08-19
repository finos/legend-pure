// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.m4.coreinstance.primitive.date;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

public class TestTimeFunctions
{
    /**
     * Each takes a whole quantity and gives back the part of it below a second, padded out to the
     * digits that quantity measures in.
     */
    @Test
    public void testSubsecondPartOfAQuantity()
    {
        Assert.assertEquals("000", TimeFunctions.subsecondFromMilliseconds(0L));
        Assert.assertEquals("007", TimeFunctions.subsecondFromMilliseconds(7L));
        Assert.assertEquals("070", TimeFunctions.subsecondFromMilliseconds(70L));
        Assert.assertEquals("999", TimeFunctions.subsecondFromMilliseconds(999L));

        Assert.assertEquals("000000", TimeFunctions.subsecondFromMicroseconds(0L));
        Assert.assertEquals("000007", TimeFunctions.subsecondFromMicroseconds(7L));
        Assert.assertEquals("999999", TimeFunctions.subsecondFromMicroseconds(999_999L));

        Assert.assertEquals("000000000", TimeFunctions.subsecondFromNanoseconds(0L));
        Assert.assertEquals("000000007", TimeFunctions.subsecondFromNanoseconds(7L));
        Assert.assertEquals("123456789", TimeFunctions.subsecondFromNanoseconds(123_456_789L));
        Assert.assertEquals("999999999", TimeFunctions.subsecondFromNanoseconds(999_999_999L));
    }

    /**
     * Whole seconds are carried by the caller, so what is left here is only the remainder.
     */
    @Test
    public void testWholeSecondsAreDroppedFromTheSubsecondPart()
    {
        Assert.assertEquals("000", TimeFunctions.subsecondFromMilliseconds(1_000L));
        Assert.assertEquals("001", TimeFunctions.subsecondFromMilliseconds(1_001L));
        Assert.assertEquals("500", TimeFunctions.subsecondFromMilliseconds(90_500L));

        Assert.assertEquals("000001", TimeFunctions.subsecondFromMicroseconds(1_000_001L));
        Assert.assertEquals("000000001", TimeFunctions.subsecondFromNanoseconds(1_000_000_001L));
    }

    /**
     * The sign belongs to the direction the caller is adjusting in, not to the digits, so a
     * negative quantity gives the same digits as its positive counterpart.
     */
    @Test
    public void testTheSubsecondPartCarriesNoSign()
    {
        Assert.assertEquals("001", TimeFunctions.subsecondFromMilliseconds(-1L));
        Assert.assertEquals("999", TimeFunctions.subsecondFromMilliseconds(-999L));
        Assert.assertEquals("001", TimeFunctions.subsecondFromMilliseconds(-1_001L));
        Assert.assertEquals("000001", TimeFunctions.subsecondFromMicroseconds(-1L));
        Assert.assertEquals("000000001", TimeFunctions.subsecondFromNanoseconds(-1L));

        // taking the remainder first keeps the sign flip inside the range the digits can hold,
        // which negating the quantity itself would not for the smallest long
        Assert.assertEquals("808", TimeFunctions.subsecondFromMilliseconds(Long.MIN_VALUE));
        Assert.assertEquals("775808", TimeFunctions.subsecondFromMicroseconds(Long.MIN_VALUE));
        Assert.assertEquals("854775808", TimeFunctions.subsecondFromNanoseconds(Long.MIN_VALUE));
    }

    /**
     * The digits are the digits 0 to 9 whatever the default locale numbers in, since everything
     * that reads a subsecond back reads it as those.
     */
    @Test
    public void testDigitsAreASCIIWhateverTheLocale()
    {
        Locale before = Locale.getDefault();
        try
        {
            for (String tag : new String[]{"en-US", "fa-IR", "hi-IN-u-nu-deva", "ar-EG-u-nu-arab", "bn-IN-u-nu-beng"})
            {
                Locale.setDefault(Locale.forLanguageTag(tag));
                Assert.assertEquals(tag, "007", TimeFunctions.subsecondFromMilliseconds(7L));
                Assert.assertEquals(tag, "000007", TimeFunctions.subsecondFromMicroseconds(7L));
                Assert.assertEquals(tag, "123456789", TimeFunctions.subsecondFromNanoseconds(123_456_789L));
            }
        }
        finally
        {
            Locale.setDefault(before);
        }
    }
}
