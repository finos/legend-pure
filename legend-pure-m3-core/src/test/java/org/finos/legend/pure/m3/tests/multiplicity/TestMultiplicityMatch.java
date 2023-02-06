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

package org.finos.legend.pure.m3.tests.multiplicity;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMultiplicityMatch extends AbstractPureTestWithCoreCompiledPlatform
{
    private static CoreInstance zeroMany;
    private static CoreInstance oneMany;
    private static CoreInstance zeroOne;
    private static CoreInstance zeroTen;
    private static CoreInstance one;
    private static CoreInstance oneSix;
    private static CoreInstance two;
    private static CoreInstance threeSeventeen;
    private static CoreInstance m;
    private static CoreInstance n;

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getExtra());
        
        zeroMany = newMultiplicity(0, -1);
        oneMany = newMultiplicity(1, -1);
        zeroOne = newMultiplicity(0, 1);
        zeroTen = newMultiplicity(0, 10);
        one = newMultiplicity(1, 1);
        oneSix = newMultiplicity(1, 6);
        two = newMultiplicity(2, 2);
        threeSeventeen = newMultiplicity(3, 17);
        m = newMultiplicity("m");
        n = newMultiplicity("n");
    }

    @Test
    public void testConcreteCovariantMultiplicityMatches()
    {
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, zeroMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, oneMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, zeroOne, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, two, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, zeroMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, two, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, oneMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroOne, zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroOne, one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, oneSix, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, oneMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, zeroOne, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, oneSix, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, zeroTen, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(two, two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, zeroTen, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, oneSix, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, two, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(threeSeventeen, threeSeventeen, true));
    }

    @Test
    public void testConcreteContravariantMultiplicityMatches()
    {
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, zeroMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, zeroOne, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneMany, oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, zeroOne, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroOne, zeroMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, oneMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroOne, zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroOne, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, zeroMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroTen, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, oneMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, zeroTen, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, one, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(one, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, one, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(oneSix, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(two, zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(two, oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(two, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, one, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(two, oneSix, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(two, two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(threeSeventeen, zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(threeSeventeen, oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, zeroOne, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, two, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(threeSeventeen, threeSeventeen, false));
    }

    @Test
    public void testNonConcreteTargetMultiplicityMatches()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.ERROR;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.MATCH_CAUTIOUSLY;

        CoreInstance m2 = newMultiplicity("m");

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, m2, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, zeroTen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, oneSix, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, two, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, threeSeventeen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, m2, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, n, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, zeroMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, oneMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, zeroOne, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, zeroTen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, one, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, oneSix, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, two, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(m, threeSeventeen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, m2, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, zeroTen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, oneSix, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, two, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, threeSeventeen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, m2, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, n, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, zeroMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, oneMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, zeroOne, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, zeroTen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, one, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, oneSix, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, two, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(n, threeSeventeen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
    }

    @Test
    public void testNonConcreteValueMultiplicityMatches()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.ERROR;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.MATCH_CAUTIOUSLY;

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(zeroMany, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroMany, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneMany, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroOne, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(zeroTen, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(one, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(oneSix, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(two, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(threeSeventeen, m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
    }

    @Test
    public void testExactMultiplicityMatch()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.ERROR;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.ERROR;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.ERROR;
        MultiplicityMatch exactMatchZeroMany = MultiplicityMatch.newMultiplicityMatch(zeroMany, zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch exactMatchOneMany = MultiplicityMatch.newMultiplicityMatch(oneMany, oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        Assert.assertEquals(exactMatchZeroMany, exactMatchOneMany);
        Assert.assertEquals(0, exactMatchOneMany.compareTo(exactMatchZeroMany));
        Assert.assertEquals(0, exactMatchZeroMany.compareTo(exactMatchOneMany));

        MultiplicityMatch exactMatchOneSix = MultiplicityMatch.newMultiplicityMatch(oneSix, oneSix, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        Assert.assertEquals(exactMatchZeroMany, exactMatchOneSix);
        Assert.assertEquals(0, exactMatchOneMany.compareTo(exactMatchOneSix));
    }

    @Test
    public void testMatchOrderingZeroManyTargetCovariant()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.MATCH_CAUTIOUSLY;
        MultiplicityMatch zeroManyMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneManyMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch zeroOneMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch mMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nullMatch = MultiplicityMatch.newMultiplicityMatch(zeroMany, null, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);

        Assert.assertNotNull(zeroManyMatch);
        Assert.assertNotNull(oneManyMatch);
        Assert.assertNotNull(zeroOneMatch);
        Assert.assertNotNull(oneMatch);
        Assert.assertNotNull(mMatch);
        Assert.assertNotNull(nMatch);
        Assert.assertNotNull(nullMatch);

        assertComparesEqual(zeroManyMatch, zeroManyMatch);
        assertComparesLessThan(zeroManyMatch, oneManyMatch);
        assertComparesLessThan(zeroManyMatch, zeroOneMatch);
        assertComparesLessThan(zeroManyMatch, oneMatch);
        assertComparesLessThan(zeroManyMatch, mMatch);
        assertComparesLessThan(zeroManyMatch, nMatch);
        assertComparesLessThan(zeroManyMatch, nullMatch);

        assertComparesGreaterThan(oneManyMatch, zeroManyMatch);
        assertComparesEqual(oneManyMatch, oneManyMatch);
        assertComparesLessThan(oneManyMatch, zeroOneMatch);
        assertComparesLessThan(oneManyMatch, oneMatch);
        assertComparesLessThan(oneManyMatch, mMatch);
        assertComparesLessThan(oneManyMatch, nMatch);
        assertComparesLessThan(oneManyMatch, nullMatch);

        assertComparesGreaterThan(zeroOneMatch, zeroManyMatch);
        assertComparesGreaterThan(zeroOneMatch, oneManyMatch);
        assertComparesEqual(zeroOneMatch, zeroOneMatch);
        assertComparesLessThan(zeroOneMatch, oneMatch);
        assertComparesLessThan(zeroOneMatch, mMatch);
        assertComparesLessThan(zeroOneMatch, nMatch);
        assertComparesLessThan(zeroOneMatch, nullMatch);

        assertComparesGreaterThan(oneMatch, zeroManyMatch);
        assertComparesGreaterThan(oneMatch, oneManyMatch);
        assertComparesGreaterThan(oneMatch, zeroOneMatch);
        assertComparesEqual(oneMatch, oneMatch);
        assertComparesLessThan(oneMatch, mMatch);
        assertComparesLessThan(oneMatch, nMatch);
        assertComparesLessThan(oneMatch, nullMatch);

        assertComparesGreaterThan(mMatch, zeroManyMatch);
        assertComparesGreaterThan(mMatch, oneManyMatch);
        assertComparesGreaterThan(mMatch, zeroOneMatch);
        assertComparesGreaterThan(mMatch, oneMatch);
        assertComparesEqual(mMatch, mMatch);
        assertComparesEqual(mMatch, nMatch);
        assertComparesLessThan(mMatch, nullMatch);

        assertComparesGreaterThan(nMatch, zeroManyMatch);
        assertComparesGreaterThan(nMatch, oneManyMatch);
        assertComparesGreaterThan(nMatch, zeroOneMatch);
        assertComparesGreaterThan(nMatch, oneMatch);
        assertComparesEqual(nMatch, mMatch);
        assertComparesEqual(nMatch, nMatch);
        assertComparesLessThan(nMatch, nullMatch);

        assertComparesGreaterThan(nullMatch, zeroManyMatch);
        assertComparesGreaterThan(nullMatch, oneManyMatch);
        assertComparesGreaterThan(nullMatch, zeroOneMatch);
        assertComparesGreaterThan(nullMatch, oneMatch);
        assertComparesGreaterThan(nullMatch, mMatch);
        assertComparesGreaterThan(nullMatch, nMatch);
        assertComparesEqual(nullMatch, nullMatch);
    }

    @Test
    public void testMatchOrderingNonConcreteTargetCovariant()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.ERROR;
        MultiplicityMatch zeroManyMatch = MultiplicityMatch.newMultiplicityMatch(m, zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneManyMatch = MultiplicityMatch.newMultiplicityMatch(m, oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch zeroOneMatch = MultiplicityMatch.newMultiplicityMatch(m, zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneMatch = MultiplicityMatch.newMultiplicityMatch(m, one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch mMatch = MultiplicityMatch.newMultiplicityMatch(m, newMultiplicity("m"), true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nMatch = MultiplicityMatch.newMultiplicityMatch(m, n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nullMatch = MultiplicityMatch.newMultiplicityMatch(m, null, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);

        Assert.assertNotNull(zeroManyMatch);
        Assert.assertNotNull(oneManyMatch);
        Assert.assertNotNull(zeroOneMatch);
        Assert.assertNotNull(oneMatch);
        Assert.assertNotNull(mMatch);
        Assert.assertNotNull(nMatch);
        Assert.assertNotNull(nullMatch);

        assertComparesEqual(zeroManyMatch, zeroManyMatch);
        assertComparesEqual(zeroManyMatch, oneManyMatch);
        assertComparesEqual(zeroManyMatch, zeroOneMatch);
        assertComparesEqual(zeroManyMatch, oneMatch);
        assertComparesEqual(zeroManyMatch, mMatch);
        assertComparesEqual(zeroManyMatch, nMatch);
        assertComparesLessThan(zeroManyMatch, nullMatch);

        assertComparesEqual(oneManyMatch, zeroManyMatch);
        assertComparesEqual(oneManyMatch, oneManyMatch);
        assertComparesEqual(oneManyMatch, zeroOneMatch);
        assertComparesEqual(oneManyMatch, oneMatch);
        assertComparesEqual(oneManyMatch, mMatch);
        assertComparesEqual(oneManyMatch, nMatch);
        assertComparesLessThan(oneManyMatch, nullMatch);

        assertComparesEqual(zeroOneMatch, zeroManyMatch);
        assertComparesEqual(zeroOneMatch, oneManyMatch);
        assertComparesEqual(zeroOneMatch, zeroOneMatch);
        assertComparesEqual(zeroOneMatch, oneMatch);
        assertComparesEqual(zeroOneMatch, mMatch);
        assertComparesEqual(zeroOneMatch, nMatch);
        assertComparesLessThan(zeroOneMatch, nullMatch);

        assertComparesEqual(oneMatch, zeroManyMatch);
        assertComparesEqual(oneMatch, oneManyMatch);
        assertComparesEqual(oneMatch, zeroOneMatch);
        assertComparesEqual(oneMatch, oneMatch);
        assertComparesEqual(oneMatch, mMatch);
        assertComparesEqual(oneMatch, nMatch);
        assertComparesLessThan(oneMatch, nullMatch);

        assertComparesEqual(mMatch, zeroManyMatch);
        assertComparesEqual(mMatch, oneManyMatch);
        assertComparesEqual(mMatch, zeroOneMatch);
        assertComparesEqual(mMatch, oneMatch);
        assertComparesEqual(mMatch, mMatch);
        assertComparesEqual(mMatch, nMatch);
        assertComparesLessThan(mMatch, nullMatch);

        assertComparesEqual(nMatch, zeroManyMatch);
        assertComparesEqual(nMatch, oneManyMatch);
        assertComparesEqual(nMatch, zeroOneMatch);
        assertComparesEqual(nMatch, oneMatch);
        assertComparesEqual(nMatch, mMatch);
        assertComparesEqual(nMatch, nMatch);
        assertComparesLessThan(nMatch, nullMatch);

        assertComparesGreaterThan(nullMatch, zeroManyMatch);
        assertComparesGreaterThan(nullMatch, oneManyMatch);
        assertComparesGreaterThan(nullMatch, zeroOneMatch);
        assertComparesGreaterThan(nullMatch, oneMatch);
        assertComparesGreaterThan(nullMatch, mMatch);
        assertComparesGreaterThan(nullMatch, nMatch);
        assertComparesEqual(nullMatch, nullMatch);
    }

    private static CoreInstance newMultiplicity(int lower, int upper)
    {
        return Multiplicity.newMultiplicity(lower, upper, processorSupport);
    }

    private static CoreInstance newMultiplicity(String parameterName)
    {
        return Multiplicity.newMultiplicity(parameterName, processorSupport);
    }

    private void assertComparesEqual(MultiplicityMatch match1, MultiplicityMatch match2)
    {
        int compare = match1.compareTo(match2);
        if (compare != 0)
        {
            Assert.fail("expected 0, got " + compare);
        }
    }

    private void assertComparesGreaterThan(MultiplicityMatch match1, MultiplicityMatch match2)
    {
        int compare = match1.compareTo(match2);
        if (compare <= 0)
        {
            Assert.fail("expected > 0, got " + compare);
        }
    }

    private void assertComparesLessThan(MultiplicityMatch match1, MultiplicityMatch match2)
    {
        int compare = match1.compareTo(match2);
        if (compare >= 0)
        {
            Assert.fail("expected < 0, got " + compare);
        }
    }
}
