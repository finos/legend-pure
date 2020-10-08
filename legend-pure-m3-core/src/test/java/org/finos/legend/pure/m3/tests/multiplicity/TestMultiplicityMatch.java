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

import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiledPlatform;
import org.finos.legend.pure.m3.navigation.generictype.match.NullMatchBehavior;
import org.finos.legend.pure.m3.navigation.generictype.match.ParameterMatchBehavior;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.multiplicity.MultiplicityMatch;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestMultiplicityMatch extends AbstractPureTestWithCoreCompiledPlatform
{
    private CoreInstance zeroMany;
    private CoreInstance oneMany;
    private CoreInstance zeroOne;
    private CoreInstance zeroTen;
    private CoreInstance one;
    private CoreInstance oneSix;
    private CoreInstance two;
    private CoreInstance threeSeventeen;
    private CoreInstance m;
    private CoreInstance n;

    @Before
    public void setUpMultiplicities()
    {
        this.zeroMany = newMultiplicity(0, -1);
        this.oneMany = newMultiplicity(1, -1);
        this.zeroOne = newMultiplicity(0, 1);
        this.zeroTen = newMultiplicity(0, 10);
        this.one = newMultiplicity(1, 1);
        this.oneSix = newMultiplicity(1, 6);
        this.two = newMultiplicity(2, 2);
        this.threeSeventeen = newMultiplicity(3, 17);
        this.m = newMultiplicity("m");
        this.n = newMultiplicity("n");
    }

    @Test
    public void testConcreteCovariantMultiplicityMatches()
    {
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.zeroMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.oneMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.zeroOne, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.two, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.zeroMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.two, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.oneMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.oneSix, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.oneMany, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.zeroOne, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.oneSix, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.zeroTen, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.one, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.zeroTen, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.oneSix, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.two, this.two, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.threeSeventeen, true));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.zeroMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.oneMany, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.zeroOne, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.zeroTen, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.one, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.oneSix, true));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.two, true));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.threeSeventeen, true));
    }

    @Test
    public void testConcreteContravariantMultiplicityMatches()
    {
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.zeroMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.zeroOne, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneMany, this.oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.zeroOne, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.zeroMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.oneMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.zeroMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.oneMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.zeroTen, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.one, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.one, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.one, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.oneSix, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.two, this.zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.two, this.oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.zeroOne, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.two, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.one, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.two, this.oneSix, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.two, this.two, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.threeSeventeen, false));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.zeroMany, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.oneMany, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.zeroOne, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.zeroTen, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.one, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.oneSix, false));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.two, false));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.threeSeventeen, false));
    }

    @Test
    public void testNonConcreteTargetMultiplicityMatches()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.ERROR;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.MATCH_CAUTIOUSLY;

        CoreInstance m2 = newMultiplicity("m");

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, m2, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.zeroTen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.oneSix, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.two, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.threeSeventeen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, m2, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.n, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.zeroMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.oneMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.zeroOne, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.zeroTen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.one, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.oneSix, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.two, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.m, this.threeSeventeen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, m2, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.zeroTen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.oneSix, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.two, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.threeSeventeen, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, m2, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.n, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.zeroMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.oneMany, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.zeroOne, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.zeroTen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.one, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.oneSix, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.two, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.n, this.threeSeventeen, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
    }

    @Test
    public void testNonConcreteValueMultiplicityMatches()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.ERROR;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.MATCH_CAUTIOUSLY;

        Assert.assertTrue(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroMany, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneMany, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroOne, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.zeroTen, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.one, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.oneSix, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.two, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));

        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
        Assert.assertFalse(MultiplicityMatch.multiplicityMatches(this.threeSeventeen, this.m, false, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior));
    }

    @Test
    public void testExactMultiplicityMatch()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.ERROR;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.ERROR;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.ERROR;
        MultiplicityMatch exactMatchZeroMany = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch exactMatchOneMany = MultiplicityMatch.newMultiplicityMatch(this.oneMany, this.oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        Assert.assertEquals(exactMatchZeroMany, exactMatchOneMany);
        Assert.assertEquals(0, exactMatchOneMany.compareTo(exactMatchZeroMany));
        Assert.assertEquals(0, exactMatchZeroMany.compareTo(exactMatchOneMany));

        MultiplicityMatch exactMatchOneSix = MultiplicityMatch.newMultiplicityMatch(this.oneSix, this.oneSix, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        Assert.assertEquals(exactMatchZeroMany, exactMatchOneSix);
        Assert.assertEquals(0, exactMatchOneMany.compareTo(exactMatchOneSix));
    }

    @Test
    public void testMatchOrderingZeroManyTargetCovariant()
    {
        NullMatchBehavior valueNullMatchBehavior = NullMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior targetParameterMatchBehavior = ParameterMatchBehavior.MATCH_ANYTHING;
        ParameterMatchBehavior valueParameterMatchBehavior = ParameterMatchBehavior.MATCH_CAUTIOUSLY;
        MultiplicityMatch zeroManyMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneManyMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch zeroOneMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch mMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.m, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, this.n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nullMatch = MultiplicityMatch.newMultiplicityMatch(this.zeroMany, null, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);

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
        MultiplicityMatch zeroManyMatch = MultiplicityMatch.newMultiplicityMatch(this.m, this.zeroMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneManyMatch = MultiplicityMatch.newMultiplicityMatch(this.m, this.oneMany, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch zeroOneMatch = MultiplicityMatch.newMultiplicityMatch(this.m, this.zeroOne, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch oneMatch = MultiplicityMatch.newMultiplicityMatch(this.m, this.one, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch mMatch = MultiplicityMatch.newMultiplicityMatch(this.m, newMultiplicity("m"), true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nMatch = MultiplicityMatch.newMultiplicityMatch(this.m, this.n, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);
        MultiplicityMatch nullMatch = MultiplicityMatch.newMultiplicityMatch(this.m, null, true, valueNullMatchBehavior, targetParameterMatchBehavior, valueParameterMatchBehavior);

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

    private CoreInstance newMultiplicity(int lower, int upper)
    {
        return Multiplicity.newMultiplicity(lower, upper, this.processorSupport);
    }

    private CoreInstance newMultiplicity(String parameterName)
    {
        return Multiplicity.newMultiplicity(parameterName, this.processorSupport);
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
