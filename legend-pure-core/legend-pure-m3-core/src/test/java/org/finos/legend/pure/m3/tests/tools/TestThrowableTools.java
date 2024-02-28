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

package org.finos.legend.pure.m3.tests.tools;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.finos.legend.pure.m3.tools.ThrowableTools;
import org.junit.Assert;
import org.junit.Test;

public class TestThrowableTools
{
    @Test
    public void testFindRootThrowable()
    {
        Throwable t0 = new RuntimeException();
        Assert.assertSame(t0, ThrowableTools.findRootThrowable(t0));

        Throwable t1 = new Exception(t0);
        Assert.assertSame(t0, ThrowableTools.findRootThrowable(t1));

        Throwable t2 = new Error(t1);
        Assert.assertSame(t0, ThrowableTools.findRootThrowable(t2));
    }

    @Test
    public void testFindTopThrowableOfClass()
    {
        Throwable t3 = new IllegalArgumentException();
        Throwable t2 = new RuntimeException(t3);
        Throwable t1 = new IOException(t2);
        Throwable t0 = new Error(t1);

        Assert.assertSame(t3, ThrowableTools.findTopThrowableOfClass(t0, IllegalArgumentException.class));
        Assert.assertSame(t2, ThrowableTools.findTopThrowableOfClass(t0, RuntimeException.class));
        Assert.assertSame(t1, ThrowableTools.findTopThrowableOfClass(t0, IOException.class));
        Assert.assertSame(t1, ThrowableTools.findTopThrowableOfClass(t0, Exception.class));
        Assert.assertSame(t0, ThrowableTools.findTopThrowableOfClass(t0, Error.class));
        Assert.assertSame(t0, ThrowableTools.findTopThrowableOfClass(t0, Throwable.class));
        Assert.assertNull(ThrowableTools.findTopThrowableOfClass(t0, FileNotFoundException.class));
    }
}
