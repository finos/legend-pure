// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m4.coreinstance;

import org.junit.Assert;
import org.junit.Test;

public class TestSourceInformation
{
    @Test
    public void testComparePositions()
    {
        // equal
        Assert.assertEquals(0, SourceInformation.comparePositions(1, 1, 1, 1));
        Assert.assertEquals(0, SourceInformation.comparePositions(2, 1, 2, 1));
        Assert.assertEquals(0, SourceInformation.comparePositions(10, 17, 10, 17));
        Assert.assertEquals(0, SourceInformation.comparePositions(234510, 17123, 234510, 17123));

        // before
        Assert.assertEquals(-1, Integer.signum(SourceInformation.comparePositions(1, 1, 2, 1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.comparePositions(1, 1, 1, 2)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.comparePositions(1, 2, 1, 3)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.comparePositions(1, 2, 2, 1)));

        // after
        Assert.assertEquals(1, Integer.signum(SourceInformation.comparePositions(2, 2, 2, 1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.comparePositions(2, 2, 1, 2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.comparePositions(2, 2, 1, 3)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.comparePositions(2, 2, 1, 1)));
    }

    @Test
    public void testCompare()
    {
        String source11 = "/platform/test1/source1.pure";
        String source12 = "/platform/test1/source2.pure";
        String source21 = "/platform/test2/source1.pure";

        SourceInformation source11_1_1_1_5_7_1 = new SourceInformation(source11, 1, 1, 1, 5, 7, 1);
        SourceInformation source11_1_1_1_5_7_2 = new SourceInformation(source11, 1, 1, 1, 5, 7, 2);
        SourceInformation source11_1_1_2_2_7_1 = new SourceInformation(source11, 1, 1, 2, 2, 7, 1);
        SourceInformation source11_2_1_2_2_7_1 = new SourceInformation(source11, 2, 1, 2, 2, 7, 1);

        SourceInformation source12_1_1_1_5_7_1 = new SourceInformation(source12, 1, 1, 1, 5, 7, 1);

        SourceInformation source21_1_1_1_5_7_1 = new SourceInformation(source21, 1, 1, 1, 5, 7, 1);

        // equal
        Assert.assertEquals(0, SourceInformation.compare(source11_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compare(source11_1_1_1_5_7_1, new SourceInformation(source11, 1, 1, 1, 5, 7, 1)));
        Assert.assertEquals(0, SourceInformation.compare(source12_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compare(source12_1_1_1_5_7_1, new SourceInformation(source12, 1, 1, 1, 5, 7, 1)));
        Assert.assertEquals(0, SourceInformation.compare(source21_1_1_1_5_7_1, source21_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compare(source21_1_1_1_5_7_1, new SourceInformation(source21, 1, 1, 1, 5, 7, 1)));

        // before
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_2, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_2_2_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_2_1_2_2_7_1, source12_1_1_1_5_7_1)));

        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_1, source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_2, source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_1_1_2_2_7_1, source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source11_2_1_2_2_7_1, source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compare(source12_1_1_1_5_7_1, source21_1_1_1_5_7_1)));

        // after
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source11_1_1_1_5_7_2, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source11_1_1_2_2_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source11_2_1_2_2_7_1, source11_1_1_1_5_7_1)));

        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source12_1_1_1_5_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source12_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source12_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source12_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source21_1_1_1_5_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source21_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source21_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source21_1_1_1_5_7_1, source11_2_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compare(source21_1_1_1_5_7_1, source12_1_1_1_5_7_1)));
    }

    @Test
    public void testCompareTo()
    {
        String source11 = "/platform/test1/source1.pure";
        String source12 = "/platform/test1/source2.pure";
        String source21 = "/platform/test2/source1.pure";

        SourceInformation source11_1_1_1_5_7_1 = new SourceInformation(source11, 1, 1, 1, 5, 7, 1);
        SourceInformation source11_1_1_1_5_7_2 = new SourceInformation(source11, 1, 1, 1, 5, 7, 2);
        SourceInformation source11_1_1_2_2_7_1 = new SourceInformation(source11, 1, 1, 2, 2, 7, 1);
        SourceInformation source11_2_1_2_2_7_1 = new SourceInformation(source11, 2, 1, 2, 2, 7, 1);

        SourceInformation source12_1_1_1_5_7_1 = new SourceInformation(source12, 1, 1, 1, 5, 7, 1);

        SourceInformation source21_1_1_1_5_7_1 = new SourceInformation(source21, 1, 1, 1, 5, 7, 1);

        // equal
        Assert.assertEquals(0, source11_1_1_1_5_7_1.compareTo(source11_1_1_1_5_7_1));
        Assert.assertEquals(0, source11_1_1_1_5_7_1.compareTo(new SourceInformation(source11, 1, 1, 1, 5, 7, 1)));
        Assert.assertEquals(0, source12_1_1_1_5_7_1.compareTo(source12_1_1_1_5_7_1));
        Assert.assertEquals(0, source12_1_1_1_5_7_1.compareTo(new SourceInformation(source12, 1, 1, 1, 5, 7, 1)));
        Assert.assertEquals(0, source21_1_1_1_5_7_1.compareTo(source21_1_1_1_5_7_1));
        Assert.assertEquals(0, source21_1_1_1_5_7_1.compareTo(new SourceInformation(source21, 1, 1, 1, 5, 7, 1)));

        // before
        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_1.compareTo(source11_1_1_1_5_7_2)));
        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_1.compareTo(source11_1_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_1.compareTo(source11_2_1_2_2_7_1)));

        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_1.compareTo(source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_2.compareTo(source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_1_1_2_2_7_1.compareTo(source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_2_1_2_2_7_1.compareTo(source12_1_1_1_5_7_1)));

        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_1.compareTo(source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_1_1_1_5_7_2.compareTo(source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_1_1_2_2_7_1.compareTo(source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source11_2_1_2_2_7_1.compareTo(source21_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(source12_1_1_1_5_7_1.compareTo(source21_1_1_1_5_7_1)));

        // after
        Assert.assertEquals(1, Integer.signum(source11_1_1_1_5_7_2.compareTo(source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(source11_1_1_2_2_7_1.compareTo(source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(source11_2_1_2_2_7_1.compareTo(source11_1_1_1_5_7_1)));

        Assert.assertEquals(1, Integer.signum(source12_1_1_1_5_7_1.compareTo(source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(source12_1_1_1_5_7_1.compareTo(source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(source12_1_1_1_5_7_1.compareTo(source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(source12_1_1_1_5_7_1.compareTo(source11_2_1_2_2_7_1)));

        Assert.assertEquals(1, Integer.signum(source21_1_1_1_5_7_1.compareTo(source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(source21_1_1_1_5_7_1.compareTo(source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(source21_1_1_1_5_7_1.compareTo(source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(source21_1_1_1_5_7_1.compareTo(source11_2_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(source21_1_1_1_5_7_1.compareTo(source12_1_1_1_5_7_1)));
    }

    @Test
    public void testCompareBySourceId()
    {
        String source11 = "/platform/test1/source1.pure";
        String source12 = "/platform/test1/source2.pure";
        String source21 = "/platform/test2/source1.pure";

        SourceInformation source11_1_1_1_5_7_1 = new SourceInformation(source11, 1, 1, 1, 5, 7, 1);
        SourceInformation source11_1_1_1_5_7_2 = new SourceInformation(source11, 1, 1, 1, 5, 7, 2);
        SourceInformation source11_1_1_2_2_7_1 = new SourceInformation(source11, 1, 1, 2, 2, 7, 1);
        SourceInformation source11_2_1_2_2_7_1 = new SourceInformation(source11, 2, 1, 2, 2, 7, 1);

        SourceInformation source12_1_1_1_5_7_1 = new SourceInformation(source12, 1, 1, 1, 5, 7, 1);

        SourceInformation source21_1_1_1_5_7_1 = new SourceInformation(source21, 1, 1, 1, 5, 7, 1);

        // equal
        Assert.assertEquals(0, SourceInformation.compareBySourceId(source11_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareBySourceId(source11_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareBySourceId(source11_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareBySourceId(source11_1_1_1_5_7_1, source11_2_1_2_2_7_1));

        Assert.assertEquals(0, SourceInformation.compareBySourceId(source12_1_1_1_5_7_1, source12_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareBySourceId(source21_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        // before
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareBySourceId(source11_1_1_1_5_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareBySourceId(source11_1_1_1_5_7_2, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareBySourceId(source11_1_1_2_2_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareBySourceId(source11_2_1_2_2_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareBySourceId(source12_1_1_1_5_7_1, source21_1_1_1_5_7_1)));

        // after
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source12_1_1_1_5_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source12_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source12_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source12_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source21_1_1_1_5_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source21_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source21_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source21_1_1_1_5_7_1, source11_2_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareBySourceId(source21_1_1_1_5_7_1, source12_1_1_1_5_7_1)));
    }

    @Test
    public void testCompareByStartPosition()
    {
        String source11 = "/platform/test1/source1.pure";
        String source12 = "/platform/test1/source2.pure";
        String source21 = "/platform/test2/source1.pure";

        SourceInformation source11_1_1_1_5_7_1 = new SourceInformation(source11, 1, 1, 1, 5, 7, 1);
        SourceInformation source11_1_1_1_5_7_2 = new SourceInformation(source11, 1, 1, 1, 5, 7, 2);
        SourceInformation source11_1_1_2_2_7_1 = new SourceInformation(source11, 1, 1, 2, 2, 7, 1);
        SourceInformation source11_2_1_2_2_7_1 = new SourceInformation(source11, 2, 1, 2, 2, 7, 1);

        SourceInformation source12_1_1_1_5_7_1 = new SourceInformation(source12, 1, 1, 1, 5, 7, 1);

        SourceInformation source21_1_1_1_5_7_1 = new SourceInformation(source21, 1, 1, 1, 5, 7, 1);

        // equal
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_2, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_2, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_2, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_2, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_1_5_7_2, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_2_2_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_2_2_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_2_2_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_2_2_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source11_1_1_2_2_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source12_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source12_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source12_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source12_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source12_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source21_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source21_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source21_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source21_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByStartPosition(source21_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        // before
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByStartPosition(source11_1_1_1_5_7_1, source11_2_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByStartPosition(source11_1_1_1_5_7_2, source11_2_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByStartPosition(source11_1_1_2_2_7_1, source11_2_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByStartPosition(source12_1_1_1_5_7_1, source11_2_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByStartPosition(source21_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        // after
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByStartPosition(source11_2_1_2_2_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByStartPosition(source11_2_1_2_2_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByStartPosition(source11_2_1_2_2_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByStartPosition(source11_2_1_2_2_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByStartPosition(source11_2_1_2_2_7_1, source21_1_1_1_5_7_1)));
    }

    @Test
    public void testCompareByMainPosition()
    {
        String source11 = "/platform/test1/source1.pure";
        String source12 = "/platform/test1/source2.pure";
        String source21 = "/platform/test2/source1.pure";

        SourceInformation source11_1_1_1_5_7_1 = new SourceInformation(source11, 1, 1, 1, 5, 7, 1);
        SourceInformation source11_1_1_1_5_7_2 = new SourceInformation(source11, 1, 1, 1, 5, 7, 2);
        SourceInformation source11_1_1_2_2_7_1 = new SourceInformation(source11, 1, 1, 2, 2, 7, 1);
        SourceInformation source11_2_1_2_2_7_1 = new SourceInformation(source11, 2, 1, 2, 2, 7, 1);

        SourceInformation source12_1_1_1_5_7_1 = new SourceInformation(source12, 1, 1, 1, 5, 7, 1);

        SourceInformation source21_1_1_1_5_7_1 = new SourceInformation(source21, 1, 1, 1, 5, 7, 1);

        // equal
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_2, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_2, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_2, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_1_5_7_2, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_2_2_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_1_1_2_2_7_1, source11_2_1_2_2_7_1));

        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_2_1_2_2_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source11_2_1_2_2_7_1, source11_2_1_2_2_7_1));

        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source12_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source12_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source12_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source12_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source21_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source21_1_1_1_5_7_1, source11_1_1_1_5_7_2));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source21_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByMainPosition(source21_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        // before
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_1_5_7_2, source11_1_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_1_5_7_2, source11_2_1_2_2_7_1)));

        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source12_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source12_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source21_1_1_1_5_7_1, source11_1_1_2_2_7_1)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByMainPosition(source21_1_1_1_5_7_1, source11_2_1_2_2_7_1)));

        // after
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_2_2_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_2_2_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_2_2_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_1_1_2_2_7_1, source21_1_1_1_5_7_1)));

        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_2_1_2_2_7_1, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_2_1_2_2_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_2_1_2_2_7_1, source12_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByMainPosition(source11_2_1_2_2_7_1, source21_1_1_1_5_7_1)));
    }

    @Test
    public void testCompareByEndPosition()
    {
        String source11 = "/platform/test1/source1.pure";
        String source12 = "/platform/test1/source2.pure";
        String source21 = "/platform/test2/source1.pure";

        SourceInformation source11_1_1_1_5_7_1 = new SourceInformation(source11, 1, 1, 1, 5, 7, 1);
        SourceInformation source11_1_1_1_5_7_2 = new SourceInformation(source11, 1, 1, 1, 5, 7, 2);
        SourceInformation source11_1_1_2_2_7_1 = new SourceInformation(source11, 1, 1, 2, 2, 7, 1);
        SourceInformation source11_2_1_2_2_7_1 = new SourceInformation(source11, 2, 1, 2, 2, 7, 1);

        SourceInformation source12_1_1_1_5_7_1 = new SourceInformation(source12, 1, 1, 1, 5, 7, 1);

        SourceInformation source21_1_1_1_5_7_1 = new SourceInformation(source21, 1, 1, 1, 5, 7, 1);

        // equal
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_1_5_7_1, source11_2_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_2_2_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_2_2_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_2_2_7_1, source11_2_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_2_2_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_1_1_2_2_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_2_1_2_2_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_2_1_2_2_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_2_1_2_2_7_1, source11_2_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_2_1_2_2_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source11_2_1_2_2_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source12_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source12_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source12_1_1_1_5_7_1, source11_2_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source12_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source12_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source21_1_1_1_5_7_1, source11_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source21_1_1_1_5_7_1, source11_1_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source21_1_1_1_5_7_1, source11_2_1_2_2_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source21_1_1_1_5_7_1, source12_1_1_1_5_7_1));
        Assert.assertEquals(0, SourceInformation.compareByEndPosition(source21_1_1_1_5_7_1, source21_1_1_1_5_7_1));

        // before
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_2_2_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByEndPosition(source11_2_1_2_2_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByEndPosition(source12_1_1_1_5_7_1, source11_1_1_1_5_7_2)));
        Assert.assertEquals(-1, Integer.signum(SourceInformation.compareByEndPosition(source21_1_1_1_5_7_1, source11_1_1_1_5_7_2)));

        // after
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_1_5_7_2, source11_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_1_5_7_2, source11_1_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_1_5_7_2, source11_2_1_2_2_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_1_5_7_2, source12_1_1_1_5_7_1)));
        Assert.assertEquals(1, Integer.signum(SourceInformation.compareByEndPosition(source11_1_1_1_5_7_2, source21_1_1_1_5_7_1)));
    }

    @Test
    public void testIsBefore()
    {
        // equal
        Assert.assertFalse(SourceInformation.isBefore(0, 0, 0, 0));
        Assert.assertFalse(SourceInformation.isBefore(1, 0, 1, 0));
        Assert.assertFalse(SourceInformation.isBefore(1, 1, 1, 1));
        Assert.assertFalse(SourceInformation.isBefore(2, 0, 2, 0));
        Assert.assertFalse(SourceInformation.isBefore(2, 1, 2, 1));
        Assert.assertFalse(SourceInformation.isBefore(10, 17, 10, 17));
        Assert.assertFalse(SourceInformation.isBefore(234510, 17123, 234510, 17123));

        // before
        Assert.assertTrue(SourceInformation.isBefore(0, 0, 1, 0));
        Assert.assertTrue(SourceInformation.isBefore(1, 0, 1, 1));
        Assert.assertTrue(SourceInformation.isBefore(1, 0, 2, 1));
        Assert.assertTrue(SourceInformation.isBefore(1, 1, 2, 1));
        Assert.assertTrue(SourceInformation.isBefore(1, 1, 1, 2));
        Assert.assertTrue(SourceInformation.isBefore(1, 2, 1, 3));
        Assert.assertTrue(SourceInformation.isBefore(1, 2, 2, 1));

        // after
        Assert.assertFalse(SourceInformation.isBefore(1, 0, 0, 0));
        Assert.assertFalse(SourceInformation.isBefore(1, 1, 0, 0));
        Assert.assertFalse(SourceInformation.isBefore(1, 1, 1, 0));
        Assert.assertFalse(SourceInformation.isBefore(2, 2, 1, 2));
        Assert.assertFalse(SourceInformation.isBefore(2, 2, 1, 3));
        Assert.assertFalse(SourceInformation.isBefore(2, 2, 1, 3));
        Assert.assertFalse(SourceInformation.isBefore(2, 2, 1, 1));
    }

    @Test
    public void testIsAfter()
    {
        // equal
        Assert.assertFalse(SourceInformation.isAfter(0, 0, 0, 0));
        Assert.assertFalse(SourceInformation.isAfter(1, 0, 1, 0));
        Assert.assertFalse(SourceInformation.isAfter(1, 1, 1, 1));
        Assert.assertFalse(SourceInformation.isAfter(2, 0, 2, 0));
        Assert.assertFalse(SourceInformation.isAfter(2, 1, 2, 1));
        Assert.assertFalse(SourceInformation.isAfter(10, 17, 10, 17));
        Assert.assertFalse(SourceInformation.isAfter(234510, 17123, 234510, 17123));

        // before
        Assert.assertFalse(SourceInformation.isAfter(0, 0, 1, 0));
        Assert.assertFalse(SourceInformation.isAfter(1, 0, 1, 1));
        Assert.assertFalse(SourceInformation.isAfter(1, 0, 2, 1));
        Assert.assertFalse(SourceInformation.isAfter(1, 1, 2, 1));
        Assert.assertFalse(SourceInformation.isAfter(1, 1, 1, 2));
        Assert.assertFalse(SourceInformation.isAfter(1, 2, 1, 3));
        Assert.assertFalse(SourceInformation.isAfter(1, 2, 2, 1));

        // after
        Assert.assertTrue(SourceInformation.isAfter(1, 0, 0, 0));
        Assert.assertTrue(SourceInformation.isAfter(1, 1, 0, 0));
        Assert.assertTrue(SourceInformation.isAfter(1, 1, 1, 0));
        Assert.assertTrue(SourceInformation.isAfter(2, 2, 1, 2));
        Assert.assertTrue(SourceInformation.isAfter(2, 2, 1, 3));
        Assert.assertTrue(SourceInformation.isAfter(2, 2, 1, 3));
        Assert.assertTrue(SourceInformation.isAfter(2, 2, 1, 1));
    }

    @Test
    public void testIsNotBefore()
    {
        // equal
        Assert.assertTrue(SourceInformation.isNotBefore(0, 0, 0, 0));
        Assert.assertTrue(SourceInformation.isNotBefore(1, 0, 1, 0));
        Assert.assertTrue(SourceInformation.isNotBefore(1, 1, 1, 1));
        Assert.assertTrue(SourceInformation.isNotBefore(2, 0, 2, 0));
        Assert.assertTrue(SourceInformation.isNotBefore(2, 1, 2, 1));
        Assert.assertTrue(SourceInformation.isNotBefore(10, 17, 10, 17));
        Assert.assertTrue(SourceInformation.isNotBefore(234510, 17123, 234510, 17123));

        // before
        Assert.assertFalse(SourceInformation.isNotBefore(0, 0, 1, 0));
        Assert.assertFalse(SourceInformation.isNotBefore(1, 0, 1, 1));
        Assert.assertFalse(SourceInformation.isNotBefore(1, 0, 2, 1));
        Assert.assertFalse(SourceInformation.isNotBefore(1, 1, 2, 1));
        Assert.assertFalse(SourceInformation.isNotBefore(1, 1, 1, 2));
        Assert.assertFalse(SourceInformation.isNotBefore(1, 2, 1, 3));
        Assert.assertFalse(SourceInformation.isNotBefore(1, 2, 2, 1));

        // after
        Assert.assertTrue(SourceInformation.isNotBefore(1, 0, 0, 0));
        Assert.assertTrue(SourceInformation.isNotBefore(1, 1, 0, 0));
        Assert.assertTrue(SourceInformation.isNotBefore(1, 1, 1, 0));
        Assert.assertTrue(SourceInformation.isNotBefore(2, 2, 1, 2));
        Assert.assertTrue(SourceInformation.isNotBefore(2, 2, 1, 3));
        Assert.assertTrue(SourceInformation.isNotBefore(2, 2, 1, 3));
        Assert.assertTrue(SourceInformation.isNotBefore(2, 2, 1, 1));
    }

    @Test
    public void testIsNotAfter()
    {
        // equal
        Assert.assertTrue(SourceInformation.isNotAfter(0, 0, 0, 0));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 0, 1, 0));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 1, 1, 1));
        Assert.assertTrue(SourceInformation.isNotAfter(2, 0, 2, 0));
        Assert.assertTrue(SourceInformation.isNotAfter(2, 1, 2, 1));
        Assert.assertTrue(SourceInformation.isNotAfter(10, 17, 10, 17));
        Assert.assertTrue(SourceInformation.isNotAfter(234510, 17123, 234510, 17123));

        // before
        Assert.assertTrue(SourceInformation.isNotAfter(0, 0, 1, 0));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 0, 1, 1));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 0, 2, 1));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 1, 2, 1));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 1, 1, 2));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 2, 1, 3));
        Assert.assertTrue(SourceInformation.isNotAfter(1, 2, 2, 1));

        // after
        Assert.assertFalse(SourceInformation.isNotAfter(1, 0, 0, 0));
        Assert.assertFalse(SourceInformation.isNotAfter(1, 1, 0, 0));
        Assert.assertFalse(SourceInformation.isNotAfter(1, 1, 1, 0));
        Assert.assertFalse(SourceInformation.isNotAfter(2, 2, 1, 2));
        Assert.assertFalse(SourceInformation.isNotAfter(2, 2, 1, 3));
        Assert.assertFalse(SourceInformation.isNotAfter(2, 2, 1, 3));
        Assert.assertFalse(SourceInformation.isNotAfter(2, 2, 1, 1));
    }

    @Test
    public void testGetMessage()
    {
        String source1 = "/platform/test/source1.pure";
        String source2 = "/platform/test/source2.pure";

        Assert.assertEquals(source1 + ":1c1", new SourceInformation(source1, 1, 1, 1, 1, 1, 1).getMessage());
        Assert.assertEquals(source1 + ":1cc1-7", new SourceInformation(source1, 1, 1, 2, 1, 1, 7).getMessage());
        Assert.assertEquals(source1 + ":1cc1-7", new SourceInformation(source1, 1, 1, 2, 5, 1, 7).getMessage());
        Assert.assertEquals(source1 + ":1c1-5c1", new SourceInformation(source1, 1, 1, 2, 1, 5, 1).getMessage());
        Assert.assertEquals(source2 + ":17c3-18c9", new SourceInformation(source2, 17, 3, 2, 1, 18, 9).getMessage());
    }

    @Test
    public void testGetIntervalMessage()
    {
        String source1 = "/platform/test/source1.pure";
        String source2 = "/platform/test/source2.pure";

        Assert.assertEquals("1c1", new SourceInformation(source1, 1, 1, 1, 1, 1, 1).getIntervalMessage());
        Assert.assertEquals("1cc1-7", new SourceInformation(source1, 1, 1, 2, 1, 1, 7).getIntervalMessage());
        Assert.assertEquals("1cc1-7", new SourceInformation(source1, 1, 1, 2, 5, 1, 7).getIntervalMessage());
        Assert.assertEquals("1c1-5c1", new SourceInformation(source1, 1, 1, 2, 1, 5, 1).getIntervalMessage());
        Assert.assertEquals("17c3-18c9", new SourceInformation(source1, 17, 3, 2, 1, 18, 9).getIntervalMessage());
        Assert.assertEquals("17c3-18c9", new SourceInformation(source2, 17, 3, 2, 1, 18, 9).getIntervalMessage());
    }

    @Test
    public void testSubsumes()
    {
        String source1 = "/platform/test/source1.pure";
        String source2 = "/platform/test/source2.pure";

        SourceInformation source1_0_0_0_0 = new SourceInformation(source1, 0, 0, 0, 0);
        SourceInformation source1_1_0_1_0 = new SourceInformation(source1, 1, 0, 1, 0);
        SourceInformation source1_1_1_10_1 = new SourceInformation(source1, 1, 1, 10, 1);
        SourceInformation source1_1_5_1_7 = new SourceInformation(source1, 1, 5, 1, 7);
        SourceInformation source1_1_5_6_1 = new SourceInformation(source1, 1, 5, 6, 1);
        SourceInformation source1_2_0_2_0 = new SourceInformation(source1, 2, 0, 2, 0);
        SourceInformation source1_2_1_6_1 = new SourceInformation(source1, 2, 1, 6, 1);
        SourceInformation source1_5_0_5_0 = new SourceInformation(source1, 5, 0, 5, 0);
        SourceInformation source1_5_1_10_1 = new SourceInformation(source1, 5, 1, 10, 1);
        SourceInformation source1_5_3_8_8 = new SourceInformation(source1, 5, 3, 8, 8);
        SourceInformation source2_1_1_10_1 = new SourceInformation(source2, 1, 1, 10, 1);

        assertSubsumes(source1_0_0_0_0, source1_0_0_0_0);
        assertNotSubsumes(source1_0_0_0_0, source1_1_0_1_0);
        assertNotSubsumes(source1_0_0_0_0, source1_1_1_10_1);
        assertNotSubsumes(source1_0_0_0_0, source1_1_5_1_7);
        assertNotSubsumes(source1_0_0_0_0, source1_1_5_6_1);
        assertNotSubsumes(source1_0_0_0_0, source1_2_0_2_0);
        assertNotSubsumes(source1_0_0_0_0, source1_2_1_6_1);
        assertNotSubsumes(source1_0_0_0_0, source1_5_0_5_0);
        assertNotSubsumes(source1_0_0_0_0, source1_5_1_10_1);
        assertNotSubsumes(source1_0_0_0_0, source1_5_3_8_8);
        assertNotSubsumes(source1_0_0_0_0, source2_1_1_10_1);

        assertNotSubsumes(source1_1_0_1_0, source1_0_0_0_0);
        assertSubsumes(source1_1_0_1_0, source1_1_0_1_0);
        assertNotSubsumes(source1_1_0_1_0, source1_1_1_10_1);
        assertNotSubsumes(source1_1_0_1_0, source1_1_5_1_7);
        assertNotSubsumes(source1_1_0_1_0, source1_1_5_6_1);
        assertNotSubsumes(source1_1_0_1_0, source1_2_0_2_0);
        assertNotSubsumes(source1_1_0_1_0, source1_2_1_6_1);
        assertNotSubsumes(source1_1_0_1_0, source1_5_0_5_0);
        assertNotSubsumes(source1_1_0_1_0, source1_5_1_10_1);
        assertNotSubsumes(source1_1_0_1_0, source1_5_3_8_8);
        assertNotSubsumes(source1_1_0_1_0, source2_1_1_10_1);

        assertNotSubsumes(source1_1_1_10_1, source1_0_0_0_0);
        assertNotSubsumes(source1_1_1_10_1, source1_1_0_1_0);
        assertSubsumes(source1_1_1_10_1, source1_1_1_10_1);
        assertSubsumes(source1_1_1_10_1, source1_1_5_1_7);
        assertSubsumes(source1_1_1_10_1, source1_1_5_6_1);
        assertSubsumes(source1_1_1_10_1, source1_2_0_2_0);
        assertSubsumes(source1_1_1_10_1, source1_2_1_6_1);
        assertSubsumes(source1_1_1_10_1, source1_5_0_5_0);
        assertSubsumes(source1_1_1_10_1, source1_5_1_10_1);
        assertSubsumes(source1_1_1_10_1, source1_5_3_8_8);
        assertNotSubsumes(source1_1_1_10_1, source2_1_1_10_1);

        assertNotSubsumes(source1_1_5_1_7, source1_0_0_0_0);
        assertNotSubsumes(source1_1_5_1_7, source1_1_0_1_0);
        assertNotSubsumes(source1_1_5_1_7, source1_1_1_10_1);
        assertSubsumes(source1_1_5_1_7, source1_1_5_1_7);
        assertNotSubsumes(source1_1_5_1_7, source1_1_5_6_1);
        assertNotSubsumes(source1_1_5_1_7, source1_2_0_2_0);
        assertNotSubsumes(source1_1_5_1_7, source1_2_1_6_1);
        assertNotSubsumes(source1_1_5_1_7, source1_5_0_5_0);
        assertNotSubsumes(source1_1_5_1_7, source1_5_1_10_1);
        assertNotSubsumes(source1_1_5_1_7, source1_5_3_8_8);
        assertNotSubsumes(source1_1_5_1_7, source2_1_1_10_1);

        assertNotSubsumes(source1_1_5_6_1, source1_0_0_0_0);
        assertNotSubsumes(source1_1_5_6_1, source1_1_0_1_0);
        assertNotSubsumes(source1_1_5_6_1, source1_1_1_10_1);
        assertSubsumes(source1_1_5_6_1, source1_1_5_1_7);
        assertSubsumes(source1_1_5_6_1, source1_1_5_6_1);
        assertSubsumes(source1_1_5_6_1, source1_2_0_2_0);
        assertSubsumes(source1_1_5_6_1, source1_2_1_6_1);
        assertSubsumes(source1_1_5_6_1, source1_5_0_5_0);
        assertNotSubsumes(source1_1_5_6_1, source1_5_1_10_1);
        assertNotSubsumes(source1_1_5_6_1, source1_5_3_8_8);
        assertNotSubsumes(source1_1_5_6_1, source2_1_1_10_1);

        assertNotSubsumes(source1_2_1_6_1, source1_0_0_0_0);
        assertNotSubsumes(source1_2_1_6_1, source1_1_0_1_0);
        assertNotSubsumes(source1_2_1_6_1, source1_1_1_10_1);
        assertNotSubsumes(source1_2_1_6_1, source1_1_5_1_7);
        assertNotSubsumes(source1_2_1_6_1, source1_1_5_6_1);
        assertNotSubsumes(source1_2_1_6_1, source1_2_0_2_0);
        assertSubsumes(source1_2_1_6_1, source1_2_1_6_1);
        assertSubsumes(source1_2_1_6_1, source1_5_0_5_0);
        assertNotSubsumes(source1_2_1_6_1, source1_5_1_10_1);
        assertNotSubsumes(source1_2_1_6_1, source1_5_3_8_8);
        assertNotSubsumes(source1_2_1_6_1, source2_1_1_10_1);

        assertNotSubsumes(source1_5_1_10_1, source1_0_0_0_0);
        assertNotSubsumes(source1_5_1_10_1, source1_1_0_1_0);
        assertNotSubsumes(source1_5_1_10_1, source1_1_1_10_1);
        assertNotSubsumes(source1_5_1_10_1, source1_1_5_1_7);
        assertNotSubsumes(source1_5_1_10_1, source1_1_5_6_1);
        assertNotSubsumes(source1_5_1_10_1, source1_2_0_2_0);
        assertNotSubsumes(source1_5_1_10_1, source1_2_1_6_1);
        assertNotSubsumes(source1_5_1_10_1, source1_5_0_5_0);
        assertSubsumes(source1_5_1_10_1, source1_5_1_10_1);
        assertSubsumes(source1_5_1_10_1, source1_5_3_8_8);
        assertNotSubsumes(source1_5_1_10_1, source2_1_1_10_1);

        assertNotSubsumes(source1_5_3_8_8, source1_0_0_0_0);
        assertNotSubsumes(source1_5_3_8_8, source1_1_0_1_0);
        assertNotSubsumes(source1_5_3_8_8, source1_1_1_10_1);
        assertNotSubsumes(source1_5_3_8_8, source1_1_5_1_7);
        assertNotSubsumes(source1_5_3_8_8, source1_1_5_6_1);
        assertNotSubsumes(source1_5_3_8_8, source1_2_0_2_0);
        assertNotSubsumes(source1_5_3_8_8, source1_2_1_6_1);
        assertNotSubsumes(source1_5_3_8_8, source1_5_0_5_0);
        assertNotSubsumes(source1_5_3_8_8, source1_5_1_10_1);
        assertSubsumes(source1_5_3_8_8, source1_5_3_8_8);
        assertNotSubsumes(source1_5_3_8_8, source2_1_1_10_1);

        assertNotSubsumes(source2_1_1_10_1, source1_0_0_0_0);
        assertNotSubsumes(source2_1_1_10_1, source1_1_0_1_0);
        assertNotSubsumes(source2_1_1_10_1, source1_1_1_10_1);
        assertNotSubsumes(source2_1_1_10_1, source1_1_5_1_7);
        assertNotSubsumes(source2_1_1_10_1, source1_1_5_6_1);
        assertNotSubsumes(source2_1_1_10_1, source1_2_0_2_0);
        assertNotSubsumes(source2_1_1_10_1, source1_2_1_6_1);
        assertNotSubsumes(source2_1_1_10_1, source1_5_0_5_0);
        assertNotSubsumes(source2_1_1_10_1, source1_5_1_10_1);
        assertNotSubsumes(source2_1_1_10_1, source1_5_3_8_8);
        assertSubsumes(source2_1_1_10_1, source2_1_1_10_1);
    }

    @Test
    public void testIntersects()
    {
        String source1 = "/platform/test/source1.pure";
        String source2 = "/platform/test/source2.pure";

        SourceInformation source1_0_0_0_0 = new SourceInformation(source1, 0, 0, 0, 0);
        SourceInformation source1_1_0_1_0 = new SourceInformation(source1, 1, 0, 1, 0);
        SourceInformation source1_1_1_10_1 = new SourceInformation(source1, 1, 1, 10, 1);
        SourceInformation source1_1_5_1_7 = new SourceInformation(source1, 1, 5, 1, 7);
        SourceInformation source1_1_5_6_1 = new SourceInformation(source1, 1, 5, 6, 1);
        SourceInformation source1_1_8_1_9 = new SourceInformation(source1, 1, 8, 1, 9);
        SourceInformation source1_2_0_2_0 = new SourceInformation(source1, 2, 0, 2, 0);
        SourceInformation source1_2_1_6_1 = new SourceInformation(source1, 2, 1, 6, 1);
        SourceInformation source1_5_0_5_0 = new SourceInformation(source1, 5, 0, 5, 0);
        SourceInformation source1_5_1_10_1 = new SourceInformation(source1, 5, 1, 10, 1);
        SourceInformation source1_5_3_8_8 = new SourceInformation(source1, 5, 3, 8, 8);
        SourceInformation source2_1_1_10_1 = new SourceInformation(source2, 1, 1, 10, 1);

        assertIntersects(source1_0_0_0_0, source1_0_0_0_0);
        assertNotIntersects(source1_0_0_0_0, source1_1_0_1_0);
        assertNotIntersects(source1_0_0_0_0, source1_1_1_10_1);
        assertNotIntersects(source1_0_0_0_0, source1_1_5_1_7);
        assertNotIntersects(source1_0_0_0_0, source1_1_5_6_1);
        assertNotIntersects(source1_0_0_0_0, source1_1_8_1_9);
        assertNotIntersects(source1_0_0_0_0, source1_2_0_2_0);
        assertNotIntersects(source1_0_0_0_0, source1_2_1_6_1);
        assertNotIntersects(source1_0_0_0_0, source1_5_0_5_0);
        assertNotIntersects(source1_0_0_0_0, source1_5_1_10_1);
        assertNotIntersects(source1_0_0_0_0, source1_5_3_8_8);
        assertNotIntersects(source1_0_0_0_0, source2_1_1_10_1);

        assertNotIntersects(source1_1_0_1_0, source1_0_0_0_0);
        assertIntersects(source1_1_0_1_0, source1_1_0_1_0);
        assertNotIntersects(source1_1_0_1_0, source1_1_1_10_1);
        assertNotIntersects(source1_1_0_1_0, source1_1_5_1_7);
        assertNotIntersects(source1_1_0_1_0, source1_1_5_6_1);
        assertNotIntersects(source1_1_0_1_0, source1_1_8_1_9);
        assertNotIntersects(source1_1_0_1_0, source1_2_0_2_0);
        assertNotIntersects(source1_1_0_1_0, source1_2_1_6_1);
        assertNotIntersects(source1_1_0_1_0, source1_5_0_5_0);
        assertNotIntersects(source1_1_0_1_0, source1_5_1_10_1);
        assertNotIntersects(source1_1_0_1_0, source1_5_3_8_8);
        assertNotIntersects(source1_1_0_1_0, source2_1_1_10_1);

        assertNotIntersects(source1_1_1_10_1, source1_0_0_0_0);
        assertNotIntersects(source1_1_1_10_1, source1_1_0_1_0);
        assertIntersects(source1_1_1_10_1, source1_1_1_10_1);
        assertIntersects(source1_1_1_10_1, source1_1_5_1_7);
        assertIntersects(source1_1_1_10_1, source1_1_5_6_1);
        assertIntersects(source1_1_1_10_1, source1_1_8_1_9);
        assertIntersects(source1_1_1_10_1, source1_2_0_2_0);
        assertIntersects(source1_1_1_10_1, source1_2_1_6_1);
        assertIntersects(source1_1_1_10_1, source1_5_0_5_0);
        assertIntersects(source1_1_1_10_1, source1_5_1_10_1);
        assertIntersects(source1_1_1_10_1, source1_5_3_8_8);
        assertNotIntersects(source1_1_1_10_1, source2_1_1_10_1);

        assertNotIntersects(source1_1_5_1_7, source1_0_0_0_0);
        assertNotIntersects(source1_1_5_1_7, source1_1_0_1_0);
        assertIntersects(source1_1_5_1_7, source1_1_1_10_1);
        assertIntersects(source1_1_5_1_7, source1_1_5_1_7);
        assertIntersects(source1_1_5_1_7, source1_1_5_6_1);
        assertNotIntersects(source1_1_5_1_7, source1_1_8_1_9);
        assertNotIntersects(source1_1_5_1_7, source1_2_0_2_0);
        assertNotIntersects(source1_1_5_1_7, source1_2_1_6_1);
        assertNotIntersects(source1_1_5_1_7, source1_5_0_5_0);
        assertNotIntersects(source1_1_5_1_7, source1_5_1_10_1);
        assertNotIntersects(source1_1_5_1_7, source1_5_3_8_8);
        assertNotIntersects(source1_1_5_1_7, source2_1_1_10_1);

        assertNotIntersects(source1_1_5_6_1, source1_0_0_0_0);
        assertNotIntersects(source1_1_5_6_1, source1_1_0_1_0);
        assertIntersects(source1_1_5_6_1, source1_1_1_10_1);
        assertIntersects(source1_1_5_6_1, source1_1_5_1_7);
        assertIntersects(source1_1_5_6_1, source1_1_5_6_1);
        assertIntersects(source1_1_5_6_1, source1_1_8_1_9);
        assertIntersects(source1_1_5_6_1, source1_2_0_2_0);
        assertIntersects(source1_1_5_6_1, source1_2_1_6_1);
        assertIntersects(source1_1_5_6_1, source1_5_0_5_0);
        assertIntersects(source1_1_5_6_1, source1_5_1_10_1);
        assertIntersects(source1_1_5_6_1, source1_5_3_8_8);
        assertNotIntersects(source1_1_5_6_1, source2_1_1_10_1);

        assertNotIntersects(source1_2_1_6_1, source1_0_0_0_0);
        assertNotIntersects(source1_2_1_6_1, source1_1_0_1_0);
        assertIntersects(source1_2_1_6_1, source1_1_1_10_1);
        assertNotIntersects(source1_2_1_6_1, source1_1_5_1_7);
        assertIntersects(source1_2_1_6_1, source1_1_5_6_1);
        assertNotIntersects(source1_2_1_6_1, source1_1_8_1_9);
        assertNotIntersects(source1_2_1_6_1, source1_2_0_2_0);
        assertIntersects(source1_2_1_6_1, source1_2_1_6_1);
        assertIntersects(source1_2_1_6_1, source1_5_0_5_0);
        assertIntersects(source1_2_1_6_1, source1_5_1_10_1);
        assertIntersects(source1_2_1_6_1, source1_5_3_8_8);
        assertNotIntersects(source1_2_1_6_1, source2_1_1_10_1);

        assertNotIntersects(source1_5_1_10_1, source1_0_0_0_0);
        assertNotIntersects(source1_5_1_10_1, source1_1_0_1_0);
        assertIntersects(source1_5_1_10_1, source1_1_1_10_1);
        assertNotIntersects(source1_5_1_10_1, source1_1_5_1_7);
        assertIntersects(source1_5_1_10_1, source1_1_5_6_1);
        assertNotIntersects(source1_5_1_10_1, source1_1_8_1_9);
        assertNotIntersects(source1_5_1_10_1, source1_2_0_2_0);
        assertIntersects(source1_5_1_10_1, source1_2_1_6_1);
        assertNotIntersects(source1_5_1_10_1, source1_5_0_5_0);
        assertIntersects(source1_5_1_10_1, source1_5_1_10_1);
        assertIntersects(source1_5_1_10_1, source1_5_3_8_8);
        assertNotIntersects(source1_5_1_10_1, source2_1_1_10_1);

        assertNotIntersects(source1_5_3_8_8, source1_0_0_0_0);
        assertNotIntersects(source1_5_3_8_8, source1_1_0_1_0);
        assertIntersects(source1_5_3_8_8, source1_1_1_10_1);
        assertNotIntersects(source1_5_3_8_8, source1_1_5_1_7);
        assertIntersects(source1_5_3_8_8, source1_1_5_6_1);
        assertNotIntersects(source1_5_3_8_8, source1_1_8_1_9);
        assertNotIntersects(source1_5_3_8_8, source1_2_0_2_0);
        assertIntersects(source1_5_3_8_8, source1_2_1_6_1);
        assertNotIntersects(source1_5_3_8_8, source1_5_0_5_0);
        assertIntersects(source1_5_3_8_8, source1_5_1_10_1);
        assertIntersects(source1_5_3_8_8, source1_5_3_8_8);
        assertNotIntersects(source1_5_3_8_8, source2_1_1_10_1);

        assertNotIntersects(source2_1_1_10_1, source1_0_0_0_0);
        assertNotIntersects(source2_1_1_10_1, source1_1_0_1_0);
        assertNotIntersects(source2_1_1_10_1, source1_1_1_10_1);
        assertNotIntersects(source2_1_1_10_1, source1_1_5_1_7);
        assertNotIntersects(source2_1_1_10_1, source1_1_5_6_1);
        assertNotIntersects(source2_1_1_10_1, source1_1_8_1_9);
        assertNotIntersects(source2_1_1_10_1, source1_2_0_2_0);
        assertNotIntersects(source2_1_1_10_1, source1_2_1_6_1);
        assertNotIntersects(source2_1_1_10_1, source1_5_0_5_0);
        assertNotIntersects(source2_1_1_10_1, source1_5_1_10_1);
        assertNotIntersects(source2_1_1_10_1, source1_5_3_8_8);
        assertIntersects(source2_1_1_10_1, source2_1_1_10_1);
    }

    @Test
    public void testIsValid()
    {
        String sourceId = "/platform/test/source1.pure";
        Assert.assertTrue(new SourceInformation(sourceId, 1, 1, 1, 1, 1, 1).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 5, 1, 5, 1, 5, 1).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 5, 1, 5, 7, 16, 1).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 5, 1, 5, 7, 16, 3).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 5, 1, 7, 7, 16, 3).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 5, 9, 7, 7, 16, 3).isValid());

        // null source information
        Assert.assertFalse(new SourceInformation(null, 1, 1, 1, 1, 1, 1).isValid());

        // negative line/column
        Assert.assertFalse(new SourceInformation(sourceId, -1, 1, 2, 1, 3, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, -1, 2, 1, 3, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, -2, 1, 3, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 2, -1, 3, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 2, 1, -3, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 2, 1, 3, -1).isValid());

        // 0 line (only valid if all line/col values are 0)
        Assert.assertTrue(new SourceInformation(sourceId, 0, 0, 0, 0, 0, 0).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 0, 0, 0, 0, 0, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 0, 1, 0, 1, 0, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 0, 1, 1, 1, 1, 1).isValid());

        // 0 column (only valid if all lines are equal and all cols are 0)
        Assert.assertTrue(new SourceInformation(sourceId, 1, 0, 1, 0, 1, 0).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 2, 0, 2, 0, 2, 0).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 5, 0, 5, 0, 5, 0).isValid());
        Assert.assertTrue(new SourceInformation(sourceId, 17, 0, 17, 0, 17, 0).isValid());

        Assert.assertFalse(new SourceInformation(sourceId, 1, 0, 1, 0, 2, 0).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 0, 2, 0, 2, 0).isValid());

        Assert.assertFalse(new SourceInformation(sourceId, 1, 0, 1, 1, 1, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 0, 1, 1, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 1, 0, 1, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 1, 1, 0, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 1, 1, 1, 1, 1, 0).isValid());

        // invalid intervals (end before start, etc)
        Assert.assertFalse(new SourceInformation(sourceId, 5, 1, 4, 1, 7, 1).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 5, 1, 5, 3, 5, 2).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 5, 3, 5, 2, 5, 8).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 5, 3, 5, 5, 4, 8).isValid());

        // miscellaneous
        Assert.assertTrue(new SourceInformation(sourceId, 5, 3, 6, 1, 7, 8).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 5, 3, 6, 0, 7, 8).isValid());
        Assert.assertFalse(new SourceInformation(sourceId, 5, 3, 6, 1, 7, 0).isValid());
    }

    private void assertSubsumes(SourceInformation sourceInfo1, SourceInformation sourceInfo2)
    {
        assertSubsumes(true, sourceInfo1, sourceInfo2);
    }

    private void assertNotSubsumes(SourceInformation sourceInfo1, SourceInformation sourceInfo2)
    {
        assertSubsumes(false, sourceInfo1, sourceInfo2);
    }

    private void assertSubsumes(boolean expected, SourceInformation sourceInfo1, SourceInformation sourceInfo2)
    {
        if (sourceInfo1.subsumes(sourceInfo2) != expected)
        {
            StringBuilder builder = sourceInfo1.appendMessage(new StringBuilder("Expected ")).append(" to ");
            if (!expected)
            {
                builder.append("not ");
            }
            Assert.fail(sourceInfo2.appendMessage(builder.append("subsume ")).toString());
        }
    }

    private void assertIntersects(SourceInformation sourceInfo1, SourceInformation sourceInfo2)
    {
        assertIntersects(true, sourceInfo1, sourceInfo2);
    }

    private void assertNotIntersects(SourceInformation sourceInfo1, SourceInformation sourceInfo2)
    {
        assertIntersects(false, sourceInfo1, sourceInfo2);
    }

    private void assertIntersects(boolean expected, SourceInformation sourceInfo1, SourceInformation sourceInfo2)
    {
        if (sourceInfo1.intersects(sourceInfo2) != expected)
        {
            StringBuilder builder = sourceInfo1.appendMessage(new StringBuilder("Expected ")).append(" to ");
            if (!expected)
            {
                builder.append("not ");
            }
            Assert.fail(sourceInfo2.appendMessage(builder.append("intersect ")).toString());
        }
    }
}
