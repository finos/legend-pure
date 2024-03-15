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

package org.finos.legend.pure.m4;

import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestPureException
{
    @Test
    public void testGetPureStackSourceInformation()
    {
        Assert.assertArrayEquals(new SourceInformation[]{null}, new BasicPureException().getPureStackSourceInformation());

        SourceInformation sourceInfo1 = new SourceInformation("/test/test1.pure", 1, 2, 3, 4, 5, 6);
        PureException pureException1 = new BasicPureException(sourceInfo1, new RuntimeException("dummy cause"));
        Assert.assertArrayEquals(new SourceInformation[] {sourceInfo1}, pureException1.getPureStackSourceInformation());

        PureException pureException2 = new BasicPureException(pureException1);
        Assert.assertArrayEquals(new SourceInformation[] {sourceInfo1, null}, pureException2.getPureStackSourceInformation());

        SourceInformation sourceInfo2 = new SourceInformation("/test/test2.pure", 7, 8, 9, 10, 11, 12);
        PureException pureException3 = new BasicPureException(sourceInfo2, pureException2);
        Assert.assertArrayEquals(new SourceInformation[] {sourceInfo1, null, sourceInfo2}, pureException3.getPureStackSourceInformation());

        SourceInformation sourceInfo3 = new SourceInformation("/test/test3.pure", 13, 14, 15, 16, 17, 18);
        PureException pureException4 = new BasicPureException(sourceInfo3, new RuntimeException(pureException3));
        Assert.assertArrayEquals(new SourceInformation[] {sourceInfo1, null, sourceInfo2, sourceInfo3}, pureException4.getPureStackSourceInformation());
    }

    @Test
    public void testFindPureException()
    {
        PureException pureException1 = new BasicPureException();
        Assert.assertSame(pureException1, PureException.findPureException(pureException1));

        PureException pureException2 = new BasicPureException(pureException1);
        Assert.assertSame(pureException2, PureException.findPureException(pureException2));

        Assert.assertSame(pureException1, PureException.findPureException(new RuntimeException(pureException1)));
        Assert.assertSame(pureException2, PureException.findPureException(new RuntimeException(pureException2)));

        Assert.assertSame(pureException1, PureException.findPureException(new IOException(new RuntimeException(new RuntimeException(pureException1)))));
        Assert.assertSame(pureException2, PureException.findPureException(new IOException(new RuntimeException(new RuntimeException(pureException2)))));

        Assert.assertNull(PureException.findPureException(new RuntimeException()));
    }

    private static class BasicPureException extends PureException
    {
        private BasicPureException(SourceInformation sourceInfo, String info, Throwable cause)
        {
            super(sourceInfo, info, cause);
        }

        private BasicPureException(SourceInformation sourceInformation, String info)
        {
            super(sourceInformation, info, null);
        }

        private BasicPureException(SourceInformation sourceInformation, Throwable cause)
        {
            super(sourceInformation, null, cause);
        }

        private BasicPureException(String info, Throwable cause)
        {
            super(null, info, cause);
        }

        private BasicPureException(SourceInformation sourceInformation)
        {
            super(sourceInformation, null, null);
        }

        private BasicPureException(String info)
        {
            super(null, info, null);
        }

        private BasicPureException(Throwable cause)
        {
            super(null, null, cause);
        }

        private BasicPureException()
        {
            super(null, null, null);
        }

        @Override
        public String getExceptionName()
        {
            return "basic";
        }
    }
}
