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

package org.finos.legend.pure.lsp.runtime;

import org.junit.Assert;
import org.junit.Test;

public class CompileProgressTrackerTest
{
    @Test
    public void parsesOrderLineForTotalThenCountsFinished()
    {
        CompileProgressTracker tracker = new CompileProgressTracker();
        tracker.onMessage("Compiling repositories in the following order:[platform,core,test]");
        Assert.assertEquals(3, tracker.getTotal());
        Assert.assertEquals(0, tracker.getCompleted());

        tracker.onMessage("Finished compiling platform");
        tracker.onMessage("Finished compiling core");
        Assert.assertEquals(3, tracker.getTotal());
        Assert.assertEquals(2, tracker.getCompleted());
    }

    @Test
    public void emptyOrderListYieldsZeroTotal()
    {
        CompileProgressTracker tracker = new CompileProgressTracker();
        tracker.onMessage("Compiling repositories in the following order:[]");
        Assert.assertEquals(0, tracker.getTotal());
        Assert.assertEquals(0, tracker.getCompleted());
    }

    @Test
    public void nullAndUnrelatedMessagesAreIgnored()
    {
        CompileProgressTracker tracker = new CompileProgressTracker();
        tracker.onMessage(null);
        tracker.onMessage("Loading something else");
        tracker.onMessage("");
        Assert.assertEquals(0, tracker.getTotal());
        Assert.assertEquals(0, tracker.getCompleted());
    }

    @Test
    public void aFreshOrderLineRestartsTheCompletedCount()
    {
        CompileProgressTracker tracker = new CompileProgressTracker();
        tracker.onMessage("Compiling repositories in the following order:[a,b,c]");
        tracker.onMessage("Finished compiling a");
        Assert.assertEquals(1, tracker.getCompleted());

        // A second order line (e.g. a re-init) starts counting again from zero.
        tracker.onMessage("Compiling repositories in the following order:[x,y]");
        Assert.assertEquals(2, tracker.getTotal());
        Assert.assertEquals(0, tracker.getCompleted());
    }

    @Test
    public void resetClearsCounts()
    {
        CompileProgressTracker tracker = new CompileProgressTracker();
        tracker.onMessage("Compiling repositories in the following order:[a,b]");
        tracker.onMessage("Finished compiling a");
        tracker.reset();
        Assert.assertEquals(0, tracker.getTotal());
        Assert.assertEquals(0, tracker.getCompleted());
    }
}
