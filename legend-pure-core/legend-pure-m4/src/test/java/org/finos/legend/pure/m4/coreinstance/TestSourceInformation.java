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
    public void testSubsumes()
    {
        String source1 = "/platform/test/source1.pure";
        String source2 = "/platform/test/source2.pure";

        SourceInformation source1_1_1_10_1 = new SourceInformation(source1, 1, 1, 10, 1);
        SourceInformation source1_1_5_1_7 = new SourceInformation(source1, 1, 5, 1, 7);
        SourceInformation source1_1_5_6_1 = new SourceInformation(source1, 1, 5, 6, 1);
        SourceInformation source1_2_1_6_1 = new SourceInformation(source1, 2, 1, 6, 1);
        SourceInformation source1_5_1_10_1 = new SourceInformation(source1, 5, 1, 10, 1);
        SourceInformation source1_5_3_8_8 = new SourceInformation(source1, 5, 3, 8, 8);
        SourceInformation source2_1_1_10_1 = new SourceInformation(source2, 1, 1, 10, 1);

        assertSubsumes(source1_1_1_10_1, source1_1_1_10_1);
        assertSubsumes(source1_1_1_10_1, source1_1_5_1_7);
        assertSubsumes(source1_1_1_10_1, source1_1_5_6_1);
        assertSubsumes(source1_1_1_10_1, source1_2_1_6_1);
        assertSubsumes(source1_1_1_10_1, source1_5_1_10_1);
        assertSubsumes(source1_1_1_10_1, source1_5_3_8_8);
        assertNotSubsumes(source1_1_1_10_1, source2_1_1_10_1);

        assertNotSubsumes(source1_1_5_1_7, source1_1_1_10_1);
        assertSubsumes(source1_1_5_1_7, source1_1_5_1_7);
        assertNotSubsumes(source1_1_5_1_7, source1_1_5_6_1);
        assertNotSubsumes(source1_1_5_1_7, source1_2_1_6_1);
        assertNotSubsumes(source1_1_5_1_7, source1_5_1_10_1);
        assertNotSubsumes(source1_1_5_1_7, source1_5_3_8_8);
        assertNotSubsumes(source1_1_5_1_7, source2_1_1_10_1);

        assertNotSubsumes(source1_1_5_6_1, source1_1_1_10_1);
        assertSubsumes(source1_1_5_6_1, source1_1_5_1_7);
        assertSubsumes(source1_1_5_6_1, source1_1_5_6_1);
        assertSubsumes(source1_1_5_6_1, source1_2_1_6_1);
        assertNotSubsumes(source1_1_5_6_1, source1_5_1_10_1);
        assertNotSubsumes(source1_1_5_6_1, source1_5_3_8_8);
        assertNotSubsumes(source1_1_5_6_1, source2_1_1_10_1);

        assertNotSubsumes(source1_2_1_6_1, source1_1_1_10_1);
        assertNotSubsumes(source1_2_1_6_1, source1_1_5_1_7);
        assertNotSubsumes(source1_2_1_6_1, source1_1_5_6_1);
        assertSubsumes(source1_2_1_6_1, source1_2_1_6_1);
        assertNotSubsumes(source1_2_1_6_1, source1_5_1_10_1);
        assertNotSubsumes(source1_2_1_6_1, source1_5_3_8_8);
        assertNotSubsumes(source1_2_1_6_1, source2_1_1_10_1);

        assertNotSubsumes(source1_5_1_10_1, source1_1_1_10_1);
        assertNotSubsumes(source1_5_1_10_1, source1_1_5_1_7);
        assertNotSubsumes(source1_5_1_10_1, source1_1_5_6_1);
        assertNotSubsumes(source1_5_1_10_1, source1_2_1_6_1);
        assertSubsumes(source1_5_1_10_1, source1_5_1_10_1);
        assertSubsumes(source1_5_1_10_1, source1_5_3_8_8);
        assertNotSubsumes(source1_5_1_10_1, source2_1_1_10_1);

        assertNotSubsumes(source1_5_3_8_8, source1_1_1_10_1);
        assertNotSubsumes(source1_5_3_8_8, source1_1_5_1_7);
        assertNotSubsumes(source1_5_3_8_8, source1_1_5_6_1);
        assertNotSubsumes(source1_5_3_8_8, source1_2_1_6_1);
        assertNotSubsumes(source1_5_3_8_8, source1_5_1_10_1);
        assertSubsumes(source1_5_3_8_8, source1_5_3_8_8);
        assertNotSubsumes(source1_5_3_8_8, source2_1_1_10_1);

        assertNotSubsumes(source2_1_1_10_1, source1_1_1_10_1);
        assertNotSubsumes(source2_1_1_10_1, source1_1_5_1_7);
        assertNotSubsumes(source2_1_1_10_1, source1_1_5_6_1);
        assertNotSubsumes(source2_1_1_10_1, source1_2_1_6_1);
        assertNotSubsumes(source2_1_1_10_1, source1_5_1_10_1);
        assertNotSubsumes(source2_1_1_10_1, source1_5_3_8_8);
        assertSubsumes(source2_1_1_10_1, source2_1_1_10_1);
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
            StringBuilder builder = new StringBuilder("Expected ");
            sourceInfo1.appendMessage(builder).append(" to ");
            if (!expected)
            {
                builder.append("not ");
            }
            builder.append("contain ");
            sourceInfo2.appendMessage(builder);
            Assert.fail(builder.toString());
        }
    }
}
